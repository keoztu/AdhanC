package com.salahtimesonly.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.salahtimesonly.R
import com.salahtimesonly.data.Cities
import com.salahtimesonly.data.City
import com.salahtimesonly.data.Place
import com.salahtimesonly.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
import kotlin.coroutines.resume

private val SUGGESTED = listOf("Rotterdam", "Amsterdam", "Istanbul", "Ankara", "London", "New York")

@Composable
fun LocationScreen(s: Settings, onboarding: Boolean, onBack: () -> Unit, onPick: (Place) -> Unit) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locale = appLocale()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<City>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<City>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        suggestions = withContext(Dispatchers.Default) {
            SUGGESTED.mapNotNull { Cities.search(context, it, 1).firstOrNull() }
        }
    }
    LaunchedEffect(query) {
        delay(120)
        results = withContext(Dispatchers.Default) { Cities.search(context, query) }
    }

    fun pickCity(c: City) = onPick(Place(c.name, c.region, c.cc, c.lat, c.lng, c.tz))

    val nearName = stringResource(R.string.near, "%s")
    fun useLocation() {
        busy = true; error = false
        scope.launch {
            val loc = currentLocation(context)
            val found = loc?.let { withContext(Dispatchers.Default) { Cities.nearest(context, it.latitude, it.longitude) } }
            busy = false
            if (loc == null || found == null) { error = true; return@launch }
            val (city, km) = found
            val tz = if (km < 300) city.tz else ZoneId.systemDefault().id
            val name = if (km < 40) city.name else nearName.replace("%s", city.name)
            onPick(Place(name, city.region, city.cc, loc.latitude, loc.longitude, tz))
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) useLocation() else error = true
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp)) {
        if (onboarding) {
            SubHeader(null, onBack)
            StepLabel(1)
            ScreenTitle(stringResource(R.string.where_are_you))
        } else {
            SubHeader(stringResource(R.string.s_location), onBack)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, modifier = Modifier.size(20.dp)) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { results.firstOrNull()?.let { pickCity(it) } }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = p.accent, unfocusedBorderColor = p.line,
                focusedContainerColor = p.surface, unfocusedContainerColor = p.surface,
                cursorColor = p.accent, focusedTextColor = p.text, unfocusedTextColor = p.text,
                focusedPlaceholderColor = p.muted, unfocusedPlaceholderColor = p.muted,
                focusedLeadingIconColor = p.muted, unfocusedLeadingIconColor = p.muted,
            ),
        )
        if (error) {
            Column(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .border(1.dp, p.line, RoundedCornerShape(10.dp))
                    .padding(14.dp),
            ) {
                Text(stringResource(R.string.location_error), color = p.text, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(R.string.choose_manually), color = p.accent, fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable(role = Role.Button) { error = false; focus.requestFocus() }
                        .heightIn(min = 32.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        IconLabelButton(R.drawable.ic_gps, stringResource(if (busy) R.string.locating else R.string.use_my_location)) {
            if (busy) return@IconLabelButton
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) useLocation()
            else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        val showing = if (query.isBlank()) suggestions else results
        LazyColumn(Modifier.weight(1f)) {
            if (query.isBlank()) item { SectionLabel(stringResource(R.string.suggestions)) }
            if (query.isNotBlank() && results.isEmpty()) {
                item { Note(stringResource(R.string.no_results), Modifier.padding(top = 8.dp)) }
            }
            items(showing) { c ->
                val selected = s.place?.let { it.name == c.name && it.cc == c.cc } == true
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.Button) { pickCity(c) }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(c.name, color = if (selected) p.accent else p.text, fontSize = 17.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    val where = listOf(c.region, Cities.countryName(c.cc, locale)).filter { it.isNotBlank() }.joinToString(", ")
                    Text(where, color = p.muted, fontSize = 14.sp, maxLines = 1, modifier = Modifier.padding(start = 12.dp))
                }
                Divider()
            }
            item { Note(stringResource(R.string.location_hint), Modifier.padding(top = 12.dp)) }
        }
    }
}

/** Coarse position from the platform location service (no Google Play Services needed). */
@SuppressLint("MissingPermission")
private suspend fun currentLocation(context: Context): Location? = withTimeoutOrNull(15_000) {
    val lm = context.getSystemService(LocationManager::class.java) ?: return@withTimeoutOrNull null
    val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
    val recent = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
        ?.takeIf { System.currentTimeMillis() - it.time < 30 * 60 * 1000 }
    if (recent != null) return@withTimeoutOrNull recent
    val provider = providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER } ?: return@withTimeoutOrNull null
    suspendCancellableCoroutine<Location?> { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            lm.getCurrentLocation(provider, signal, context.mainExecutor) { loc -> if (cont.isActive) cont.resume(loc) }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) { if (cont.isActive) cont.resume(location) }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) { if (cont.isActive) cont.resume(null) }
            }
            @Suppress("DEPRECATION")
            lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            cont.invokeOnCancellation { lm.removeUpdates(listener) }
        }
    }
}
