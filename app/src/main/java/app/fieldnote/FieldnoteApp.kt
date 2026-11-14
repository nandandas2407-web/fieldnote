package app.fieldnote

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import app.fieldnote.core.TimerEngine
import app.fieldnote.data.*
import app.fieldnote.ui.FN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class FieldnoteApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var db: AppDatabase
    lateinit var settings: SettingsStore
    lateinit var engine: TimerEngine
    lateinit var subjects: StateFlow<List<Subject>>
    lateinit var sessions: StateFlow<List<StudySession>>
    lateinit var todos: StateFlow<List<Todo>>
    lateinit var exams: StateFlow<List<Exam>>
    lateinit var notes: StateFlow<List<Note>>

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.build(this)
        settings = SettingsStore(this)
        FN.apply(settings.state.value)
        scope.launch(Dispatchers.Main) { settings.state.collect { FN.apply(it) } }
        engine = TimerEngine(this, db, scope)
        subjects = db.subjectDao().all().stateIn(scope, SharingStarted.Eagerly, emptyList())
        sessions = db.sessionDao().all().stateIn(scope, SharingStarted.Eagerly, emptyList())
        todos = db.todoDao().all().stateIn(scope, SharingStarted.Eagerly, emptyList())
        exams = db.examDao().all().stateIn(scope, SharingStarted.Eagerly, emptyList())
        notes = db.noteDao().all().stateIn(scope, SharingStarted.Eagerly, emptyList())
        createChannels()
        io {
            if (db.subjectDao().count() == 0) {
                PALETTE.forEach { db.subjectDao().insert(Subject(name = it.first, color = Color(it.second).toArgb())) }
            }
            val sp = getSharedPreferences("meta", MODE_PRIVATE)
            if (!sp.getBoolean("palette_v2", false)) {
                PALETTE.forEach { db.subjectDao().recolor(it.first, Color(it.second).toArgb()) }
                sp.edit().putBoolean("palette_v2", true).apply()
            }
        }
        scope.launch {
            engine.state.map { Triple(it.active, it.running, it.subjectId) }.distinctUntilChanged()
                .collect { app.fieldnote.widget.Widgets.update(this@FieldnoteApp) }
        }
    }

    fun io(block: suspend () -> Unit) { scope.launch(Dispatchers.IO) { block() } }

    fun subjectName(id: Long): String = subjects.value.firstOrNull { it.id == id }?.name ?: "Deleted subject"

    fun toggleTodo(t: Todo) = io {
        db.todoDao().update(t.copy(done = !t.done))
        if (!t.done && t.repeatDaily) {
            db.todoDao().insert(t.copy(id = 0, done = false, dueDate = (t.dueDate ?: LocalDate.now().toEpochDay()) + 1))
        }
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_TIMER, "Focus timer", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_ALERT, "Focus alerts", NotificationManager.IMPORTANCE_HIGH))
    }

    companion object {
        val PALETTE = listOf("Physics" to 0xFF8BC53D, "Mathematics" to 0xFF5CC8E8, "Chemistry" to 0xFFF5B544, "English" to 0xFFA98BE0)
        const val CH_TIMER = "timer"
        const val CH_ALERT = "alerts"
    }
}
