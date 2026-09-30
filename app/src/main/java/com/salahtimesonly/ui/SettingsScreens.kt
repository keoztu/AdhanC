package com.salahtimesonly.ui

import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.salahtimesonly.BuildConfig
import com.salahtimesonly.Config
import com.salahtimesonly.R
import com.salahtimesonly.calc.Method
import com.salahtimesonly.calc.Prayer
import com.salahtimesonly.data.Cities
import com.salahtimesonly.data.Settings
import com.salahtimesonly.schedule.AlarmScheduler
import com.salahtimesonly.schedule.Channels
import com.salahtimesonly.schedule.PrayerNotifier

@Composable
private fun Page(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SettingsScreen(s: Settings, model: AppModel, open: (Screen) -> Unit) {
    val place = s.place
    val notifSummary = when {
        !s.notifications -> stringResource(R.string.off)
        !s.adhan -> stringResource(R.string.summary_no_adhan)
        s.adhanFull -> stringResource(R.string.summary_full)
        else -> stringResource(R.string.summary_short)
    }
    Page {
        Spacer(Modifier.height(8.dp))
        ScreenTitle(stringResource(R.string.settings))

        SectionLabel(stringResource(R.string.s_location))
        Group {
            NavRow(place?.name ?: "", value = place?.let { Cities.countryName(it.cc, appLocale()) }) { open(Screen.LOCATION) }
        }

        SectionLabel(stringResource(R.string.s_method))
        Group {
            NavRow(stringResource(methodName(s.method)), sub = stringResource(R.string.fajr_isha, s.method.summary)) { open(Screen.METHOD) }
            Divider()
            NavRow(stringResource(R.string.s_asr),
                value = stringResource(if (s.asrFactor == 2) R.string.asr_hanafi else R.string.asr_standard)) { open(Screen.ASR) }
        }

        SectionLabel(stringResource(R.string.s_notifications))
        Group { NavRow(stringResource(R.string.prayer_notifications), value = notifSummary) { open(Screen.NOTIFICATIONS) } }

        SectionLabel(stringResource(R.string.appearance))
        Group {
            Segmented(
                listOf("system" to stringResource(R.string.theme_system), "light" to stringResource(R.string.theme_light), "dark" to stringResource(R.string.theme_dark)),
                s.theme, model::setTheme,
            )
        }

        SectionLabel(stringResource(R.string.language))
        Group {
            Segmented(
                listOf("system" to stringResource(R.string.lang_system), "en" to "English", "tr" to "Türkçe"),
                s.lang, model::setLang,
            )
        }

        Spacer(Modifier.height(24.dp))
        Group { NavRow(stringResource(R.string.about)) { open(Screen.ABOUT) } }
    }
}

@Composable
fun MethodScreen(s: Settings, onBack: () -> Unit, onSelect: (String) -> Unit) {
    Page {
        SubHeader(stringResource(R.string.s_method), onBack)
        Spacer(Modifier.height(14.dp))
        Group {
            Method.entries.forEachIndexed { i, m ->
                if (i > 0) Divider()
                RadioRow(stringResource(methodName(m)), sub = stringResource(R.string.fajr_isha, m.summary), selected = m == s.method) {
                    onSelect(m.key)
                }
            }
        }
        Note(stringResource(R.string.method_note))
    }
}

@Composable
fun AsrScreen(s: Settings, onBack: () -> Unit, onSelect: (Int) -> Unit) {
    Page {
        SubHeader(stringResource(R.string.s_asr), onBack)
        Spacer(Modifier.height(14.dp))
        Group {
            RadioRow(stringResource(R.string.asr_standard), stringResource(R.string.asr_standard_sub), s.asrFactor == 1) { onSelect(1) }
            Divider()
            RadioRow(stringResource(R.string.asr_hanafi), stringResource(R.string.asr_hanafi_sub), s.asrFactor == 2) { onSelect(2) }
        }
    }
}

private class AdhanPreview(private val context: Context) {
    private var player: MediaPlayer? = null

    fun play(voice: Int, full: Boolean, onEnd: () -> Unit) {
        stop()
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                setDataSource(context, Channels.adhanUri(context, voice, full))
                setOnCompletionListener { onEnd(); stop() }
                prepare()
                start()
            }
        }.getOrNull()
        if (player == null) onEnd()
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }
}

@Composable
fun NotificationsScreen(s: Settings, model: AppModel, onBack: () -> Unit) {
    val p = LocalPalette.current
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    val canPost = remember(tick) { Channels.canPost(context) }
    val exact = remember(tick) { AlarmScheduler.canScheduleExact(context) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        model.setNotifications(true)
        if (!granted) openNotificationSettings(context)
        tick++
    }
    val preview = remember { AdhanPreview(context) }
    var playing by remember { mutableStateOf<Int?>(null) }
    DisposableEffect(Unit) { onDispose { preview.stop() } }

    val adhanOn = s.notifications && s.adhan

    Page {
        SubHeader(stringResource(R.string.s_notifications), onBack)
        Spacer(Modifier.height(14.dp))
        Group {
            SwitchRow(stringResource(R.string.prayer_notifications), s.notifications) { v ->
                if (v && Build.VERSION.SDK_INT >= 33 && !canPost) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else model.setNotifications(v)
            }
            Divider()
            SwitchRow(stringResource(R.string.adhan), s.adhan, enabled = s.notifications, onChange = model::setAdhan)
        }
        if (s.notifications && !canPost) {
            Note(stringResource(R.string.notif_blocked))
            IconLabelButton(R.drawable.ic_settings, stringResource(R.string.open_settings)) { openNotificationSettings(context) }
        }

        SectionLabel(stringResource(R.string.adhan_length))
        Group {
            RadioRow(stringResource(R.string.short_label), stringResource(R.string.short_sub), !s.adhanFull, enabled = adhanOn) { model.setAdhanFull(false) }
            Divider()
            RadioRow(stringResource(R.string.full_label), stringResource(R.string.full_sub), s.adhanFull, enabled = adhanOn) { model.setAdhanFull(true) }
        }

        SectionLabel(stringResource(R.string.adhan_sound))
        Group {
            Channels.VOICES.forEachIndexed { i, v ->
                if (i > 0) Divider()
                RadioRow(
                    label = stringResource(if (v == 1) R.string.adhan_1 else R.string.adhan_2),
                    selected = s.adhanVoice == v,
                    enabled = adhanOn,
                    trailing = {
                        Text(
                            stringResource(if (playing == v) R.string.stop else R.string.preview),
                            color = p.accent, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) {
                                    if (playing == v) { preview.stop(); playing = null }
                                    else { playing = v; preview.play(v, s.adhanFull) { playing = null } }
                                }
                                .heightIn(min = 44.dp)
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                        )
                    },
                ) { model.setVoice(v) }
            }
        }

        SectionLabel(stringResource(R.string.per_prayer))
        Group {
            Prayer.SALAH.forEachIndexed { i, pr ->
                if (i > 0) Divider()
                SwitchRow(stringResource(PrayerNotifier.prayerName(pr)), s.perPrayer[pr] ?: true, enabled = adhanOn) { model.setPer(pr, it) }
            }
        }

        SectionLabel(stringResource(R.string.reliability))
        Group {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                NavRow(stringResource(R.string.exact_row),
                    value = stringResource(if (exact) R.string.exact_allowed else R.string.exact_allow)) { openExactAlarmSettings(context) }
                Divider()
            }
            NavRow(stringResource(R.string.battery_row), sub = stringResource(R.string.battery_sub)) { openBatterySettings(context) }
        }
        Note(stringResource(R.string.reliability_note))
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit, onPrivacy: () -> Unit) {
    val p = LocalPalette.current
    val context = LocalContext.current
    Page {
        SubHeader(null, onBack)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Image(painterResource(R.drawable.app_mark), null, Modifier.size(56.dp))
            Column {
                Text(stringResource(R.string.app_name), color = p.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), color = p.muted, fontSize = 13.sp)
            }
        }
        Text(stringResource(R.string.about_desc), color = p.text, fontSize = 16.sp, lineHeight = 23.sp,
            modifier = Modifier.padding(top = 18.dp))
        Spacer(Modifier.height(18.dp))
        Group {
            NavRow(stringResource(R.string.privacy), onClick = onPrivacy)
            Divider()
            NavRow(stringResource(R.string.contact)) { openUrl(context, "mailto:${Config.SUPPORT_EMAIL}") }
        }
        SectionLabel(stringResource(R.string.credits))
        Text(stringResource(R.string.credits_body), color = p.muted, fontSize = 13.sp, lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 4.dp))
        if (Config.DONATION_URL.isNotBlank()) {
            Spacer(Modifier.height(28.dp))
            Divider()
            Text(stringResource(R.string.support_body), color = p.muted, fontSize = 14.sp, lineHeight = 20.sp,
                modifier = Modifier.padding(top = 16.dp))
            IconLabelButton(R.drawable.ic_chevron_right, stringResource(R.string.support_title)) { openUrl(context, Config.DONATION_URL) }
        }
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val p = LocalPalette.current
    Page {
        SubHeader(stringResource(R.string.privacy), onBack)
        Spacer(Modifier.height(12.dp))
        for (id in listOf(R.string.privacy_1, R.string.privacy_2, R.string.privacy_3, R.string.privacy_4)) {
            Text(stringResource(id), color = p.text, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}
