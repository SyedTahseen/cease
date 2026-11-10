package com.freezer.io

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class AutoFreezeService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastHomeAt = 0L
    private val freezeTask = Runnable { runFreeze() }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val prefs = AppPrefs(context)
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF ->
                    if (prefs.autoMode == AutoMode.SCREEN_OFF) schedule(prefs.screenOffDelaySec * 1000L)
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> cancel()
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS ->
                    if (intent.getStringExtra("reason") == "homekey") onHomeKey(prefs.autoMode)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(), type)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (AppPrefs(this).autoMode == AutoMode.NEVER) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        cancel()
        runCatching { unregisterReceiver(receiver) }
        super.onDestroy()
    }

    private fun onHomeKey(mode: AutoMode) {
        val now = SystemClock.elapsedRealtime()
        when (mode) {
            AutoMode.HOME -> schedule(HOME_SETTLE_MS)
            AutoMode.DOUBLE_HOME ->
                if (now - lastHomeAt < DOUBLE_TAP_MS) { lastHomeAt = 0; schedule(HOME_SETTLE_MS) } else lastHomeAt = now
            else -> Unit
        }
    }

    private fun schedule(delayMs: Long) {
        cancel()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "freezer:auto").apply {
            acquire(delayMs + 60_000L)
        }
        handler.postDelayed(freezeTask, delayMs)
    }

    private fun cancel() {
        handler.removeCallbacks(freezeTask)
        releaseLock()
    }

    private fun releaseLock() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
    }

    private fun runFreeze() {
        val ctx = applicationContext
        Thread {
            runCatching { AutoFreezer.run(ctx) }
            handler.post { releaseLock() }
        }.start()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Auto Freeze", NotificationManager.IMPORTANCE_MIN),
        )
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Auto Freeze is on")
            .setContentText(AppPrefs(this).autoMode.label)
            .setOngoing(true)
            .build()

    companion object {
        private const val CHANNEL = "autofreeze"
        private const val NOTIF_ID = 1
        private const val HOME_SETTLE_MS = 700L
        private const val DOUBLE_TAP_MS = 500L

        fun sync(ctx: Context) {
            val app = ctx.applicationContext
            val intent = Intent(app, AutoFreezeService::class.java)
            runCatching {
                if (AppPrefs(app).autoMode == AutoMode.NEVER) app.stopService(intent)
                else ContextCompat.startForegroundService(app, intent)
            }
        }
    }
}
