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
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.repeatOnLifecycle
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
 var manual by remember {mutableStateOf(false)}
 var latitude by remember {mutableStateOf("")}
 var longitude by remember {mutableStateOf("")}
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
            modifier = Modifier.fillMaxWidth().testTag("$label-query"),
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
        TextButton(modifier=Modifier.testTag("$label-manual"),onClick={manual=!manual}){Text("Choose coordinates manually")}
 if(manual) {
 OutlinedTextField(latitude,{latitude=it},label={Text("Latitude")},modifier=Modifier.testTag("$label-latitude"))
 OutlinedTextField(longitude,{longitude=it},label={Text("Longitude")},modifier=Modifier.testTag("$label-longitude"))
 TextButton(modifier=Modifier.testTag("$label-use-coordinates"),onClick={val lat=latitude.toDoubleOrNull();val lng=longitude.toDoubleOrNull();if(lat!=null&&lng!=null&&lat in -90.0..90.0&&lng in -180.0..180.0&&query.isNotBlank()){onSelect(SharedPoint(lat,lng,query.take(250)));manual=false;error=""}else error="Enter a place label and valid coordinates."}){Text("Use coordinates")}
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
 val serviceState by com.example.ridedriver.data.location.DriverLocationService.state.collectAsState()
 Text(serviceState)
 LaunchedEffect(trip,owner){owner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED){
 com.example.ridedriver.data.location.DriverLocationService.startFromVisibleScreen(context)
 kotlinx.coroutines.awaitCancellation()
 }}
 }
}

@Composable
internal fun SharedTripHelp(trip: SharedTrip) {
 val scope=rememberCoroutineScope()
 var open by remember {mutableStateOf(false)}
 var description by remember {mutableStateOf("")}
 var busy by remember {mutableStateOf(false)}
 var notice by remember {mutableStateOf("")}
 var location by remember {mutableStateOf<SharedPoint?>(null)}
 TextButton(onClick={open=!open}) {Text("Safety and trip support")}
 if(open) {
 OutlinedTextField(description,{description=it.take(4000)},label={Text("Describe the trip issue")})
 Button(enabled=!busy&&description.isNotBlank(),onClick={busy=true;scope.launch{try{
 RidaApiClient.transport.support(SharedSupportInput("Shared trip support",description,shared_trip_id=trip.id));notice="Support request sent with your shared-trip reference.";description=""
 }catch(e:Exception){notice=transportError(e)}finally{busy=false}}}){Text("Contact trip support")}
 if(trip.status in listOf("waiting","in_progress")){
 Text("For immediate danger, contact local emergency services. Rida SOS alerts the support team; delivery depends on connectivity.")
 UseLocation {location=it}
 PlaceField("Your current location (manual fallback)",location){location=it}
 Button(enabled=!busy&&location!=null,onClick={val p=location;if(p!=null){busy=true;scope.launch{try{
 RidaApiClient.transport.sos(trip.id,SharedSOSInput(p.lat,p.lng,100.0));notice="SOS alert sent to Rida support."
 }catch(e:Exception){notice=transportError(e)}finally{busy=false}}}}){Text("Send SOS for this shared trip")}
 }
 }
 if(notice.isNotBlank()) Text(notice)
}

@Composable
internal fun SharedTripEntry(onOpen:()->Unit,label:String) {
 val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
 var active by remember {mutableStateOf(false)}
 LaunchedEffect(owner) {
 owner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED){
 while(true){try{
 active=RidaApiClient.transport.list("mine").trips.any{it.status in listOf("waiting","in_progress")}
 }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){}
 kotlinx.coroutines.delay(5000)
 }
 }
 }
 TextButton(onClick=onOpen){Text(if(active) "Resume your shared trip" else label)}
}

@Composable
internal fun SharedTripRating(trip:SharedTrip,seatId:String?=null){
 val scope=rememberCoroutineScope();var stars by remember(trip.id,seatId){mutableIntStateOf(0)};var info by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
 Text(if(seatId==null) "Rate your shared-trip driver" else "Rate ${trip.passenger_names[seatId]?:"passenger"}")
 Row{(1..5).forEach{n->TextButton(enabled=!busy,onClick={stars=n}){Text(if(n==stars) "★ $n" else "$n")}}}
 TextButton(enabled=stars>0&&!busy,onClick={busy=true;scope.launch{try{RidaApiClient.transport.rate(trip.id,SharedRatingInput(stars,seat_id=seatId));info="Rating saved."}catch(e:Exception){info=transportError(e)}finally{busy=false}}}){Text("Save shared-trip rating")}
 if(info.isNotBlank())Text(info)
}
