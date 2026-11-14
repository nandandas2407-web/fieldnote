package app.fieldnote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldnote.FieldnoteApp
import app.fieldnote.core.*
import app.fieldnote.data.Exam
import app.fieldnote.data.Todo
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun TodoRow(app: FieldnoteApp, t: Todo) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FnCheck(t.done) { app.toggleTodo(t) }
        Column(Modifier.weight(1f)) {
            Text(
                t.title, color = if (t.done) FN.Faint else FN.Mint,
                textDecoration = if (t.done) TextDecoration.LineThrough else null
            )
            val meta = buildList {
                add(listOf("Low", "Medium", "High")[t.priority.coerceIn(0, 2)])
                if (t.plannedMin > 0) add("${t.plannedMin} min planned")
                if (t.repeatDaily) add("Repeats daily")
            }.joinToString(" · ")
            Text(meta, color = FN.Muted, fontSize = 11.sp)
        }
        IconButton({ app.io { app.db.todoDao().delete(t.id) } }) { Icon(Icons.Outlined.Delete, "Delete task", tint = FN.Muted) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(app: FieldnoteApp) {
    val wide = LocalWide.current
    val todos by app.todos.collectAsState()
    val sessions by app.sessions.collectAsState()
    val exams by app.exams.collectAsState()
    val today = LocalDate.now()
    var view by rememberSaveable { mutableIntStateOf(0) }
    var selEpoch by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var monthEpoch by rememberSaveable { mutableLongStateOf(today.withDayOfMonth(1).toEpochDay()) }
    val sel = LocalDate.ofEpochDay(selEpoch)
    val byDay = sessions.focusByDay()

    var title by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableIntStateOf(1) }
    var planned by rememberSaveable { mutableIntStateOf(0) }
    var repeat by rememberSaveable { mutableStateOf(false) }
    var showExam by remember { mutableStateOf(false) }

    val dayTodos = todos.filter {
        it.dueDate == selEpoch || (sel == today && !it.done && (it.dueDate ?: selEpoch) < selEpoch)
    }
    val plannedMs = dayTodos.sumOf { it.plannedMin } * 60_000L
    val studiedMs = byDay[sel] ?: 0L

    Column(Modifier.verticalScroll(rememberScrollState()).padding(if (wide) 28.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenTitle("Daily · Weekly · Monthly", "Planner")
        Seg(listOf("Day", "Week", "Month"), view, { view = it }, Modifier.widthIn(max = 420.dp))

        when (view) {
            0 -> Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ selEpoch -= 1 }) { Icon(Icons.Outlined.ChevronLeft, "Previous day") }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Big(sel.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()), 22.sp)
                        Text("${sel.dayOfMonth} ${sel.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${sel.year}", color = FN.Muted, fontSize = 12.sp)
                    }
                    IconButton({ selEpoch += 1 }) { Icon(Icons.Outlined.ChevronRight, "Next day") }
                }
            }
            1 -> Panel(title = "Week") {
                val mon = mondayOf(sel)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0..6) {
                        val d = mon.plusDays(i.toLong())
                        val ts = todos.filter { it.dueDate == d.toEpochDay() }
                        val isSel = d == sel
                        Column(
                            Modifier.weight(1f).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(if (isSel) FN.AccentBrush else androidx.compose.ui.graphics.SolidColor(FN.Mint.copy(alpha = 0.07f)))
                                .clickable { selEpoch = d.toEpochDay() }.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val c = if (isSel) FN.Bg else FN.Mint
                            Text(d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3), fontSize = 10.sp, color = if (isSel) FN.Bg else FN.Muted)
                            Text("${d.dayOfMonth}", fontSize = 18.sp, color = c)
                            Text("${ts.count { it.done }}/${ts.size}", fontSize = 10.sp, color = c)
                            Text(fmtHmShort(byDay[d] ?: 0L), fontSize = 10.sp, color = c)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    TextButton({ selEpoch -= 7 }) { Text("Previous week", color = FN.Muted) }
                    Spacer(Modifier.weight(1f))
                    TextButton({ selEpoch += 7 }) { Text("Next week", color = FN.Muted) }
                }
            }
            else -> Panel(title = "Month") {
                val ym = YearMonth.from(LocalDate.ofEpochDay(monthEpoch))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ monthEpoch = ym.minusMonths(1).atDay(1).toEpochDay() }) { Icon(Icons.Outlined.ChevronLeft, "Previous month") }
                    Text("${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}", Modifier.weight(1f), color = FN.Mint, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton({ monthEpoch = ym.plusMonths(1).atDay(1).toEpochDay() }) { Icon(Icons.Outlined.ChevronRight, "Next month") }
                }
                MonthHeat(ym, byDay, sel) { selEpoch = it.toEpochDay() }
            }
        }

        val addTask = {
            if (title.isNotBlank()) {
                val t = Todo(title = title.trim(), dueDate = selEpoch, priority = priority, repeatDaily = repeat, plannedMin = planned)
                app.io { app.db.todoDao().insert(t) }
                title = ""; planned = 0; repeat = false
            }
        }
        Cols(wide, {
            Panel(title = "Tasks · ${sel.dayOfMonth} ${sel.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}") {
                // Adding a task lives right where the list is, so one job stays in one place.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FnField(title, { title = it }, "Add a task", Modifier.weight(1f))
                    FnButton("Add", { addTask() }, icon = Icons.Outlined.Add, enabled = title.isNotBlank())
                }
                Spacer(Modifier.height(10.dp))
                Seg(listOf("Low", "Medium", "High"), priority, { priority = it }, Modifier.widthIn(max = 360.dp))
                Stepper("Planned study time", planned, { planned = it }, 15, 0, 480, " min")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FnCheck(repeat) { repeat = it }
                    Text("Repeat daily", color = FN.Mint)
                }
                Spacer(Modifier.height(12.dp))
                if (plannedMs > 0 || studiedMs > 0) {
                    Text("Planned ${fmtHm(plannedMs)} · Studied ${fmtHm(studiedMs)}", color = FN.Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Progress(if (plannedMs > 0) studiedMs.toFloat() / plannedMs else 1f)
                    Spacer(Modifier.height(10.dp))
                }
                if (dayTodos.isEmpty()) EmptyState("No tasks", "Add a task for this day.")
                else dayTodos.forEach { TodoRow(app, it) }
                Spacer(Modifier.height(10.dp))
                FnButton(
                    "Move unfinished to tomorrow",
                    { app.io { app.db.todoDao().moveUnfinished(selEpoch, selEpoch + 1) } },
                    Modifier.fillMaxWidth(), filled = false, icon = Icons.Outlined.ArrowForward
                )
            }
        }, {
                Panel(title = "D-Day countdowns", action = { IconButton({ showExam = true }) { Icon(Icons.Outlined.Add, "Add exam", tint = FN.Accent) } }) {
                    if (exams.isEmpty()) EmptyState("No exams", "Add a date to count down to.")
                    exams.forEach { e ->
                        val d = ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(e.date))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.name, color = FN.Mint)
                                Text(LocalDate.ofEpochDay(e.date).toString(), color = FN.Muted, fontSize = 11.sp)
                            }
                            Mono(if (d > 0) "D-$d" else if (d == 0L) "TODAY" else "D+${-d}", 18.sp, color = FN.Accent)
                            IconButton({ app.io { app.db.examDao().delete(e.id) } }) { Icon(Icons.Outlined.Delete, "Delete exam", tint = FN.Muted) }
                        }
                    }
                }
        }, 1.5f, 1f)
    }

    if (showExam) {
        var name by remember { mutableStateOf("") }
        val ds = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showExam = false },
            confirmButton = {
                TextButton({
                    val ms = ds.selectedDateMillis
                    if (ms != null && name.isNotBlank()) {
                        val day = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        app.io { app.db.examDao().insert(Exam(name = name.trim(), date = day)) }
                        showExam = false
                    }
                }) { Text("Add", color = FN.Accent) }
            },
            dismissButton = { TextButton({ showExam = false }) { Text("Cancel", color = FN.Muted) } },
            colors = DatePickerDefaults.colors(containerColor = FN.Panel)
        ) {
            Column(Modifier.padding(16.dp)) { FnField(name, { name = it }, "Exam name") }
            DatePicker(ds)
        }
    }
}

