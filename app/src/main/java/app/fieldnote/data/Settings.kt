package app.fieldnote.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Settings(
    val dailyGoalMin: Int = 480,
    val workMin: Int = 25,
    val breakMin: Int = 5,
    val blockerEnabled: Boolean = false,
    val allowed: Set<String> = emptySet(),
    val holdSeconds: Int = 5,
    val serverUrl: String = "",
    val userName: String = "Student",
    // Floating pop-up when a blocked app is opened
    val overlayEnabled: Boolean = true,
    // Themes and customisation
    val themeId: String = "forest",
    val customAccent: Int = 0,            // 0 = use the theme's own accent
    val bgImage: Boolean = false,
    val bgVersion: Int = 0,
    val bgBlur: Int = 28,                 // dp, 0..60
    val bgDim: Int = 45,                  // percent, 0..85
    val glass: Int = 100,                 // percent, 40..160
    val corner: Int = 24,                 // dp, 12..36
    val timerStyle: String = "digital",
    val timerSeconds: Boolean = true,
    val timerScale: Int = 100,            // percent, 70..130
    val railExpanded: Boolean = false,
    val hiddenTabs: Set<String> = emptySet()
)

class SettingsStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<Settings> = _state

    private fun read() = Settings(
        dailyGoalMin = sp.getInt("goal", 480),
        workMin = sp.getInt("work", 25),
        breakMin = sp.getInt("break", 5),
        blockerEnabled = sp.getBoolean("blocker", false),
        allowed = sp.getStringSet("allowed", emptySet())?.toSet() ?: emptySet(),
        holdSeconds = sp.getInt("hold", 5),
        serverUrl = sp.getString("server", "") ?: "",
        userName = sp.getString("name", "Student") ?: "Student",
        overlayEnabled = sp.getBoolean("overlay", true),
        themeId = sp.getString("theme", "forest") ?: "forest",
        customAccent = sp.getInt("accent", 0),
        bgImage = sp.getBoolean("bgImage", false),
        bgVersion = sp.getInt("bgVersion", 0),
        bgBlur = sp.getInt("bgBlur", 28),
        bgDim = sp.getInt("bgDim", 45),
        glass = sp.getInt("glass", 100),
        corner = sp.getInt("corner", 24),
        timerStyle = sp.getString("timerStyle", "digital") ?: "digital",
        timerSeconds = sp.getBoolean("timerSeconds", true),
        timerScale = sp.getInt("timerScale", 100),
        railExpanded = sp.getBoolean("rail", false),
        hiddenTabs = sp.getStringSet("hiddenTabs", emptySet())?.toSet() ?: emptySet()
    )

    fun update(f: (Settings) -> Settings) {
        val n = f(_state.value)
        sp.edit()
            .putInt("goal", n.dailyGoalMin)
            .putInt("work", n.workMin)
            .putInt("break", n.breakMin)
            .putBoolean("blocker", n.blockerEnabled)
            .putStringSet("allowed", HashSet(n.allowed))
            .putInt("hold", n.holdSeconds)
            .putString("server", n.serverUrl)
            .putString("name", n.userName)
            .putBoolean("overlay", n.overlayEnabled)
            .putString("theme", n.themeId)
            .putInt("accent", n.customAccent)
            .putBoolean("bgImage", n.bgImage)
            .putInt("bgVersion", n.bgVersion)
            .putInt("bgBlur", n.bgBlur)
            .putInt("bgDim", n.bgDim)
            .putInt("glass", n.glass)
            .putInt("corner", n.corner)
            .putString("timerStyle", n.timerStyle)
            .putBoolean("timerSeconds", n.timerSeconds)
            .putInt("timerScale", n.timerScale)
            .putBoolean("rail", n.railExpanded)
            .putStringSet("hiddenTabs", HashSet(n.hiddenTabs))
            .apply()
        _state.value = n
    }
}
