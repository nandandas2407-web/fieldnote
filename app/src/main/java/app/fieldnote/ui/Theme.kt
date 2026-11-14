package app.fieldnote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Size
import app.fieldnote.data.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

data class Palette(
    val id: String, val name: String,
    val accent: Color, val mint: Color, val bg: Color, val bgTop: Color, val tint: Color, val glow: Color
)

object Palettes {
    val Forest = Palette("forest", "Forest", Color(0xFF8BC53D), Color(0xFFE2F0CC), Color(0xFF011207), Color(0xFF04230F), Color(0xFF2B7D40), Color(0xFF3FBF6A))
    val Ocean = Palette("ocean", "Ocean", Color(0xFF4FC3F7), Color(0xFFDDF1FB), Color(0xFF020E1A), Color(0xFF04223A), Color(0xFF1F6FA8), Color(0xFF2E9BD6))
    val Violet = Palette("violet", "Violet", Color(0xFFB388FF), Color(0xFFEBE0FF), Color(0xFF0B0617), Color(0xFF1A0B3A), Color(0xFF6A3FB5), Color(0xFF9B6BFF))
    val Sunset = Palette("sunset", "Sunset", Color(0xFFFFA64D), Color(0xFFFFE8D2), Color(0xFF1A0803), Color(0xFF3A1406), Color(0xFFB3541E), Color(0xFFFF7A3D))
    val Rose = Palette("rose", "Rose", Color(0xFFFF7FA8), Color(0xFFFFE0EA), Color(0xFF18050E), Color(0xFF38091F), Color(0xFFA8325F), Color(0xFFFF5C8A))
    val Graphite = Palette("graphite", "Graphite", Color(0xFFD0D6DC), Color(0xFFECEFF1), Color(0xFF070809), Color(0xFF15191C), Color(0xFF4A5560), Color(0xFF8794A0))
    val all = listOf(Forest, Ocean, Violet, Sunset, Rose, Graphite)
    fun byId(id: String): Palette = all.firstOrNull { it.id == id } ?: Forest
}

/**
 * Live design tokens. Every value is a getter over Compose state, so changing the theme,
 * accent, glass strength or corner roundness recomposes the whole app instantly.
 */
object FN {
    private var pal by mutableStateOf(Palettes.Forest)
    private var accentOv by mutableStateOf<Color?>(null)
    private var glassK by mutableFloatStateOf(1f)
    private var cornerDp by mutableIntStateOf(24)

    fun apply(s: Settings) {
        pal = Palettes.byId(s.themeId)
        accentOv = if (s.customAccent != 0) Color(s.customAccent) else null
        glassK = s.glass / 100f
        cornerDp = s.corner
    }

    val Bg: Color get() = pal.bg
    val BgTop: Color get() = pal.bgTop
    val Glow: Color get() = pal.glow
    val Mint: Color get() = pal.mint
    val Accent: Color get() = accentOv ?: pal.accent
    val AccentLight: Color get() = lerp(Accent, Color.White, 0.35f)
    val AccentDark: Color get() = lerp(Accent, Color.Black, 0.40f)
    val Ink: Color get() = lerp(pal.bg, Accent, 0.10f)
    val Tint: Color get() = accentOv?.let { lerp(pal.tint, lerp(it, Color.Black, 0.45f), 0.6f) } ?: pal.tint
    val Panel: Color get() = lerp(pal.bg, Tint, 0.30f)       // opaque surface for dialogs
    val Panel2: Color get() = lerp(pal.bg, Tint, 0.50f)
    val Border: Color get() = Mint.copy(alpha = 0.16f)
    val Muted: Color get() = Mint.copy(alpha = 0.68f)
    val Faint: Color get() = Mint.copy(alpha = 0.40f)

    val Shape: RoundedCornerShape get() = RoundedCornerShape((cornerDp * 0.67f).dp)
    val PanelShape: RoundedCornerShape get() = RoundedCornerShape(cornerDp.dp)
    val Pill = RoundedCornerShape(50)

    val AccentBrush: Brush get() = Brush.verticalGradient(listOf(AccentLight, Accent, AccentDark))
    val AccentBrushH: Brush get() = Brush.horizontalGradient(listOf(AccentDark, Accent, AccentLight))
    val GlassBrush: Brush
        get() = Brush.linearGradient(
            listOf(
                Tint.copy(alpha = (0.46f * glassK).coerceIn(0.06f, 0.95f)),
                lerp(pal.bg, Tint, 0.12f).copy(alpha = (0.78f * glassK).coerceIn(0.10f, 0.97f))
            )
        )
    /** Soft edge light only; no heavy outlines. */
    val GlassBorder: Brush
        get() = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.02f), Accent.copy(alpha = 0.20f)))
    val Sheen: Brush get() = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent))

    val SubjectColors = listOf(
        0xFF8BC53D, 0xFF5CC8E8, 0xFFF5B544, 0xFFA98BE0,
        0xFFE2F0CC, 0xFF3FBFA0, 0xFFE8788A, 0xFF9FB59A
    )
}

/**
 * Liquid-glass surface: tinted gradient body, a top-left specular highlight, a soft accent glow
 * in the opposite corner and a hairline edge light.
 */
fun Modifier.glass(shape: Shape = FN.PanelShape): Modifier = this
    .clip(shape)
    .background(FN.GlassBrush)
    .drawBehind {
        if (size.minDimension <= 0f) return@drawBehind
        drawRect(FN.Sheen, size = Size(size.width, size.height * 0.45f))
        drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent), Offset(size.width * 0.10f, 0f), size.maxDimension * 0.55f))
        drawRect(Brush.radialGradient(listOf(FN.Accent.copy(alpha = 0.12f), Color.Transparent), Offset(size.width, size.height), size.maxDimension * 0.50f))
    }
    .border(0.75.dp, FN.GlassBorder, shape)

private fun Typography.withFont(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f)
)

@Composable
fun FieldnoteTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val fonts = remember { loadFonts(ctx) }
    CompositionLocalProvider(LocalFonts provides fonts) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = FN.Accent, onPrimary = FN.Ink,
                secondary = FN.Mint, onSecondary = FN.Ink,
                background = FN.Bg, onBackground = FN.Mint,
                surface = FN.Panel, onSurface = FN.Mint,
                surfaceVariant = FN.Panel2, onSurfaceVariant = FN.Muted,
                outline = FN.Border, outlineVariant = FN.Border,
                surfaceContainer = FN.Panel, surfaceContainerHigh = FN.Panel, surfaceContainerHighest = FN.Panel2,
                surfaceContainerLow = FN.Panel, surfaceContainerLowest = FN.Bg
            ),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)
            ),
            typography = Typography().withFont(fonts.ui)
        ) {
            CompositionLocalProvider(LocalContentColor provides FN.Mint) {
                ProvideTextStyle(TextStyle(fontFamily = fonts.ui, color = FN.Mint), content)
            }
        }
    }
}
