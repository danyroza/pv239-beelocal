package com.pv239.beelocal.ui.screens.routes.components

import android.location.Location
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pv239.beelocal.model.RoutePoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * OSM map that displays all [points] of a route.
 *
 * Each pin shows the checkpoint **number** (1-based) inside the circle:
 * - Completed checkpoints → green pin with a white checkmark overlay.
 * - Current checkpoint    → amber/yellow pin (larger).
 * - Pending checkpoints   → grey pin.
 * - User location         → blue pulsing dot from the provided or live GPS location.
 */
@Composable
fun RouteMapView(
    modifier: Modifier = Modifier,
    points: List<RoutePoint>,
    completedPointIndices: Set<Int>,
    currentPointIndex: Int,
    userLatLng: Pair<Double, Double>? = null,
    height: Dp = 220.dp,
) {
    if (points.isEmpty()) return

    val context = LocalContext.current
    val density = LocalDensity.current.density
    val resources = LocalResources.current
    var liveUserLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    DisposableEffect(context) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5_000L,
        ).setMinUpdateDistanceMeters(5f).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    liveUserLatLng = location.toLatLng()
                }
            }
        }

        try {
            client.requestLocationUpdates(request, callback, null)
        } catch (_: SecurityException) {
            // Fine location permission not granted; the map will simply omit the user dot.
        }

        onDispose { client.removeLocationUpdates(callback) }
    }

    val effectiveUserLatLng = userLatLng ?: liveUserLatLng

    // Build one icon per checkpoint (keyed on index so numbers are correct).
    val checkpointIcons = remember(points, completedPointIndices, currentPointIndex, density) {
        points.mapIndexed { index, _ ->
            val number = index + 1
            when {
                index in completedPointIndices -> createCheckpointBitmap(
                    number = number,
                    sizeDp = 36,
                    density = density,
                    fillColor = AndroidColor.rgb(34, 160, 70),   // green
                ).toDrawable(resources)

                index == currentPointIndex -> createCheckpointBitmap(
                    number = number,
                    sizeDp = 40,
                    density = density,
                    fillColor = AndroidColor.rgb(255, 167, 0),   // amber
                ).toDrawable(resources)

                else -> createCheckpointBitmap(
                    number = number,
                    sizeDp = 32,
                    density = density,
                    fillColor = AndroidColor.rgb(120, 120, 130), // grey
                ).toDrawable(resources)
            }
        }
    }

    val userDotIcon = remember(density) {
        createUserDotBitmap(sizeDp = 20, density = density).toDrawable(resources)
    }

    val boundingBox = remember(points, effectiveUserLatLng) {
        val lats = buildList {
            addAll(points.map { it.location.latitude })
            effectiveUserLatLng?.let { add(it.first) }
        }
        val lngs = buildList {
            addAll(points.map { it.location.longitude })
            effectiveUserLatLng?.let { add(it.second) }
        }
        BoundingBox(
            lats.max() + 0.003,
            lngs.max() + 0.004,
            lats.min() - 0.003,
            lngs.min() - 0.004,
        )
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp)),
        factory = { ctx ->
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isClickable = true
                isFocusable = true

                // Prevent LazyColumn from stealing scroll gestures
                setOnTouchListener { v, event ->
                    v.performClick()
                    when (event.action) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE -> v.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            // Checkpoint markers (numbered)
            points.forEachIndexed { index, point ->
                val marker = Marker(mapView).apply {
                    position = OsmGeoPoint(point.location.latitude, point.location.longitude)
                    icon = checkpointIcons[index]
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "${index + 1}. ${point.name}"
                    snippet = point.description.take(80)
                }
                mapView.overlays.add(marker)
            }

            // User location dot
            effectiveUserLatLng?.let { (lat, lng) ->
                val marker = Marker(mapView).apply {
                    position = OsmGeoPoint(lat, lng)
                    icon = userDotIcon
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = null
                    snippet = null
                }
                mapView.overlays.add(marker)
            }

            mapView.post {
                mapView.zoomToBoundingBox(boundingBox, false, 48)
            }
            mapView.invalidate()
        },
        onRelease = { mapView -> mapView.onDetach() },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Bitmap helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws a teardrop-shaped marker with a [number] label inside the circle.
 */
private fun createCheckpointBitmap(
    number: Int,
    sizeDp: Int,
    density: Float,
    fillColor: Int,
): Bitmap {
    val w = (sizeDp * density).toInt().coerceAtLeast(4)
    val h = (sizeDp * 1.4f * density).toInt().coerceAtLeast(4)
    val bitmap = createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = w / 2f
    val r = w / 2f

    // Drop shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(60, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx + 1f, r + 1f, r * 0.85f, shadowPaint)

    // Main circle fill
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, r, r * 0.85f, fillPaint)

    // White border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = w * 0.08f
    }
    canvas.drawCircle(cx, r, r * 0.85f, borderPaint)

    // Teardrop tip
    val tipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val tipPath = android.graphics.Path().apply {
        moveTo(cx - r * 0.35f, r + r * 0.55f)
        lineTo(cx + r * 0.35f, r + r * 0.55f)
        lineTo(cx, h.toFloat())
        close()
    }
    canvas.drawPath(tipPath, tipPaint)

    // Number label
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textAlign = Paint.Align.CENTER
        textSize = r * 0.85f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    // Vertically centre the number inside the circle
    val textY = r - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(number.toString(), cx, textY, textPaint)

    return bitmap
}

private fun Location.toLatLng(): Pair<Double, Double> = Pair(latitude, longitude)

/** Google-Maps-style blue dot for the user's current position. */
private fun createUserDotBitmap(sizeDp: Int, density: Float): Bitmap {
    val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
    val bitmap = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f

    // White halo
    val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cx, cx, outerPaint)

    // Blue inner dot
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(25, 118, 210)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cx, cx * 0.65f, innerPaint)

    return bitmap
}
