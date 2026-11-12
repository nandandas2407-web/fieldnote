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
    val serverUrl: String = ""
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
        serverUrl = sp.getString("server", "") ?: ""
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
            .apply()
        _state.value = n
    }
}
