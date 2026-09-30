package com.salahtimesonly.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PrayerCalcTest {
    private fun times(d: LocalDate, lat: Double, lng: Double, m: Method, tz: String): List<String> {
        val f = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of(tz))
        val t = PrayerCalc.compute(d, lat, lng, m, 1)
        return Prayer.entries.map { f.format(t.getValue(it)) }
    }

    /** Diyanet's published times for these days are within one minute of these values. */
    @Test fun istanbulDiyanet() = assertEquals(
        listOf("05:27", "06:51", "12:59", "16:18", "18:57", "20:16"),
        times(LocalDate.of(2026, 9, 29), 41.0082, 28.9784, Method.DIYANET, "Europe/Istanbul"),
    )

    @Test fun ankaraDiyanetWinter() = assertEquals(
        listOf("06:33", "08:01", "13:03", "15:32", "17:55", "19:18"),
        times(LocalDate.of(2026, 1, 15), 39.9334, 32.8597, Method.DIYANET, "Europe/Istanbul"),
    )

    @Test fun daylightSavingDayUsesSummerTime() = assertEquals(
        "07:22", times(LocalDate.of(2026, 3, 29), 52.3676, 4.9041, Method.MWL, "Europe/Amsterdam")[1],
    )

    @Test fun polarDaysStillProduceOrderedTimes() {
        val t = PrayerCalc.compute(LocalDate.of(2026, 6, 21), 69.6492, 18.9553, Method.MWL, 1)
        assertTrue(t.getValue(Prayer.FAJR) < t.getValue(Prayer.DHUHR))
        assertTrue(t.getValue(Prayer.DHUHR) < t.getValue(Prayer.MAGHRIB))
    }

    @Test fun qiblaFromRotterdam() = assertEquals(124.7, PrayerCalc.qibla(51.9244, 4.4777), 0.1)
}
