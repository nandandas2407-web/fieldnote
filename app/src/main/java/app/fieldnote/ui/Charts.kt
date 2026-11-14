package app.fieldnote.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

@Composable
private fun rememberGrow(): Float {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val p by animateFloatAsState(if (go) 1f else 0f, tween(700), label = "grow")
    return p
}

private fun axisLabel(v: Float, unit: String) = (if (v >= 10f) "%.0f".format(v) else "%.1f".format(v)) + unit

@Composable
private fun YAxis(maxV: Float, unit: String, height: Dp) {
    Column(Modifier.width(36.dp).height(height), verticalArrangement = Arrangement.SpaceBetween) {
        Text(axisLabel(maxV, unit), fontSize = 10.sp, color = FN.Muted)
        Text(axisLabel(maxV / 2, unit), fontSize = 10.sp, color = FN.Muted)
        Text("0", fontSize = 10.sp, color = FN.Muted)
    }
}

@Composable
fun BarChart(
    values: List<Float>, labels: List<String>, modifier: Modifier = Modifier,
    unit: String = "h", height: Dp = 150.dp, highlight: Int = -1
) {
    val maxV = maxOf(values.maxOrNull() ?: 0f, 0.5f)
    val grow = rememberGrow()
    Row(modifier.fillMaxWidth()) {
        YAxis(maxV, unit, height)
        Column(Modifier.weight(1f)) {
            Canvas(Modifier.fillMaxWidth().height(height)) {
                listOf(0f, 0.5f, 1f).forEach { f ->
                    val y = size.height * (1 - f)
                    drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                val slot = size.width / maxOf(values.size, 1)
                values.forEachIndexed { i, v ->
                    val h = size.height * (v / maxV) * grow
                    val bw = slot * 0.56f
                    if (h > 0.5f) {
                        val top = size.height - h
                        val brush = if (i == highlight)
                            Brush.verticalGradient(listOf(Color.White, FN.Mint, FN.Mint.copy(alpha = 0.55f)), startY = top, endY = size.height)
                        else Brush.verticalGradient(listOf(FN.AccentLight, FN.Accent, FN.AccentDark), startY = top, endY = size.height)
                        drawRoundRect(brush, Offset(i * slot + (slot - bw) / 2, top), Size(bw, h), CornerRadius(7.dp.toPx()))
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
fun StackedBarChart(days: List<List<Pair<Color, Float>>>, labels: List<String>, modifier: Modifier = Modifier, height: Dp = 150.dp) {
    val maxV = maxOf(days.maxOfOrNull { d -> d.sumOf { it.second.toDouble() }.toFloat() } ?: 0f, 0.5f)
    val grow = rememberGrow()
    Row(modifier.fillMaxWidth()) {
        YAxis(maxV, "h", height)
        Column(Modifier.weight(1f)) {
            Canvas(Modifier.fillMaxWidth().height(height)) {
                listOf(0f, 0.5f, 1f).forEach { f ->
                    val y = size.height * (1 - f)
                    drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                val slot = size.width / maxOf(days.size, 1)
                days.forEachIndexed { i, segs ->
                    var y = size.height
                    val bw = slot * 0.56f
                    segs.forEachIndexed { k, (c, v) ->
                        val h = size.height * (v / maxV) * grow
                        if (h > 0.5f) {
                            val brush = Brush.verticalGradient(listOf(c, c.copy(alpha = 0.72f)), startY = y - h, endY = y)
                            drawRoundRect(brush, Offset(i * slot + (slot - bw) / 2, y - h + 1f), Size(bw, maxOf(h - 2f, 0.5f)), CornerRadius(if (k == segs.lastIndex) 7.dp.toPx() else 2.dp.toPx()))
                        }
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
            val stroke = 20.dp.toPx()
            val inset = stroke / 2
            val tl = Offset(inset, inset)
            val sz = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.08f), 0f, 360f, false, tl, sz, style = Stroke(stroke))
            if (total > 0f) {
                var start = -90f
                slices.forEach { (c, v) ->
                    val sweep = 360f * v / total * grow
                    val gap = if (slices.size > 1) 3f else 0f
                    drawArc(c, start + gap / 2, maxOf(sweep - gap, 0.5f), false, tl, sz, style = Stroke(stroke))
                    start += sweep
                }
            }
        }
        Text(center, fontSize = 15.sp, color = FN.Mint, fontFamily = LocalFonts.current.mono)
    }
}

/** Large progress ring with a gradient, rounded arc. */
@Composable
fun GoalRing(fraction: Float, size: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val p by animateFloatAsState(if (go) fraction.coerceIn(0f, 1f) else 0f, tween(900), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 14.dp.toPx()
            val tl = Offset(sw / 2, sw / 2)
            val sz = Size(this.size.width - sw, this.size.height - sw)
            drawArc(Color.White.copy(alpha = 0.09f), 0f, 360f, false, tl, sz, style = Stroke(sw))
            if (p > 0.002f) {
                drawArc(
                    Brush.linearGradient(listOf(FN.AccentDark, FN.Accent, FN.AccentLight), Offset(0f, this.size.height), Offset(this.size.width, 0f)),
                    -90f, 360f * p, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

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
                    drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                val slot = size.width / maxOf(points.size, 1)
                points.forEachIndexed { i, p ->
                    if (p != null) {
                        val x = i * slot + slot / 2
                        val ys = size.height * p.first / 1440f
                        val ye = size.height * p.second / 1440f
                        drawLine(Brush.verticalGradient(listOf(FN.Accent.copy(alpha = 0.5f), FN.Mint.copy(alpha = 0.3f)), startY = ys, endY = maxOf(ye, ys + 1f)), Offset(x, ys), Offset(x, ye), 4.dp.toPx(), StrokeCap.Round)
                        drawCircle(FN.Accent, 4.dp.toPx(), Offset(x, ys))
                        drawCircle(FN.Mint, 4.dp.toPx(), Offset(x, ye))
                    }
                }
            }
        }
        Row(Modifier.padding(top = 8.dp, start = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(FN.Accent)); Spacer(Modifier.width(6.dp))
            Text(startLabel, fontSize = 11.sp, color = FN.Muted); Spacer(Modifier.width(16.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(FN.Mint)); Spacer(Modifier.width(6.dp))
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
    val shape = RoundedCornerShape(10.dp)
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
                        Spacer(Modifier.weight(1f).height(54.dp))
                    } else {
                        val d = month.atDay(idx + 1)
                        val ms = byDay[d] ?: 0L
                        val h = ms / 3_600_000f
                        val a = when { ms <= 0 -> 0f; h < 1 -> 0.22f; h < 3 -> 0.40f; h < 5 -> 0.58f; h < 8 -> 0.76f; else -> 0.95f }
                        val strong = a > 0.55f
                        Box(
                            Modifier.weight(1f).height(54.dp).padding(2.dp).clip(shape)
                                .background(if (a == 0f) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)))
                                else Brush.verticalGradient(listOf(FN.AccentLight.copy(alpha = a), FN.AccentDark.copy(alpha = a))))
                                .then(if (d == selected) Modifier.border(1.5.dp, FN.Mint, shape) else Modifier)
                                .clickable { onSelect(d) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${idx + 1}", fontSize = 11.sp, color = if (strong) FN.Ink else FN.Muted)
                                if (ms > 0) {
                                    val m = ms / 60000
                                    Text("%d:%02d".format(m / 60, m % 60), fontSize = 10.sp, color = if (strong) FN.Ink else FN.Mint)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
