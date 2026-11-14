package app.fieldnote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldnote.FieldnoteApp
import app.fieldnote.data.Note
import java.time.Instant
import java.time.ZoneId

// Strokes are stored as fractions of canvas width: "x,y x,y;x,y x,y"
private fun encode(strokes: List<List<Offset>>): String =
    strokes.joinToString(";") { s -> s.joinToString(" ") { "%.4f,%.4f".format(it.x, it.y) } }

private fun decode(s: String): List<List<Offset>> =
    if (s.isBlank()) emptyList() else s.split(";").map { st ->
        st.split(" ").mapNotNull { p ->
            val a = p.split(",")
            if (a.size == 2) { val x = a[0].toFloatOrNull(); val y = a[1].toFloatOrNull(); if (x != null && y != null) Offset(x, y) else null } else null
        }
    }

@Composable
fun LibraryScreen(app: FieldnoteApp) {
    val wide = LocalWide.current
    val notes by app.notes.collectAsState()
    var editing by remember { mutableStateOf<Note?>(null) }
    var creating by remember { mutableStateOf(false) }

    if (editing != null || creating) {
        NoteEditor(app, editing) { editing = null; creating = false }
        return
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(if (wide) 28.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenTitle("Notes · Sketches", "Library", trailing = { FnButton("New note", { creating = true }, icon = Icons.Outlined.Add) })
        if (notes.isEmpty()) {
            Panel { EmptyState("No notes yet", "Write notes or sketch with a stylus or finger. Everything stays on this device.") }
        } else {
            notes.forEach { n ->
                Panel(Modifier.clickable { editing = n }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Big(n.title.ifBlank { "Untitled" }, 20.sp)
                            if (n.body.isNotBlank()) Text(n.body.take(140), color = FN.Muted, fontSize = 13.sp, maxLines = 2)
                            val d = Instant.ofEpochMilli(n.updatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                            Text("$d" + if (n.strokes.isNotBlank()) " · has sketch" else "", color = FN.Faint, fontSize = 11.sp)
                        }
                        IconButton({ app.io { app.db.noteDao().delete(n.id) } }) { Icon(Icons.Outlined.Delete, "Delete note", tint = FN.Muted) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditor(app: FieldnoteApp, note: Note?, onClose: () -> Unit) {
    val wide = LocalWide.current
    var title by remember { mutableStateOf(note?.title ?: "") }
    var body by remember { mutableStateOf(note?.body ?: "") }
    val strokes: SnapshotStateList<List<Offset>> = remember { mutableStateListOf<List<Offset>>().also { it.addAll(decode(note?.strokes ?: "")) } }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(if (wide) 28.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClose) { Icon(Icons.Outlined.ArrowBack, "Back") }
            Big(if (note == null) "New note" else "Edit note", 24.sp, Modifier.weight(1f))
            FnButton("Save", {
                val n = Note(id = note?.id ?: 0, title = title.trim(), body = body, strokes = encode(strokes), updatedAt = System.currentTimeMillis())
                app.io { app.db.noteDao().upsert(n) }
                onClose()
            }, icon = Icons.Outlined.Check)
        }
        FnField(title, { title = it }, "Title")
        FnField(body, { body = it }, "Notes", singleLine = false, minLines = 5)
        Panel(title = "Sketch pad", action = {
            TextButton({ if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }) { Text("Undo", color = FN.Accent) }
            TextButton({ strokes.clear() }) { Text("Clear", color = FN.Muted) }
        }) {
            var width by remember { mutableFloatStateOf(1f) }
            Canvas(
                Modifier.fillMaxWidth().height(320.dp).background(FN.Bg).border(1.dp, FN.Border)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { p -> current = listOf(Offset(p.x / size.width, p.y / size.width)) },
                            onDragEnd = { if (current.isNotEmpty()) strokes.add(current); current = emptyList() },
                            onDragCancel = { current = emptyList() }
                        ) { change, _ ->
                            change.consume()
                            current = current + Offset(change.position.x / size.width, change.position.y / size.width)
                        }
                    }
            ) {
                width = size.width
                fun draw(s: List<Offset>) {
                    if (s.isEmpty()) return
                    if (s.size == 1) { drawCircle(FN.Mint, 1.5.dp.toPx(), Offset(s[0].x * size.width, s[0].y * size.width)); return }
                    val path = Path()
                    path.moveTo(s[0].x * size.width, s[0].y * size.width)
                    for (i in 1 until s.size) path.lineTo(s[i].x * size.width, s[i].y * size.width)
                    drawPath(path, FN.Mint, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                strokes.forEach { draw(it) }
                draw(current)
            }
            Spacer(Modifier.height(6.dp))
            Text("Draw with a stylus or your finger.", color = FN.Muted, fontSize = 12.sp)
        }
    }
}
