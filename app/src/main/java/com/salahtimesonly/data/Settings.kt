package com.salahtimesonly.data

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.salahtimesonly.calc.Method
import com.salahtimesonly.calc.Prayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

data class Place(
    val name: String,
    val region: String,
    val cc: String,
    val lat: Double,
    val lng: Double,
    val tz: String,
)

data class Settings(
    val setupDone: Boolean = false,
    val place: Place? = null,
    val methodKey: String = "",      // "" = automatic default
    val asrFactor: Int = 1,          // 1 = standard, 2 = Hanafi
    val theme: String = "system",    // system | light | dark
    val lang: String = "system",     // system | en | tr
    val notifications: Boolean = false,
    val adhan: Boolean = true,
    val adhanFull: Boolean = false,  // false = short (~5 s)
    val adhanVoice: Int = 1,
    val perPrayer: Map<Prayer, Boolean> = Prayer.SALAH.associateWith { true },
) {
    val isTurkish: Boolean
        get() = when (lang) {
            "tr" -> true
            "en" -> false
            else -> Resources.getSystem().configuration.locales[0].language == "tr"
        }

    /** The explicitly chosen method, or a default: Diyanet in Turkish or in Turkey, otherwise regional. */
    val method: Method
        get() = Method.of(methodKey) ?: defaultMethod(place?.cc, isTurkish)

    val locale: Locale
        get() = when (lang) {
            "tr" -> Locale("tr", "TR")
            "en" -> Locale.ENGLISH
            else -> Resources.getSystem().configuration.locales[0]
        }

    fun adhanFor(p: Prayer) = notifications && adhan && (perPrayer[p] ?: true)

    companion object {
        fun defaultMethod(cc: String?, turkish: Boolean): Method {
            if (turkish || cc == "TR") return Method.DIYANET
            return when (cc) {
                "US", "CA", "MX" -> Method.ISNA
                "EG", "SD", "LY", "SY", "LB", "IQ" -> Method.EGYPT
                "SA", "YE" -> Method.MAKKAH
                "PK", "IN", "BD", "AF" -> Method.KARACHI
                "AE", "KW", "QA", "BH", "OM" -> Method.GULF
                "FR" -> Method.UOIF
                "MY", "SG", "ID", "BN" -> Method.JAKIM
                else -> Method.MWL
            }
        }
    }
}

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** All settings live on the device only. */
class SettingsStore(context: Context) {
    private val ds = context.applicationContext.store

    private object K {
        val setup = booleanPreferencesKey("setup")
        val pName = stringPreferencesKey("p_name")
        val pRegion = stringPreferencesKey("p_region")
        val pCc = stringPreferencesKey("p_cc")
        val pLat = doublePreferencesKey("p_lat")
        val pLng = doublePreferencesKey("p_lng")
        val pTz = stringPreferencesKey("p_tz")
        val method = stringPreferencesKey("method")
        val asr = intPreferencesKey("asr")
        val theme = stringPreferencesKey("theme")
        val lang = stringPreferencesKey("lang")
        val notif = booleanPreferencesKey("notif")
        val adhan = booleanPreferencesKey("adhan")
        val adhanFull = booleanPreferencesKey("adhan_full")
        val voice = intPreferencesKey("adhan_voice")
        fun per(p: Prayer) = booleanPreferencesKey("per_" + p.name.lowercase(Locale.ROOT))
    }

    val flow: Flow<Settings> = ds.data.map { p ->
        val name = p[K.pName]
        Settings(
            setupDone = p[K.setup] ?: false,
            place = if (name != null && p[K.pLat] != null && p[K.pLng] != null && p[K.pTz] != null)
                Place(name, p[K.pRegion] ?: "", p[K.pCc] ?: "", p[K.pLat]!!, p[K.pLng]!!, p[K.pTz]!!) else null,
            methodKey = p[K.method] ?: "",
            asrFactor = p[K.asr] ?: 1,
            theme = p[K.theme] ?: "system",
            lang = p[K.lang] ?: "system",
            notifications = p[K.notif] ?: false,
            adhan = p[K.adhan] ?: true,
            adhanFull = p[K.adhanFull] ?: false,
            adhanVoice = p[K.voice] ?: 1,
            perPrayer = Prayer.SALAH.associateWith { p[K.per(it)] ?: true },
        )
    }

    suspend fun current(): Settings = flow.first()

    suspend fun setPlace(place: Place) = edit {
        it[K.pName] = place.name; it[K.pRegion] = place.region; it[K.pCc] = place.cc
        it[K.pLat] = place.lat; it[K.pLng] = place.lng; it[K.pTz] = place.tz
    }
    suspend fun setSetupDone() = edit { it[K.setup] = true }
    suspend fun setMethod(key: String) = edit { it[K.method] = key }
    suspend fun setAsr(f: Int) = edit { it[K.asr] = f }
    suspend fun setTheme(v: String) = edit { it[K.theme] = v }
    suspend fun setLang(v: String) = edit { it[K.lang] = v }
    suspend fun setNotifications(v: Boolean) = edit { it[K.notif] = v }
    suspend fun setAdhan(v: Boolean) = edit { it[K.adhan] = v }
    suspend fun setAdhanFull(v: Boolean) = edit { it[K.adhanFull] = v }
    suspend fun setVoice(v: Int) = edit { it[K.voice] = v }
    suspend fun setPer(p: Prayer, v: Boolean) = edit { it[K.per(p)] = v }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        ds.edit { block(it) }
    }
}

/** A context whose resources use the app language, for receivers and services. */
fun Context.localized(settings: Settings): Context {
    if (settings.lang == "system") return this
    val cfg = Configuration(resources.configuration)
    cfg.setLocale(settings.locale)
    return createConfigurationContext(cfg)
}
