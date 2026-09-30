package com.salahtimesonly.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.salahtimesonly.MainActivity
import com.salahtimesonly.R
import com.salahtimesonly.calc.Prayer
import com.salahtimesonly.data.Day
import com.salahtimesonly.data.SettingsStore
import com.salahtimesonly.data.localized
import com.salahtimesonly.schedule.PrayerNotifier
import java.time.Instant
import java.time.format.DateTimeFormatter
import androidx.glance.color.ColorProvider as dayNight

/** The app's only widget: today's six times in a small table. */
class TimesWidget : GlanceAppWidget() {

    private data class Cell(val name: String, val time: String, val next: Boolean, val sunrise: Boolean)
    private data class Model(val place: String?, val nextLabel: String, val cells: List<Cell>, val empty: String)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = SettingsStore(context).current()
        val ctx = context.localized(s)
        val model = if (s.place == null) {
            Model(null, "", emptyList(), ctx.getString(R.string.widget_empty))
        } else {
            val day = Day(s)
            val now = Instant.now()
            val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(day.zone)
            val times = day.times(day.localDate(now))
            val next = day.next(now)
            Model(
                place = s.place.name,
                nextLabel = ctx.getString(R.string.widget_next, ctx.getString(PrayerNotifier.prayerName(next.prayer)), fmt.format(next.at)),
                cells = Prayer.entries.map { p ->
                    Cell(ctx.getString(PrayerNotifier.prayerName(p)), fmt.format(times.getValue(p)),
                        next = !next.tomorrow && next.prayer == p, sunrise = p == Prayer.SUNRISE)
                },
                empty = "",
            )
        }
        provideContent { Content(model) }
    }

    @Composable
    private fun Content(m: Model) {
        val bg = dayNight(day = Color(0xFFFAF9F5), night = Color(0xFF143129))
        val text = dayNight(day = Color(0xFF10251F), night = Color(0xFFF1EBDD))
        val muted = dayNight(day = Color(0xFF5C6861), night = Color(0xFFA9B5A8))
        Column(
            modifier = GlanceModifier.fillMaxSize().background(bg).cornerRadius(18.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            if (m.place == null) {
                Text(m.empty, style = TextStyle(color = text, fontSize = 13.sp))
                return@Column
            }
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(m.place, maxLines = 1, style = TextStyle(color = text, fontSize = 13.sp, fontWeight = FontWeight.Medium))
                Spacer(GlanceModifier.defaultWeight())
                Text(m.nextLabel, maxLines = 1, style = TextStyle(color = muted, fontSize = 12.sp))
            }
            Spacer(GlanceModifier.height(6.dp))
            for (row in m.cells.chunked(3)) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    row.forEachIndexed { i, c ->
                        if (i > 0) Spacer(GlanceModifier.width(4.dp))
                        CellView(c)
                    }
                }
                Spacer(GlanceModifier.height(4.dp))
            }
        }
    }

    @Composable
    private fun RowScope.CellView(c: Cell) {
        val accent = dayNight(day = Color(0xFF86652A), night = Color(0xFFD2B06B))
        val text = dayNight(day = Color(0xFF10251F), night = Color(0xFFF1EBDD))
        val muted = dayNight(day = Color(0xFF5C6861), night = Color(0xFFA9B5A8))
        val soft = dayNight(day = Color(0xFFEDE5D3), night = Color(0xFF1A3A31))
        var mod = GlanceModifier.defaultWeight().cornerRadius(10.dp).padding(horizontal = 8.dp, vertical = 4.dp)
        if (c.next) mod = GlanceModifier.defaultWeight().background(soft).cornerRadius(10.dp).padding(horizontal = 8.dp, vertical = 4.dp)
        Column(modifier = mod) {
            Text(c.name, maxLines = 1, style = TextStyle(color = if (c.next) accent else muted, fontSize = 11.sp))
            Text(
                c.time,
                style = TextStyle(
                    color = if (c.next) accent else if (c.sunrise) muted else text,
                    fontSize = 15.sp,
                    fontWeight = if (c.sunrise) FontWeight.Normal else FontWeight.Bold,
                ),
            )
        }
    }

    companion object {
        suspend fun refresh(context: Context) {
            runCatching { TimesWidget().updateAll(context) }
        }
    }
}

class TimesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimesWidget()
}
