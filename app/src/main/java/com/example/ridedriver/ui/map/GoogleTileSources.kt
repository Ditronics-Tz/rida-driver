package com.example.ridedriver.ui.map

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex

/**
 * Google tile providers — same URLs from the Flutter proposal, now for Android osmdroid.
 *   Road Map:         https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}
 *   Satellite+Roads:  https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}
 *
 * Retina (HD) variants append &scale=2 and serve true 512px tiles — crisp on
 * high-dpi phones instead of upscaled 256px blur. Each has its own cache name
 * so 256px and 512px tiles never mix in the tile cache.
 *
 * Note: these endpoints are not an official public API. They work for MVP / university
 * projects but may be restricted for production.
 */
object GoogleTileSources {

    private fun hdUrl(x: Int, y: Int, z: Int, baseUrl: String): String {
        return baseUrl + x + "&y=" + y + "&z=" + z + "&scale=2"
    }

    // Road map, retina (lyrs=m, 512px)
    val GoogleRoadHD = object : OnlineTileSourceBase(
        "GoogleRoadHD", 0, 20, 512, "",
        arrayOf(
            "https://mt0.google.com/vt/lyrs=m&x=",
            "https://mt1.google.com/vt/lyrs=m&x=",
            "https://mt2.google.com/vt/lyrs=m&x=",
            "https://mt3.google.com/vt/lyrs=m&x="
        )
    ) {
        override fun getTileURLString(pTileIndex: Long): String {
            return hdUrl(MapTileIndex.getX(pTileIndex), MapTileIndex.getY(pTileIndex), MapTileIndex.getZoom(pTileIndex), baseUrl)
        }
    }

    // Satellite + roads overlay, retina (lyrs=y, 512px)
    val GoogleSatelliteHD = object : OnlineTileSourceBase(
        "GoogleSatHD", 0, 20, 512, "",
        arrayOf(
            "https://mt0.google.com/vt/lyrs=y&x=",
            "https://mt1.google.com/vt/lyrs=y&x=",
            "https://mt2.google.com/vt/lyrs=y&x=",
            "https://mt3.google.com/vt/lyrs=y&x="
        )
    ) {
        override fun getTileURLString(pTileIndex: Long): String {
            return hdUrl(MapTileIndex.getX(pTileIndex), MapTileIndex.getY(pTileIndex), MapTileIndex.getZoom(pTileIndex), baseUrl)
        }
    }

    // Road map at night, retina — same Google URLs, transformed on-device by NightTiles.
    // Cached under its own name so night + day tiles never mix.
    val GoogleRoadNightHD = object : OnlineTileSourceBase(
        "GoogleRoadNightHD", 0, 20, 512, "",
        arrayOf(
            "https://mt0.google.com/vt/lyrs=m&x=",
            "https://mt1.google.com/vt/lyrs=m&x=",
            "https://mt2.google.com/vt/lyrs=m&x=",
            "https://mt3.google.com/vt/lyrs=m&x="
        )
    ) {
        override fun getTileURLString(pTileIndex: Long): String {
            return hdUrl(MapTileIndex.getX(pTileIndex), MapTileIndex.getY(pTileIndex), MapTileIndex.getZoom(pTileIndex), baseUrl)
        }
    }
}
