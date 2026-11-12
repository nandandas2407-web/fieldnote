package app.fieldnote

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.fieldnote.core.fmtHms
import app.fieldnote.ui.*

enum class Dest(val label: String, val icon: ImageVector) {
    Overview("Overview", Icons.Outlined.Dashboard),
    Focus("Focus", Icons.Outlined.Timer),
    Planner("Planner", Icons.Outlined.Checklist),
    Analytics("Analytics", Icons.Outlined.BarChart),
    Groups("Groups", Icons.Outlined.Groups),
    Rankings("Rankings", Icons.Outlined.EmojiEvents),
    Library("Library", Icons.Outlined.MenuBook),
    Settings("Settings", Icons.Outlined.Settings),
    More("More", Icons.Outlined.MoreHoriz)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as FieldnoteApp
        setContent { FieldnoteTheme { Root(app) } }
    }
}

private val primary = listOf(Dest.Overview, Dest.Focus, Dest.Planner, Dest.Analytics)
private val secondary = listOf(Dest.Groups, Dest.Rankings, Dest.Library, Dest.Settings)

@Composable
private fun Root(app: FieldnoteApp) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var dest by rememberSaveable { mutableStateOf(Dest.Overview) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    BackHandler(enabled = dest != Dest.Overview) { dest = Dest.Overview }

    BoxWithConstraints(Modifier.fillMaxSize().background(FN.Bg)) {
        val wide = maxWidth >= 720.dp
        CompositionLocalProvider(LocalWide provides wide) {
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    Sidebar(app, dest) { dest = it }
                    Box(Modifier.weight(1f).fillMaxHeight()) { Content(app, dest) { dest = it } }
                }
            } else {
                Scaffold(
                    containerColor = FN.Bg,
                    bottomBar = {
                        Column {
                            MiniTimer(app, dest) { dest = Dest.Focus }
                            NavigationBar(containerColor = FN.Panel, contentColor = FN.Mint) {
                                (primary + Dest.More).forEach { d ->
                                    val selected = d == dest || (d == Dest.More && (dest in secondary || dest == Dest.More))
                                    NavigationBarItem(
                                        selected = selected, onClick = { dest = d },
                                        icon = { Icon(d.icon, d.label) },
                                        label = { Text(d.label, fontSize = 11.sp, maxLines = 1) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = FN.Accent, selectedTextColor = FN.Accent,
                                            indicatorColor = FN.Accent.copy(alpha = 0.16f),
                                            unselectedIconColor = FN.Muted, unselectedTextColor = FN.Muted
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { pad -> Box(Modifier.padding(pad).fillMaxSize()) { Content(app, dest) { dest = it } } }
            }
        }
    }
}

@Composable
private fun Content(app: FieldnoteApp, dest: Dest, go: (Dest) -> Unit) {
    when (dest) {
        Dest.Overview -> OverviewScreen(app, go)
        Dest.Focus -> FocusScreen(app)
        Dest.Planner -> PlannerScreen(app)
        Dest.Analytics -> AnalyticsScreen(app)
        Dest.Groups -> GroupsScreen(app)
        Dest.Rankings -> RankingsScreen(app)
        Dest.Library -> LibraryScreen(app)
        Dest.Settings -> SettingsScreen(app)
        Dest.More -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenTitle("Workspace", "More")
            secondary.forEach { d ->
                Row(
                    Modifier.fillMaxWidth().background(FN.Panel).border(1.dp, FN.Border, FN.Shape)
                        .clickable { go(d) }.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(d.icon, null, tint = FN.Accent)
                    Spacer(Modifier.width(16.dp))
                    Text(d.label, color = FN.Mint, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun Sidebar(app: FieldnoteApp, dest: Dest, go: (Dest) -> Unit) {
    Column(
        Modifier.width(232.dp).fillMaxHeight().background(FN.Panel).border(1.dp, FN.Border).padding(vertical = 24.dp)
    ) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Big("FIELDNOTE", 20.sp)
            Spacer(Modifier.height(4.dp))
            val d = java.time.LocalDate.now()
            Label("${d.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())} · ${d.dayOfMonth} ${d.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())}")
        }
        Spacer(Modifier.height(24.dp))
        Label("Workspace", Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(8.dp))
        (primary + secondary).forEach { d ->
            val sel = d == dest
            Row(
                Modifier.fillMaxWidth().background(if (sel) FN.Accent.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { go(d) }.padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(3.dp).height(18.dp).background(if (sel) FN.Accent else androidx.compose.ui.graphics.Color.Transparent))
                Spacer(Modifier.width(12.dp))
                Icon(d.icon, null, tint = if (sel) FN.Accent else FN.Muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(d.label, color = if (sel) FN.Mint else FN.Muted, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        MiniTimer(app, dest) { go(Dest.Focus) }
    }
}

@Composable
private fun MiniTimer(app: FieldnoteApp, dest: Dest, open: () -> Unit) {
    val t by app.engine.state.collectAsState()
    val now by rememberNow()
    if (!t.active || dest == Dest.Focus) return
    Row(
        Modifier.fillMaxWidth().background(FN.Accent).clickable(onClick = open).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(app.subjectName(t.subjectId).uppercase(), Modifier.weight(1f), color = FN.Bg, fontSize = 11.sp, letterSpacing = 1.sp, maxLines = 1)
        Mono(fmtHms(t.focusNow(now)), 16.sp, color = FN.Bg)
        if (!t.running) Text("  PAUSED", color = FN.Bg, fontSize = 10.sp)
    }
}
