package app.fieldnote

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import app.fieldnote.core.TimerEngine
import app.fieldnote.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
        engine = TimerEngine(this, db, scope)
        subjects = db.subjectDao().all().stateIn(scope, SharingStarted.Eagerly, emptyList())
        sessions = db.sessionDao().all().stateIn(scope, SharingStarted.Eagerly, emptyList())
        todos = db.todoDao().all().stateIn(scope, SharingStarted.Eagerly, emptyList())
        exams = db.examDao().all().stateIn(scope, SharingStarted.Eagerly, emptyList())
        notes = db.noteDao().all().stateIn(scope, SharingStarted.Eagerly, emptyList())
        createChannels()
        io {
            if (db.subjectDao().count() == 0) {
                listOf("Physics" to 0xFF8BC53D, "Mathematics" to 0xFFE2F0CC, "Chemistry" to 0xFF4F8A2B, "English" to 0xFFB7D98A)
                    .forEach { db.subjectDao().insert(Subject(name = it.first, color = Color(it.second).toArgb())) }
            }
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
        const val CH_TIMER = "timer"
        const val CH_ALERT = "alerts"
    }
}
