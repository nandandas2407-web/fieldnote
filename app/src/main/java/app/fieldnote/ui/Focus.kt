package app.fieldnote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldnote.FieldnoteApp
import app.fieldnote.core.*
import app.fieldnote.data.StudySession
import app.fieldnote.data.Subject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(app: FieldnoteApp) {
    val wide = LocalWide.current
    val t by app.engine.state.collectAsState()
    val subjects by app.subjects.collectAsState()
    val settings by app.settings.state.collectAsState()
    val sessions by app.sessions.collectAsState()
    val now by rememberNow()
    var picked by rememberSaveable { mutableLongStateOf(-1L) }
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var showFinish by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    val effId = if (subjects.any { it.id == picked }) picked else subjects.firstOrNull()?.id ?: -1L
    val scope = rememberCoroutineScope()

    Column(Modifier.verticalScroll(rememberScrollState()).padding(if (wide) 28.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenTitle("Stopwatch · Pomodoro", "Focus")

        if (!t.active) {
            Cols(wide, {
                Panel(title = "New session") {
                    Label("Subject")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        subjects.forEach { s -> Tag(s.name, s.id == effId, Color(s.color)) { picked = s.id } }
                        Tag("+ Add", false) { showAdd = true }
                    }
                    Spacer(Modifier.height(16.dp))
                    Seg(listOf("Stopwatch", "Pomodoro"), mode, { mode = it })
                    if (mode == 1) {
                        Spacer(Modifier.height(8.dp))
                        Stepper("Work interval", settings.workMin, { v -> app.settings.update { it.copy(workMin = v) } }, 5, 5, 90, " min")
                        Stepper("Break", settings.breakMin, { v -> app.settings.update { it.copy(breakMin = v) } }, 1, 1, 30, " min")
                    }
                    Spacer(Modifier.height(16.dp))
                    FnButton(
                        "Start", { app.engine.start(effId, mode == 1, settings.workMin, settings.breakMin) },
                        Modifier.fillMaxWidth(), icon = Icons.Outlined.PlayArrow, enabled = effId != -1L
                    )
                    if (settings.blockerEnabled) {
                        Spacer(Modifier.height(10.dp))
                        Text("Focus protection is on. Apps outside your allowed list will trigger an alert.", color = FN.Muted, fontSize = 12.sp)
                    }
                }
            }, {
                RecentSessions(app, sessions.take(8))
            })
        } else {
            val focus = t.focusNow(now)
            val sName = app.subjectName(t.subjectId)
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Label(if (t.running) "Focusing · $sName" else "On break · $sName", color = if (t.running) FN.Accent else FN.Muted, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (t.pomodoro) {
                        val interval = t.workMin * 60_000L
                        val inInterval = focus % interval
                        val frac = inInterval.toFloat() / interval
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(if (wide) 280.dp else 240.dp)) {
                                val sw = 10.dp.toPx()
                                drawArc(FN.Mint.copy(alpha = 0.1f), 0f, 360f, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw))
                                drawArc(FN.Accent, -90f, 360f * frac, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Butt))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Mono(fmtMs(interval - inInterval), 48.sp)
                                Text("Total ${fmtHms(focus)}", color = FN.Muted, fontSize = 12.sp)
                                Text("Interval ${focus / interval + 1}", color = FN.Muted, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Mono(fmtHms(focus), if (wide) 88.sp else 56.sp)
                    }
                }
                if (t.pomodoro && focus > 0 && focus % (t.workMin * 60_000L) < 8000 && t.running) {
                    Spacer(Modifier.height(8.dp))
                    Text("Interval complete. Pause for a ${t.breakMin} minute break.", color = FN.Accent, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (t.running) FnButton("Pause", { app.engine.pause() }, Modifier.weight(1f), icon = Icons.Outlined.Pause)
                    else FnButton("Resume", { app.engine.resume() }, Modifier.weight(1f), icon = Icons.Outlined.PlayArrow)
                    FnButton("Finish", { showFinish = true }, Modifier.weight(1f), filled = false, icon = Icons.Outlined.Stop)
                }
            }
            Cols(wide, {
                Panel(title = "This session") {
                    Row(Modifier.fillMaxWidth()) {
                        Stat("Break time", fmtHm(t.breakNow(now)), Modifier.weight(1f))
                        Stat("Pauses", "${t.pauses}", Modifier.weight(1f))
                        Stat("Distractions", "${t.distractions}", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Label("Switch subject")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        subjects.forEach { s -> Tag(s.name, s.id == t.subjectId, Color(s.color)) { app.engine.switchSubject(s.id) } }
                    }
                }
            }, {
                if (settings.blockerEnabled) {
                    Panel(title = "Focus protection") {
                        if (t.protectionOff) {
                            Text("Suspended for this session.", color = FN.Muted)
                        } else {
                            Text("Active. Leaving to an app that is not allowed raises an alert and is counted as a distraction.", color = FN.Muted, fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            HoldButton("Hold ${settings.holdSeconds}s for emergency exit", settings.holdSeconds * 1000L) { app.engine.suspendProtection() }
                        }
                    }
                } else {
                    Panel(title = "Focus protection") {
                        Text("Off. Enable it in Settings to be alerted when you open apps outside your allowed list.", color = FN.Muted, fontSize = 13.sp)
                    }
                }
            })
        }
    }

    if (showFinish) {
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFinish = false },
            containerColor = FN.Panel,
            title = { Text("Finish session") },
            text = { FnField(note, { note = it }, "Session note (optional)", singleLine = false, minLines = 2) },
            confirmButton = { TextButton({ app.engine.finish(note); showFinish = false }) { Text("Save", color = FN.Accent) } },
            dismissButton = {
                Row {
                    TextButton({ app.engine.discard(); showFinish = false }) { Text("Discard", color = FN.Muted) }
                    TextButton({ showFinish = false }) { Text("Cancel", color = FN.Muted) }
                }
            }
        )
    }
    if (showAdd) AddSubjectDialog(app) { showAdd = false }
}

@Composable
fun HoldButton(text: String, holdMs: Long, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(FN.Shape).background(FN.Mint.copy(alpha = 0.06f))
            .pointerInput(holdMs) {
                detectTapGestures(onPress = {
                    val job = scope.launch {
                        val steps = 20
                        for (i in 1..steps) { delay(holdMs / steps); progress = i / steps.toFloat() }
                        onDone()
                    }
                    tryAwaitRelease()
                    job.cancel()
                    progress = 0f
                })
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.matchParentSize()) { Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(FN.Accent.copy(alpha = 0.35f))) }
        Text(text.uppercase(), Modifier.padding(16.dp), fontSize = 12.sp, letterSpacing = 1.sp, color = FN.Mint)
    }
}

@Composable
fun RecentSessions(app: FieldnoteApp, list: List<StudySession>) {
    Panel(title = "Recent sessions") {
        if (list.isEmpty()) {
            EmptyState("No sessions yet", "Finished sessions appear here and feed every chart in Analytics.")
        } else {
            list.forEach { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val c = app.subjects.value.firstOrNull { it.id == s.subjectId }?.color?.let { Color(it) } ?: FN.Muted
                    Box(Modifier.width(3.dp).height(36.dp).background(c))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.subjectName(s.subjectId), color = FN.Mint)
                        Text("${s.startedAt.toDate()} · ${fmtClock(s.startedAt)}–${fmtClock(s.endedAt)}", color = FN.Muted, fontSize = 12.sp)
                    }
                    Mono(fmtHms(s.focusMs), 15.sp)
                    IconButton({ app.io { app.db.sessionDao().delete(s.id) } }) { Icon(Icons.Outlined.Delete, "Delete session", tint = FN.Muted) }
                }
            }
        }
    }
}

@Composable
fun AddSubjectDialog(app: FieldnoteApp, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onClose, containerColor = FN.Panel,
        title = { Text("New subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FnField(name, { name = it }, "Name")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FN.SubjectColors.forEachIndexed { i, c ->
                        Box(
                            Modifier.size(28.dp).background(Color(c))
                                .then(if (i == color) Modifier.border(2.dp, FN.Mint) else Modifier)
                                .pointerInput(i) { detectTapGestures { color = i } }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) app.io { app.db.subjectDao().insert(Subject(name = name.trim(), color = Color(FN.SubjectColors[color]).toArgb())) }
                onClose()
            }) { Text("Add", color = FN.Accent) }
        },
        dismissButton = { TextButton(onClose) { Text("Cancel", color = FN.Muted) } }
    )
}
