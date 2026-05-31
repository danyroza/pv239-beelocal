package com.pv239.beelocal.ui.screens.dailychallenge.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import com.google.firebase.firestore.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.drawable.toDrawable

private const val CIRCLE_RADIUS_METERS = 350.0
private const val OFFSET_MIN_METERS = 75.0
private const val OFFSET_RANGE_METERS = 75.0    // offset will be 75–150 m

private fun computeObfuscatedCenter(
    challengeLocation: GeoPoint,
    challengeId: String,
): Pair<Double, Double> {
    val hash = challengeId.hashCode()
    val bearingDegrees = hash % 360
    val bearingRadians = Math.toRadians(bearingDegrees.toDouble())
    val distanceMeters = OFFSET_MIN_METERS + (abs(hash / 360) % (OFFSET_RANGE_METERS + 1).toInt())

    val lat = challengeLocation.latitude
    val lng = challengeLocation.longitude

    val deltaLat = distanceMeters * cos(bearingRadians) / 111_320.0
    val deltaLng = distanceMeters * sin(bearingRadians) / (111_320.0 * cos(Math.toRadians(lat)))

    return Pair(lat + deltaLat, lng + deltaLng)
}

/** Creates a Google-Maps-style blue dot bitmap for the user's position. */
private fun createUserDotBitmap(sizeDp: Int, density: Float): Bitmap {
    val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
    val bitmap = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // Outer white ring
    val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, cx, outerPaint)

    // Inner blue fill
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(25, 118, 210)  // Material blue 700
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, cx * 0.65f, innerPaint)

    return bitmap
}

@Composable
fun ChallengeMapView(
    challengeLocation: GeoPoint,
    challengeId: String,
    userLatLng: Pair<Double, Double>?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val resources = LocalResources.current

    val obfuscatedCenter = remember(challengeId) {
        computeObfuscatedCenter(challengeLocation, challengeId)
    }
    val osmCenter = remember(obfuscatedCenter) {
        OsmGeoPoint(obfuscatedCenter.first, obfuscatedCenter.second)
    }
    val userDotIcon = remember(density) {
        val bmp = createUserDotBitmap(sizeDp = 20, density = density)
        bmp.toDrawable(resources)
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp)),
        factory = { ctx ->
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))

            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isClickable = true
                isFocusable = true

                // When the user touches the map, stop the LazyColumn from
                // stealing the gesture so panning/zooming works smoothly.
                setOnTouchListener { v, event ->
                    v.performClick()
                    when (event.action) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE -> v.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false // let MapView process the event normally
                }

                controller.setZoom(14.0)
                controller.setCenter(osmCenter)

                val circlePoints = Polygon.pointsAsCircle(osmCenter, CIRCLE_RADIUS_METERS)
                val circle = Polygon(this).apply {
                    points = circlePoints
                    fillPaint.color = AndroidColor.argb(60, 56, 200, 90)    // light green fill
                    outlinePaint.color = AndroidColor.rgb(34, 160, 70)       // green stroke
                    outlinePaint.strokeWidth = 5f
                }
                overlays.add(circle)
            }
        },
        update = { mapView ->
            // Remove any previous user marker and re-add if location is known
            mapView.overlays.removeAll { it is Marker }
            userLatLng?.let { (lat, lng) ->
                val marker = Marker(mapView).apply {
                    position = OsmGeoPoint(lat, lng)
                    icon = userDotIcon
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = null
                    snippet = null
                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        },
        onRelease = { mapView ->
            mapView.onDetach()
        }
    )
}
