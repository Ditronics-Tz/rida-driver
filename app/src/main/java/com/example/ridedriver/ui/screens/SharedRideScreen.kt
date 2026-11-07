package com.example.ridedriver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.ridedriver.data.remote.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
fun SharedRideScreen(back: () -> Unit) {
 val context=LocalContext.current
 val session by com.example.ridedriver.data.local.SessionManager.observe(context).collectAsState(initial=com.example.ridedriver.data.local.SessionManager.Session())
 key(session.loggedIn,session.sessionId) { if(session.loggedIn) SharedRideContent(back) else Text("Sign in to view shared rides.") }
}

@Composable
private fun SharedRideContent(back: () -> Unit) {
    val api = RidaApiClient.transport
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    val lock = remember { Mutex() }
    var actor by remember { mutableStateOf("") }
    var fees by remember { mutableStateOf<TransportFees?>(null) }
    var mine by remember { mutableStateOf(emptyList<SharedTrip>()) }
    var active by remember { mutableStateOf<SharedTrip?>(null) }
    var error by remember { mutableStateOf("") }
    var connection by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
 var cursor by remember { mutableLongStateOf(0) }
    var historyOffset by rememberSaveable {mutableIntStateOf(0)}
 var history by rememberSaveable { mutableStateOf(false) }

    var pickup by remember { mutableStateOf<SharedPoint?>(null) }
    var dropoff by remember { mutableStateOf<SharedPoint?>(null) }
    var capacity by rememberSaveable { mutableStateOf("") }
    var seats by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var review by remember {mutableStateOf(false)}
 var routeEstimate by remember {mutableStateOf("")}
 var wait by rememberSaveable { mutableStateOf("") }
    var cancelReason by remember { mutableStateOf("") }
 var cancelPrompt by remember { mutableStateOf(false) }

    suspend fun refresh() {
        if (actor.isEmpty()) actor = RidaApiClient.api.me().user.id
        fees = api.fees()
        if (capacity.isBlank()) {
            capacity = RidaApiClient.driverApi.verification().profile.passenger_seats
                .takeIf { it > 0 }?.toString().orEmpty()
        }
        val activePage=mergeTransportSnapshots(listOfNotNull(active)+mine,api.list("mine").trips)
 val page=if(history&&historyOffset>0)api.list("mine",offset=historyOffset).trips else activePage
 mine=mergeTransportSnapshots(mine,page)
        active = activePage.firstOrNull { it.status in listOf("waiting", "in_progress") }

        connection = ""
    }
    LaunchedEffect(owner,history,historyOffset) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                try {
                    lock.withLock { refresh() }
                } catch (e: Exception) {
                    connection = transportError(e)
                }
                try {
                    val trip = active
                    if (connection.isNotEmpty()) delay(2000)
                    else if (trip != null) api.waitForUpdate(tripId = trip.id,after=cursor).also { cursor=it.cursor } else delay(2000)
                } catch (e: Exception) {
                    connection = transportError(e)
                    delay(2000)
                }
            }
        }
    }
    fun perform(signature: String, command: suspend (String) -> SharedTrip) {
        if (busy || actor.isBlank()) return
        busy = true
        error = ""
        scope.launch {
            try {
                lock.withLock {
                    val trip = rideCommand(context, actor, signature, command)
                    active = trip
                    refresh()
                }
            } catch (e: Exception) {
                error = transportError(e)
            } finally {
                busy = false
            }
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("shared-trip-content").padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = back) { Text("Back") }
                TextButton(onClick = { history = !history }) {
                    Text(if (history) "Current trip" else "My trips")
                }
            }
        }
        item { Text("Shared rides", style = MaterialTheme.typography.headlineSmall) }
        if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
        if (connection.isNotBlank()) item { Text(connection) }
        if (history) {
            if (mine.isEmpty()) item { Text("No shared trips yet.") }
            mine.forEach { t -> item(key = t.id) { TripSummary(t);SharedTripHelp(t); if(t.status=="completed")t.manifest.filter{it.status=="completed"}.forEach{SharedTripRating(t,it.id)} } }
 item {Row{TextButton(enabled=historyOffset>0,onClick={historyOffset=(historyOffset-50).coerceAtLeast(0)}){Text("Previous history")};TextButton(enabled=mine.size==50,onClick={historyOffset+=50}){Text("Next history")}}}
        } else {
            val trip = active
            if (trip != null) {
                item {
                    TripSummary(trip)
 SharedTripHelp(trip)
                    ShareDriverLocation(trip.id)
 NavigateToSharedDestination(trip.dropoff)
                }

                item {
                    Text(
                        "Whole-trip fee: TZS ${trip.fee}. Verified fares: TZS ${trip.confirmed * trip.price}."
                    )
                }
                trip.manifest.forEach { seat ->
                    item(key = seat.id) {
                        var pin by remember(seat.id) { mutableStateOf("") }
                        Text("${trip.passenger_names[seat.id] ?: "Passenger"} · ${seat.status}")
 Text("Hold expires: "+java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(java.util.Date(seat.expires*1000)))
                        if (seat.status == "held") {
                            OutlinedTextField(
                                pin,
                                { pin = it.filter(Char::isDigit).take(6) },
                                label = { Text("Passenger pickup code") },
                                keyboardOptions =
                                    KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            )
                            Button(
                                enabled = !busy && pin.length == 6,
                                onClick = {
                                    perform("verify/${trip.id}/${seat.id}/$pin") { key ->
                                        api.act(trip.id, "verify", key, PickupAction(seat.id, pin))
                                    }
                                },
                            ) {
                                Text("Verify at pickup")
                            }
                        }
                    }
                }
                if (trip.status == "waiting")
                    item {
                        Button(
                            enabled = !busy && trip.confirmed > 0 && trip.held == 0,
                            onClick = {
                                perform("start/${trip.id}") { api.act(trip.id, "start", it) }
                            },
                        ) {
                            Text("Start shared trip")
                        }
                        TextButton(enabled = !busy, onClick = { cancelPrompt = true }) {
                            Text("Cancel trip")
                        }
                        if (cancelPrompt)
                            AlertDialog(
                                onDismissRequest = { cancelPrompt = false },
                                title = { Text("Cancel this trip?") },
                                text = { Column {Text("This releases every passenger seat.");OutlinedTextField(cancelReason,{cancelReason=it.take(250)},label={Text("Cancellation reason")})} },
                                confirmButton = {
                                    TextButton(
 enabled=cancelReason.isNotBlank(),
                                        onClick = {
                                            cancelPrompt = false
                                            perform("cancel/${trip.id}/$cancelReason") {
                                                api.act(trip.id, "cancel", it,PickupAction(reason=cancelReason))
                                            }
                                        }
                                    ) {
                                        Text("Cancel trip")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { cancelPrompt = false }) {
                                        Text("Keep trip")
                                    }
                                },
                            )
                    }
                if (trip.status == "in_progress")
                    item {
                        Button(
                            enabled = !busy,
                            onClick = {
                                perform("complete/${trip.id}") { api.act(trip.id, "complete", it) }
                            },
                        ) {
                            Text("Complete shared trip")
                        }
                    }
            } else {
                if (fees == null) item { Text("Loading trip settings…") }
                else if (fees?.enabled != true)
                    item { Text("Ride sharing is closed for new bookings.") }
                else {
                    item {
                        PlaceField("Pickup", pickup) { pickup = it }
                        UseLocation { pickup = it }
                        pickup?.let { Text("Pickup: ${it.address}") }
                    }
                    item { PlaceField("Destination", dropoff) { dropoff = it } }
                    item {
                        NumberField("Passenger seats in your vehicle (exclude driver)", capacity) {
                            capacity = it
                        }
                    }
                    item { NumberField("Seats offered on this trip", seats) { seats = it } }
                    item { NumberField("Price per seat (TZS)", price) { price = it } }
                    item { NumberField("Minutes to wait (6–180)", wait) { wait = it } }
                    item {
                        Text(
                            "Rida charges TZS ${fees?.shared_fee} once when you complete this trip."
                        )
 val estimatedGross=(seats.toLongOrNull()?:0)*(price.toLongOrNull()?:0)
 Text("If all offered seats are verified: gross TZS $estimatedGross · fee TZS ${fees?.shared_fee} · net TZS ${estimatedGross-(fees?.shared_fee?:0)}")
 Text("Cash fares are paid to you. The fixed platform fee can exceed your fare revenue and is owed once per completed trip.")
 if(review) AlertDialog(onDismissRequest={review=false},title={Text("Review shared trip")},text={Column {
 Text("${pickup?.address} → ${dropoff?.address}")
 Text("$seats seats · TZS $price per seat · $wait minute wait")
 Text("Whole-trip fee TZS ${fees?.shared_fee}")
 SharedRoutePreview(pickup,dropoff)
 }},confirmButton={TextButton(enabled=!busy,onClick={
 val p=pickup;val d=dropoff
 if(p!=null&&d!=null){val request=PublishTrip(p,d,capacity.toInt(),seats.toInt(),price.toLong(),wait.toInt());review=false;perform("publish/$request"){api.publish(it,request)}}
 }){Text("Publish trip")}},dismissButton={TextButton(onClick={review=false}){Text("Edit")}})

                        Button(
                            enabled = !busy && actor.isNotEmpty(),
                            onClick = {
                                val p = pickup
                                val d = dropoff
                                val c = capacity.toIntOrNull() ?: 0
                                val n = seats.toIntOrNull() ?: 0
                                val fare = price.toLongOrNull() ?: 0
                                val minutes = wait.toIntOrNull() ?: 0
                                if (
                                    p == null ||
                                        d == null ||
                                        c !in 1..100 ||
                                        n !in 1..c ||
                                        fare < 1 ||
                                        minutes !in 6..180
                                )
                                    error =
                                        "Choose both places, enter capacity and seats, a price and a 6–180 minute wait."
                                else {
                                    review=true
                                }
                            },
                        ) {
                            Text("Review trip")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripSummary(t: SharedTrip) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            "${t.pickup.address} → ${t.dropoff.address}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text("${t.vehicle} · TZS ${t.price} per seat · ${t.status}")
        Text("${t.available} available · ${t.held} held · ${t.confirmed} verified")
 Text("Pickup deadline: "+java.text.SimpleDateFormat("dd MMM HH:mm",java.util.Locale.getDefault()).apply{timeZone=java.util.TimeZone.getTimeZone("Africa/Dar_es_Salaam")}.format(java.util.Date(t.deadline*1000)))
 if(t.driver_name.isNotBlank()) Text("Driver: ${t.driver_name}")
 t.receipt?.let { r->
 Text("Trip receipt · Cash paid directly to driver")
 Text("Gross TZS ${r.gross} · Whole-trip fee TZS ${r.fee} · Driver net TZS ${r.net}")
 Text(if(r.status=="wallet_debited") "Platform fee posted to driver wallet" else "Platform fee receivable — collection pending")
 }
 t.my_seat?.takeIf{it.status=="completed"}?.let{Text("Your cash fare: TZS ${t.price}") }
    }
}

@Composable
private fun NumberField(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(
        value,
        { change(it.filter(Char::isDigit)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
