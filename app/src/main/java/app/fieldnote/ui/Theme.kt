package app.fieldnote.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object FN {
    val Bg = Color(0xFF011207)
    val Panel = Color(0xFF012F13)
    val Accent = Color(0xFF8BC53D)
    val Mint = Color(0xFFE2F0CC)
    // Derived neutrals (opacity/blends of the four brand colours)
    val Panel2 = Color(0xFF0A3B1C)
    val Border = Mint.copy(alpha = 0.14f)
    val Muted = Mint.copy(alpha = 0.62f)
    val Faint = Mint.copy(alpha = 0.36f)
    val Shape = RoundedCornerShape(4.dp)

    // Subject palette: greens and sage tones derived from the brand palette.
    val SubjectColors = listOf(
        0xFF8BC53D, 0xFFE2F0CC, 0xFF4F8A2B, 0xFFB7D98A,
        0xFF2E7D4F, 0xFF6FA38A, 0xFFC9D96B, 0xFF9FB59A
    )
}

@Composable
fun FieldnoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = FN.Accent, onPrimary = FN.Bg,
            secondary = FN.Mint, onSecondary = FN.Bg,
            background = FN.Bg, onBackground = FN.Mint,
            surface = FN.Panel, onSurface = FN.Mint,
            surfaceVariant = FN.Panel2, onSurfaceVariant = FN.Muted,
            outline = FN.Border, outlineVariant = FN.Border,
            surfaceContainer = FN.Panel, surfaceContainerHigh = FN.Panel, surfaceContainerHighest = FN.Panel2,
            surfaceContainerLow = FN.Panel, surfaceContainerLowest = FN.Bg
        ),
        shapes = Shapes(
            extraSmall = FN.Shape, small = FN.Shape, medium = FN.Shape,
            large = FN.Shape, extraLarge = FN.Shape
        ),
        content = content
    )
}
