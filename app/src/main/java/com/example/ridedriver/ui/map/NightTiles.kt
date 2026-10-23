package com.example.ridedriver.ui.map

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.MapTileRequestState
import org.osmdroid.tileprovider.tilesource.ITileSource

/**
 * Night-mode tile pipeline — the $0 dark map.
 *
 * Google road tiles (lyrs=m, already working on this network) are downloaded
 * normally, then each tile is transformed on osmdroid's background loader
 * threads with an invert + 180° hue-rotate matrix. That combination inverts
 * luminance while preserving hue, i.e. a true night map: near-black ground,
 * light roads/labels — matching the dark home reference.
 *
 * The transform runs inside the loader (see NightDownloader.java — Java, so
 * it can extend osmdroid's protected TileLoader inner class), which covers
 * fresh downloads AND filesystem-cache hits. The in-memory tile cache above
 * it keeps transformed drawables, so each tile pays the cost once.
 */
object NightTiles {

    // Invert (diag -1, offset 255) composed with 180° hue rotation.
    private val NIGHT_MATRIX = ColorMatrix(
        floatArrayOf(
            0.333f, -0.667f, -0.667f, 0f, 255f,
            -0.667f, 0.333f, -0.667f, 0f, 255f,
            -0.667f, -0.667f, 0.333f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    @JvmStatic
    fun toNight(drawable: Drawable): Drawable {
        val src = (drawable as? BitmapDrawable)?.bitmap ?: return drawable
        if (src.isRecycled || src.width <= 0 || src.height <= 0) return drawable
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        // Local Paint per call — loaders run on several threads at once.
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(NIGHT_MATRIX) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return BitmapDrawable(Resources.getSystem(), out)
    }
}

/**
 * Tile provider that serves night-transformed tiles for any tile source.
 * Pass a Google road source for the dark home map.
 *
 * The transform hooks into the provider callbacks (plain public interface —
 * no fragile loader subclassing): every completed tile, whether freshly
 * downloaded or served from the filesystem cache, is night-shifted before
 * it enters the in-memory cache that the map draws from.
 */
class NightTileProvider(
    context: Context,
    tileSource: ITileSource
) : MapTileProviderBasic(context, tileSource) {

    override fun mapTileRequestCompleted(state: MapTileRequestState?, drawable: Drawable?) {
        super.mapTileRequestCompleted(state, drawable?.let { NightTiles.toNight(it) })
    }

    override fun mapTileRequestExpiredTile(state: MapTileRequestState?, drawable: Drawable?) {
        super.mapTileRequestExpiredTile(state, drawable?.let { NightTiles.toNight(it) })
    }
}
