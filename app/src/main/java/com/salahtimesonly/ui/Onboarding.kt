package com.salahtimesonly.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.salahtimesonly.R
import com.salahtimesonly.data.Settings
import com.salahtimesonly.schedule.AlarmScheduler

@Composable
fun WelcomeScreen(s: Settings, onLang: (String) -> Unit, onStart: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            for ((tag, label) in listOf("en" to "English", "tr" to "Türkçe")) {
                val on = (tag == "tr") == s.isTurkish
                Text(
                    label,
                    color = if (on) p.accent else p.muted,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) p.soft else p.bg)
                        .clickable(role = Role.RadioButton) { onLang(tag) }
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Image(painterResource(R.drawable.app_mark), contentDescription = null, modifier = Modifier.size(88.dp))
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.app_name), color = p.text, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, lineHeight = 40.sp)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.tagline), color = p.muted, fontSize = 19.sp, lineHeight = 26.sp)
        Spacer(Modifier.weight(1.2f))
        PrimaryButton(stringResource(R.string.get_started), onClick = onStart)
    }
}

@Composable
fun OnboardingMethodScreen(s: Settings, onBack: () -> Unit, onChange: () -> Unit, onContinue: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SubHeader(null, onBack)
            StepLabel(2)
            ScreenTitle(stringResource(R.string.calc_title))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.calc_intro, s.place?.name ?: ""), color = p.muted, fontSize = 16.sp, lineHeight = 23.sp)
            Spacer(Modifier.height(20.dp))
            Group {
                NavRow(
                    label = stringResource(methodName(s.method)),
                    sub = stringResource(R.string.fajr_isha, s.method.summary),
                    value = stringResource(R.string.change),
                    onClick = onChange,
                )
            }
        }
        PrimaryButton(stringResource(R.string.continue_), onClick = onContinue)
    }
}

@Composable
fun OnboardingNotificationsScreen(onBack: () -> Unit, onDone: (Boolean) -> Unit) {
    val p = LocalPalette.current
    val context = LocalContext.current
    var askExact by remember { mutableStateOf(false) }

    fun afterPermission(granted: Boolean) {
        if (granted && !AlarmScheduler.canScheduleExact(context)) askExact = true else onDone(granted)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { afterPermission(it) }

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SubHeader(null, onBack)
            StepLabel(3)
            ScreenTitle(stringResource(R.string.notif_title))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.notif_body), color = p.muted, fontSize = 16.sp, lineHeight = 23.sp)
        }
        PrimaryButton(stringResource(R.string.enable_notifications)) {
            if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else afterPermission(true)
        }
        Spacer(Modifier.height(6.dp))
        QuietButton(stringResource(R.string.not_now)) { onDone(false) }
    }

    if (askExact) {
        ExactAlarmDialog(
            onOpen = { askExact = false; openExactAlarmSettings(context); onDone(true) },
            onDismiss = { askExact = false; onDone(true) },
        )
    }
}

@Composable
fun ExactAlarmDialog(onOpen: () -> Unit, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surface,
        titleContentColor = p.text,
        textContentColor = p.muted,
        title = { Text(stringResource(R.string.exact_title)) },
        text = { Text(stringResource(R.string.exact_body)) },
        confirmButton = { TextButton(onClick = onOpen) { Text(stringResource(R.string.open_settings), color = p.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now), color = p.muted) } },
    )
}
