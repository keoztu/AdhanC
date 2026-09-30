package com.salahtimesonly.ui

import android.graphics.Color as AColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahtimesonly.R
import com.salahtimesonly.SalahApp
import com.salahtimesonly.data.Place
import com.salahtimesonly.data.Settings
import com.salahtimesonly.data.SettingsStore
import com.salahtimesonly.schedule.AlarmScheduler
import kotlinx.coroutines.launch

enum class Screen(val tab: Boolean = false) {
    WELCOME, OB_LOCATION, OB_METHOD, OB_NOTIF,
    HOME(true), QIBLA(true), SETTINGS(true),
    LOCATION, METHOD, ASR, NOTIFICATIONS, ABOUT, PRIVACY,
}

/** Settings actions. Every change re-arms alarms and refreshes the widget. */
class AppModel(private val app: SalahApp) {
    val store = SettingsStore(app)

    private fun update(block: suspend SettingsStore.() -> Unit) {
        app.scope.launch {
            store.block()
            AlarmScheduler.refreshAll(app)
        }
    }

    fun setPlace(p: Place) = update { setPlace(p) }
    fun setMethod(key: String) = update { setMethod(key) }
    fun setAsr(f: Int) = update { setAsr(f) }
    fun setTheme(v: String) = update { setTheme(v) }
    fun setNotifications(v: Boolean) = update { setNotifications(v) }
    fun setAdhan(v: Boolean) = update { setAdhan(v) }
    fun setAdhanFull(v: Boolean) = update { setAdhanFull(v) }
    fun setVoice(v: Int) = update { setVoice(v) }
    fun setPer(p: com.salahtimesonly.calc.Prayer, v: Boolean) = update { setPer(p, v) }
    fun finishSetup(notifications: Boolean) = update {
        setNotifications(notifications)
        setSetupDone()
    }

    fun setLang(v: String) {
        app.scope.launch {
            store.setLang(v)
            AppCompatDelegate.setApplicationLocales(
                if (v == "system") LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(v)
            )
            AlarmScheduler.refreshAll(app)
        }
    }
}

@Composable
fun Root() {
    val context = LocalContext.current
    val model = remember { AppModel(context.applicationContext as SalahApp) }
    val settings by model.store.flow.collectAsStateWithLifecycle<Settings?>(initialValue = null)
    val s = settings ?: return

    val dark = when (s.theme) { "light" -> false; "dark" -> true; else -> isSystemInDarkTheme() }
    val activity = context as? ComponentActivity
    LaunchedEffect(dark) {
        val style = if (dark) SystemBarStyle.dark(AColor.TRANSPARENT)
        else SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT)
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    SalahTheme(dark) {
        val p = LocalPalette.current
        var stack by rememberSaveable {
            mutableStateOf(listOf(if (s.setupDone && s.place != null) Screen.HOME else Screen.WELCOME))
        }
        fun push(x: Screen) { stack = stack + x }
        fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
        fun root(x: Screen) { stack = listOf(x) }

        val current = stack.last()
        BackHandler(enabled = stack.size > 1) { pop() }

        Column(
            Modifier
                .fillMaxSize()
                .background(p.bg)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Box(Modifier.weight(1f)) {
                when (current) {
                    Screen.WELCOME -> WelcomeScreen(s, onLang = model::setLang) { push(Screen.OB_LOCATION) }
                    Screen.OB_LOCATION -> LocationScreen(s, onboarding = true, onBack = ::pop) {
                        model.setPlace(it); push(Screen.OB_METHOD)
                    }
                    Screen.OB_METHOD -> OnboardingMethodScreen(s, onBack = ::pop, onChange = { push(Screen.METHOD) }) {
                        push(Screen.OB_NOTIF)
                    }
                    Screen.OB_NOTIF -> OnboardingNotificationsScreen(onBack = ::pop) { enabled ->
                        model.finishSetup(enabled); root(Screen.HOME)
                    }
                    Screen.HOME -> HomeScreen(s, onPlace = { push(Screen.LOCATION) }, onMethod = { push(Screen.METHOD) })
                    Screen.QIBLA -> QiblaScreen(s)
                    Screen.SETTINGS -> SettingsScreen(s, model, open = ::push)
                    Screen.LOCATION -> LocationScreen(s, onboarding = false, onBack = ::pop) {
                        model.setPlace(it); pop()
                    }
                    Screen.METHOD -> MethodScreen(s, onBack = ::pop) { model.setMethod(it); pop() }
                    Screen.ASR -> AsrScreen(s, onBack = ::pop) { model.setAsr(it) }
                    Screen.NOTIFICATIONS -> NotificationsScreen(s, model, onBack = ::pop)
                    Screen.ABOUT -> AboutScreen(onBack = ::pop, onPrivacy = { push(Screen.PRIVACY) })
                    Screen.PRIVACY -> PrivacyScreen(onBack = ::pop)
                }
            }
            if (current.tab) TabBar(current) { root(it) }
        }
    }
}

@Composable
private fun TabBar(current: Screen, onSelect: (Screen) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind { drawLine(p.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(top = 6.dp, bottom = 6.dp),
    ) {
        val items = listOf(
            Triple(Screen.HOME, R.drawable.ic_today, R.string.tab_today),
            Triple(Screen.QIBLA, R.drawable.ic_qibla, R.string.tab_qibla),
            Triple(Screen.SETTINGS, R.drawable.ic_settings, R.string.tab_settings),
        )
        for ((screen, icon, label) in items) {
            val on = screen == current
            val color = if (on) p.accent else p.muted
            Column(
                Modifier
                    .weight(1f)
                    .semantics { selected = on }
                    .clickable(role = Role.Tab) { onSelect(screen) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(painterResource(icon), null, tint = color, modifier = Modifier.size(22.dp))
                Text(stringResource(label), color = color, fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}
