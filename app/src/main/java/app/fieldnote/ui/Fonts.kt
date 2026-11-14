package app.fieldnote.ui

import android.content.Context
import java.io.File
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

data class AppFonts(val display: FontFamily, val ui: FontFamily, val mono: FontFamily)

val LocalFonts = staticCompositionLocalOf { AppFonts(FontFamily.Serif, FontFamily.SansSerif, FontFamily.Monospace) }

private fun weightOf(name: String): FontWeight {
    val n = name.lowercase()
    return when {
        "extrabold" in n -> FontWeight(800)
        "semibold" in n -> FontWeight(600)
        "extralight" in n -> FontWeight(200)
        "bold" in n -> FontWeight(700)
        "light" in n -> FontWeight(300)
        "thin" in n -> FontWeight(100)
        "medium" in n -> FontWeight(500)
        "black" in n -> FontWeight(900)
        else -> FontWeight(400)
    }
}

/**
 * Loads premium fonts from assets/fonts if present (static or variable TTFs).
 * Missing files fall back to system fonts, so the app always builds and runs.
 *   display: PlayfairDisplay*, Fraunces*, CormorantGaramond*
 *   ui:      PlusJakartaSans*, Manrope*
 *   mono:    JetBrainsMono*, SpaceGrotesk*
 */
@OptIn(ExperimentalTextApi::class)
fun loadFonts(ctx: Context): AppFonts {
    val am = ctx.assets
    val files = try {
        am.list("fonts")?.filter { it.endsWith(".ttf", true) || it.endsWith(".otf", true) } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    val dir = File(ctx.cacheDir, "fonts").apply { mkdirs() }
    fun fileFor(name: String): File {
        val f = File(dir, name)
        if (!f.exists() || f.length() == 0L) {
            am.open("fonts/$name").use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        }
        return f
    }

    fun family(prefixes: List<String>, fallback: FontFamily): FontFamily {
        val fonts = mutableListOf<Font>()
        files.filter { f -> prefixes.any { f.startsWith(it, ignoreCase = true) } && !f.contains("italic", true) }
            .forEach { f ->
                try {
                    val file = fileFor(f)
                    if (f.contains("[") || f.contains("-VAR", true)) {
                        listOf(300, 400, 500, 600, 700).forEach { w ->
                            fonts += Font(file, FontWeight(w), FontStyle.Normal, FontVariation.Settings(FontVariation.weight(w)))
                        }
                    } else {
                        fonts += Font(file, weightOf(f), FontStyle.Normal)
                    }
                } catch (e: Exception) { /* skip unreadable font */ }
            }
        return if (fonts.isEmpty()) fallback else FontFamily(fonts)
    }

    return AppFonts(
        display = family(listOf("PlayfairDisplay", "Fraunces", "CormorantGaramond"), FontFamily.Serif),
        ui = family(listOf("PlusJakartaSans", "Manrope"), FontFamily.SansSerif),
        mono = family(listOf("JetBrainsMono", "SpaceGrotesk"), FontFamily.Monospace)
    )
}
