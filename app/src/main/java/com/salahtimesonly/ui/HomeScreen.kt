package com.salahtimesonly.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahtimesonly.R
import com.salahtimesonly.calc.Prayer
import com.salahtimesonly.data.Day
import com.salahtimesonly.data.Settings
import com.salahtimesonly.schedule.PrayerNotifier
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@Composable
fun HomeScreen(s: Settings, onPlace: () -> Unit, onMethod: () -> Unit) {
    val p = LocalPalette.current
    val place = s.place ?: return
    val day = remember(s) { Day(s) }
    val fmt = remember(day) { timeFormatter(day.zone) }
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(1000 - (System.currentTimeMillis() % 1000))
        }
    }
    val date = day.localDate(now)
    val times = remember(day, date) { day.times(date) }
    val next = day.next(now)
    val prev = day.previous(now)
    val progress = (Duration.between(prev, now).toMillis().toFloat() / Duration.between(prev, next.at).toMillis().coerceAtLeast(1)).coerceIn(0f, 1f)
    val nextName = stringResource(PrayerNotifier.prayerName(next.prayer))
    val remaining = remainingText(Duration.between(now, next.at))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onPlace)
                .heightIn(min = 40.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(place.name, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Icon(painterResource(R.drawable.ic_chevron_down), null, tint = p.muted, modifier = Modifier.padding(start = 4.dp).size(16.dp))
        }
        Text(dateText(now, day.zone, appLocale()), color = p.muted, fontSize = 14.sp)

        Box(
            Modifier
                .padding(top = 22.dp)
                .size(240.dp, 290.dp)
                .semantics { contentDescription = "$nextName ${fmt.format(next.at)}, $remaining" },
        ) {
            Arch(progress, p.accent, Modifier.fillMaxSize())
            Column(
                Modifier.fillMaxWidth().padding(top = 104.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(nextName, color = p.accent, fontSize = 18.sp, letterSpacing = 1.sp)
                    if (next.tomorrow) Text("  " + stringResource(R.string.tomorrow), color = p.muted, fontSize = 14.sp)
                }
                Text(fmt.format(next.at), color = p.text, fontSize = 64.sp, fontWeight = FontWeight.Light, lineHeight = 68.sp)
                Text(remaining, color = p.muted, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }

        Spacer(Modifier.height(26.dp))
        for (row in Prayer.entries.chunked(3)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (prayer in row) {
                    val t = times.getValue(prayer)
                    TimeCell(
                        name = stringResource(PrayerNotifier.prayerName(prayer)),
                        time = fmt.format(t),
                        isNext = !next.tomorrow && next.prayer == prayer,
                        isPast = t <= now,
                        isSunrise = prayer == Prayer.SUNRISE,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Text(
            stringResource(R.string.method_suffix, stringResource(methodName(s.method))),
            color = p.muted, fontSize = 13.sp,
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(role = Role.Button, onClick = onMethod)
                .heightIn(min = 36.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun RowScope.TimeCell(name: String, time: String, isNext: Boolean, isPast: Boolean, isSunrise: Boolean) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(14.dp)
    val labelColor = when { isNext -> p.accent; isPast -> p.faint; else -> p.muted }
    val timeColor = when { isNext -> p.accent; isPast -> p.faint; isSunrise -> p.muted; else -> p.text }
    Column(
        Modifier
            .weight(1f)
            .clip(shape)
            .background(if (isNext) p.soft else Color.Transparent)
            .border(if (isNext) 1.5.dp else 1.dp, if (isNext) p.accent else p.line, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(name, color = labelColor, fontSize = 13.sp, maxLines = 1)
        Text(time, color = timeColor, fontSize = 22.sp,
            fontWeight = if (isSunrise) FontWeight.Light else FontWeight.Medium)
    }
}

/** The mihrab outline. The solid part traces how far we are between the previous and next prayer. */
@Composable
private fun Arch(progress: Float, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val sx = size.width / 240f
        val sy = size.height / 290f
        fun path(inset: Float) = Path().apply {
            moveTo((12 + inset) * sx, 289 * sy)
            lineTo((12 + inset) * sx, (122 + inset / 5) * sy)
            cubicTo((12 + inset) * sx, 66 * sy, 64 * sx, (28 + inset) * sy, 120 * sx, (4 + inset * 1.2f) * sy)
            cubicTo(176 * sx, (28 + inset) * sy, (228 - inset) * sx, 66 * sy, (228 - inset) * sx, (122 + inset / 5) * sy)
            lineTo((228 - inset) * sx, 289 * sy)
        }
        val outer = path(0f)
        drawPath(path(10f), color.copy(alpha = 0.3f), style = Stroke(width = 1.dp.toPx()))
        drawPath(outer, color.copy(alpha = 0.35f), style = Stroke(width = 1.5.dp.toPx()))
        val pm = PathMeasure()
        pm.setPath(outer, false)
        val part = Path()
        if (pm.getSegment(0f, pm.length * progress, part, true)) {
            drawPath(part, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
