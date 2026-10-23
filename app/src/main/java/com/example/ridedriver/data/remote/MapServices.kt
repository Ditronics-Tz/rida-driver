package com.example.ridedriver.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ---------------------------------------------------------------------------
// Nominatim — free place search
// https://nominatim.openstreetmap.org/search?q=Kariakoo,Dar+es+Salaam&format=jsonv2
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class NominatimPlace(
    @param:Json(name = "place_id") val placeId: Long,
    @param:Json(name = "display_name") val displayName: String,
    val lat: String,
    val lon: String,
    val type: String? = null,
    @param:Json(name = "importance") val importance: Double? = null
)

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 5,
        @Query("countrycodes") countrycodes: String = "tz",
        @Query("viewbox") viewbox: String? = "33.0,-7.5,40.5,-5.0", // Dar clamp
        @Query("bounded") bounded: Int = 0,
        @Query("addressdetails") addressdetails: Int = 1
    ): List<NominatimPlace>

    // Reverse geocode (for "Your location" label)
    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "jsonv2"
    ): NominatimPlace
}

// ---------------------------------------------------------------------------
// OSRM — free routing
// https://router.project-osrm.org/route/v1/driving/{lon1},{lat1};{lon2},{lat2}?overview=full&geometries=geojson
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class OsrmResponse(
    val code: String,
    val routes: List<OsrmRoute>,
    val waypoints: List<OsrmWaypoint>? = null
)

@JsonClass(generateAdapter = true)
data class OsrmRoute(
    val distance: Double, // meters
    val duration: Double, // seconds
    val geometry: OsrmGeometry
)

@JsonClass(generateAdapter = true)
data class OsrmGeometry(
    val coordinates: List<List<Double>>, // [lon, lat][]
    val type: String = "LineString"
)

@JsonClass(generateAdapter = true)
data class OsrmWaypoint(
    val name: String?,
    val location: List<Double>
)

interface OsrmApi {
    @GET("route/v1/driving/{coords}")
    suspend fun route(
        @Path(value = "coords", encoded = true) coords: String, // "lon1,lat1;lon2,lat2"
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("steps") steps: Boolean = false
    ): OsrmResponse
}

// ---------------------------------------------------------------------------
// Factory — provide configured Retrofit services
// ---------------------------------------------------------------------------

object MapServiceFactory {
    // Moshi needs the Kotlin adapter: our models are Kotlin data classes and
    // there is no codegen step, so without this EVERY response throws on parse.
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun moshiFactory() = MoshiConverterFactory.create(moshi)

    private val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                // Nominatim requires a valid User-Agent
                .header("User-Agent", "RIDEPASSENGER2/1.0 (student-mvp)")
                .build()
            chain.proceed(req)
        }
        .build()

    // Nominatim base: https://nominatim.openstreetmap.org/
    val nominatim: NominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttp)
            .addConverterFactory(moshiFactory())
            .build()
            .create(NominatimApi::class.java)
    }

    // OSRM demo server; swap to self-hosted for production
    val osrm: OsrmApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .client(okHttp)
            .addConverterFactory(moshiFactory())
            .build()
            .create(OsrmApi::class.java)
    }
}

// Convenience extension to build OSRM coords string
fun osrmCoords(fromLon: Double, fromLat: Double, toLon: Double, toLat: Double): String =
    "$fromLon,$fromLat;$toLon,$toLat"
