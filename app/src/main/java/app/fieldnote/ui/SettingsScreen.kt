package app.fieldnote.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONObject
import java.time.LocalDate
import kotlinx.coroutines.launch
import app.fieldnote.core.Backup
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.fieldnote.FieldnoteApp
import app.fieldnote.core.Overlay
import app.fieldnote.core.Usage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
private fun rememberResumeTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }
    return tick
}

@Composable
fun SettingsScreen(app: FieldnoteApp) {
    val wide = LocalWide.current
    val ctx = LocalContext.current
    val s by app.settings.state.collectAsState()
    val subjects by app.subjects.collectAsState()
    val tick = rememberResumeTick()
    val usageOk = remember(tick) { Usage.granted(ctx) }
    val notifOk = remember(tick) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    var showApps by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(if (wide) 28.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenTitle("Preferences", "Settings")
        Cols(wide, {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Panel(title = "Goals and timer") {
                    FnField(s.userName, { v -> app.settings.update { it.copy(userName = v) } }, "Your name")
                    Spacer(Modifier.height(8.dp))
                    Stepper("Daily goal", s.dailyGoalMin / 60, { v -> app.settings.update { it.copy(dailyGoalMin = v * 60) } }, 1, 1, 16, " h")
                    Stepper("Pomodoro work", s.workMin, { v -> app.settings.update { it.copy(workMin = v) } }, 5, 5, 90, " min")
                    Stepper("Pomodoro break", s.breakMin, { v -> app.settings.update { it.copy(breakMin = v) } }, 1, 1, 30, " min")
                }
                Panel(title = "Subjects", action = { TextButton({ showAdd = true }) { Text("Add", color = FN.Accent) } }) {
                    subjects.forEach { sub ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).then(Modifier)) { androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawRect(Color(sub.color)) } }
                            Spacer(Modifier.width(12.dp))
                            Text(sub.name, Modifier.weight(1f), color = FN.Mint)
                            IconButton({ app.io { app.db.subjectDao().archive(sub.id) } }) { Icon(Icons.Outlined.Delete, "Archive ${sub.name}", tint = FN.Muted) }
                        }
                    }
                    Text("Archived subjects keep their recorded sessions.", color = FN.Muted, fontSize = 11.sp)
                }
            }
        }, {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Panel(title = "Focus protection") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Alert on distracting apps", color = FN.Mint)
                            Text("While a session runs, opening an app outside the allowed list raises an alert and counts as a distraction.", color = FN.Muted, fontSize = 12.sp)
                        }
                        FnSwitch(s.blockerEnabled) { v -> app.settings.update { it.copy(blockerEnabled = v) } }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Floating pop-up", color = FN.Mint)
                            Text("Shows a small card over the blocked app with a button back to your session.", color = FN.Muted, fontSize = 12.sp)
                        }
                        FnSwitch(s.overlayEnabled) { v -> app.settings.update { it.copy(overlayEnabled = v) } }
                    }
                    Spacer(Modifier.height(8.dp))
                    FnButton("Allowed apps (${s.allowed.size})", { showApps = true }, Modifier.fillMaxWidth(), filled = false)
                    Spacer(Modifier.height(4.dp))
                    Stepper("Emergency exit hold", s.holdSeconds, { v -> app.settings.update { it.copy(holdSeconds = v) } }, 1, 1, 15, " s")
                    Text(
                        "When you open an app that is not allowed, a card appears over it with a button back to your session. Fieldnote never force-closes other apps.",
                        color = FN.Muted, fontSize = 11.sp
                    )
                }
                PermissionChecklist(tick, usageOk, notifOk)
                BackupPanel(app)
                Panel(title = "About") {
                    Text("Fieldnote 1.0.0 · all data is stored locally on this device.", color = FN.Muted, fontSize = 13.sp)
                }
            }
        })
    }

    if (showAdd) AddSubjectDialog(app) { showAdd = false }
    if (showApps) AllowedAppsDialog(s.allowed, { set -> app.settings.update { it.copy(allowed = set) } }) { showApps = false }
}

private class Perm(val name: String, val why: String, val ok: Boolean, val required: Boolean, val open: () -> Unit)

@Composable
private fun PermissionChecklist(tick: Int, usageOk: Boolean, notifOk: Boolean) {
    val ctx = LocalContext.current
    val overlayOk = remember(tick) { Overlay.canDraw(ctx) }
    val batteryOk = remember(tick) {
        (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
    }
    fun launch(i: Intent) { try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { /* screen missing on this device */ } }
    val items = listOf(
        Perm("Usage access", "Lets Fieldnote see which app is in front.", usageOk, true) { launch(Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS)) },
        Perm("Display over other apps", "Needed to show the pop-up on top of blocked apps.", overlayOk, true) {
            launch(Intent(AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
        },
        Perm("Notifications", "Shows the running timer and fallback alerts.", notifOk, true) {
            launch(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName))
        },
        Perm("Unrestricted battery", "Keeps the timer and pop-up alive when the screen is off.", batteryOk, false) {
            launch(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    )
    val done = items.count { it.ok }
    Panel(title = "Permissions checklist") {
        Text(if (done == items.size) "Everything is ready." else "$done of ${items.size} ready", color = if (done == items.size) FN.Accent else FN.Muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Progress(done / items.size.toFloat())
        Spacer(Modifier.height(8.dp))
        items.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (p.ok) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                    tint = if (p.ok) FN.Accent else FN.Faint, modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name + if (!p.required) " (recommended)" else "", color = FN.Mint, fontSize = 14.sp)
                    Text(p.why, color = FN.Muted, fontSize = 12.sp)
                }
                if (!p.ok) TextButton(p.open) { Text("Grant", color = FN.Accent) }
            }
        }
    }
}

@Composable
private fun AllowedAppsDialog(initial: Set<String>, onSave: (Set<String>) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    var chosen by remember { mutableStateOf(initial) }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
                .distinctBy { it.first }.filter { it.first != ctx.packageName }
                .sortedBy { it.second.lowercase() }
        }
    }
    AlertDialog(
        onDismissRequest = onClose, containerColor = FN.Panel,
        title = { Text("Allowed apps") },
        text = {
            val list = apps
            if (list == null) Text("Loading…", color = FN.Muted)
            else if (list.isEmpty()) Text("No launchable apps found.", color = FN.Muted)
            else LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(list, key = { it.first }) { (pkg, label) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FnCheck(pkg in chosen) { on -> chosen = if (on) chosen + pkg else chosen - pkg }
                        Text(label, color = FN.Mint, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton({ onSave(chosen); onClose() }) { Text("Save", color = FN.Accent) } },
        dismissButton = { TextButton(onClose) { Text("Cancel", color = FN.Muted) } }
    )
}

@Composable
private fun BackupPanel(app: FieldnoteApp) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val timer by app.engine.state.collectAsState()
    var status by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<JSONObject?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            status = try {
                val text = Backup.export(app)
                withContext(Dispatchers.IO) {
                    (ctx.contentResolver.openOutputStream(uri, "wt") ?: error("could not open the file")).use {
                        it.write(text.toByteArray(Charsets.UTF_8))
                    }
                }
                "Exported your full history to the file you chose."
            } catch (e: Exception) { "Export failed: ${e.message}" }
            busy = false
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val text = withContext(Dispatchers.IO) {
                    (ctx.contentResolver.openInputStream(uri) ?: error("could not read the file")).use { it.readBytes().toString(Charsets.UTF_8) }
                }
                pending = withContext(Dispatchers.Default) { Backup.parse(text) }
                status = null
            } catch (e: Exception) { status = "That file can't be imported: ${e.message}" }
            busy = false
        }
    }

    fun runImport(root: JSONObject, replace: Boolean) {
        pending = null; confirmReplace = false
        scope.launch {
            busy = true
            status = try {
                val r = withContext(Dispatchers.Default) { Backup.import(app, root, replace) }
                "Imported ${r.sessions} sessions, ${r.todos} tasks, ${r.exams} exams, ${r.notes} notes and ${r.subjects} new subjects." +
                    if (r.skipped > 0) " ${r.skipped} duplicates or invalid entries were skipped." else ""
            } catch (e: Exception) { "Import failed: ${e.message}" }
            busy = false
        }
    }

    Panel(title = "Backup and restore") {
        Text(
            "Export your whole history (sessions, subjects, tasks, exams, notes and preferences) to a JSON file, or import one to restore it.",
            color = FN.Muted, fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FnButton(
                "Export", { exportLauncher.launch("fieldnote-backup-${LocalDate.now()}.json") },
                Modifier.weight(1f), icon = Icons.Outlined.FileDownload, enabled = !busy
            )
            FnButton(
                "Import", { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                Modifier.weight(1f), filled = false, icon = Icons.Outlined.FileUpload, enabled = !busy && !timer.active
            )
        }
        if (timer.active) {
            Spacer(Modifier.height(8.dp))
            Text("Finish the running session before importing.", color = FN.Muted, fontSize = 12.sp)
        }
        status?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = FN.Accent, fontSize = 13.sp)
        }
    }

    val root = pending
    if (root != null) {
        val sm = Backup.summary(root)
        val whenText = if (sm.exportedAt > 0) " Exported ${java.time.Instant.ofEpochMilli(sm.exportedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}." else ""
        if (!confirmReplace) {
            AlertDialog(
                onDismissRequest = { pending = null }, containerColor = FN.Panel,
                title = { Text("Import backup") },
                text = {
                    Text(
                        "This file has ${sm.sessions} sessions, ${sm.todos} tasks, ${sm.exams} exams, ${sm.notes} notes and ${sm.subjects} subjects.$whenText\n\n" +
                            "Merge adds what is missing and keeps your current data.\n" +
                            "Replace erases the current data and restores the file, including preferences.",
                        color = FN.Muted, fontSize = 14.sp
                    )
                },
                confirmButton = { TextButton({ runImport(root, false) }) { Text("Merge", color = FN.Accent) } },
                dismissButton = {
                    Row {
                        TextButton({ confirmReplace = true }) { Text("Replace…", color = FN.Muted) }
                        TextButton({ pending = null }) { Text("Cancel", color = FN.Muted) }
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { confirmReplace = false }, containerColor = FN.Panel,
                title = { Text("Erase and restore?") },
                text = { Text("All sessions, tasks, exams, notes and subjects on this device will be deleted and replaced by the file. This cannot be undone.", color = FN.Muted, fontSize = 14.sp) },
                confirmButton = { TextButton({ runImport(root, true) }) { Text("Replace everything", color = FN.Accent) } },
                dismissButton = { TextButton({ confirmReplace = false }) { Text("Back", color = FN.Muted) } }
            )
        }
    }
}
