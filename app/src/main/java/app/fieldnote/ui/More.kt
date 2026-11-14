package app.fieldnote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldnote.Dest
import app.fieldnote.FieldnoteApp
import app.fieldnote.core.*
import java.time.LocalDate

@Composable
fun MoreScreen(app: FieldnoteApp, items: List<Dest>, go: (Dest) -> Unit) {
    val sessions by app.sessions.collectAsState()
    val settings by app.settings.state.collectAsState()
    val today = LocalDate.now()
    val byDay = sessions.focusByDay()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val vals = days.map { (byDay[it] ?: 0L).toFloat() }
    val weekMs = days.sumOf { byDay[it] ?: 0L }
    val st = streak(byDay, today)

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(FN.AccentBrush), contentAlignment = Alignment.Center) {
                Text((settings.userName.firstOrNull() ?: 'S').uppercase(), color = FN.Ink, fontSize = 24.sp, fontFamily = LocalFonts.current.display)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Big(settings.userName.ifBlank { "Student" }, 24.sp)
                Text("Study smarter", color = FN.Accent, fontSize = 12.sp)
            }
        }

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label("Study streak")
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Big("$st", 40.sp, color = FN.Accent)
                        Spacer(Modifier.width(6.dp))
                        Text(if (st == 1) "day" else "days", color = FN.Muted, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                val mx = maxOf(vals.maxOrNull() ?: 0f, 1f)
                Canvas(Modifier.size(width = 110.dp, height = 56.dp)) {
                    val slot = size.width / vals.size
                    vals.forEachIndexed { i, v ->
                        val h = maxOf(size.height * v / mx, 4.dp.toPx())
                        drawRoundRect(
                            Brush.verticalGradient(listOf(FN.AccentLight, FN.AccentDark), startY = size.height - h, endY = size.height),
                            Offset(i * slot + slot * 0.18f, size.height - h), Size(slot * 0.64f, h), CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }
        }

        Label("More", Modifier.padding(start = 4.dp))
        items.forEach { d ->
            Row(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)).clickable { go(d) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Brush.verticalGradient(listOf(FN.Accent.copy(alpha = 0.35f), FN.Accent.copy(alpha = 0.10f)))), contentAlignment = Alignment.Center) {
                    Icon(d.icon, null, tint = FN.Accent)
                }
                Spacer(Modifier.width(16.dp))
                Text(d.label, Modifier.weight(1f), color = FN.Mint, fontSize = 16.sp)
                Icon(Icons.Outlined.ChevronRight, null, tint = FN.Muted)
            }
        }

        Panel {
            Big("Keep going.", 22.sp, color = FN.Accent)
            Spacer(Modifier.height(4.dp))
            Text(
                if (weekMs > 0) "${fmtHm(weekMs)} logged in the last 7 days." else "Log your first session this week.",
                color = FN.Muted, fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
