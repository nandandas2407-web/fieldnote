package app.fieldnote.core

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.fieldnote.ui.FN

/**
 * Small floating card drawn over whatever app is open (needs "Display over other apps").
 * It appears when a blocked app comes to the front and offers a one-tap way back to the session.
 */
object Overlay {
    private var view: View? = null

    fun canDraw(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    private fun dp(ctx: Context, v: Float): Int = (v * ctx.resources.displayMetrics.density + 0.5f).toInt()
    private fun Color.argb(alpha: Float = this.alpha): Int = copy(alpha = alpha).toArgb()

    fun show(ctx: Context, appLabel: String, onBack: () -> Unit) {
        hide(ctx)
        if (!canDraw(ctx)) return
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 18f), dp(ctx, 14f), dp(ctx, 18f), dp(ctx, 16f))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(FN.Tint.argb(0.88f), FN.Bg.argb(0.94f))
            ).apply {
                cornerRadius = dp(ctx, 26f).toFloat()
                setStroke(dp(ctx, 1f), 0x40FFFFFF)
            }
            elevation = dp(ctx, 14f).toFloat()
        }

        val header = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(ctx).apply {
            text = "NOT ALLOWED RIGHT NOW"
            setTextColor(FN.Accent.argb(1f)); textSize = 11f; letterSpacing = 0.14f; setTypeface(typeface, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(TextView(ctx).apply {
            text = "✕"; setTextColor(FN.Mint.argb(0.7f)); textSize = 16f
            setPadding(dp(ctx, 10f), dp(ctx, 2f), 0, dp(ctx, 2f))
            setOnClickListener { hide(ctx) }
        })
        card.addView(header)

        card.addView(TextView(ctx).apply {
            text = "$appLabel is not on your allowed list."
            setTextColor(FN.Mint.argb(1f)); textSize = 16f; setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(ctx, 8f), 0, 0)
        })
        card.addView(TextView(ctx).apply {
            text = "Your focus session is still running."
            setTextColor(FN.Mint.argb(0.72f)); textSize = 13f
            setPadding(0, dp(ctx, 2f), 0, dp(ctx, 14f))
        })
        card.addView(TextView(ctx).apply {
            text = "Back to session"
            gravity = Gravity.CENTER
            setTextColor(FN.Ink.argb(1f)); textSize = 15f; setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(ctx, 16f), dp(ctx, 13f), dp(ctx, 16f), dp(ctx, 13f))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(FN.AccentLight.argb(1f), FN.Accent.argb(1f), FN.AccentDark.argb(1f))
            ).apply { cornerRadius = dp(ctx, 18f).toFloat() }
            setOnClickListener { onBack() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val width = minOf(dp(ctx, 360f), ctx.resources.displayMetrics.widthPixels - dp(ctx, 24f))
        val lp = WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(ctx, 56f)
            // Frosted-glass blur of whatever is behind the card, where the device supports it.
            if (Build.VERSION.SDK_INT >= 31) {
                flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                blurBehindRadius = 40
            }
        }
        try {
            card.alpha = 0f
            card.translationY = -dp(ctx, 18f).toFloat()
            wm.addView(card, lp)
            card.animate().alpha(1f).translationY(0f).setDuration(220).start()
            view = card
        } catch (e: Exception) {
            view = null
        }
    }

    fun hide(ctx: Context) {
        val v = view ?: return
        view = null
        try {
            (ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v)
        } catch (e: Exception) { /* already gone */ }
    }

    val showing: Boolean get() = view != null
}
