package com.salahtimesonly.calc

import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class Prayer {
    FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA;

    val isSalah: Boolean get() = this != SUNRISE

    companion object {
        val SALAH = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    }
}

/** Twilight angles in degrees; [ishaMinutes] replaces the Isha angle when set. Offsets in minutes. */
enum class Method(
    val key: String,
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaMinutes: Int? = null,
    val offsets: Map<Prayer, Int> = emptyMap(),
) {
    DIYANET("diyanet", 18.0, 17.0, offsets = mapOf(Prayer.SUNRISE to -7, Prayer.DHUHR to 5, Prayer.ASR to 4, Prayer.MAGHRIB to 7)),
    MWL("mwl", 18.0, 17.0),
    ISNA("isna", 15.0, 15.0),
    EGYPT("egypt", 19.5, 17.5),
    MAKKAH("makkah", 18.5, null, ishaMinutes = 90),
    KARACHI("karachi", 18.0, 18.0),
    GULF("gulf", 19.5, null, ishaMinutes = 90),
    UOIF("uoif", 12.0, 12.0),
    JAKIM("jakim", 20.0, 18.0);

    val summary: String
        get() = "${fmt(fajrAngle)}° / " + (ishaMinutes?.let { "$it min" } ?: "${fmt(ishaAngle!!)}°")

    companion object {
        fun of(key: String?): Method? = entries.firstOrNull { it.key == key }
        private fun fmt(d: Double) = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()
    }
}

/** Astronomical prayer-time engine (PrayTimes.org algorithm) with the angle-based high-latitude rule. */
object PrayerCalc {
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LNG = 39.8262

    private fun dtr(d: Double) = Math.toRadians(d)
    private fun rtd(r: Double) = Math.toDegrees(r)
    private fun dsin(d: Double) = sin(dtr(d))
    private fun dcos(d: Double) = cos(dtr(d))
    private fun dtan(d: Double) = tan(dtr(d))
    private fun dasin(x: Double) = rtd(asin(x))
    private fun dacos(x: Double) = rtd(acos(x))
    private fun datan2(y: Double, x: Double) = rtd(atan2(y, x))
    private fun dacot(x: Double) = rtd(atan(1 / x))
    private fun fix(a: Double, b: Double): Double {
        val r = a - b * floor(a / b)
        return if (r < 0) r + b else r
    }
    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun fixHour(a: Double) = fix(a, 24.0)
    private fun diff(a: Double, b: Double) = fixHour(b - a)

    private fun julian(y0: Int, m0: Int, d: Int): Double {
        var y = y0
        var m = m0
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    private class Sun(val decl: Double, val eqt: Double)

    private fun sunPos(jd: Double): Sun {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(datan2(dcos(e) * dsin(l), dcos(l)) / 15)
        return Sun(dasin(dsin(e) * dsin(l)), q / 15 - ra)
    }

    /** Prayer times for a civil [date] at the location, as absolute instants. */
    fun compute(date: LocalDate, lat: Double, lng: Double, method: Method, asrFactor: Int): Map<Prayer, Instant> {
        val raw = hours(date, lat, lng, method, asrFactor)
        // Polar day/night: fall back to the nearest latitude where the sun still rises and sets.
        val h = if (raw.values.any { it.isNaN() }) hours(date, sign(lat) * min(abs(lat), 65.0), lng, method, asrFactor) else raw
        val base = date.toEpochDay() * 86_400_000L
        return Prayer.entries.associateWith { p ->
            val utcHours = h.getValue(p) - lng / 15 + (method.offsets[p] ?: 0) / 60.0
            Instant.ofEpochMilli(base + (utcHours * 60).roundToLong() * 60_000L)
        }
    }

    private fun hours(date: LocalDate, lat: Double, lng: Double, m: Method, asrFactor: Int): Map<Prayer, Double> {
        val jDate = julian(date.year, date.monthValue, date.dayOfMonth) - lng / (15 * 24)
        fun midDay(t: Double) = fixHour(12 - sunPos(jDate + t).eqt)
        fun angleTime(angle: Double, t: Double, ccw: Boolean = false): Double {
            val decl = sunPos(jDate + t).decl
            val v = dacos((-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))) / 15
            return midDay(t) + if (ccw) -v else v
        }
        fun asrTime(f: Int, t: Double): Double {
            val decl = sunPos(jDate + t).decl
            return angleTime(-dacot(f + dtan(abs(lat - decl))), t)
        }

        val sunrise = angleTime(0.833, 6 / 24.0, true)
        val sunset = angleTime(0.833, 18 / 24.0)
        var fajr = angleTime(m.fajrAngle, 5 / 24.0, true)
        var isha = if (m.ishaMinutes != null) sunset + m.ishaMinutes / 60.0 else angleTime(m.ishaAngle!!, 18 / 24.0)

        val night = diff(sunset, sunrise)
        fun adjust(time: Double, base: Double, angle: Double, ccw: Boolean): Double {
            val portion = angle / 60 * night
            val d = if (ccw) diff(time, base) else diff(base, time)
            return if (time.isNaN() || d > portion) base + (if (ccw) -portion else portion) else time
        }
        fajr = adjust(fajr, sunrise, m.fajrAngle, true)
        if (m.ishaMinutes == null) isha = adjust(isha, sunset, m.ishaAngle!!, false)

        return mapOf(
            Prayer.FAJR to fajr,
            Prayer.SUNRISE to sunrise,
            Prayer.DHUHR to midDay(12 / 24.0),
            Prayer.ASR to asrTime(asrFactor, 13 / 24.0),
            Prayer.MAGHRIB to sunset,
            Prayer.ISHA to isha,
        )
    }

    /** Initial great-circle bearing to the Kaaba, degrees clockwise from true north. */
    fun qibla(lat: Double, lng: Double): Double = fixAngle(
        datan2(
            dsin(KAABA_LNG - lng) * dcos(KAABA_LAT),
            dcos(lat) * dsin(KAABA_LAT) - dsin(lat) * dcos(KAABA_LAT) * dcos(KAABA_LNG - lng),
        )
    )

    fun distanceKm(lat: Double, lng: Double): Double {
        val s1 = sin(dtr(KAABA_LAT - lat) / 2)
        val s2 = sin(dtr(KAABA_LNG - lng) / 2)
        val a = s1 * s1 + dcos(lat) * dcos(KAABA_LAT) * s2 * s2
        return 2 * 6371 * asin(sqrt(a))
    }
}
