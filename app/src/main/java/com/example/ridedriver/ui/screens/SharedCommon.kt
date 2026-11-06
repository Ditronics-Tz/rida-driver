package com.example.ridedriver.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.repeatOnLifecycle
import com.example.ridedriver.data.remote.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException

internal fun transportError(e: Exception): String {
    if (e is CancellationException) throw e
    if (e is HttpException) {
        val message = runCatching {
            org.json.JSONObject(e.response()?.errorBody()?.string().orEmpty()).getString("error")
        }
            .getOrNull()
        return message ?: "Request failed (HTTP ${e.code()})."
    }
    if (e is IllegalArgumentException || e is IllegalStateException)
        return e.message ?: "Invalid request."
    return "Connection interrupted. Check your connection and retry."
}

// Persist a key before sending the command. Retrying an ambiguous network failure
// after process death uses the same key, scoped to the authenticated account.
internal suspend fun <T> rideCommand(
    context: Context,
    actor: String,
    payload: String,
    call: suspend (String) -> T,
): T {
    val prefs = context.getSharedPreferences("shared_ride_requests", Context.MODE_PRIVATE)
    val digest = MessageDigest.getInstance("SHA-256").digest(("$actor/$payload").toByteArray())
    val fingerprint = android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP)
    val key =
        prefs.getString(fingerprint, null)
            ?: UUID.randomUUID().toString().also {
                check(prefs.edit().putString(fingerprint, it).commit())
            }
    try {
        return call(key).also { prefs.edit().remove(fingerprint).apply() }
    } catch (e: HttpException) {
        if (e.code() in 400..499 && e.code() != 429) prefs.edit().remove(fingerprint).apply()
        throw e
    }
}

@Composable
internal fun PlaceField(label: String, selected: SharedPoint?, onSelect: (SharedPoint?) -> Unit) {
    var query by remember { mutableStateOf(selected?.address.orEmpty()) }
    var results by remember { mutableStateOf(emptyList<NominatimPlace>()) }
    var error by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column {
        OutlinedTextField(
            query,
            {
                query = it
                results = emptyList()
                onSelect(null)
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            enabled = !searching && query.isNotBlank(),
            onClick = {
                val search = query
                scope.launch {
                    searching = true
                    try {
                        val matches = MapServiceFactory.nominatim.search(search)
                        if (search == query) {
                            results = matches
                            error = if (matches.isEmpty()) "No matching places." else ""
                        }
                    } catch (e: Exception) {
                        error = transportError(e)
                    } finally {
                        searching = false
                    }
                }
            },
        ) {
            Text(if (searching) "Searching…" else "Find place")
        }
        results.forEach { place ->
            TextButton(
                onClick = {
                    val lat = place.lat.toDoubleOrNull()
                    val lng = place.lon.toDoubleOrNull()
                    if (lat != null && lng != null) {
                        query = place.displayName
                        onSelect(SharedPoint(lat, lng, query.take(250)))
                        results = emptyList()
                        error = ""
                    }
                }
            ) {
                Text(place.displayName)
            }
        }
        if (error.isNotEmpty()) Text(error)
    }
}

@Composable
internal fun UseLocation(onPoint: (SharedPoint) -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    val token = remember { CancellationTokenSource() }
    DisposableEffect(token) { onDispose { token.cancel() } }
    fun locate() {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            message = "Choose a pickup area using search."
            return
        }
        message = "Getting your location…"
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token)
            .addOnSuccessListener { location ->
                if (location == null) message = "Location unavailable. Search for a pickup area."
                else {
                    message = ""
                    onPoint(SharedPoint(location.latitude, location.longitude, "Current location"))
                }
            }
            .addOnFailureListener { message = "Location unavailable. Search for a pickup area." }
    }
    val permissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            locate()
        }
    TextButton(
        onClick = {
            permissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            )
        }
    ) {
        Text("Use current location")
    }
    if (message.isNotEmpty()) Text(message)
}

@android.annotation.SuppressLint("MissingPermission")
@Composable
internal fun ShareDriverLocation(trip: String) {
    val context = LocalContext.current
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var status by remember { mutableStateOf("Driver location unavailable") }
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted = it
        }
    if (!granted) {
        TextButton(onClick = { request.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
            Text("Share live location on this trip")
        }
    } else {
        Text(status)
        LaunchedEffect(trip, owner) {
            owner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                kotlinx.coroutines.coroutineScope {
                    val scope = this
                    val client = LocationServices.getFusedLocationProviderClient(context)
                    val callback =
                        object : com.google.android.gms.location.LocationCallback() {
                            override fun onLocationResult(
                                result: com.google.android.gms.location.LocationResult
                            ) {
                                val point = result.lastLocation ?: return
                                val age =
                                    android.os.SystemClock.elapsedRealtimeNanos() -
                                        point.elapsedRealtimeNanos
                                if (age !in 0..120_000_000_000L || point.accuracy > 100) {
                                    status = "Waiting for a fresh GPS fix"
                                    return
                                }
                                scope.launch {
                                    try {
                                        val sent =
                                            RidaApiClient.driverApi.updateLocation(
                                                DriverLocationRequest(
                                                    point.latitude,
                                                    point.longitude,
                                                    accuracy = point.accuracy,
                                                )
                                            )
                                        status =
                                            if (sent.isSuccessful) "Sharing live trip location"
                                            else "Could not share trip location"
                                    } catch (e: Exception) {
                                        status = transportError(e)
                                    }
                                }
                            }
                        }
                    try {
                        client
                            .requestLocationUpdates(
                                com.google.android.gms.location.LocationRequest.Builder(
                                        Priority.PRIORITY_HIGH_ACCURACY,
                                        10000,
                                    )
                                    .build(),
                                callback,
                                android.os.Looper.getMainLooper(),
                            )
                            .addOnFailureListener { status = "Driver location unavailable" }
                        kotlinx.coroutines.awaitCancellation()
                    } finally {
                        client.removeLocationUpdates(callback)
                    }
                }
            }
        }
    }
}
