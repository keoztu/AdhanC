package com.salahtimesonly.schedule

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.salahtimesonly.MainActivity
import com.salahtimesonly.R
import com.salahtimesonly.calc.Prayer
import com.salahtimesonly.data.Day
import com.salahtimesonly.data.Settings
import com.salahtimesonly.data.localized
import java.time.Instant
import java.time.format.DateTimeFormatter

object Channels {
    const val PLAIN = "prayer_plain"
    const val FULL = "adhan_full"
    fun short(voice: Int) = "adhan_short_$voice"
    val VOICES = listOf(1, 2)

    fun adhanUri(context: Context, voice: Int, full: Boolean): Uri =
        Uri.parse("android.resource://${context.packageName}/raw/adhan_${voice}_${if (full) "full" else "short"}")

    fun ensure(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val list = mutableListOf(
            NotificationChannel(PLAIN, context.getString(R.string.channel_plain), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_plain_desc)
            },
            NotificationChannel(FULL, context.getString(R.string.channel_full), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_full_desc)
                setSound(null, null)
            },
        )
        for (v in VOICES) {
            list += NotificationChannel(short(v), context.getString(R.string.channel_short, v), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_short_desc)
                setSound(adhanUri(context, v, false), attrs)
            }
        }
        nm.createNotificationChannels(list)
    }

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (android.os.Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
}

object PrayerNotifier {
    const val NOTIFICATION_ID = 1001

    fun prayerName(p: Prayer): Int = when (p) {
        Prayer.FAJR -> R.string.p_fajr
        Prayer.SUNRISE -> R.string.p_sunrise
        Prayer.DHUHR -> R.string.p_dhuhr
        Prayer.ASR -> R.string.p_asr
        Prayer.MAGHRIB -> R.string.p_maghrib
        Prayer.ISHA -> R.string.p_isha
    }

    fun texts(context: Context, s: Settings, p: Prayer, at: Instant): Pair<String, String> {
        val ctx = context.localized(s)
        val name = ctx.getString(prayerName(p))
        val time = DateTimeFormatter.ofPattern("HH:mm").withZone(Day(s).zone).format(at)
        return "$name — $time" to ctx.getString(R.string.notif_entered, name)
    }

    fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun announce(context: Context, s: Settings, p: Prayer, at: Instant) {
        Channels.ensure(context)
        if (!Channels.canPost(context)) return
        val (title, body) = texts(context, s, p, at)
        if (s.adhanFor(p) && s.adhanFull && AdhanService.start(context, title, body, s.adhanVoice)) return
        val channel = if (s.adhanFor(p)) Channels.short(s.adhanVoice) else Channels.PLAIN
        post(context, channel, title, body)
    }

    fun post(context: Context, channel: String, title: String, body: String) {
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .build()
        if (Channels.canPost(context)) {
            try { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n) } catch (_: SecurityException) { }
        }
    }
}
