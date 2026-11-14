package app.fieldnote.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val archived: Boolean = false
)

@Entity(tableName = "sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val focusMs: Long,
    val breakMs: Long,
    val pauses: Int,
    val distractions: Int,
    val status: String,
    val note: String = ""
)

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDate: Long?,          // epoch day
    val priority: Int = 1,       // 0 low, 1 medium, 2 high
    val done: Boolean = false,
    val repeatDaily: Boolean = false,
    val plannedMin: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val date: Long               // epoch day
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val strokes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE archived = 0 ORDER BY id")
    fun all(): Flow<List<Subject>>
    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int
    @Insert suspend fun insert(s: Subject): Long
    @Query("UPDATE subjects SET color = :c WHERE name = :n")
    suspend fun recolor(n: String, c: Int)
    @Query("UPDATE subjects SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
    @Query("SELECT * FROM subjects ORDER BY id")
    suspend fun allOnce(): List<Subject>
    @Query("DELETE FROM subjects")
    suspend fun clear()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun all(): Flow<List<StudySession>>
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    suspend fun allOnce(): List<StudySession>
    @Insert suspend fun insert(s: StudySession): Long
    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("DELETE FROM sessions")
    suspend fun clear()
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY done, priority DESC, createdAt")
    fun all(): Flow<List<Todo>>
    @Query("SELECT * FROM todos ORDER BY createdAt")
    suspend fun allOnce(): List<Todo>
    @Query("DELETE FROM todos")
    suspend fun clear()
    @Insert suspend fun insert(t: Todo): Long
    @Update suspend fun update(t: Todo)
    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("UPDATE todos SET dueDate = :to WHERE done = 0 AND dueDate IS NOT NULL AND dueDate <= :from")
    suspend fun moveUnfinished(from: Long, to: Long)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY date")
    fun all(): Flow<List<Exam>>
    @Query("SELECT * FROM exams ORDER BY date")
    suspend fun allOnce(): List<Exam>
    @Query("DELETE FROM exams")
    suspend fun clear()
    @Insert suspend fun insert(e: Exam): Long
    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun all(): Flow<List<Note>>
    @Query("SELECT * FROM notes ORDER BY updatedAt")
    suspend fun allOnce(): List<Note>
    @Query("DELETE FROM notes")
    suspend fun clear()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(n: Note): Long
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(
    entities = [Subject::class, StudySession::class, Todo::class, Exam::class, Note::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun sessionDao(): SessionDao
    abstract fun todoDao(): TodoDao
    abstract fun examDao(): ExamDao
    abstract fun noteDao(): NoteDao

    companion object {
        fun build(ctx: Context): AppDatabase =
            Room.databaseBuilder(ctx, AppDatabase::class.java, "fieldnote.db").build()
    }
}
