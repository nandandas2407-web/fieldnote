package app.fieldnote.core

import androidx.room.withTransaction
import app.fieldnote.FieldnoteApp
import app.fieldnote.data.Exam
import app.fieldnote.data.Note
import app.fieldnote.data.StudySession
import app.fieldnote.data.Subject
import app.fieldnote.data.Todo
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Whole-history export and import as a single JSON file.
 *
 * Format (version 1): { format, version, exportedAt, subjects[], sessions[], todos[], exams[], notes[], settings{} }.
 * Ids inside the file are only used to link sessions to subjects; they are re-created on import.
 */
object Backup {
    const val FORMAT = "fieldnote-backup"
    const val VERSION = 1

    class Summary(
        val subjects: Int, val sessions: Int, val todos: Int, val exams: Int, val notes: Int,
        val hasSettings: Boolean, val exportedAt: Long
    )

    class Result(val subjects: Int, val sessions: Int, val todos: Int, val exams: Int, val notes: Int, val skipped: Int)

    // ───────────────────────── export ─────────────────────────

    suspend fun export(app: FieldnoteApp): String {
        val db = app.db
        val now = System.currentTimeMillis()
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("exportedAt", now)
        root.put("exportedAtIso", Instant.ofEpochMilli(now).toString())

        root.put("subjects", JSONArray().also { a ->
            db.subjectDao().allOnce().forEach {
                a.put(JSONObject().put("id", it.id).put("name", it.name).put("color", it.color).put("archived", it.archived))
            }
        })
        root.put("sessions", JSONArray().also { a ->
            db.sessionDao().allOnce().sortedBy { it.startedAt }.forEach {
                a.put(
                    JSONObject().put("id", it.id).put("subjectId", it.subjectId)
                        .put("startedAt", it.startedAt).put("startedAtIso", Instant.ofEpochMilli(it.startedAt).toString())
                        .put("endedAt", it.endedAt).put("focusMs", it.focusMs).put("breakMs", it.breakMs)
                        .put("pauses", it.pauses).put("distractions", it.distractions)
                        .put("status", it.status).put("note", it.note)
                )
            }
        })
        root.put("todos", JSONArray().also { a ->
            db.todoDao().allOnce().forEach {
                a.put(
                    JSONObject().put("title", it.title).put("dueDate", it.dueDate ?: JSONObject.NULL)
                        .put("priority", it.priority).put("done", it.done).put("repeatDaily", it.repeatDaily)
                        .put("plannedMin", it.plannedMin).put("createdAt", it.createdAt)
                )
            }
        })
        root.put("exams", JSONArray().also { a ->
            db.examDao().allOnce().forEach { a.put(JSONObject().put("name", it.name).put("date", it.date)) }
        })
        root.put("notes", JSONArray().also { a ->
            db.noteDao().allOnce().forEach {
                a.put(JSONObject().put("title", it.title).put("body", it.body).put("strokes", it.strokes).put("updatedAt", it.updatedAt))
            }
        })
        val s = app.settings.state.value
        root.put(
            "settings",
            JSONObject()
                .put("userName", s.userName).put("dailyGoalMin", s.dailyGoalMin)
                .put("workMin", s.workMin).put("breakMin", s.breakMin)
                .put("blockerEnabled", s.blockerEnabled).put("allowed", JSONArray(s.allowed.toList()))
                .put("holdSeconds", s.holdSeconds).put("overlayEnabled", s.overlayEnabled)
                .put("themeId", s.themeId).put("customAccent", s.customAccent)
                .put("bgBlur", s.bgBlur).put("bgDim", s.bgDim).put("glass", s.glass).put("corner", s.corner)
                .put("timerStyle", s.timerStyle).put("timerSeconds", s.timerSeconds).put("timerScale", s.timerScale)
                .put("railExpanded", s.railExpanded).put("hiddenTabs", JSONArray(s.hiddenTabs.toList()))
        )
        return root.toString(2)
    }

    // ───────────────────────── parse / preview ─────────────────────────

    /** Parses and validates a backup file. Throws with a readable message when it is not a Fieldnote backup. */
    fun parse(text: String): JSONObject {
        val root = try { JSONObject(text) } catch (e: Exception) { throw IllegalArgumentException("not valid JSON") }
        if (root.optString("format") != FORMAT) throw IllegalArgumentException("this is not a Fieldnote backup")
        val v = root.optInt("version", 0)
        if (v < 1 || v > VERSION) throw IllegalArgumentException("unsupported backup version $v")
        return root
    }

    fun summary(root: JSONObject) = Summary(
        subjects = root.optJSONArray("subjects")?.length() ?: 0,
        sessions = root.optJSONArray("sessions")?.length() ?: 0,
        todos = root.optJSONArray("todos")?.length() ?: 0,
        exams = root.optJSONArray("exams")?.length() ?: 0,
        notes = root.optJSONArray("notes")?.length() ?: 0,
        hasSettings = root.optJSONObject("settings") != null,
        exportedAt = root.optLong("exportedAt", 0L)
    )

    // ───────────────────────── import ─────────────────────────

    private inline fun JSONArray?.forEachObj(f: (JSONObject) -> Unit) {
        if (this == null) return
        for (i in 0 until length()) optJSONObject(i)?.let(f)
    }

    /**
     * @param replace true: erase current data and restore the file (including preferences).
     *                false: merge, adding only what is missing and keeping everything already on the device.
     */
    suspend fun import(app: FieldnoteApp, root: JSONObject, replace: Boolean): Result {
        val db = app.db
        var nSub = 0; var nSes = 0; var nTodo = 0; var nExam = 0; var nNote = 0; var skipped = 0

        db.withTransaction {
            if (replace) {
                db.sessionDao().clear(); db.todoDao().clear(); db.examDao().clear()
                db.noteDao().clear(); db.subjectDao().clear()
            }

            // Subjects: match by name so merging never duplicates "Physics".
            val subjects = db.subjectDao().allOnce().toMutableList()
            val idMap = HashMap<Long, Long>()
            root.optJSONArray("subjects").forEachObj { o ->
                val name = o.optString("name").trim()
                if (name.isEmpty()) return@forEachObj
                val old = o.optLong("id", -1L)
                val match = subjects.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (match != null) {
                    idMap[old] = match.id
                } else {
                    val sub = Subject(name = name, color = o.optInt("color", 0xFF8BC53D.toInt()), archived = o.optBoolean("archived", false))
                    val id = db.subjectDao().insert(sub)
                    subjects += sub.copy(id = id)
                    idMap[old] = id
                    nSub++
                }
            }
            var fallback: Long? = null
            suspend fun fallbackSubject(): Long {
                fallback?.let { return it }
                val existing = subjects.firstOrNull { it.name.equals("Imported", ignoreCase = true) }
                val id = existing?.id ?: db.subjectDao().insert(Subject(name = "Imported", color = 0xFF9FB59A.toInt()))
                fallback = id
                return id
            }

            val sessionKeys = db.sessionDao().allOnce().map { Triple(it.startedAt, it.endedAt, it.subjectId) }.toHashSet()
            root.optJSONArray("sessions").forEachObj { o ->
                val start = o.optLong("startedAt", 0L)
                val end = o.optLong("endedAt", 0L)
                if (start <= 0L || end < start) { skipped++; return@forEachObj }
                val sid = idMap[o.optLong("subjectId", -1L)] ?: fallbackSubject()
                if (!sessionKeys.add(Triple(start, end, sid))) { skipped++; return@forEachObj }
                db.sessionDao().insert(
                    StudySession(
                        subjectId = sid, startedAt = start, endedAt = end,
                        focusMs = o.optLong("focusMs", 0L).coerceAtLeast(0L), breakMs = o.optLong("breakMs", 0L).coerceAtLeast(0L),
                        pauses = o.optInt("pauses", 0), distractions = o.optInt("distractions", 0),
                        status = o.optString("status", "completed").ifBlank { "completed" }, note = o.optString("note", "")
                    )
                )
                nSes++
            }

            val todoKeys = db.todoDao().allOnce().map { Triple(it.title, it.dueDate, it.createdAt) }.toHashSet()
            root.optJSONArray("todos").forEachObj { o ->
                val title = o.optString("title").trim()
                if (title.isEmpty()) return@forEachObj
                val due = if (o.isNull("dueDate")) null else o.optLong("dueDate")
                val created = o.optLong("createdAt", System.currentTimeMillis())
                if (!todoKeys.add(Triple(title, due, created))) { skipped++; return@forEachObj }
                db.todoDao().insert(
                    Todo(
                        title = title, dueDate = due, priority = o.optInt("priority", 1).coerceIn(0, 2),
                        done = o.optBoolean("done", false), repeatDaily = o.optBoolean("repeatDaily", false),
                        plannedMin = o.optInt("plannedMin", 0).coerceAtLeast(0), createdAt = created
                    )
                )
                nTodo++
            }

            val examKeys = db.examDao().allOnce().map { it.name to it.date }.toHashSet()
            root.optJSONArray("exams").forEachObj { o ->
                val name = o.optString("name").trim()
                val date = o.optLong("date", Long.MIN_VALUE)
                if (name.isEmpty() || date == Long.MIN_VALUE) return@forEachObj
                if (!examKeys.add(name to date)) { skipped++; return@forEachObj }
                db.examDao().insert(Exam(name = name, date = date))
                nExam++
            }

            val noteKeys = db.noteDao().allOnce().map { it.title to it.updatedAt }.toHashSet()
            root.optJSONArray("notes").forEachObj { o ->
                val title = o.optString("title")
                val updated = o.optLong("updatedAt", System.currentTimeMillis())
                if (!noteKeys.add(title to updated)) { skipped++; return@forEachObj }
                db.noteDao().upsert(Note(title = title, body = o.optString("body", ""), strokes = o.optString("strokes", ""), updatedAt = updated))
                nNote++
            }
        }

        if (replace) root.optJSONObject("settings")?.let { applySettings(app, it) }
        return Result(nSub, nSes, nTodo, nExam, nNote, skipped)
    }

    private fun JSONArray?.strings(): Set<String>? =
        this?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf { v -> v.isNotEmpty() } }.toSet() }

    private fun applySettings(app: FieldnoteApp, o: JSONObject) {
        app.settings.update { c ->
            c.copy(
                userName = o.optString("userName", c.userName).ifBlank { c.userName },
                dailyGoalMin = o.optInt("dailyGoalMin", c.dailyGoalMin).coerceIn(60, 960),
                workMin = o.optInt("workMin", c.workMin).coerceIn(5, 90),
                breakMin = o.optInt("breakMin", c.breakMin).coerceIn(1, 30),
                blockerEnabled = o.optBoolean("blockerEnabled", c.blockerEnabled),
                allowed = o.optJSONArray("allowed").strings() ?: c.allowed,
                holdSeconds = o.optInt("holdSeconds", c.holdSeconds).coerceIn(1, 15),
                overlayEnabled = o.optBoolean("overlayEnabled", c.overlayEnabled),
                themeId = o.optString("themeId", c.themeId),
                customAccent = o.optInt("customAccent", c.customAccent),
                bgBlur = o.optInt("bgBlur", c.bgBlur).coerceIn(0, 60),
                bgDim = o.optInt("bgDim", c.bgDim).coerceIn(0, 85),
                glass = o.optInt("glass", c.glass).coerceIn(40, 160),
                corner = o.optInt("corner", c.corner).coerceIn(12, 36),
                timerStyle = o.optString("timerStyle", c.timerStyle),
                timerSeconds = o.optBoolean("timerSeconds", c.timerSeconds),
                timerScale = o.optInt("timerScale", c.timerScale).coerceIn(70, 130),
                railExpanded = o.optBoolean("railExpanded", c.railExpanded),
                hiddenTabs = o.optJSONArray("hiddenTabs").strings() ?: c.hiddenTabs
            )
        }
    }
}
