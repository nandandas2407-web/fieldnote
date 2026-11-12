package app.fieldnote.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
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
    Text(text.uppercase(), modifier, color = color, fontSize = 11.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun Big(text: String, size: TextUnit = 40.sp, modifier: Modifier = Modifier, color: Color = FN.Mint) {
    Text(text, modifier, color = color, fontSize = size, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal)
}

@Composable
fun Mono(text: String, size: TextUnit = 40.sp, modifier: Modifier = Modifier, color: Color = FN.Mint) {
    Text(text, modifier, color = color, fontSize = size, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light)
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier.fillMaxWidth().clip(FN.Shape).background(FN.Panel)
            .border(1.dp, FN.Border, FN.Shape).padding(16.dp)
    ) {
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
fun Cols(wide: Boolean, a: @Composable () -> Unit, b: @Composable () -> Unit, wa: Float = 1f, wb: Float = 1f) {
    if (wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
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
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text.uppercase(), fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
    }
    if (filled) {
        Button(
            onClick, modifier.heightIn(min = 48.dp), enabled = enabled, shape = FN.Shape,
            colors = ButtonDefaults.buttonColors(containerColor = FN.Accent, contentColor = FN.Bg, disabledContainerColor = FN.Faint),
            content = content
        )
    } else {
        OutlinedButton(
            onClick, modifier.heightIn(min = 48.dp), enabled = enabled, shape = FN.Shape,
            border = BorderStroke(1.dp, FN.Border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = FN.Mint),
            content = content
        )
    }
}

@Composable
fun Seg(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(FN.Shape).border(1.dp, FN.Border, FN.Shape)) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).background(if (sel) FN.Accent else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 12.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    o.uppercase(), fontSize = 11.sp, letterSpacing = 1.sp, maxLines = 1,
                    color = if (sel) FN.Bg else FN.Mint, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun Tag(text: String, selected: Boolean, color: Color = FN.Accent, onClick: () -> Unit) {
    Row(
        Modifier.clip(FN.Shape)
            .border(1.dp, if (selected) color else FN.Border, FN.Shape)
            .background(if (selected) color.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = FN.Mint)
    }
}

@Composable
fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, step: Int, min: Int, max: Int, suffix: String = "") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = FN.Mint, fontSize = 14.sp)
        IconButton(onClick = { onChange((value - step).coerceAtLeast(min)) }) { Icon(Icons.Outlined.Remove, "Decrease $label") }
        Text("$value$suffix", Modifier.widthIn(min = 56.dp), textAlign = TextAlign.Center, color = FN.Mint, fontFamily = FontFamily.Monospace)
        IconButton(onClick = { onChange((value + step).coerceAtMost(max)) }) { Icon(Icons.Outlined.Add, "Increase $label") }
    }
}

@Composable
fun FnField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true, minLines: Int = 1) {
    OutlinedTextField(
        value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = singleLine, minLines = minLines,
        shape = FN.Shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FN.Accent, unfocusedBorderColor = FN.Border,
            focusedLabelColor = FN.Accent, unfocusedLabelColor = FN.Muted,
            cursorColor = FN.Accent, focusedTextColor = FN.Mint, unfocusedTextColor = FN.Mint
        )
    )
}

@Composable
fun FnCheck(checked: Boolean, onChange: (Boolean) -> Unit) {
    Checkbox(
        checked, onChange,
        colors = CheckboxDefaults.colors(checkedColor = FN.Accent, checkmarkColor = FN.Bg, uncheckedColor = FN.Muted)
    )
}

@Composable
fun FnSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked, onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = FN.Bg, checkedTrackColor = FN.Accent,
            uncheckedThumbColor = FN.Muted, uncheckedTrackColor = FN.Panel2, uncheckedBorderColor = FN.Border
        )
    )
}

@Composable
fun Progress(fraction: Float, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val p by androidx.compose.animation.core.animateFloatAsState(
        if (go) fraction.coerceIn(0f, 1f) else 0f,
        androidx.compose.animation.core.tween(700), label = "progress"
    )
    Box(modifier.fillMaxWidth().height(height).background(FN.Mint.copy(alpha = 0.1f))) {
        Box(Modifier.fillMaxWidth(p).fillMaxHeight().background(FN.Accent))
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
            Big(title, 32.sp)
        }
        trailing?.invoke()
    }
}
