package app.fieldnote.core

import app.fieldnote.data.StudySession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Long.toDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun minuteOfDay(ms: Long): Int {
    val t = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    return t.hour * 60 + t.minute
}

private val clockFmt = DateTimeFormatter.ofPattern("HH:mm")
fun fmtClock(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(clockFmt)

fun fmtHms(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

fun fmtMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

fun fmtHm(ms: Long): String {
    val m = (ms / 60000).coerceAtLeast(0)
    return if (m >= 60) "${m / 60}h ${"%02d".format(m % 60)}m" else "${m}m"
}

fun fmtHmShort(ms: Long): String {
    val m = (ms / 60000).coerceAtLeast(0)
    return "%d:%02d".format(m / 60, m % 60)
}

fun List<StudySession>.focusByDay(): Map<LocalDate, Long> =
    groupBy { it.startedAt.toDate() }.mapValues { e -> e.value.sumOf { it.focusMs } }

fun List<StudySession>.on(d: LocalDate): List<StudySession> = filter { it.startedAt.toDate() == d }

fun List<StudySession>.between(a: LocalDate, b: LocalDate): List<StudySession> =
    filter { val d = it.startedAt.toDate(); !d.isBefore(a) && !d.isAfter(b) }

fun streak(byDay: Map<LocalDate, Long>, today: LocalDate): Int {
    var d = today
    if ((byDay[d] ?: 0L) <= 0L) d = d.minusDays(1)
    var c = 0
    while ((byDay[d] ?: 0L) > 0L) { c++; d = d.minusDays(1) }
    return c
}

fun mondayOf(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value - 1).toLong())

fun csvOf(sessions: List<StudySession>, subjectName: (Long) -> String): String {
    val sb = StringBuilder("date,subject,start,end,focus_minutes,break_minutes,pauses,distractions,status,note\n")
    sessions.sortedBy { it.startedAt }.forEach {
        sb.append(it.startedAt.toDate()).append(',')
            .append('"').append(subjectName(it.subjectId).replace("\"", "\"\"")).append('"').append(',')
            .append(fmtClock(it.startedAt)).append(',').append(fmtClock(it.endedAt)).append(',')
            .append(it.focusMs / 60000).append(',').append(it.breakMs / 60000).append(',')
            .append(it.pauses).append(',').append(it.distractions).append(',').append(it.status).append(',')
            .append('"').append(it.note.replace("\"", "\"\"").replace("\n", " ")).append('"').append('\n')
    }
    return sb.toString()
}
