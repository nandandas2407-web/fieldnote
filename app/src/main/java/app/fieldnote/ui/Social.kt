package app.fieldnote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldnote.FieldnoteApp
import app.fieldnote.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

@Composable
fun GroupsScreen(app: FieldnoteApp) {
    val wide = LocalWide.current
    val settings by app.settings.state.collectAsState()
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf(settings.serverUrl) }
    var result by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(if (wide) 28.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenTitle("Study rooms", "Groups")
        Cols(wide, {
            Panel(title = "Study server") {
                EmptyState(
                    "Not connected",
                    "Public and private groups, live member status and shared rankings need a server. Everything else in Fieldnote works fully offline."
                )
                FnField(url, { url = it }, "Server URL (https://…)")
                Spacer(Modifier.height(10.dp))
                FnButton(if (busy) "Checking…" else "Save and check connection", {
                    val clean = url.trim().trimEnd('/')
                    app.settings.update { it.copy(serverUrl = clean) }
                    if (clean.isNotEmpty()) {
                        busy = true
                        scope.launch {
                            result = withContext(Dispatchers.IO) {
                                try {
                                    val c = URL("$clean/health").openConnection() as HttpURLConnection
                                    c.connectTimeout = 5000; c.readTimeout = 5000
                                    val code = c.responseCode
                                    c.disconnect()
                                    if (code in 200..299) "Server reachable (HTTP $code)." else "Server answered HTTP $code."
                                } catch (e: Exception) { "Could not reach server: ${e.message}" }
                            }
                            busy = false
                        }
                    } else result = "Server URL cleared."
                }, Modifier.fillMaxWidth(), enabled = !busy)
                result?.let { Spacer(Modifier.height(10.dp)); Text(it, color = FN.Muted, fontSize = 13.sp) }
            }
        }, {
            Panel(title = "Backend contract") {
                Text(
                    "This build checks reachability only. It ships no group protocol client yet, so no groups, members or live status are shown, and none are simulated. " +
                        "The planned REST + WebSocket contract is documented in docs/BACKEND.md.",
                    color = FN.Muted, fontSize = 13.sp
                )
            }
        })
    }
}

@Composable
fun RankingsScreen(app: FieldnoteApp) {
    val wide = LocalWide.current
    val sessions by app.sessions.collectAsState()
    val subjects by app.subjects.collectAsState()
    val today = LocalDate.now()
    var period by rememberSaveable { mutableIntStateOf(1) }
    val from = when (period) { 0 -> today; 1 -> today.minusDays(6); 2 -> today.minusDays(29); else -> LocalDate.of(2000, 1, 1) }
    val inRange = sessions.between(from, today)
    val per = inRange.groupBy { it.subjectId }.mapValues { e -> e.value.sumOf { it.focusMs } }.entries.sortedByDescending { it.value }
    val top = per.firstOrNull()?.value ?: 1L
    val byDay = sessions.focusByDay()
    val mon = mondayOf(today)
    val bestWeek = sessions.groupBy { mondayOf(it.startedAt.toDate()) }.values.maxOfOrNull { l -> l.sumOf { it.focusMs } } ?: 0L

    Column(Modifier.verticalScroll(rememberScrollState()).padding(if (wide) 28.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenTitle("Your records", "Rankings")
        Seg(listOf("Today", "7 days", "30 days", "All time"), period, { period = it })
        Cols(wide, {
            Panel(title = "Subjects ranked by focus time") {
                if (per.isEmpty()) EmptyState("No data", "Focus on a subject to see it ranked here.")
                per.forEachIndexed { i, (id, ms) ->
                    val c = subjects.firstOrNull { it.id == id }?.let { Color(it.color) } ?: FN.Accent
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Mono("${i + 1}", 16.sp, Modifier.width(28.dp), color = FN.Accent)
                            Text(app.subjectName(id), Modifier.weight(1f), color = FN.Mint)
                            Mono(fmtHm(ms), 14.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Progress(ms.toFloat() / top, height = 4.dp)
                    }
                }
            }
        }, {
            Panel(title = "Personal records") {
                Row(Modifier.fillMaxWidth()) {
                    Stat("Best day", fmtHm(byDay.values.maxOrNull() ?: 0L), Modifier.weight(1f))
                    Stat("Best week", fmtHm(bestWeek), Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    Stat("Longest session", fmtHm(sessions.maxOfOrNull { it.focusMs } ?: 0L), Modifier.weight(1f))
                    Stat("Streak", "${streak(byDay, today)} d", Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Text("Global, friend and group leaderboards require the study server (see Groups).", color = FN.Muted, fontSize = 12.sp)
            }
        })
    }
}
