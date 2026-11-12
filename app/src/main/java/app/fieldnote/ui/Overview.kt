package app.fieldnote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldnote.Dest
import app.fieldnote.FieldnoteApp
import app.fieldnote.core.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun OverviewScreen(app: FieldnoteApp, go: (Dest) -> Unit) {
    val wide = LocalWide.current
    val sessions by app.sessions.collectAsState()
    val todos by app.todos.collectAsState()
    val exams by app.exams.collectAsState()
    val settings by app.settings.state.collectAsState()
    val timer by app.engine.state.collectAsState()
    val now by rememberNow()
    val today = LocalDate.now()
    val byDay = sessions.focusByDay()
    val live = if (timer.active) timer.focusNow(now) else 0L
    val todayMs = (byDay[today] ?: 0L) + live
    val goalMs = settings.dailyGoalMin * 60_000L
    val todaySessions = sessions.on(today)
    val maxFocus = maxOf(todaySessions.maxOfOrNull { it.focusMs } ?: 0L, live)
    val streakDays = streak(byDay.toMutableMap().also { if (live > 0) it[today] = (it[today] ?: 0L) + live }, today)

    Column(Modifier.verticalScroll(rememberScrollState()).padding(if (wide) 28.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenTitle(
            today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase() + " · " + today.dayOfMonth + " " + today.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
            "Overview"
        )
        Cols(wide, {
            Panel(title = "Total focus today") {
                Mono(fmtHms(todayMs), if (wide) 64.sp else 48.sp)
                Spacer(Modifier.height(10.dp))
                Progress(todayMs.toFloat() / goalMs.coerceAtLeast(1))
                Spacer(Modifier.height(8.dp))
                Text(
                    "${(todayMs * 100 / goalMs.coerceAtLeast(1))}% of ${fmtHm(goalMs)} daily goal",
                    color = FN.Muted, fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
                FnButton(
                    if (timer.active) "Open running session" else "Start focus session",
                    { go(Dest.Focus) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.PlayArrow
                )
            }
        }, {
            Panel(title = "Today") {
                Row(Modifier.fillMaxWidth()) {
                    Stat("Sessions", "${todaySessions.size + if (timer.active) 1 else 0}", Modifier.weight(1f))
                    Stat("Streak", "$streakDays d", Modifier.weight(1f))
                    Stat("Longest", fmtHm(maxFocus), Modifier.weight(1f))
                }
            }
        }, 1.4f, 1f)

        Cols(wide, {
            Panel(title = "Weekly study activity") {
                val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
                BarChart(
                    days.map { ((byDay[it] ?: 0L) + if (it == today) live else 0L) / 3_600_000f },
                    days.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3) },
                    highlight = 6
                )
            }
        }, {
            val dayTodos = todos.filter { it.dueDate == today.toEpochDay() || (!it.done && (it.dueDate ?: today.toEpochDay()) < today.toEpochDay()) }
            Panel(title = "Today's plan", action = { FnButton("Planner", { go(Dest.Planner) }, filled = false) }) {
                if (dayTodos.isEmpty()) {
                    EmptyState("Nothing planned", "Add tasks in the Planner to see them here.")
                } else {
                    dayTodos.take(6).forEach { TodoRow(app, it) }
                }
            }
        }, 1.3f, 1f)

        if (exams.isNotEmpty()) {
            Panel(title = "D-Day") {
                exams.take(3).forEach { e ->
                    val d = ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(e.date))
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(e.name, Modifier.weight(1f), color = FN.Mint)
                        Mono(if (d > 0) "D-$d" else if (d == 0L) "TODAY" else "D+${-d}", 20.sp, color = FN.Accent)
                    }
                }
            }
        }
    }
}

@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Label(label)
        Spacer(Modifier.height(4.dp))
        Big(value, 26.sp)
    }
}
