package app.fieldnote.ui

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldnote.FieldnoteApp
import app.fieldnote.core.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AnalyticsScreen(app: FieldnoteApp) {
    val wide = LocalWide.current
    val ctx = LocalContext.current
    val sessions by app.sessions.collectAsState()
    val subjects by app.subjects.collectAsState()
    val today = LocalDate.now()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selEpoch by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val sel = LocalDate.ofEpochDay(selEpoch)
    val byDay = sessions.focusByDay()
    fun colorOf(id: Long): Color = subjects.firstOrNull { it.id == id }?.let { Color(it.color) } ?: FN.Muted

    Column(Modifier.verticalScroll(rememberScrollState()).padding(if (wide) 28.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenTitle("Statistics", "Analytics", trailing = {
            FnButton("Export CSV", {
                val csv = csvOf(sessions) { app.subjectName(it) }
                val i = Intent(Intent.ACTION_SEND).setType("text/csv")
                    .putExtra(Intent.EXTRA_SUBJECT, "Fieldnote sessions.csv").putExtra(Intent.EXTRA_TEXT, csv)
                ctx.startActivity(Intent.createChooser(i, "Export sessions"))
            }, filled = false, icon = Icons.Outlined.Share, enabled = sessions.isNotEmpty())
        })
        Seg(listOf("Day", "Week", "Month", "Trend"), tab, { tab = it })

        if (sessions.isEmpty()) {
            Panel { EmptyState("No data yet", "Finish a focus session and every chart here is computed from your recorded sessions.") }
        } else when (tab) {
            0 -> {
                val ds = sessions.on(sel).sortedBy { it.startedAt }
                val total = ds.sumOf { it.focusMs }
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton({ selEpoch -= 1 }) { Icon(Icons.Outlined.ChevronLeft, "Previous day") }
                        Text(sel.toString(), Modifier.weight(1f), textAlign = TextAlign.Center, color = FN.Mint)
                        IconButton({ if (selEpoch < today.toEpochDay()) selEpoch += 1 }) { Icon(Icons.Outlined.ChevronRight, "Next day") }
                    }
                }
                Cols(wide, {
                    Panel(title = "Summary") {
                        Row(Modifier.fillMaxWidth()) {
                            Stat("Total study", fmtHms(total), Modifier.weight(1f))
                            Stat("Max focus", fmtHms(ds.maxOfOrNull { it.focusMs } ?: 0L), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Stat("Start", ds.firstOrNull()?.let { fmtClock(it.startedAt) } ?: "—", Modifier.weight(1f))
                            Stat("End", ds.maxOfOrNull { it.endedAt }?.let { fmtClock(it) } ?: "—", Modifier.weight(1f))
                        }
                    }
                }, {
                    Panel(title = "By subject") {
                        val per = ds.groupBy { it.subjectId }.mapValues { e -> e.value.sumOf { it.focusMs } }
                        if (per.isEmpty()) EmptyState("No study", "Nothing recorded on this day.")
                        else Row(verticalAlignment = Alignment.CenterVertically) {
                            Donut(per.map { colorOf(it.key) to it.value.toFloat() }, fmtHm(total))
                            Spacer(Modifier.width(16.dp))
                            Column {
                                per.entries.sortedByDescending { it.value }.forEach { (id, ms) ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                                        Box(Modifier.size(8.dp).background(colorOf(id)))
                                        Spacer(Modifier.width(8.dp))
                                        Text("${app.subjectName(id)}  ${fmtHm(ms)} · ${if (total > 0) ms * 100 / total else 0}%", fontSize = 12.sp, color = FN.Mint)
                                    }
                                }
                            }
                        }
                    }
                })
                Panel(title = "Timeline") {
                    if (ds.isEmpty()) EmptyState("No sessions", "Nothing recorded on this day.")
                    ds.forEach { s ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(3.dp).height(44.dp).background(colorOf(s.subjectId)))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.subjectName(s.subjectId), color = FN.Mint)
                                Text("${fmtClock(s.startedAt)} – ${fmtClock(s.endedAt)} · break ${fmtHm(s.breakMs)} · ${s.pauses} pauses", color = FN.Muted, fontSize = 11.sp)
                                if (s.note.isNotBlank()) Text(s.note, color = FN.Muted, fontSize = 11.sp)
                            }
                            Mono(fmtHm(s.focusMs), 15.sp)
                        }
                    }
                }
            }
            1 -> {
                val mon = mondayOf(sel)
                val week = (0..6).map { mon.plusDays(it.toLong()) }
                val thisTotal = week.sumOf { byDay[it] ?: 0L }
                val prevTotal = week.sumOf { byDay[it.minusDays(7)] ?: 0L }
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton({ selEpoch -= 7 }) { Icon(Icons.Outlined.ChevronLeft, "Previous week") }
                        Text("${mon} → ${mon.plusDays(6)}", Modifier.weight(1f), textAlign = TextAlign.Center, color = FN.Mint, fontSize = 13.sp)
                        IconButton({ if (mon.plusDays(7) <= today) selEpoch += 7 }) { Icon(Icons.Outlined.ChevronRight, "Next week") }
                    }
                }
                Cols(wide, {
                    Panel(title = "Study by subject") {
                        val stacks = week.map { d ->
                            sessions.on(d).groupBy { it.subjectId }.map { (id, l) -> colorOf(id) to l.sumOf { it.focusMs } / 3_600_000f }
                        }
                        StackedBarChart(stacks, week.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3) })
                        Spacer(Modifier.height(10.dp))
                        Legend(subjects.map { it.name to Color(it.color) })
                    }
                }, {
                    Panel(title = "Comparison") {
                        Stat("This week", fmtHm(thisTotal))
                        Spacer(Modifier.height(10.dp))
                        Stat("Previous week", fmtHm(prevTotal))
                        Spacer(Modifier.height(10.dp))
                        val diff = if (prevTotal > 0) ((thisTotal - prevTotal) * 100 / prevTotal).toString() + "%" else "n/a"
                        Stat("Change", (if (prevTotal > 0 && thisTotal >= prevTotal) "+" else "") + diff)
                    }
                })
            }
            2 -> {
                val ym = YearMonth.from(sel)
                val monthDays = (1..ym.lengthOfMonth()).map { ym.atDay(it) }
                val total = monthDays.sumOf { byDay[it] ?: 0L }
                val active = monthDays.count { (byDay[it] ?: 0L) > 0 }
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton({ selEpoch = ym.minusMonths(1).atDay(1).toEpochDay() }) { Icon(Icons.Outlined.ChevronLeft, "Previous month") }
                        Text("${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}", Modifier.weight(1f), textAlign = TextAlign.Center, color = FN.Mint)
                        IconButton({ if (ym.plusMonths(1).atDay(1) <= today) selEpoch = ym.plusMonths(1).atDay(1).toEpochDay() }) { Icon(Icons.Outlined.ChevronRight, "Next month") }
                    }
                }
                Cols(wide, {
                    Panel(title = "Calendar heatmap") { MonthHeat(ym, byDay, null) { selEpoch = it.toEpochDay(); tab = 0 } }
                }, {
                    Panel(title = "Month totals") {
                        Stat("Total", fmtHm(total))
                        Spacer(Modifier.height(10.dp))
                        Stat("Active days", "$active / ${ym.lengthOfMonth()}")
                        Spacer(Modifier.height(10.dp))
                        Stat("Average per active day", fmtHm(if (active > 0) total / active else 0L))
                        Spacer(Modifier.height(10.dp))
                        Stat("Best day", fmtHm(monthDays.maxOfOrNull { byDay[it] ?: 0L } ?: 0L))
                    }
                })
            }
            else -> {
                val mon = mondayOf(today)
                val weeks = (11 downTo 0).map { mon.minusWeeks(it.toLong()) }
                val weekVals = weeks.map { w -> (0..6).sumOf { byDay[w.plusDays(it.toLong())] ?: 0L } / 3_600_000f }
                val last30 = (29 downTo 0).map { today.minusDays(it.toLong()) }
                val reg = last30.map { d ->
                    val l = sessions.on(d)
                    if (l.isEmpty()) null else minuteOfDay(l.minOf { it.startedAt }) to minuteOfDay(l.maxOf { it.endedAt })
                }
                val studiedDays = last30.count { (byDay[it] ?: 0L) > 0 }
                Cols(wide, {
                    Panel(title = "Weekly totals · last 12 weeks") {
                        BarChart(weekVals, weeks.map { "${it.monthValue}/${it.dayOfMonth}" }, highlight = 11)
                    }
                }, {
                    Panel(title = "Study start / end regularity · 30 days") {
                        RegularityChart(reg, "Start time", "End time")
                    }
                })
                Panel(title = "Consistency") {
                    Row(Modifier.fillMaxWidth()) {
                        Stat("Streak", "${streak(byDay, today)} d", Modifier.weight(1f))
                        Stat("Active, 30 d", "$studiedDays / 30", Modifier.weight(1f))
                        Stat("Longest session", fmtHm(sessions.maxOfOrNull { it.focusMs } ?: 0L), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Comparison with similar students needs the study server, which is not connected.", color = FN.Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Legend(items: List<Pair<String, Color>>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (n, c) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(c)); Spacer(Modifier.width(6.dp))
                Text(n, fontSize = 11.sp, color = FN.Muted)
            }
        }
    }
}
