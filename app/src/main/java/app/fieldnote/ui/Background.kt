package app.fieldnote.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import app.fieldnote.data.Settings
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun DrawScope.leaf(cx: Float, cy: Float, len: Float, wid: Float, rot: Float, c1: Color, c2: Color) {
    withTransform({ translate(cx, cy); rotate(rot, Offset.Zero) }) {
        val p = Path().apply {
            moveTo(0f, -len / 2)
            cubicTo(wid, -len / 4, wid, len / 4, 0f, len / 2)
            cubicTo(-wid, len / 4, -wid, -len / 4, 0f, -len / 2)
            close()
        }
        drawPath(p, Brush.linearGradient(listOf(c1, c2), start = Offset(0f, -len / 2), end = Offset(0f, len / 2)))
        drawLine(c1.copy(alpha = 0.5f), Offset(0f, -len / 2), Offset(0f, len / 2), strokeWidth = 3f)
    }
}

private fun loadBackground(ctx: android.content.Context, useCustom: Boolean, heavyBlur: Boolean, blurDp: Int): ImageBitmap? {
    val custom = java.io.File(ctx.filesDir, "bg_custom.jpg")
    fun decode(open: () -> java.io.InputStream): android.graphics.Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1400) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return open().use { BitmapFactory.decodeStream(it, null, opts) }
    }
    val loaded: android.graphics.Bitmap? = try {
        if (useCustom && custom.exists()) decode { custom.inputStream() } else decode { ctx.assets.open("bg.jpg") }
    } catch (e: Exception) { null }
    val bmp = loaded ?: return null
    // Below Android 12 there is no render-effect blur, so soften by down-scaling instead.
    if (!heavyBlur) return bmp.asImageBitmap()
    val div = 6 + blurDp / 2
    val small = android.graphics.Bitmap.createScaledBitmap(bmp, maxOf(8, bmp.width / div), maxOf(8, bmp.height / div), true)
    return small.asImageBitmap()
}

/**
 * Themed backdrop that gives the glass panels something to refract. A chosen photo is always
 * blurred (glassmorphism) and dimmed; without one, soft drawn leaves are used.
 */
@Composable
fun GlassBackground(s: Settings, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val hasBlurFx = android.os.Build.VERSION.SDK_INT >= 31
    val photo by produceState<ImageBitmap?>(null, s.bgImage, s.bgVersion, hasBlurFx, if (hasBlurFx) 0 else s.bgBlur) {
        value = withContext(Dispatchers.IO) { loadBackground(ctx, s.bgImage, !hasBlurFx, s.bgBlur) }
    }
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(FN.BgTop, FN.Bg, FN.Bg.copy(alpha = 1f).let { lerp(it, Color.Black, 0.6f) })))
        }
        val p = photo
        if (p != null) {
            val img = if (hasBlurFx) Modifier.fillMaxSize().blur(s.bgBlur.dp, BlurredEdgeTreatment.Unbounded) else Modifier.fillMaxSize()
            Image(p, null, img, contentScale = ContentScale.Crop)
        } else {
            Canvas(Modifier.fillMaxSize().blur(26.dp, BlurredEdgeTreatment.Unbounded)) {
                val h = size.height; val w = size.width
                val c1 = FN.Glow.copy(alpha = 0.70f)
                val c2 = FN.Tint.copy(alpha = 0.15f)
                leaf(w * 0.04f, h * 0.20f, h * 0.55f, h * 0.15f, -35f, c1, c2)
                leaf(w * 0.96f, h * 0.12f, h * 0.50f, h * 0.14f, 40f, c1, c2)
                leaf(w * 0.10f, h * 0.88f, h * 0.60f, h * 0.17f, 25f, c1, c2)
                leaf(w * 0.97f, h * 0.80f, h * 0.55f, h * 0.15f, -30f, c1, c2)
                leaf(w * 0.55f, h * 0.00f, h * 0.35f, h * 0.10f, 70f, c1, c2)
                leaf(w * 0.72f, h * 1.00f, h * 0.40f, h * 0.12f, -60f, c1, c2)
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.radialGradient(listOf(FN.Accent.copy(alpha = 0.22f), Color.Transparent), Offset(size.width * 0.15f, size.height * 0.08f), size.maxDimension * 0.55f))
            drawRect(Brush.radialGradient(listOf(FN.Glow.copy(alpha = 0.18f), Color.Transparent), Offset(size.width * 0.95f, size.height * 0.95f), size.maxDimension * 0.6f))
        }
        val dim = if (p != null) s.bgDim / 100f else 0.32f
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = dim * 0.45f), Color.Black.copy(alpha = dim * 1.1f).coerceAlpha()))))
    }
}

private fun Color.coerceAlpha(): Color = copy(alpha = alpha.coerceIn(0f, 0.95f))
