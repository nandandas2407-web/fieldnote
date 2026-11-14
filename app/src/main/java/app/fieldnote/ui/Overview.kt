package app.fieldnote.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldnote.Dest
import app.fieldnote.FieldnoteApp
import app.fieldnote.R
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
    val subjects by app.subjects.collectAsState()
    val settings by app.settings.state.collectAsState()
    val timer by app.engine.state.collectAsState()
    val now by rememberNow()
    val today = LocalDate.now()
    val byDay = sessions.focusByDay()
    val live = if (timer.active) timer.focusNow(now) else 0L
    val todayMs = (byDay[today] ?: 0L) + live
    val goalMs = settings.dailyGoalMin * 60_000L
    val todaySessions = sessions.on(today).sortedBy { it.startedAt }
    val maxFocus = maxOf(todaySessions.maxOfOrNull { it.focusMs } ?: 0L, live)
    val streakDays = streak(byDay.toMutableMap().also { if (live > 0) it[today] = (it[today] ?: 0L) + live }, today)
    val pct = (todayMs * 100 / goalMs.coerceAtLeast(1)).toInt()
    val startStr = todaySessions.firstOrNull()?.let { fmtClock(it.startedAt) } ?: if (timer.active) fmtClock(timer.startedAt) else "--:--"
    val endStr = todaySessions.maxOfOrNull { it.endedAt }?.let { fmtClock(it) } ?: "--:--"

    Column(Modifier.verticalScroll(rememberScrollState()).padding(if (wide) 28.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).glass(RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(78.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Welcome back,", color = FN.Muted, fontSize = 13.sp)
                Big(settings.userName.ifBlank { "Student" }, 28.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Label(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()), color = FN.Accent)
                Text("${today.dayOfMonth} ${today.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${today.year}", color = FN.Muted, fontSize = 12.sp)
            }
        }

        // Hero
        Panel {
            val ring: @Composable () -> Unit = {
                GoalRing(todayMs.toFloat() / goalMs.coerceAtLeast(1), if (wide) 210.dp else 176.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Label("Today")
                        Spacer(Modifier.height(2.dp))
                        Mono(fmtHms(todayMs), if (wide) 30.sp else 26.sp)
                        Text("$pct% of ${fmtHm(goalMs)}", color = FN.Muted, fontSize = 11.sp)
                    }
                }
            }
            val side: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Stat("Sessions", "${todaySessions.size + if (timer.active) 1 else 0}", Modifier.weight(1f))
                        Stat("Streak", "$streakDays d", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Stat("Longest", fmtHm(maxFocus), Modifier.weight(1f))
                        Stat("Goal", fmtHm(goalMs), Modifier.weight(1f))
                    }
                    FnButton(
                        if (timer.active) "Open running session" else "Start focus session",
                        { go(Dest.Focus) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.PlayArrow
                    )
                }
            }
            if (wide) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    ring(); side(Modifier.weight(1f))
                }
            } else {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ring() }
                Spacer(Modifier.height(18.dp))
                side(Modifier.fillMaxWidth())
            }
        }

        // Total / Max focus / Start / End
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(Modifier.weight(1f)) { Label("Total study"); Spacer(Modifier.height(6.dp)); Mono(fmtHms(todayMs), 22.sp) }
            Panel(Modifier.weight(1f)) { Label("Max focus"); Spacer(Modifier.height(6.dp)); Mono(fmtHms(maxFocus), 22.sp) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(Modifier.weight(1f)) { Label("Start"); Spacer(Modifier.height(6.dp)); Mono(startStr, 22.sp, color = FN.Accent) }
            Panel(Modifier.weight(1f)) { Label("End"); Spacer(Modifier.height(6.dp)); Mono(endStr, 22.sp, color = FN.Accent) }
        }

        Cols(wide, {
            Panel(title = "By subject") {
                val per = todaySessions.groupBy { it.subjectId }.mapValues { e -> e.value.sumOf { it.focusMs } }
                if (per.isEmpty()) {
                    EmptyState("Nothing logged yet", "Finish a session today to see your subject split.")
                } else {
                    val tot = per.values.sum()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Donut(per.map { (id, v) -> (subjects.firstOrNull { it.id == id }?.let { Color(it.color) } ?: FN.Muted) to v.toFloat() }, fmtHm(tot), size = 140.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            per.entries.sortedByDescending { it.value }.forEach { (id, v) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(9.dp).background(subjects.firstOrNull { it.id == id }?.let { Color(it.color) } ?: FN.Muted, CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Text("${app.subjectName(id)}  ${if (tot > 0) v * 100 / tot else 0}%", fontSize = 12.sp, color = FN.Mint)
                                }
                            }
                        }
                    }
                }
            }
        }, {
            Panel(title = "Weekly study activity") {
                val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
                BarChart(
                    days.map { ((byDay[it] ?: 0L) + if (it == today) live else 0L) / 3_600_000f },
                    days.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3) },
                    highlight = 6
                )
            }
        })

        val dayTodos = todos.filter { it.dueDate == today.toEpochDay() || (!it.done && (it.dueDate ?: today.toEpochDay()) < today.toEpochDay()) }
        Panel(title = "Today's plan", action = { FnButton("Planner", { go(Dest.Planner) }, filled = false, modifier = Modifier.heightIn(min = 36.dp)) }) {
            if (dayTodos.isEmpty()) EmptyState("Nothing planned", "Add tasks in the Planner to see them here.")
            else dayTodos.take(6).forEach { TodoRow(app, it) }
        }

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
        Spacer(Modifier.height(8.dp))
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
