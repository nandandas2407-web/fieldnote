package app.fieldnote.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val LocalWide = compositionLocalOf { false }

@Composable
fun rememberNow(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) { value = System.currentTimeMillis(); delay(500) }
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = FN.Muted) {
    Text(text.uppercase(), modifier, color = color, fontSize = 10.5.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun Big(text: String, size: TextUnit = 40.sp, modifier: Modifier = Modifier, color: Color = FN.Mint) {
    Text(text, modifier, color = color, fontSize = size, fontFamily = LocalFonts.current.display, fontWeight = FontWeight.Medium)
}

@Composable
fun Mono(text: String, size: TextUnit = 40.sp, modifier: Modifier = Modifier, color: Color = FN.Mint) {
    Text(text, modifier, color = color, fontSize = size, fontFamily = LocalFonts.current.mono, fontWeight = FontWeight.Light)
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth().glass(FN.PanelShape).padding(if (LocalWide.current) 22.dp else 18.dp)) {
        if (title != null || action != null) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (title != null) Label(title, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
                action?.invoke()
            }
        }
        content()
    }
}

@Composable
fun Cols(wide: Boolean, a: @Composable () -> Unit, b: @Composable () -> Unit, wa: Float = 1.3f, wb: Float = 1f) {
    if (wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(wa)) { a() }
            Box(Modifier.weight(wb)) { b() }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { a(); b() }
    }
}

@Composable
fun FnButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    filled: Boolean = true, icon: ImageVector? = null, enabled: Boolean = true
) {
    val shape = FN.Shape
    val fg = if (filled) FN.Ink else FN.Mint
    Row(
        modifier.heightIn(min = 50.dp).alpha(if (enabled) 1f else 0.4f).clip(shape)
            .background(if (filled) FN.AccentBrush else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.05f))))
            .drawBehind {
                if (filled) drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.Transparent), endY = size.height * 0.55f))
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = fg); Spacer(Modifier.width(8.dp)) }
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
    }
}

@Composable
fun Seg(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(FN.Pill).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.22f), Color.White.copy(alpha = 0.07f))))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).clip(FN.Pill)
                    .background(if (sel) FN.AccentBrush else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable(role = Role.Tab) { onSelect(i) }.padding(vertical = 10.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(o, fontSize = 13.sp, maxLines = 1, color = if (sel) FN.Ink else FN.Mint, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun Tag(text: String, selected: Boolean, color: Color = FN.Accent, onClick: () -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    Row(
        Modifier.clip(shape)
            .background(if (selected) Brush.linearGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.08f))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f))))
            .then(if (selected) Modifier.border(1.dp, color.copy(alpha = 0.75f), shape) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(9.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = FN.Mint, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, step: Int, min: Int, max: Int, suffix: String = "") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = FN.Mint, fontSize = 14.sp)
        IconButton(onClick = { onChange((value - step).coerceAtLeast(min)) }) { Icon(Icons.Outlined.Remove, "Decrease $label", tint = FN.Accent) }
        Text("$value$suffix", Modifier.widthIn(min = 60.dp), textAlign = TextAlign.Center, color = FN.Mint, fontFamily = LocalFonts.current.mono, fontSize = 14.sp)
        IconButton(onClick = { onChange((value + step).coerceAtMost(max)) }) { Icon(Icons.Outlined.Add, "Increase $label", tint = FN.Accent) }
    }
}

@Composable
fun FnField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true, minLines: Int = 1) {
    OutlinedTextField(
        value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = singleLine, minLines = minLines,
        shape = FN.Shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FN.Accent.copy(alpha = 0.8f), unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = FN.Accent, unfocusedLabelColor = FN.Muted,
            cursorColor = FN.Accent, focusedTextColor = FN.Mint, unfocusedTextColor = FN.Mint,
            focusedContainerColor = Color.White.copy(alpha = 0.10f), unfocusedContainerColor = Color.White.copy(alpha = 0.08f)
        )
    )
}

@Composable
fun FnCheck(checked: Boolean, onChange: (Boolean) -> Unit) {
    Checkbox(
        checked, onChange,
        colors = CheckboxDefaults.colors(checkedColor = FN.Accent, checkmarkColor = FN.Ink, uncheckedColor = FN.Muted)
    )
}

@Composable
fun FnSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked, onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = FN.Accent, checkedBorderColor = FN.AccentLight,
            uncheckedThumbColor = FN.Muted, uncheckedTrackColor = Color.White.copy(alpha = 0.08f), uncheckedBorderColor = FN.Border
        )
    )
}

@Composable
fun Progress(fraction: Float, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val p by animateFloatAsState(if (go) fraction.coerceIn(0f, 1f) else 0f, tween(800), label = "progress")
    Box(modifier.fillMaxWidth().height(height).clip(FN.Pill).background(Color.White.copy(alpha = 0.10f))) {
        Box(Modifier.fillMaxWidth(p).fillMaxHeight().clip(FN.Pill).background(FN.AccentBrushH))
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Big(title, 20.sp)
        Spacer(Modifier.height(6.dp))
        Text(body, color = FN.Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(14.dp)); action() }
    }
}

@Composable
fun ScreenTitle(kicker: String, title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Label(kicker, color = FN.Accent)
            Spacer(Modifier.height(4.dp))
            Big(title, 36.sp)
        }
        trailing?.invoke()
    }
}
