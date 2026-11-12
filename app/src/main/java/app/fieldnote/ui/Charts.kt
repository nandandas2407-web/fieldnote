package app.fieldnote.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import java.time.LocalDate
import java.time.YearMonth

@Composable
private fun rememberGrow(): Float {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val p by animateFloatAsState(if (go) 1f else 0f, tween(600), label = "grow")
    return p
}

private fun axisLabel(v: Float, unit: String) = (if (v >= 10f) "%.0f".format(v) else "%.1f".format(v)) + unit

@Composable
fun BarChart(
    values: List<Float>, labels: List<String>, modifier: Modifier = Modifier,
    unit: String = "h", height: Dp = 150.dp, highlight: Int = -1
) {
    val maxV = maxOf(values.maxOrNull() ?: 0f, 0.5f)
    val grow = rememberGrow()
    Row(modifier.fillMaxWidth()) {
        Column(Modifier.width(36.dp).height(height), verticalArrangement = Arrangement.SpaceBetween) {
            Text(axisLabel(maxV, unit), fontSize = 10.sp, color = FN.Muted)
            Text(axisLabel(maxV / 2, unit), fontSize = 10.sp, color = FN.Muted)
            Text("0", fontSize = 10.sp, color = FN.Muted)
        }
        Column(Modifier.weight(1f)) {
            Canvas(Modifier.fillMaxWidth().height(height)) {
                listOf(0f, 0.5f, 1f).forEach { f ->
                    val y = size.height * (1 - f)
                    drawLine(FN.Mint.copy(alpha = 0.1f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                val slot = size.width / maxOf(values.size, 1)
                values.forEachIndexed { i, v ->
                    val h = size.height * (v / maxV) * grow
                    val bw = slot * 0.6f
                    drawRect(
                        if (i == highlight) FN.Mint else FN.Accent,
                        Offset(i * slot + (slot - bw) / 2, size.height - h), Size(bw, h)
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                labels.forEach { Text(it, Modifier.weight(1f), fontSize = 10.sp, color = FN.Muted, textAlign = TextAlign.Center, maxLines = 1) }
            }
        }
    }
}

/** days[i] = list of (colour, hours) segments stacked bottom-up. */
@Composable
fun StackedBarChart(days: List<List<Pair<Color, Float>>>, labels: List<String>, modifier: Modifier = Modifier, height: Dp = 150.dp) {
    val maxV = maxOf(days.maxOfOrNull { d -> d.sumOf { it.second.toDouble() }.toFloat() } ?: 0f, 0.5f)
    val grow = rememberGrow()
    Row(modifier.fillMaxWidth()) {
        Column(Modifier.width(36.dp).height(height), verticalArrangement = Arrangement.SpaceBetween) {
            Text(axisLabel(maxV, "h"), fontSize = 10.sp, color = FN.Muted)
            Text(axisLabel(maxV / 2, "h"), fontSize = 10.sp, color = FN.Muted)
            Text("0", fontSize = 10.sp, color = FN.Muted)
        }
        Column(Modifier.weight(1f)) {
            Canvas(Modifier.fillMaxWidth().height(height)) {
                listOf(0f, 0.5f, 1f).forEach { f ->
                    val y = size.height * (1 - f)
                    drawLine(FN.Mint.copy(alpha = 0.1f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                val slot = size.width / maxOf(days.size, 1)
                days.forEachIndexed { i, segs ->
                    var y = size.height
                    val bw = slot * 0.6f
                    segs.forEach { (c, v) ->
                        val h = size.height * (v / maxV) * grow
                        drawRect(c, Offset(i * slot + (slot - bw) / 2, y - h), Size(bw, h))
                        y -= h
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                labels.forEach { Text(it, Modifier.weight(1f), fontSize = 10.sp, color = FN.Muted, textAlign = TextAlign.Center, maxLines = 1) }
            }
        }
    }
}

@Composable
fun Donut(slices: List<Pair<Color, Float>>, center: String, modifier: Modifier = Modifier, size: Dp = 150.dp) {
    val total = slices.sumOf { it.second.toDouble() }.toFloat()
    val grow = rememberGrow()
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 22.dp.toPx()
            val inset = stroke / 2
            if (total <= 0f) {
                drawArc(FN.Mint.copy(alpha = 0.1f), 0f, 360f, false, Offset(inset, inset), Size(this.size.width - stroke, this.size.height - stroke), style = Stroke(stroke))
            } else {
                var start = -90f
                slices.forEach { (c, v) ->
                    val sweep = 360f * v / total * grow
                    drawArc(c, start, maxOf(sweep - 1.5f, 0f), false, Offset(inset, inset), Size(this.size.width - stroke, this.size.height - stroke), style = Stroke(stroke))
                    start += sweep
                }
            }
        }
        Text(center, fontSize = 15.sp, color = FN.Mint, fontFamily = FontFamily.Monospace)
    }
}

/** Start/end time-of-day per day over a period (oldest first). Entries are null for days without study. */
@Composable
fun RegularityChart(points: List<Pair<Int, Int>?>, startLabel: String, endLabel: String, modifier: Modifier = Modifier, height: Dp = 170.dp) {
    Column(modifier.fillMaxWidth()) {
        Row {
            Column(Modifier.width(36.dp).height(height), verticalArrangement = Arrangement.SpaceBetween) {
                Text("00:00", fontSize = 10.sp, color = FN.Muted)
                Text("12:00", fontSize = 10.sp, color = FN.Muted)
                Text("24:00", fontSize = 10.sp, color = FN.Muted)
            }
            Canvas(Modifier.weight(1f).height(height)) {
                listOf(0f, 0.5f, 1f).forEach { f ->
                    val y = size.height * f
                    drawLine(FN.Mint.copy(alpha = 0.1f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                val slot = size.width / maxOf(points.size, 1)
                points.forEachIndexed { i, p ->
                    if (p != null) {
                        val x = i * slot + slot / 2
                        val ys = size.height * p.first / 1440f
                        val ye = size.height * p.second / 1440f
                        drawLine(FN.Accent.copy(alpha = 0.35f), Offset(x, ys), Offset(x, ye), 3.dp.toPx())
                        drawCircle(FN.Accent, 3.5.dp.toPx(), Offset(x, ys))
                        drawCircle(FN.Mint, 3.5.dp.toPx(), Offset(x, ye))
                    }
                }
            }
        }
        Row(Modifier.padding(top = 8.dp, start = 36.dp)) {
            Box(Modifier.size(8.dp).background(FN.Accent)); Spacer(Modifier.width(6.dp))
            Text(startLabel, fontSize = 11.sp, color = FN.Muted); Spacer(Modifier.width(16.dp))
            Box(Modifier.size(8.dp).background(FN.Mint)); Spacer(Modifier.width(6.dp))
            Text(endLabel, fontSize = 11.sp, color = FN.Muted)
        }
    }
}

@Composable
fun MonthHeat(
    month: YearMonth, byDay: Map<LocalDate, Long>, selected: LocalDate?,
    modifier: Modifier = Modifier,
    onSelect: (LocalDate) -> Unit
) {
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1
    val cells = lead + month.lengthOfMonth()
    val rows = (cells + 6) / 7
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp, color = FN.Muted)
            }
        }
        Spacer(Modifier.height(4.dp))
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val idx = r * 7 + c - lead
                    if (idx < 0 || idx >= month.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f).height(52.dp))
                    } else {
                        val d = month.atDay(idx + 1)
                        val ms = byDay[d] ?: 0L
                        val h = ms / 3_600_000f
                        val a = when { ms <= 0 -> 0f; h < 1 -> 0.16f; h < 3 -> 0.32f; h < 5 -> 0.5f; h < 8 -> 0.68f; else -> 0.9f }
                        val isSel = d == selected
                        Box(
                            Modifier.weight(1f).height(52.dp).padding(1.dp)
                                .background(FN.Accent.copy(alpha = a))
                                .background(if (a == 0f) FN.Mint.copy(alpha = 0.04f) else Color.Transparent)
                                .then(if (isSel) Modifier.border(1.5.dp, FN.Mint) else Modifier)
                                .clickable { onSelect(d) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${idx + 1}", fontSize = 11.sp, color = if (a > 0.5f) FN.Bg else FN.Muted)
                                if (ms > 0) {
                                    val m = ms / 60000
                                    Text("%d:%02d".format(m / 60, m % 60), fontSize = 10.sp, color = if (a > 0.5f) FN.Bg else FN.Mint)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
