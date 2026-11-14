package app.fieldnote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.fieldnote.core.fmtHms
import app.fieldnote.ui.*
import app.fieldnote.widget.Widgets

enum class Dest(val label: String, val icon: ImageVector) {
    Overview("Overview", Icons.Outlined.Dashboard),
    Focus("Focus", Icons.Outlined.Timer),
    Planner("Planner", Icons.Outlined.Checklist),
    Analytics("Analytics", Icons.Outlined.BarChart),
    Groups("Groups", Icons.Outlined.Groups),
    Rankings("Rankings", Icons.Outlined.EmojiEvents),
    Library("Library", Icons.Outlined.MenuBook),
    Customize("Themes", Icons.Outlined.Palette),
    Settings("Settings", Icons.Outlined.Settings),
    More("More", Icons.Outlined.MoreHoriz)
}

class MainActivity : ComponentActivity() {
    private val requested = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        requested.value = intent?.getStringExtra("dest")
        val app = application as FieldnoteApp
        setContent { FieldnoteTheme { Root(app, requested) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requested.value = intent.getStringExtra("dest")
    }

    override fun onStop() {
        super.onStop()
        Widgets.refresh(this)
    }
}

private val primary = listOf(Dest.Overview, Dest.Focus, Dest.Planner, Dest.Analytics)
private val secondary = listOf(Dest.Groups, Dest.Rankings, Dest.Library, Dest.Customize, Dest.Settings)

@Composable
private fun Root(app: FieldnoteApp, requested: MutableState<String?>) {
    val ctx = LocalContext.current
    var dest by rememberSaveable { mutableStateOf(Dest.Overview) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(requested.value) {
        requested.value?.let { r -> Dest.values().firstOrNull { it.name == r }?.let { dest = it } }
        requested.value = null
    }
    BackHandler(enabled = dest != Dest.Overview) { dest = Dest.Overview }

    val s by app.settings.state.collectAsState()
    val hidden = s.hiddenTabs
    fun visible(d: Dest) = d == Dest.Settings || d == Dest.Customize || d.name !in hidden
    val prim = primary.filter { visible(it) }
    val sec = secondary.filter { visible(it) }

    Box(Modifier.fillMaxSize()) {
        GlassBackground(s)
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            val wide = maxWidth >= 720.dp
            CompositionLocalProvider(LocalWide provides wide) {
                if (wide) {
                    Row(Modifier.fillMaxSize()) {
                        Sidebar(app, dest, prim + sec, s.railExpanded) { dest = it }
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                            Box(Modifier.widthIn(max = 1240.dp).fillMaxSize()) { Content(app, dest, sec) { dest = it } }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) { Content(app, dest, sec) { dest = it } }
                        MiniTimer(app, dest) { dest = Dest.Focus }
                        BottomBar(dest, prim) { dest = it }
                    }
                }
            }
        }
    }
}

@Composable
private fun Content(app: FieldnoteApp, dest: Dest, more: List<Dest>, go: (Dest) -> Unit) {
    when (dest) {
        Dest.Overview -> OverviewScreen(app, go)
        Dest.Focus -> FocusScreen(app)
        Dest.Planner -> PlannerScreen(app)
        Dest.Analytics -> AnalyticsScreen(app)
        Dest.Groups -> GroupsScreen(app)
        Dest.Rankings -> RankingsScreen(app)
        Dest.Library -> LibraryScreen(app)
        Dest.Customize -> CustomizeScreen(app)
        Dest.Settings -> SettingsScreen(app)
        Dest.More -> MoreScreen(app, more, go)
    }
}

@Composable
private fun BottomBar(dest: Dest, items: List<Dest>, go: (Dest) -> Unit) {
    Row(
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
            .glass(RoundedCornerShape(28.dp)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (items + Dest.More).forEach { d ->
            val sel = d == dest || (d == Dest.More && dest in secondary)
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                    .background(if (sel) Brush.verticalGradient(listOf(FN.Accent.copy(alpha = 0.32f), FN.Accent.copy(alpha = 0.08f))) else SolidColor(Color.Transparent))
                    .clickable { go(d) }.padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(d.icon, d.label, tint = if (sel) FN.Accent else FN.Muted, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(2.dp))
                Text(d.label, fontSize = 10.5.sp, maxLines = 1, color = if (sel) FN.Accent else FN.Muted)
            }
        }
    }
}

@Composable
private fun Sidebar(app: FieldnoteApp, dest: Dest, items: List<Dest>, expanded: Boolean, go: (Dest) -> Unit) {
    if (expanded) {
        Column(
            Modifier.padding(16.dp).width(232.dp).glass(RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState()).padding(vertical = 22.dp)
        ) {
            Column(Modifier.padding(horizontal = 22.dp)) {
                Big("Fieldnote", 26.sp)
                Spacer(Modifier.height(2.dp))
                Label("Study · Plan · Focus", color = FN.Accent)
            }
            Spacer(Modifier.height(22.dp))
            items.forEach { d ->
                val sel = d == dest
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 2.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(if (sel) Brush.horizontalGradient(listOf(FN.Accent.copy(alpha = 0.32f), FN.Accent.copy(alpha = 0.04f))) else SolidColor(Color.Transparent))
                        .clickable { go(d) }.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(d.icon, null, tint = if (sel) FN.Accent else FN.Muted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(d.label, color = if (sel) FN.Mint else FN.Muted, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            MiniTimer(app, dest, tight = true) { go(Dest.Focus) }
        }
    } else {
        // Slim icon rail: takes little width and wraps its content instead of stretching full height.
        Column(
            Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp).width(84.dp).glass(RoundedCornerShape(30.dp))
                .verticalScroll(rememberScrollState()).padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(FN.AccentBrush), contentAlignment = Alignment.Center) {
                Text("F", color = FN.Ink, fontSize = 22.sp, fontFamily = LocalFonts.current.display)
            }
            Spacer(Modifier.height(14.dp))
            items.forEach { d ->
                val sel = d == dest
                Column(
                    Modifier.padding(horizontal = 8.dp, vertical = 2.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(if (sel) Brush.verticalGradient(listOf(FN.Accent.copy(alpha = 0.34f), FN.Accent.copy(alpha = 0.06f))) else SolidColor(Color.Transparent))
                        .clickable { go(d) }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(d.icon, d.label, tint = if (sel) FN.Accent else FN.Muted, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(d.label, fontSize = 10.5.sp, maxLines = 1, color = if (sel) FN.Mint else FN.Muted)
                }
            }
            Spacer(Modifier.height(10.dp))
            MiniTimer(app, dest, tight = true, compact = true) { go(Dest.Focus) }
        }
    }
}

@Composable
private fun MiniTimer(app: FieldnoteApp, dest: Dest, tight: Boolean = false, compact: Boolean = false, open: () -> Unit) {
    val t by app.engine.state.collectAsState()
    val now by rememberNow()
    if (!t.active || dest == Dest.Focus) return
    if (compact) {
        Column(
            Modifier.padding(horizontal = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FN.AccentBrush)
                .clickable(onClick = open).padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Mono(fmtHms(t.focusNow(now)), 11.sp, color = FN.Ink)
            if (!t.running) Text("paused", color = FN.Ink, fontSize = 9.sp)
        }
        return
    }
    Row(
        Modifier.padding(horizontal = if (tight) 12.dp else 16.dp, vertical = 4.dp).fillMaxWidth()
            .clip(FN.Pill).background(FN.AccentBrush).clickable(onClick = open).padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(app.subjectName(t.subjectId), Modifier.weight(1f), color = FN.Ink, fontSize = 12.sp, maxLines = 1)
        Mono(fmtHms(t.focusNow(now)), 16.sp, color = FN.Ink)
        if (!t.running) Text("  paused", color = FN.Ink, fontSize = 10.sp)
    }
}
