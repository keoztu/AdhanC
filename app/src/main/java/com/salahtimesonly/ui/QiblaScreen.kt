package com.salahtimesonly.ui

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahtimesonly.R
import com.salahtimesonly.calc.PrayerCalc
import com.salahtimesonly.data.Settings
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToInt

class CompassState {
    var heading by mutableStateOf<Float?>(null)
    var available by mutableStateOf(true)
    var lowAccuracy by mutableStateOf(false)
}

/** True-north heading from the rotation-vector sensor (or accelerometer + magnetometer), corrected for declination. */
@Composable
fun rememberCompass(lat: Double, lng: Double): CompassState {
    val context = LocalContext.current
    val state = remember { CompassState() }
    DisposableEffect(lat, lng) {
        val sm = context.getSystemService(SensorManager::class.java)
        val rotation = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = sm?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (sm == null || (rotation == null && (accel == null || magnet == null))) {
            state.available = false
            return@DisposableEffect onDispose { }
        }
        val declination = GeomagneticField(lat.toFloat(), lng.toFloat(), 0f, System.currentTimeMillis()).declination
        val rm = FloatArray(9)
        val out = FloatArray(9)
        val orientation = FloatArray(3)
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val ready = when (e.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> { SensorManager.getRotationMatrixFromVector(rm, e.values); true }
                    Sensor.TYPE_ACCELEROMETER -> { gravity = e.values.clone(); rotation == null && fromRaw() }
                    Sensor.TYPE_MAGNETIC_FIELD -> { geomagnetic = e.values.clone(); rotation == null && fromRaw() }
                    else -> false
                }
                if (!ready) return
                remap(context, rm, out)
                SensorManager.getOrientation(out, orientation)
                val h = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + declination) % 360f + 360f) % 360f
                val prev = state.heading
                state.heading = if (prev == null) h else {
                    val d = ((h - prev + 540f) % 360f) - 180f
                    ((prev + d * 0.2f) % 360f + 360f) % 360f
                }
            }

            private fun fromRaw(): Boolean {
                val g = gravity ?: return false
                val m = geomagnetic ?: return false
                return SensorManager.getRotationMatrix(rm, null, g, m)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD || sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    state.lowAccuracy = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
                }
            }
        }
        rotation?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        accel?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        magnet?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sm.unregisterListener(listener) }
    }
    return state
}

@Suppress("DEPRECATION")
private fun remap(context: Context, rm: FloatArray, out: FloatArray) {
    val rotation = context.getSystemService(WindowManager::class.java)?.defaultDisplay?.rotation ?: Surface.ROTATION_0
    when (rotation) {
        Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(rm, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, out)
        Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(rm, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, out)
        Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(rm, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, out)
        else -> rm.copyInto(out)
    }
}

@Composable
fun QiblaScreen(s: Settings) {
    val p = LocalPalette.current
    val place = s.place ?: return
    val bearing = remember(place) { PrayerCalc.qibla(place.lat, place.lng).toFloat() }
    val km = remember(place) { PrayerCalc.distanceKm(place.lat, place.lng) }
    val compass = rememberCompass(place.lat, place.lng)
    val heading = compass.heading
    val delta = heading?.let { ((bearing - it + 540f) % 360f) - 180f }
    val aligned = delta != null && abs(delta) <= 4f
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(aligned) { if (aligned) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    val letters = stringResource(R.string.compass_letters).split(",")
    val measurer = rememberTextMeasurer()
    val degrees = bearing.roundToInt()
    val qiblaLabel = stringResource(R.string.qibla)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.qibla), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Text(place.name, color = p.muted, fontSize = 14.sp)

        Canvas(
            Modifier
                .padding(top = 30.dp, bottom = 16.dp)
                .size(300.dp)
                .semantics { contentDescription = "$qiblaLabel $degrees°" },
        ) {
            val r = size.minDimension / 2 * (134f / 150f)
            val c = center
            drawCircle(p.accent.copy(alpha = if (aligned) 1f else 0.6f), radius = r,
                style = Stroke(width = (if (aligned) 3.dp else 1.5.dp).toPx()))
            rotate(-(heading ?: 0f), c) {
                for (a in 0 until 360 step 10) {
                    val major = a % 30 == 0
                    rotate(a.toFloat(), c) {
                        drawLine(
                            if (major) p.muted else p.faint,
                            Offset(c.x, c.y - r + 6.dp.toPx()),
                            Offset(c.x, c.y - r + (if (major) 18.dp else 12.dp).toPx()),
                            strokeWidth = (if (major) 2.dp else 1.dp).toPx(),
                        )
                    }
                }
                letters.forEachIndexed { i, l ->
                    rotate(i * 90f, c) {
                        val layout = measurer.measure(l, TextStyle(fontSize = 15.sp, fontFamily = Readex,
                            fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Medium, color = if (i == 0) p.text else p.muted))
                        drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - r + 40.dp.toPx() - layout.size.height / 2f))
                    }
                }
                rotate(bearing, c) {
                    drawLine(p.accent, Offset(c.x, c.y - 18.dp.toPx()), Offset(c.x, c.y - r * 0.52f),
                        strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                    val k = 18.dp.toPx()
                    val ky = c.y - r * 0.64f
                    rotate(-bearing, Offset(c.x, ky)) {
                        drawRoundRect(p.text, Offset(c.x - k / 2, ky - k / 2), Size(k, k), CornerRadius(1.5.dp.toPx()))
                        drawRect(p.accent, Offset(c.x - k / 2, ky - k * 0.25f), Size(k, k * 0.15f))
                    }
                }
                drawCircle(p.text, radius = 5.dp.toPx(), center = c)
            }
            val tip = Path().apply {
                moveTo(c.x, c.y - r - 15.dp.toPx())
                lineTo(c.x - 8.dp.toPx(), c.y - r - 3.dp.toPx())
                lineTo(c.x + 8.dp.toPx(), c.y - r - 3.dp.toPx())
                close()
            }
            drawPath(tip, p.text)
        }

        Text("$degrees°", color = p.text, fontSize = 52.sp, fontWeight = FontWeight.Light)
        Text(
            stringResource(R.string.from_north_km, NumberFormat.getIntegerInstance(appLocale()).format(km.roundToInt())),
            color = p.muted, fontSize = 14.sp, textAlign = TextAlign.Center,
        )

        val status = when {
            !compass.available -> stringResource(R.string.no_compass)
            delta == null -> stringResource(R.string.hold_flat)
            aligned -> stringResource(R.string.facing_qibla)
            delta > 0 -> stringResource(R.string.turn_right, abs(delta).roundToInt())
            else -> stringResource(R.string.turn_left, abs(delta).roundToInt())
        }
        Text(
            status,
            color = if (aligned) p.accent else p.text,
            fontSize = 17.sp,
            fontWeight = if (aligned || !compass.available) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
        val help = when {
            !compass.available -> stringResource(R.string.no_compass_body, degrees)
            else -> stringResource(R.string.calibrate)
        }
        Text(
            help,
            color = if (compass.lowAccuracy) p.accent else p.muted,
            fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp),
        )
    }
}
