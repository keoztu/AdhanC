package com.salahtimesonly.data

import com.salahtimesonly.calc.Prayer
import com.salahtimesonly.calc.PrayerCalc
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class NextPrayer(val prayer: Prayer, val at: Instant, val tomorrow: Boolean)

/** Prayer times for the selected place and method, in the place's own time zone. */
class Day(private val s: Settings) {
    private val place = requireNotNull(s.place)
    val zone: ZoneId = runCatching { ZoneId.of(place.tz) }.getOrDefault(ZoneId.systemDefault())

    fun localDate(now: Instant): LocalDate = now.atZone(zone).toLocalDate()

    fun times(date: LocalDate): Map<Prayer, Instant> =
        PrayerCalc.compute(date, place.lat, place.lng, s.method, s.asrFactor)

    fun next(now: Instant): NextPrayer {
        val today = localDate(now)
        val t = times(today)
        for (p in Prayer.SALAH) if (t.getValue(p) > now) return NextPrayer(p, t.getValue(p), false)
        return NextPrayer(Prayer.FAJR, times(today.plusDays(1)).getValue(Prayer.FAJR), true)
    }

    /** Start of the current prayer period, used for the progress line. */
    fun previous(now: Instant): Instant {
        val today = localDate(now)
        val t = times(today)
        return Prayer.SALAH.map { t.getValue(it) }.lastOrNull { it <= now }
            ?: times(today.minusDays(1)).getValue(Prayer.ISHA)
    }

    /** Upcoming salah instants after [now], across today and the next two days. */
    fun upcoming(now: Instant): List<Pair<Prayer, Instant>> {
        val today = localDate(now)
        return (0L..2L).flatMap { d ->
            val t = times(today.plusDays(d))
            Prayer.SALAH.map { it to t.getValue(it) }
        }.filter { it.second > now }.sortedBy { it.second }
    }
}
