package app.fieldnote.core

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.fieldnote.data.AppDatabase
import app.fieldnote.data.StudySession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Timer state is stored as wall-clock timestamps, so it survives process death and
 * device restarts of the app: elapsed time is always recomputed from the stored values.
 */
data class TimerState(
    val active: Boolean = false,
    val subjectId: Long = -1,
    val startedAt: Long = 0,
    val focusMs: Long = 0,
    val resumedAt: Long = 0,      // 0 when paused
    val breakMs: Long = 0,
    val breakStart: Long = 0,     // 0 when running
    val pauses: Int = 0,
    val distractions: Int = 0,
    val pomodoro: Boolean = false,
    val workMin: Int = 25,
    val breakMin: Int = 5,
    val protectionOff: Boolean = false
) {
    val running: Boolean get() = active && resumedAt > 0
    fun focusNow(now: Long): Long = focusMs + if (running) (now - resumedAt) else 0L
    fun breakNow(now: Long): Long = breakMs + if (active && !running && breakStart > 0) (now - breakStart) else 0L
}

class TimerEngine(
    private val ctx: Context,
    private val db: AppDatabase,
    private val scope: CoroutineScope
) {
    private val sp = ctx.getSharedPreferences("timer", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<TimerState> = _state

    private fun load() = TimerState(
        active = sp.getBoolean("active", false),
        subjectId = sp.getLong("subject", -1),
        startedAt = sp.getLong("startedAt", 0),
        focusMs = sp.getLong("focusMs", 0),
        resumedAt = sp.getLong("resumedAt", 0),
        breakMs = sp.getLong("breakMs", 0),
        breakStart = sp.getLong("breakStart", 0),
        pauses = sp.getInt("pauses", 0),
        distractions = sp.getInt("distractions", 0),
        pomodoro = sp.getBoolean("pomodoro", false),
        workMin = sp.getInt("workMin", 25),
        breakMin = sp.getInt("breakMin", 5),
        protectionOff = sp.getBoolean("protectionOff", false)
    )

    private fun save(s: TimerState) {
        sp.edit()
            .putBoolean("active", s.active).putLong("subject", s.subjectId)
            .putLong("startedAt", s.startedAt).putLong("focusMs", s.focusMs)
            .putLong("resumedAt", s.resumedAt).putLong("breakMs", s.breakMs)
            .putLong("breakStart", s.breakStart).putInt("pauses", s.pauses)
            .putInt("distractions", s.distractions).putBoolean("pomodoro", s.pomodoro)
            .putInt("workMin", s.workMin).putInt("breakMin", s.breakMin)
            .putBoolean("protectionOff", s.protectionOff)
            .apply()
        _state.value = s
    }

    private fun ensureService() {
        ContextCompat.startForegroundService(ctx, Intent(ctx, TimerService::class.java))
    }

    fun start(subjectId: Long, pomodoro: Boolean, workMin: Int, breakMin: Int) {
        val now = System.currentTimeMillis()
        save(TimerState(true, subjectId, now, 0, now, 0, 0, 0, 0, pomodoro, workMin, breakMin, false))
        ensureService()
    }

    fun pause() {
        val s = _state.value
        if (!s.running) return
        val now = System.currentTimeMillis()
        save(s.copy(focusMs = s.focusMs + (now - s.resumedAt), resumedAt = 0, breakStart = now, pauses = s.pauses + 1))
    }

    fun resume() {
        val s = _state.value
        if (!s.active || s.running) return
        val now = System.currentTimeMillis()
        save(s.copy(breakMs = s.breakMs + (now - s.breakStart), breakStart = 0, resumedAt = now))
    }

    fun addDistraction() { val s = _state.value; if (s.active) save(s.copy(distractions = s.distractions + 1)) }

    fun suspendProtection() { val s = _state.value; if (s.active) save(s.copy(protectionOff = true)) }

    fun finish(note: String) {
        val s = _state.value
        if (!s.active) return
        val now = System.currentTimeMillis()
        val focus = s.focusNow(now)
        val brk = s.breakNow(now)
        save(TimerState())
        if (focus >= 3000) {
            val session = StudySession(
                subjectId = s.subjectId, startedAt = s.startedAt, endedAt = now,
                focusMs = focus, breakMs = brk, pauses = s.pauses,
                distractions = s.distractions, status = "completed", note = note
            )
            scope.launch(Dispatchers.IO) { db.sessionDao().insert(session) }
        }
    }

    fun discard() { if (_state.value.active) save(TimerState()) }

    fun switchSubject(id: Long) {
        val s = _state.value
        if (!s.active || s.subjectId == id) return
        val pom = s.pomodoro; val w = s.workMin; val b = s.breakMin
        finish("")
        start(id, pom, w, b)
    }
}
