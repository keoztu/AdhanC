package com.salahtimesonly.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

private fun Context.tryStart(vararg intents: Intent) {
    for (i in intents) {
        try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: ActivityNotFoundException) { } catch (_: SecurityException) { }
    }
}

private fun Context.appDetails() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.tryStart(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")), context.appDetails())
    }
}

fun openNotificationSettings(context: Context) = context.tryStart(
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    context.appDetails(),
)

fun openBatterySettings(context: Context) = context.tryStart(
    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
    context.appDetails(),
)

fun openUrl(context: Context, url: String) = context.tryStart(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
