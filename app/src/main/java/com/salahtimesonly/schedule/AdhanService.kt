package com.salahtimesonly.schedule

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.salahtimesonly.R

/** Plays the full Adhan in the foreground with a Stop action. Respects silent mode and Do Not Disturb. */
class AdhanService : Service() {
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private var title = ""
    private var body = ""
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            if (started) finish() else stopSelf()
            return START_NOT_STICKY
        }
        if (started) return START_NOT_STICKY
        started = true
        title = intent?.getStringExtra(EXTRA_TITLE) ?: ""
        body = intent?.getStringExtra(EXTRA_BODY) ?: ""
        val voice = intent?.getIntExtra(EXTRA_VOICE, 1) ?: 1
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, PrayerNotifier.NOTIFICATION_ID, notification(playing = true), type)
        if (!audible()) { finish(); return START_NOT_STICKY }
        play(voice)
        return START_NOT_STICKY
    }

    private fun audible(): Boolean {
        val am = getSystemService(AudioManager::class.java)
        val nm = getSystemService(NotificationManager::class.java)
        val filter = nm.currentInterruptionFilter
        return am.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
            (filter == NotificationManager.INTERRUPTION_FILTER_ALL || filter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN)
    }

    private fun play(voice: Int) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val am = getSystemService(AudioManager::class.java)
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) finish()
            }
            .build()
        focus = req
        if (am.requestAudioFocus(req) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { finish(); return }
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(this@AdhanService, Channels.adhanUri(this@AdhanService, voice, full = true))
                setOnCompletionListener { finish() }
                setOnErrorListener { _, _, _ -> finish(); true }
                prepare()
                start()
            }
        } catch (e: Exception) {
            finish()
        }
    }

    private fun notification(playing: Boolean) = NotificationCompat.Builder(this, Channels.FULL)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(PrayerNotifier.openApp(this))
        .setAutoCancel(!playing)
        .setOngoing(false)
        .apply {
            if (playing) {
                setDeleteIntent(stopIntent(this@AdhanService))
                addAction(0, getString(R.string.stop_adhan), stopIntent(this@AdhanService))
            }
        }
        .build()

    private var finished = false
    private fun finish() {
        if (finished) return
        finished = true
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        if (Channels.canPost(this)) {
            try { getSystemService(NotificationManager::class.java).notify(PrayerNotifier.NOTIFICATION_ID, notification(playing = false)) } catch (_: SecurityException) { }
        }
        stopSelf()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.salahtimesonly.STOP_ADHAN"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_BODY = "body"
        private const val EXTRA_VOICE = "voice"

        private fun stopIntent(context: Context): PendingIntent = PendingIntent.getService(
            context, 3, Intent(context, AdhanService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /** Returns false if the system refuses a foreground start, so the caller can fall back. */
        fun start(context: Context, title: String, body: String, voice: Int): Boolean = try {
            val i = Intent(context, AdhanService::class.java)
                .putExtra(EXTRA_TITLE, title).putExtra(EXTRA_BODY, body).putExtra(EXTRA_VOICE, voice)
            context.startForegroundService(i)
            true
        } catch (e: Exception) {
            false
        }
    }
}
