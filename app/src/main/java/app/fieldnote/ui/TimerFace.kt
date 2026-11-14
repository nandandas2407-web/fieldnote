package app.fieldnote.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class TimerStyle(val id: String, val label: String) {
    Digital("digital", "Digital"),
    Serif("serif", "Classic"),
    Ring("ring", "Ring"),
    Analog("analog", "Analog"),
    Orb("orb", "Liquid orb");

    companion object {
        fun of(id: String) = values().firstOrNull { it.id == id } ?: Digital
    }
}

private fun clockText(ms: Long, showSeconds: Boolean, countdown: Boolean): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val sec = total % 60
    return when {
        countdown && showSeconds -> "%02d:%02d".format(total / 60, sec)
        countdown -> "${(total + 59) / 60} min"
        showSeconds -> "%d:%02d:%02d".format(h, m, sec)
        else -> "%d:%02d".format(h, m)
    }
}

/**
 * The running timer, drawn in one of several styles.
 * [dim] is the diameter of the round styles and the base for text sizes.
 */
@Composable
fun TimerFace(
    style: TimerStyle,
    focusMs: Long,
    pomodoro: Boolean,
    workMin: Int,
    showSeconds: Boolean,
    dim: Dp,
    modifier: Modifier = Modifier
) {
    val interval = workMin * 60_000L
    val inInterval = if (pomodoro) focusMs % interval else focusMs
    val shownMs = if (pomodoro) interval - inInterval else focusMs
    val frac = if (pomodoro) inInterval.toFloat() / interval else (focusMs % 3_600_000L) / 3_600_000f
    val text = clockText(shownMs, showSeconds, pomodoro)
    val caption = if (pomodoro) "Interval ${focusMs / interval + 1}" else "Focus time"
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val d = minOf(dim.value, maxWidth.value)
        // text-only styles shrink to fit the available width
        val fit = maxWidth.value / (text.length * 0.64f)
        when (style) {
            TimerStyle.Digital -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text,
                    style = TextStyle(
                        brush = Brush.verticalGradient(listOf(FN.Mint, FN.AccentLight)),
                        fontSize = minOf(d * 0.30f, fit).sp, fontFamily = LocalFonts.current.mono, fontWeight = FontWeight.Light
                    ),
                    maxLines = 1
                )
                Text(caption.uppercase(), color = FN.Muted, fontSize = 11.sp, letterSpacing = 2.sp)
            }

            TimerStyle.Serif -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text,
                    style = TextStyle(
                        color = FN.Mint, fontSize = minOf(d * 0.30f, fit).sp,
                        fontFamily = LocalFonts.current.display, fontWeight = FontWeight.Normal
                    ),
                    maxLines = 1
                )
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width((d * 0.45f).dp).height(3.dp).background(FN.AccentBrushH, RoundedCornerShape(50)))
                Spacer(Modifier.height(8.dp))
                Text(caption, color = FN.Muted, fontSize = 13.sp, letterSpacing = 1.sp)
            }

            TimerStyle.Ring -> Box(Modifier.size(d.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(d.dp)) { drawRing(frac) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text, color = FN.Mint, fontSize = (d * 0.17f).sp, fontFamily = LocalFonts.current.mono, fontWeight = FontWeight.Light, maxLines = 1)
                    Text(caption, color = FN.Muted, fontSize = 11.sp)
                }
            }

            TimerStyle.Analog -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(d.dp)) { drawAnalog(shownMs) }
                Spacer(Modifier.height(8.dp))
                Text(text, color = FN.Mint, fontSize = (d * 0.10f).coerceAtLeast(14f).sp, fontFamily = LocalFonts.current.mono, fontWeight = FontWeight.Light)
            }

            TimerStyle.Orb -> Box(Modifier.size(d.dp), contentAlignment = Alignment.Center) {
                OrbCanvas(d.dp, frac)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text, color = Color.White, fontSize = (d * 0.17f).sp, fontFamily = LocalFonts.current.mono, fontWeight = FontWeight.Normal, maxLines = 1)
                    Text(caption, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                }
            }
        }
    }
}

private fun DrawScope.drawRing(frac: Float) {
    val sw = size.minDimension * 0.06f
    val tl = Offset(sw / 2, sw / 2)
    val sz = Size(size.width - sw, size.height - sw)
    // glass track
    drawArc(Color.White.copy(alpha = 0.10f), 0f, 360f, false, tl, sz, style = Stroke(sw))
    val sweep = 360f * frac.coerceIn(0.002f, 1f)
    // soft glow under the arc
    drawArc(FN.Accent.copy(alpha = 0.20f), -90f, sweep, false, Offset(0f, 0f), Size(size.width, size.height), style = Stroke(sw * 1.9f, cap = StrokeCap.Round))
    drawArc(
        Brush.sweepGradient(listOf(FN.AccentDark, FN.Accent, FN.AccentLight, FN.AccentDark), center),
        -90f, sweep, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round)
    )
    // leading dot
    val ang = (-90f + sweep) * (PI.toFloat() / 180f)
    val r = sz.width / 2
    drawCircle(Color.White, sw * 0.42f, Offset(center.x + r * cos(ang), center.y + r * sin(ang)))
}

private fun DrawScope.drawAnalog(ms: Long) {
    val r = size.minDimension / 2
    val c = center
    // glass face
    drawCircle(Brush.radialGradient(listOf(FN.Tint.copy(alpha = 0.55f), FN.Bg.copy(alpha = 0.85f)), c, r), r)
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), Offset(c.x - r * 0.35f, c.y - r * 0.55f), r), r)
    drawCircle(Color.White.copy(alpha = 0.18f), r - 1.dp.toPx(), c, style = Stroke(1.dp.toPx()))
    // ticks
    for (i in 0 until 60) {
        val a = (i * 6 - 90) * (PI.toFloat() / 180f)
        val major = i % 5 == 0
        val len = if (major) r * 0.11f else r * 0.05f
        val o = r * 0.92f
        drawLine(
            if (major) FN.Mint.copy(alpha = 0.85f) else FN.Mint.copy(alpha = 0.30f),
            Offset(c.x + (o - len) * cos(a), c.y + (o - len) * sin(a)),
            Offset(c.x + o * cos(a), c.y + o * sin(a)),
            strokeWidth = if (major) 2.5.dp.toPx() else 1.dp.toPx(), cap = StrokeCap.Round
        )
    }
    val total = ms.coerceAtLeast(0) / 1000f
    fun hand(frac: Float, len: Float, w: Float, color: Color) {
        val a = (frac * 360f - 90f) * (PI.toFloat() / 180f)
        drawLine(color, c, Offset(c.x + r * len * cos(a), c.y + r * len * sin(a)), strokeWidth = w, cap = StrokeCap.Round)
    }
    hand((total / 3600f / 12f) % 1f, 0.45f, 5.dp.toPx(), FN.Mint)          // hours
    hand((total / 3600f) % 1f, 0.68f, 3.5.dp.toPx(), FN.Mint)               // minutes
    hand((total / 60f) % 1f, 0.82f, 1.8.dp.toPx(), FN.Accent)               // seconds
    drawCircle(FN.Accent, 5.dp.toPx(), c)
    drawCircle(FN.Ink, 2.dp.toPx(), c)
}

@Composable
private fun OrbCanvas(dim: Dp, frac: Float) {
    val t = rememberInfiniteTransition(label = "orb")
    val phase by t.animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart), label = "phase"
    )
    Canvas(Modifier.size(dim)) {
        val r = size.minDimension / 2
        val c = center
        val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
        // empty glass sphere
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.22f)), c, r), r)
        val level = size.height * (1f - (0.08f + 0.84f * frac.coerceIn(0f, 1f)))
        clipPath(circle) {
            fun wave(offset: Float, amp: Float, brush: Brush) {
                val p = Path()
                p.moveTo(0f, size.height)
                var x = 0f
                while (x <= size.width + 4f) {
                    val y = level + amp * sin(x / size.width * 2f * PI.toFloat() * 1.3f + phase + offset)
                    p.lineTo(x, y)
                    x += 4f
                }
                p.lineTo(size.width, size.height)
                p.close()
                drawPath(p, brush)
            }
            wave(1.7f, size.height * 0.025f, Brush.verticalGradient(listOf(FN.AccentLight.copy(alpha = 0.45f), FN.Accent.copy(alpha = 0.30f)), startY = level, endY = size.height))
            wave(0f, size.height * 0.03f, Brush.verticalGradient(listOf(FN.Accent.copy(alpha = 0.95f), FN.AccentDark), startY = level, endY = size.height))
        }
        // glass highlight and rim
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.30f), Color.Transparent), Offset(c.x - r * 0.40f, c.y - r * 0.55f), r * 0.75f), r)
        drawCircle(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.05f), FN.Accent.copy(alpha = 0.35f))), r - 1.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
    }
}
