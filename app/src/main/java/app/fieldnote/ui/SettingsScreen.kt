package app.fieldnote.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
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
                    PermRow("Usage access", usageOk) { ctx.startActivity(Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    PermRow("Notifications", notifOk) {
                        ctx.startActivity(
                            Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    FnButton("Allowed apps (${s.allowed.size})", { showApps = true }, Modifier.fillMaxWidth(), filled = false)
                    Spacer(Modifier.height(4.dp))
                    Stepper("Emergency exit hold", s.holdSeconds, { v -> app.settings.update { it.copy(holdSeconds = v) } }, 1, 1, 15, " s")
                    Text(
                        "Fieldnote detects and alerts. It does not force-close other apps; Android only allows that through accessibility or device-admin permissions, which this build does not request.",
                        color = FN.Muted, fontSize = 11.sp
                    )
                }
                Panel(title = "About") {
                    Text("Fieldnote 1.0.0 · all data is stored locally on this device.", color = FN.Muted, fontSize = 13.sp)
                }
            }
        })
    }

    if (showAdd) AddSubjectDialog(app) { showAdd = false }
    if (showApps) AllowedAppsDialog(s.allowed, { set -> app.settings.update { it.copy(allowed = set) } }) { showApps = false }
}

@Composable
private fun PermRow(name: String, ok: Boolean, open: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), color = FN.Mint)
        Text(if (ok) "GRANTED" else "NOT GRANTED", color = if (ok) FN.Accent else FN.Muted, fontSize = 11.sp, letterSpacing = 1.sp)
        TextButton(open) { Text("Open", color = FN.Accent) }
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
