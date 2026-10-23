package com.example.ridedriver.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ridedriver.ui.components.RidaIcon
import com.example.ridedriver.ui.components.RidaIcons
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Compose wrapper around osmdroid MapView.
 * - Google tiles (lyrs=m / y), night-transformed via NightTileProvider
 * - Draws current location, destination marker, and OSRM route polyline
 * - Lightweight: no fragment, direct AndroidView lifecycle
 * - Draws current location, destination marker, and OSRM route polyline
 * - Lightweight: no fragment, direct AndroidView lifecycle
 */
@Composable
fun RidaMapView(
    modifier: Modifier = Modifier,
    center: GeoPoint = GeoPoint(-6.7924, 39.2083), // Dar es Salaam fallback
    zoom: Double = 13.5,
    useGoogleTiles: Boolean = true,
    useSatellite: Boolean = false,
    useDarkTiles: Boolean = false,
    currentLocation: GeoPoint? = null,
    destination: GeoPoint? = null,
    routePoints: List<GeoPoint> = emptyList(),
    driverLocations: List<GeoPoint> = emptyList(),
    onMapReady: ((MapView) -> Unit)? = null,
    enableMyLocation: Boolean = false
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(if (useDarkTiles) Color(0xFF0A0F14) else Color(0xFFE5E7EB)),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RidaIcon(RidaIcons.Map, contentDescription = null, modifier = Modifier.size(24.dp), tint = if (useDarkTiles) Color.White else Color.Black)
                Text(text = "Map Preview", color = if (useDarkTiles) Color.White else Color.Black, modifier = Modifier.padding(start = 6.dp))
            }
        }
        return
    }

    val context = LocalContext.current

    // osmdroid REQUIRED setup — must run before ANY provider/MapView is created,
    // otherwise the tile writer initialises with a null base path and no tile
    // is ever persisted (plus a storm of NPE debug logs).
    Configuration.getInstance().load(
        context,
        context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    )
    Configuration.getInstance().userAgentValue = context.packageName

    // Bolt-style puck, built once: dark skeuomorphic disc, green core.
    val puck = remember(context) { makeLocationPuck(context) }
    // Drop-off pin, built once: dark teardrop, mint ring, white core.
    val dropoffPin = remember(context) { makeDropoffPin(context) }

    val mapView = remember {
        // Dark mode gets its own provider: same Google tiles, night-transformed
        // on the loader threads (see NightTiles). Provider and source stay in
        // sync through the LaunchedEffect below.
        val initialSource = when {
            useSatellite -> GoogleTileSources.GoogleSatelliteHD
            useDarkTiles -> GoogleTileSources.GoogleRoadNightHD
            else -> GoogleTileSources.GoogleRoadHD
        }
        val view = if (useDarkTiles) {
            MapView(context, NightTileProvider(context, initialSource))
        } else {
            MapView(context)
        }
        view.apply {
            setTileSource(initialSource)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(zoom)
            controller.setCenter(center)
            // crisp tiles on high-dpi
            isTilesScaledToDpi = true
            // Canvas background and tile loading grid colors match map theme:
            // White for light tiles so unloaded areas show clean white, dark for dark tiles.
            val bg = if (useDarkTiles) AndroidColor.parseColor("#0A0F14") else AndroidColor.WHITE
            val lineBg = if (useDarkTiles) AndroidColor.parseColor("#1C252D") else AndroidColor.parseColor("#F5F5F5")
            setBackgroundColor(bg)
            overlayManager.tilesOverlay.apply {
                setLoadingBackgroundColor(bg)
                setLoadingLineColor(lineBg)
            }
        }
    }

    // Reflect tile-source toggles without recreating view.
    // NOTE: on the night provider every source is night-transformed by the
    // loader, so switching sources here can never leak a bright tile.
    LaunchedEffect(useGoogleTiles, useSatellite, useDarkTiles) {
        val bg = if (useDarkTiles) AndroidColor.parseColor("#0A0F14") else AndroidColor.WHITE
        val lineBg = if (useDarkTiles) AndroidColor.parseColor("#1C252D") else AndroidColor.parseColor("#F5F5F5")
        mapView.setBackgroundColor(bg)
        mapView.overlayManager.tilesOverlay.apply {
            setLoadingBackgroundColor(bg)
            setLoadingLineColor(lineBg)
        }
        mapView.setTileSource(
            when {
                useSatellite -> GoogleTileSources.GoogleSatelliteHD
                useDarkTiles -> GoogleTileSources.GoogleRoadNightHD
                useGoogleTiles -> GoogleTileSources.GoogleRoadHD
                else -> GoogleTileSources.GoogleRoadHD
            }
        )
        mapView.invalidate()
    }

    // Keep center/zoom in sync
    LaunchedEffect(center, zoom) {
        mapView.controller.setZoom(zoom)
        mapView.controller.setCenter(center)
    }

    // Overlays: location + route + markers — rebuilt on each param change
    LaunchedEffect(currentLocation, destination, routePoints, driverLocations, enableMyLocation) {
        mapView.overlays.clear()

        // MyLocation overlay (accuracy ring) with the Bolt-style puck person icon.
        // NOTE: follow-mode is intentionally OFF — the user pans freely and
        // returns via the re-center button.
        if (enableMyLocation) {
            val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
                enableMyLocation()
                setPersonIcon(puck)
                setPersonAnchor(0.5f, 0.5f)
            }
            mapView.overlays.add(myLocationOverlay)
        }

        // Route polyline (OSRM)
        if (routePoints.size >= 2) {
            val polyline = Polyline().apply {
                setPoints(routePoints)
                outlinePaint.apply {
                    color = AndroidColor.parseColor("#43D2A1")
                    strokeWidth = 10f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                }
                // white halo underneath for contrast on both light/dark tiles
            }
            // halo polyline (slightly wider, white, drawn first)
            val halo = Polyline().apply {
                setPoints(routePoints)
                outlinePaint.apply {
                    color = AndroidColor.WHITE
                    strokeWidth = 16f
                    strokeCap = Paint.Cap.ROUND
                    isAntiAlias = true
                    alpha = 200
                }
            }
            mapView.overlays.add(halo)
            mapView.overlays.add(polyline)
        }

        // Destination marker — Bolt-style drop-off pin
        destination?.let { dest ->
            val marker = Marker(mapView).apply {
                position = dest
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Destination"
                icon = BitmapDrawable(context.resources, dropoffPin)
            }
            mapView.overlays.add(marker)
        }

        // Current location puck (when MyLocation overlay not used, or as extra)
        if (currentLocation != null && !enableMyLocation) {
            val me = Marker(mapView).apply {
                position = currentLocation
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "You"
                icon = BitmapDrawable(context.resources, puck)
            }
            mapView.overlays.add(me)
        }

        driverLocations.forEach { location ->
            val driver = Marker(mapView).apply {
                position = location
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "Driver"
            }
            mapView.overlays.add(driver)
        }

        mapView.invalidate()
        onMapReady?.invoke(mapView)
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { /* handled by LaunchedEffects */ }
    )

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }
}

// Helper to move map smoothly
fun MapView.animateTo(geoPoint: GeoPoint, zoom: Double = 15.0) {
    controller.animateTo(geoPoint)
    controller.setZoom(zoom)
    invalidate()
}

/**
 * Bolt-style location puck: dark skeuomorphic disc (radial shading + rim +
 * top light-catch) with a glowing green core.
 */
private fun makeLocationPuck(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (34 * density).toInt()
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = size / 2f
    val cy = size / 2f

    // Soft outer glow
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(70, 67, 210, 161)
    }
    val discR = size * 0.36f
    c.drawCircle(cx, cy, discR + 5 * density, glowPaint)

    // Dark disc — bright middle fading to near-black edge (skeuomorphic depth)
    val discColors: IntArray = intArrayOf(0xFF3B4653.toInt(), 0xFF161D24.toInt(), 0xFF080C10.toInt())
    val discStops: FloatArray = floatArrayOf(0f, 0.65f, 1f)
    val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(cx, cy, discR, discColors, discStops, Shader.TileMode.CLAMP)
    }
    c.drawCircle(cx, cy, discR, discPaint)
    // Top light pool (the offset glow a two-point gradient would give)
    val poolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(38, 255, 255, 255)
    }
    c.drawCircle(cx, cy - discR * 0.38f, discR * 0.52f, poolPaint)

    // Thin rim ring
    val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * density
        color = 0xFF4A5560.toInt()
    }
    c.drawCircle(cx, cy, discR - 1 * density, rimPaint)

    // Top light-catch arc
    val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density
        strokeCap = Paint.Cap.ROUND
        color = AndroidColor.argb(110, 255, 255, 255)
    }
    val inset = discR - 3.2f * density
    c.drawArc(cx - inset, cy - inset, cx + inset, cy + inset, 200f, 140f, false, arcPaint)

    // Dark gap ring around the core
    val coreR = size * 0.17f
    val gapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0A0E12.toInt()
    }
    c.drawCircle(cx, cy, coreR + 2.2f * density, gapPaint)

    // Green core — bright center fading to mint edge
    val coreColors: IntArray = intArrayOf(0xFF9DF5CB.toInt(), 0xFF43D2A1.toInt(), 0xFF23A67D.toInt())
    val coreStops: FloatArray = floatArrayOf(0f, 0.6f, 1f)
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(cx, cy, coreR, coreColors, coreStops, Shader.TileMode.CLAMP)
    }
    c.drawCircle(cx, cy, coreR, corePaint)

    return bmp
}

/**
 * Bolt-style drop-off pin: dark teardrop with a mint ring and white core.
 * Anchored bottom-center so the tip lands exactly on the destination.
 */
private fun makeDropoffPin(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (40 * density).toInt()
    val h = (54 * density).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = w / 2f
    val headR = w * 0.36f
    val headCy = headR + 3 * density
    val tipY = h - 2 * density

    // Soft glow behind
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(60, 67, 210, 161)
    }
    c.drawCircle(cx, headCy, headR + 4 * density, glowPaint)

    // Teardrop body
    val body = Path().apply {
        val left = cx - headR
        val right = cx + headR
        moveTo(left, headCy)
        // round head
        arcTo(RectF(left, headCy - headR, right, headCy + headR), 180f, 180f, false)
        // taper to tip
        quadTo(right - 2 * density, headCy + headR * 0.9f, cx, tipY)
        quadTo(left + 2 * density, headCy + headR * 0.9f, left, headCy)
        close()
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx, headCy, headR * 1.1f,
            intArrayOf(0xFF2C3742.toInt(), 0xFF12181E.toInt(), 0xFF070B0E.toInt()),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    c.drawPath(body, bodyPaint)

    // Mint ring edge
    val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density
        color = 0xFF43D2A1.toInt()
    }
    c.drawPath(body, edgePaint)

    // White core with mint halo
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(70, 67, 210, 161)
    }
    c.drawCircle(cx, headCy, headR * 0.48f, haloPaint)
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
    }
    c.drawCircle(cx, headCy, headR * 0.32f, corePaint)

    return bmp
}
