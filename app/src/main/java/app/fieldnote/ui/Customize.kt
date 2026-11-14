package app.fieldnote.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldnote.Dest
import app.fieldnote.FieldnoteApp
import app.fieldnote.data.Settings
import java.io.File
import kotlin.math.roundToInt

private val AccentChoices = listOf(
    0xFF8BC53D, 0xFF4FC3F7, 0xFFB388FF, 0xFFFFA64D,
    0xFFFF7FA8, 0xFFF5D76E, 0xFF3FBFA0, 0xFFE8788A
)

private val HideableTabs = listOf(
    Dest.Overview, Dest.Focus, Dest.Planner, Dest.Analytics, Dest.Groups, Dest.Rankings, Dest.Library
)

@Composable
private fun FnSlider(label: String, value: Int, range: IntRange, suffix: String, onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = FN.Mint, fontSize = 14.sp)
            Text("$value$suffix", color = FN.Muted, fontSize = 13.sp, fontFamily = LocalFonts.current.mono)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = FN.AccentLight, activeTrackColor = FN.Accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.14f)
            )
        )
    }
}

@Composable
private fun SwitchRow(title: String, body: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = FN.Mint, fontSize = 14.sp)
            if (body != null) Text(body, color = FN.Muted, fontSize = 12.sp)
        }
        FnSwitch(checked, onChange)
    }
}

@Composable
private fun ThemeSwatch(p: Palette, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(88.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(62.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(p.bgTop, p.tint, p.accent)))
                .then(if (selected) Modifier.border(2.dp, FN.Mint, CircleShape) else Modifier)
        )
        Spacer(Modifier.height(6.dp))
        Text(p.name, fontSize = 12.sp, color = if (selected) FN.Mint else FN.Muted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizeScreen(app: FieldnoteApp) {
    val wide = LocalWide.current
    val ctx = LocalContext.current
    val s by app.settings.state.collectAsState()
    val now by rememberNow()
    fun upd(f: (Settings) -> Settings) = app.settings.update(f)

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) app.io {
            try {
                ctx.contentResolver.openInputStream(uri)?.use { i ->
                    File(ctx.filesDir, "bg_custom.jpg").outputStream().use { o -> i.copyTo(o) }
                }
                upd { it.copy(bgImage = true, bgVersion = it.bgVersion + 1) }
            } catch (e: Exception) { /* unreadable image: keep the current background */ }
        }
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(if (wide) 28.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle("Make it yours", "Themes")

        Cols(wide, {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Panel(title = "Theme") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Palettes.all.forEach { p ->
                            ThemeSwatch(p, p.id == s.themeId && s.customAccent == 0) {
                                upd { it.copy(themeId = p.id, customAccent = 0) }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Label("Accent colour")
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccentChoices.forEach { c ->
                            val argb = Color(c).toArgb()
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(Color(c))
                                    .then(if (s.customAccent == argb) Modifier.border(2.dp, FN.Mint, CircleShape) else Modifier)
                                    .clickable { upd { it.copy(customAccent = argb) } }
                            )
                        }
                        Tag("Theme default", s.customAccent == 0, FN.Accent) { upd { it.copy(customAccent = 0) } }
                    }
                }

                Panel(title = "Background") {
                    Text(
                        "Choose a photo. It is always blurred so the glass panels stay readable.",
                        color = FN.Muted, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FnButton(
                            "Choose image",
                            { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            Modifier.weight(1f), icon = Icons.Outlined.Image
                        )
                        FnButton(
                            "Remove",
                            {
                                app.io { File(ctx.filesDir, "bg_custom.jpg").delete() }
                                upd { it.copy(bgImage = false, bgVersion = it.bgVersion + 1) }
                            },
                            Modifier.weight(1f), filled = false, enabled = s.bgImage
                        )
                    }
                    if (s.bgImage) {
                        Spacer(Modifier.height(10.dp))
                        FnSlider("Blur", s.bgBlur, 0..60, " dp") { v -> upd { it.copy(bgBlur = v) } }
                        FnSlider("Dim", s.bgDim, 0..85, " %") { v -> upd { it.copy(bgDim = v) } }
                    }
                }

                Panel(title = "Glass") {
                    FnSlider("Glass strength", s.glass, 40..160, " %") { v -> upd { it.copy(glass = v) } }
                    FnSlider("Corner roundness", s.corner, 12..36, " dp") { v -> upd { it.copy(corner = v) } }
                }
            }
        }, {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Panel(title = "Timer style") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimerStyle.values().forEach { st ->
                            Tag(st.label, st.id == s.timerStyle, FN.Accent) { upd { it.copy(timerStyle = st.id) } }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(FN.Shape).background(Color.Black.copy(alpha = 0.20f)).padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TimerFace(
                            TimerStyle.of(s.timerStyle), 3_000_000L + (now % 3_600_000L), false, 25, s.timerSeconds,
                            (200 * s.timerScale / 100f).dp, Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    SwitchRow("Show seconds", null, s.timerSeconds) { v -> upd { it.copy(timerSeconds = v) } }
                    FnSlider("Size", s.timerScale, 70..130, " %") { v -> upd { it.copy(timerScale = v) } }
                }

                Panel(title = "Navigation") {
                    SwitchRow(
                        "Expanded sidebar", "On tablets, show labels beside the icons instead of the slim rail.",
                        s.railExpanded
                    ) { v -> upd { it.copy(railExpanded = v) } }
                    Spacer(Modifier.height(10.dp))
                    Label("Visible tabs")
                    Spacer(Modifier.height(6.dp))
                    HideableTabs.forEach { d ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(d.icon, null, tint = FN.Muted, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(14.dp))
                            Text(d.label, Modifier.weight(1f), color = FN.Mint, fontSize = 14.sp)
                            FnSwitch(d.name !in s.hiddenTabs) { on ->
                                upd { it.copy(hiddenTabs = if (on) it.hiddenTabs - d.name else it.hiddenTabs + d.name) }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Themes and Settings always stay visible.", color = FN.Muted, fontSize = 11.sp)
                }

                FnButton(
                    "Reset look to defaults",
                    {
                        app.io { File(ctx.filesDir, "bg_custom.jpg").delete() }
                        upd {
                            it.copy(
                                themeId = "forest", customAccent = 0, bgImage = false, bgVersion = it.bgVersion + 1,
                                bgBlur = 28, bgDim = 45, glass = 100, corner = 24,
                                timerStyle = "digital", timerSeconds = true, timerScale = 100,
                                railExpanded = false, hiddenTabs = emptySet()
                            )
                        }
                    },
                    Modifier.fillMaxWidth(), filled = false, icon = Icons.Outlined.Restore
                )
            }
        })
    }
}
