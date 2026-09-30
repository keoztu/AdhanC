package com.salahtimesonly.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salahtimesonly.calc.Prayer
import com.salahtimesonly.data.Day
import com.salahtimesonly.data.Settings
import com.salahtimesonly.data.SettingsStore
import com.salahtimesonly.widget.TimesWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * One exact alarm is always armed for the next salah. When it fires, the receiver notifies and arms
 * the following one. Every system event that could invalidate it (reboot, time or zone change, app
 * update, permission change) re-arms it, and a periodic worker double-checks twice a day.
 */
object AlarmScheduler {
    private const val REQUEST = 7
    const val ACTION_PRAYER = "com.salahtimesonly.PRAYER_TIME"
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_AT = "at"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun schedule(context: Context, s: Settings, after: Instant = Instant.now()) {
        val am = context.getSystemService(AlarmManager::class.java)
        if (s.place == null || !s.setupDone) {
            am.cancel(pending(context, null, 0L))
            return
        }
        val next = Day(s).upcoming(after).firstOrNull() ?: return
        val at = next.second.toEpochMilli()
        val pi = pending(context, next.first, at)
        try {
            if (canScheduleExact(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun pending(context: Context, prayer: Prayer?, at: Long): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).setAction(ACTION_PRAYER)
        if (prayer != null) intent.putExtra(EXTRA_PRAYER, prayer.name).putExtra(EXTRA_AT, at)
        return PendingIntent.getBroadcast(
            context, REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Re-arm alarms and refresh the widget after any settings change. */
    suspend fun refreshAll(context: Context) {
        val s = SettingsStore(context).current()
        schedule(context, s)
        TimesWidget.refresh(context)
    }

    fun ensureWorker(context: Context) {
        val req = PeriodicWorkRequestBuilder<RescheduleWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("reschedule", ExistingPeriodicWorkPolicy.KEEP, req)
    }
}

internal fun BroadcastReceiver.launchAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try { block() } finally { pending.finish() }
    }
}

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_PRAYER) return
        val prayer = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
        val at = Instant.ofEpochMilli(intent.getLongExtra(AlarmScheduler.EXTRA_AT, 0L))
        val app = context.applicationContext
        launchAsync {
            val s = SettingsStore(app).current()
            val now = Instant.now()
            // Only announce if we are close to the actual time (not after a long delay or a clock change).
            if (prayer != null && s.place != null && s.notifications && Duration.between(at, now).abs() < Duration.ofMinutes(15)) {
                PrayerNotifier.announce(app, s, prayer, at)
            }
            val after = if (now.isAfter(at)) now else at
            AlarmScheduler.schedule(app, s, after.plusSeconds(1))
            TimesWidget.refresh(app)
        }
    }
}

class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        launchAsync { AlarmScheduler.refreshAll(app) }
    }
}

class RescheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        AlarmScheduler.refreshAll(applicationContext)
        return Result.success()
    }
}
