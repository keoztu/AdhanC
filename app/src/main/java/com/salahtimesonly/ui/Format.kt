package com.salahtimesonly.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.salahtimesonly.R
import com.salahtimesonly.calc.Method
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun timeFormatter(zone: ZoneId): DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)

fun dateText(now: Instant, zone: ZoneId, locale: Locale): String {
    val pattern = if (locale.language == "tr") "d MMMM EEEE" else "EEEE, d MMMM"
    return DateTimeFormatter.ofPattern(pattern, locale).withZone(zone).format(now)
}

@Composable
fun appLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun remainingText(d: Duration): String {
    val ms = d.toMillis()
    if (ms < 60_000) return stringResource(R.string.remaining, stringResource(R.string.less_than_minute))
    val mins = (ms + 59_999) / 60_000
    val h = (mins / 60).toInt()
    val m = (mins % 60).toInt()
    val t = if (h > 0) stringResource(R.string.hm, h, m) else stringResource(R.string.m, m)
    return stringResource(R.string.remaining, t)
}

@StringRes
fun methodName(m: Method): Int = when (m) {
    Method.DIYANET -> R.string.method_diyanet
    Method.MWL -> R.string.method_mwl
    Method.ISNA -> R.string.method_isna
    Method.EGYPT -> R.string.method_egypt
    Method.MAKKAH -> R.string.method_makkah
    Method.KARACHI -> R.string.method_karachi
    Method.GULF -> R.string.method_gulf
    Method.UOIF -> R.string.method_uoif
    Method.JAKIM -> R.string.method_jakim
}
