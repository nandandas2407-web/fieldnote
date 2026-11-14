package app.fieldnote.core

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.fieldnote.FieldnoteApp
import app.fieldnote.MainActivity
import app.fieldnote.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private lateinit var app: FieldnoteApp
    private var lastPkg: String? = null
    private var lastAlerted: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        app = application as FieldnoteApp
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> { if (app.engine.state.value.running) app.engine.pause() else app.engine.resume() }
            ACTION_FINISH -> app.engine.finish("")
        }
        val s = app.engine.state.value
        goForeground(build(s))
        if (!s.active) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (job == null) {
            job = scope.launch {
                launch {
                    app.engine.state.collect { st ->
                        if (!st.active) {
                            Overlay.hide(this@TimerService)
                            ServiceCompat.stopForeground(this@TimerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        } else {
                            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                            nm.notify(NOTIF_ID, build(st))
                        }
                    }
                }
                launch { while (true) { delay(1000); monitor() } }
            }
        }
        return START_STICKY
    }

    private fun goForeground(n: Notification) {
        ServiceCompat.startForeground(this, NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }

    private fun pi(action: String, code: Int): PendingIntent =
        PendingIntent.getService(
            this, code, Intent(this, TimerService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun build(s: TimerState): Notification {
        val name = app.subjects.value.firstOrNull { it.id == s.subjectId }?.name ?: "Focus"
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val b = NotificationCompat.Builder(this, FieldnoteApp.CH_TIMER)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(name)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        val now = System.currentTimeMillis()
        if (s.running) {
            b.setContentText("Focusing")
                .setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(now - s.focusNow(now))
                .addAction(0, "Pause", pi(ACTION_TOGGLE, 1))
        } else {
            b.setContentText(if (s.active) "On break · ${fmtHms(s.focusNow(now))} focused" else "Finishing")
                .setUsesChronometer(false)
                .setShowWhen(false)
                .addAction(0, "Resume", pi(ACTION_TOGGLE, 1))
        }
        b.addAction(0, "Finish", pi(ACTION_FINISH, 2))
        return b.build()
    }

    private fun monitor() {
        val s = app.engine.state.value
        val st = app.settings.state.value
        if (!s.running || !st.blockerEnabled || s.protectionOff || !Usage.granted(this)) { Overlay.hide(this); return }
        val pkg = Usage.foreground(this) ?: lastPkg ?: return
        lastPkg = pkg
        if (pkg == packageName || pkg in st.allowed || pkg in Usage.systemSafe(this)) {
            lastAlerted = null
            Overlay.hide(this)
            return
        }
        if (pkg != lastAlerted) {
            lastAlerted = pkg
            app.engine.addDistraction()
            val label = Usage.label(this, pkg)
            if (st.overlayEnabled && Overlay.canDraw(this)) {
                Overlay.show(this, label) { returnToSession() }
            } else {
                // No overlay permission: fall back to a heads-up notification.
                val open = PendingIntent.getActivity(
                    this, 3, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val n = NotificationCompat.Builder(this, FieldnoteApp.CH_ALERT)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle("Back to focus")
                    .setContentText("$label is not on your allowed list.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setTimeoutAfter(10_000)
                    .setContentIntent(open)
                    .build()
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ALERT, n)
            }
        }
    }

    /** Brings Fieldnote back to the front (allowed because an overlay window is visible). */
    private fun returnToSession() {
        // Start first, hide after: the visible overlay is what lets a background app launch an activity.
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("dest", "Focus")
        )
        Overlay.hide(this)
    }

    override fun onDestroy() {
        Overlay.hide(this)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIF_ID = 1001
        const val NOTIF_ALERT = 1002
        const val ACTION_TOGGLE = "app.fieldnote.TOGGLE"
        const val ACTION_FINISH = "app.fieldnote.FINISH"
    }
}
