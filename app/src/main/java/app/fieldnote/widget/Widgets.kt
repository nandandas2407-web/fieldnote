package app.fieldnote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import app.fieldnote.FieldnoteApp
import app.fieldnote.MainActivity
import app.fieldnote.R
import app.fieldnote.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

object Widgets {
    fun refresh(ctx: Context) {
        val app = ctx.applicationContext as FieldnoteApp
        app.io { update(app) }
    }

    suspend fun update(app: FieldnoteApp) {
        val mgr = AppWidgetManager.getInstance(app)
        val focusIds = mgr.getAppWidgetIds(ComponentName(app, FocusWidget::class.java))
        val streakIds = mgr.getAppWidgetIds(ComponentName(app, StreakWidget::class.java))
        if (focusIds.isEmpty() && streakIds.isEmpty()) return

        val sessions = app.db.sessionDao().allOnce()
        val today = LocalDate.now()
        val byDay = sessions.focusByDay().toMutableMap()
        val t = app.engine.state.value
        val now = System.currentTimeMillis()
        val live = if (t.active) t.focusNow(now) else 0L
        byDay[today] = (byDay[today] ?: 0L) + live
        val todayMs = byDay[today] ?: 0L
        val goalMs = app.settings.state.value.dailyGoalMin * 60_000L
        val pct = (todayMs * 100 / goalMs.coerceAtLeast(1)).toInt().coerceIn(0, 100)
        val st = streak(byDay, today)

        for (id in focusIds) mgr.updateAppWidget(id, focusViews(app, t, now, todayMs, goalMs, pct, st))
        for (id in streakIds) mgr.updateAppWidget(id, streakViews(app, todayMs, goalMs, pct, st))
    }

    private fun openApp(ctx: Context, dest: String?, code: Int): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (dest != null) i.putExtra("dest", dest)
        return PendingIntent.getActivity(ctx, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun focusViews(app: FieldnoteApp, t: TimerState, now: Long, todayMs: Long, goalMs: Long, pct: Int, st: Int): RemoteViews {
        val v = RemoteViews(app.packageName, R.layout.widget_focus)
        v.setOnClickPendingIntent(R.id.w_root, openApp(app, null, 1))
        v.setTextViewText(R.id.w_streak, "$st day streak")
        v.setProgressBar(R.id.w_progress, 100, pct, false)
        v.setTextViewText(R.id.w_goal, "$pct% of ${fmtHm(goalMs)} goal")
        if (t.active) {
            val name = app.subjectName(t.subjectId)
            if (t.running) {
                v.setTextViewText(R.id.w_label, "FOCUSING")
                v.setViewVisibility(R.id.w_time, View.GONE)
                v.setViewVisibility(R.id.w_chrono, View.VISIBLE)
                v.setChronometer(R.id.w_chrono, SystemClock.elapsedRealtime() - t.focusNow(now), null, true)
            } else {
                v.setTextViewText(R.id.w_label, "ON BREAK")
                v.setViewVisibility(R.id.w_chrono, View.GONE)
                v.setViewVisibility(R.id.w_time, View.VISIBLE)
                v.setTextViewText(R.id.w_time, fmtHms(t.focusNow(now)))
            }
            v.setTextViewText(R.id.w_sub, name)
            v.setTextViewText(R.id.w_btn, if (t.running) "Pause" else "Resume")
            val toggle = PendingIntent.getForegroundService(
                app, 10, Intent(app, TimerService::class.java).setAction(TimerService.ACTION_TOGGLE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.w_btn, toggle)
        } else {
            v.setTextViewText(R.id.w_label, "TODAY")
            v.setViewVisibility(R.id.w_chrono, View.GONE)
            v.setViewVisibility(R.id.w_time, View.VISIBLE)
            v.setTextViewText(R.id.w_time, fmtHms(todayMs))
            v.setTextViewText(R.id.w_sub, "Ready when you are")
            v.setTextViewText(R.id.w_btn, "Start")
            v.setOnClickPendingIntent(R.id.w_btn, openApp(app, "Focus", 2))
        }
        return v
    }

    private fun streakViews(app: FieldnoteApp, todayMs: Long, goalMs: Long, pct: Int, st: Int): RemoteViews {
        val v = RemoteViews(app.packageName, R.layout.widget_streak)
        v.setOnClickPendingIntent(R.id.s_root, openApp(app, null, 3))
        v.setTextViewText(R.id.s_num, "$st")
        v.setTextViewText(R.id.s_unit, if (st == 1) "DAY STREAK" else "DAY STREAK")
        v.setTextViewText(R.id.s_today, "${fmtHm(todayMs)} today")
        v.setProgressBar(R.id.s_progress, 100, pct, false)
        return v
    }
}

class FocusWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pr = goAsync()
        val app = context.applicationContext as FieldnoteApp
        app.scope.launch(Dispatchers.IO) { try { Widgets.update(app) } finally { pr.finish() } }
    }
}

class StreakWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pr = goAsync()
        val app = context.applicationContext as FieldnoteApp
        app.scope.launch(Dispatchers.IO) { try { Widgets.update(app) } finally { pr.finish() } }
    }
}
