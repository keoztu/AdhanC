package com.salahtimesonly.data

import android.content.Context
import java.text.Normalizer
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot

data class City(
    val name: String,
    val region: String,
    val cc: String,
    val lat: Double,
    val lng: Double,
    val tz: String,
    val population: Long,
    internal val keys: List<String>,
)

/** Offline place search over the bundled GeoNames extract (assets/cities.tsv). */
object Cities {
    @Volatile private var cache: List<City>? = null

    fun all(context: Context): List<City> = cache ?: synchronized(this) {
        cache ?: load(context).also { cache = it }
    }

    private fun load(context: Context): List<City> =
        context.assets.open("cities.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.mapNotNull { line ->
                val f = line.split('\t')
                if (f.size < 8) return@mapNotNull null
                City(
                    name = f[0], region = f[2], cc = f[3],
                    lat = f[4].toDoubleOrNull() ?: return@mapNotNull null,
                    lng = f[5].toDoubleOrNull() ?: return@mapNotNull null,
                    tz = f[6], population = f[7].toLongOrNull() ?: 0,
                    keys = f[1].split('|').map(::normalize).distinct(),
                )
            }.toList()
        }

    fun search(context: Context, query: String, limit: Int = 40): List<City> {
        val q = normalize(query.trim())
        if (q.isEmpty()) return emptyList()
        val list = all(context)
        val exact = ArrayList<City>(); val prefix = ArrayList<City>(); val contains = ArrayList<City>()
        for (c in list) {
            var best = 3
            for (k in c.keys) {
                val r = when { k == q -> 0; k.startsWith(q) -> 1; k.contains(q) -> 2; else -> 3 }
                if (r < best) best = r
                if (best == 0) break
            }
            when (best) { 0 -> exact += c; 1 -> prefix += c; 2 -> contains += c }
            if (exact.size >= limit) break
        }
        // The file is sorted by population, so each bucket is already in a sensible order.
        return (exact + prefix + contains).take(limit)
    }

    fun nearest(context: Context, lat: Double, lng: Double): Pair<City, Double>? {
        var best: City? = null; var bestKm = Double.MAX_VALUE
        val k = cos(Math.toRadians(lat))
        for (c in all(context)) {
            val d = hypot(c.lat - lat, (c.lng - lng) * k) * 111.0
            if (d < bestKm) { bestKm = d; best = c }
        }
        return best?.let { it to bestKm }
    }

    fun normalize(s: String): String {
        val lowered = s.replace('İ', 'i').replace('I', 'i').lowercase(Locale.ROOT).replace('ı', 'i')
        return Normalizer.normalize(lowered, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace("'", "").replace("’", "")
    }

    /** Country name in the app's language. */
    fun countryName(cc: String, locale: Locale): String =
        Locale("", cc).getDisplayCountry(locale).ifBlank { cc }
}
