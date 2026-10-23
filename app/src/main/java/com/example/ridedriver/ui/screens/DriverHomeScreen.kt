package com.example.ridedriver.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridedriver.data.driver.ActiveTrip
import com.example.ridedriver.data.driver.DriverRepositoryProvider
import com.example.ridedriver.data.driver.DriverRules
import com.example.ridedriver.data.driver.RideOffer
import com.example.ridedriver.data.driver.TripPhase
import com.example.ridedriver.data.driver.TripRecord
import com.example.ridedriver.data.remote.MapServiceFactory
import com.example.ridedriver.data.remote.osrmCoords
import com.example.ridedriver.ui.common.TzMoney
import com.example.ridedriver.ui.components.RidaIcon
import com.example.ridedriver.ui.components.RidaIcons
import com.example.ridedriver.ui.components.UserAvatar
import com.example.ridedriver.ui.map.RidaMapView
import com.example.ridedriver.ui.map.animateTo
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import com.example.ridedriver.ui.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DriverHomeScreen(
    onNavigateToEarnings: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val repo = remember { DriverRepositoryProvider.get() }
    val state by repo.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPreview = LocalInspectionMode.current

    var mapCenter by remember { mutableStateOf(GeoPoint(-6.7924, 39.2083)) }
    var mapZoom by remember { mutableStateOf(14.5) }
    var currentGeoLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    var showCancelDialog by remember { mutableStateOf(false) }

    // Location Permission
    val permissionState = if (isPreview) null else rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val isPermissionGranted = permissionState?.status?.isGranted == true
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        if (!isPermissionGranted) return
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    val gp = GeoPoint(loc.latitude, loc.longitude)
                    currentGeoLocation = gp
                    mapCenter = gp
                    mapViewRef?.animateTo(gp, 15.5)
                    scope.launch {
                        repo.updateLocation(loc.latitude, loc.longitude, loc.bearing)
                    }
                }
            }
    }

    LaunchedEffect(Unit) {
        if (!isPermissionGranted && !isPreview) {
            permissionState?.launchPermissionRequest()
        }
    }

    LaunchedEffect(isPermissionGranted) {
        if (isPermissionGranted) {
            fetchLocation()
        }
    }

    // Recalculate route whenever active trip target changes
    LaunchedEffect(state.trip?.phase, state.trip?.id, currentGeoLocation) {
        val trip = state.trip
        if (trip != null) {
            val origin = currentGeoLocation ?: mapCenter
            val dest = trip.target
            try {
                val coords = osrmCoords(origin.longitude, origin.latitude, dest.lng, dest.lat)
                val resp = MapServiceFactory.osrm.route(coords = coords)
                val coordsList = resp.routes.firstOrNull()?.geometry?.coordinates
                if (!coordsList.isNullOrEmpty()) {
                    routePoints = coordsList.map { GeoPoint(it[1], it[0]) }
                }
            } catch (_: Exception) {
                routePoints = listOf(origin, GeoPoint(dest.lat, dest.lng))
            }
        } else {
            routePoints = emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        val targetPoint = state.trip?.let { GeoPoint(it.target.lat, it.target.lng) }
        RidaMapView(
            modifier = Modifier.fillMaxSize(),
            center = mapCenter,
            zoom = mapZoom,
            currentLocation = currentGeoLocation,
            destination = targetPoint,
            routePoints = routePoints,
            enableMyLocation = isPermissionGranted,
            onMapReady = { mapViewRef = it }
        )

        // Top Status Header (Profile, Rating, Earnings pill)
        TopBarOverlay(
            driverName = state.profile.name.ifBlank { "Driver" },
            rating = state.profile.rating,
            tripsCount = state.history.size,
            todayGrossTzs = state.history.take(4).sumOf { it.netTzs },
            onEarningsClick = onNavigateToEarnings,
            onProfileClick = onNavigateToProfile,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        // Floating Online/Offline Switch & Re-center button
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 74.dp, end = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Re-center FAB
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable { fetchLocation() },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RidaIcon(icon = RidaIcons.Locate, contentDescription = "My location", tint = RidaInk, modifier = Modifier.size(22.dp))
                }
            }
        }

        // Bottom Panels Area
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Online/Offline Main Toggle Bar (shown when not in an active trip)
            if (state.trip == null && state.offer == null) {
                OnlineTogglePill(
                    online = state.online,
                    goingOnline = state.goingOnline,
                    onToggle = {
                        scope.launch {
                            repo.setOnline(!state.online)
                        }
                    }
                )
            }

            // Incoming Ride Offer Sheet
            AnimatedVisibility(
                visible = state.offer != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                state.offer?.let { offer ->
                    IncomingOfferCard(
                        offer = offer,
                        onAccept = {
                            scope.launch {
                                val res = repo.acceptOffer(offer.id)
                                if (res.isFailure) {
                                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "Could not accept", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDecline = {
                            scope.launch {
                                repo.declineOffer(offer.id)
                            }
                        }
                    )
                }
            }

            // Active Trip Bottom Sheet
            AnimatedVisibility(
                visible = state.trip != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                state.trip?.let { trip ->
                    ActiveTripPanel(
                        trip = trip,
                        onAdvancePhase = {
                            scope.launch {
                                repo.advanceTripPhase()
                            }
                        },
                        onCallPassenger = {
                            val phone = trip.offer.passengerPhone ?: "+255712345678"
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        },
                        onMessagePassenger = {
                            val phone = trip.offer.passengerPhone ?: "+255712345678"
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")))
                        },
                        onNavigateExternal = {
                            val dest = trip.target
                            val uri = Uri.parse("google.navigation:q=${dest.lat},${dest.lng}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Maps application not found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCancelClick = { showCancelDialog = true }
                    )
                }
            }
        }

        // Trip Completion & Cash Collection Modal
        state.lastCompleted?.let { record ->
            TripCompletionDialog(
                record = record,
                onRate = { score ->
                    scope.launch {
                        repo.ratePassenger(record.id, score)
                        repo.dismissCompletedTrip()
                    }
                },
                onDismiss = {
                    scope.launch {
                        repo.dismissCompletedTrip()
                    }
                }
            )
        }

        // Cancel Active Trip Confirmation Dialog
        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Cancel Trip?", fontWeight = FontWeight.Bold, color = RidaInk) },
                text = { Text("Are you sure you want to cancel this trip? Frequent cancellations affect your driver acceptance rating.", color = RidaSecondary) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCancelDialog = false
                            scope.launch {
                                repo.cancelActiveTrip("Driver cancelled")
                            }
                        }
                    ) {
                        Text("Yes, Cancel", color = RidaRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("Keep Driving", color = RidaActionEnd)
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

@Composable
private fun TopBarOverlay(
    driverName: String,
    rating: Double,
    tripsCount: Int,
    todayGrossTzs: Long,
    onEarningsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Driver Profile info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onProfileClick)
                    .padding(4.dp)
            ) {
                UserAvatar(name = driverName, photoUrl = null, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(text = driverName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RidaIcon(icon = RidaIcons.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(text = String.format("%.2f", rating), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
                        Text(text = " • $tripsCount trips", fontSize = 11.sp, color = RidaSecondary)
                    }
                }
            }

            // Quick earnings pill
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onEarningsClick),
                shape = RoundedCornerShape(14.dp),
                color = RidaPrimary.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RidaIcon(icon = RidaIcons.Earnings, contentDescription = null, tint = RidaActionEnd, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = TzMoney.format(todayGrossTzs.toInt()),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RidaForest
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineTogglePill(
    online: Boolean,
    goingOnline: Boolean,
    onToggle: () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    val bgColors = if (online) listOf(RidaForest, RidaActionEnd) else listOf(Color(0xFF374151), Color(0xFF1F2937))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .clickable(enabled = !goingOnline, role = Role.Switch, onClick = onToggle),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(bgColors))
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (goingOnline) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (online) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Spacer(Modifier.width(10.dp))
                }

                Text(
                    text = if (online) "YOU'RE ONLINE • READY FOR RIDES" else "YOU'RE OFFLINE • TAP TO GO ONLINE",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun IncomingOfferCard(
    offer: RideOffer,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var secondsLeft by remember(offer.id) { mutableIntStateOf(offer.expiresInSeconds) }

    LaunchedEffect(offer.id) {
        while (secondsLeft > 0 && isActive) {
            delay(1000)
            secondsLeft--
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with Timer + Fare
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(42.dp)) {
                        CircularProgressIndicator(
                            progress = { secondsLeft.toFloat() / RideOffer.OFFER_TIMEOUT_SECONDS },
                            modifier = Modifier.fillMaxSize(),
                            color = RidaPrimary,
                            trackColor = Color(0xFFE5EAE7),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "${secondsLeft}s",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RidaInk
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(text = "New Ride Request", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                        Text(
                            text = "${offer.distanceToPickupMeters}m to pickup",
                            fontSize = 12.sp,
                            color = RidaActionEnd,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Fare Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = TzMoney.format(offer.fareTzs.toInt()),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RidaForest
                    )
                    Text(
                        text = if (offer.paymentMethod == "cash") "Cash" else "Mobile Money",
                        fontSize = 11.sp,
                        color = RidaSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = RidaBorder)
            Spacer(Modifier.height(14.dp))

            // Passenger info
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(name = offer.passengerName, photoUrl = null, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Text(text = offer.passengerName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
                Spacer(Modifier.width(6.dp))
                RidaIcon(icon = RidaIcons.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(2.dp))
                Text(text = String.format("%.1f", offer.passengerRating), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${offer.tripDistanceMeters / 1000} km trip",
                    fontSize = 12.sp,
                    color = RidaSecondary
                )
            }

            Spacer(Modifier.height(14.dp))

            // Pickup & Dropoff routes
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RidaPrimary))
                    Spacer(Modifier.width(8.dp))
                    Text(text = offer.pickup.address, fontSize = 13.sp, color = RidaInk, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                    Spacer(Modifier.width(8.dp))
                    Text(text = offer.dropoff.address, fontSize = 13.sp, color = RidaSecondary, maxLines = 1)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Accept & Decline buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RidaBorder)
                ) {
                    Text("Decline", color = RidaSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                val acceptShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(50.dp)
                        .clip(acceptShape)
                        .background(Brush.horizontalGradient(listOf(RidaForest, RidaActionEnd)))
                        .clickable(role = Role.Button, onClick = onAccept),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Accept Ride", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ActiveTripPanel(
    trip: ActiveTrip,
    onAdvancePhase: () -> Unit,
    onCallPassenger: () -> Unit,
    onMessagePassenger: () -> Unit,
    onNavigateExternal: () -> Unit,
    onCancelClick: () -> Unit
) {
    val phase = trip.phase

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Phase indicator badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RidaPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = phase.title.uppercase(),
                        color = RidaActionEnd,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = TzMoney.format(trip.offer.fareTzs.toInt()),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RidaForest
                )
            }

            Spacer(Modifier.height(12.dp))

            // Passenger profile row + Contact buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(name = trip.offer.passengerName, photoUrl = null, modifier = Modifier.size(42.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = trip.offer.passengerName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                    Text(text = if (trip.offer.paymentMethod == "cash") "Cash Payment" else "Mobile Money", fontSize = 12.sp, color = RidaSecondary)
                }

                // Call button
                IconButton(
                    onClick = onCallPassenger,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(RidaPrimary.copy(alpha = 0.1f))
                ) {
                    RidaIcon(icon = RidaIcons.Phone, contentDescription = "Call", tint = RidaActionEnd, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                // Message button
                IconButton(
                    onClick = onMessagePassenger,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(RidaPrimary.copy(alpha = 0.1f))
                ) {
                    RidaIcon(icon = RidaIcons.Chat, contentDescription = "Chat", tint = RidaActionEnd, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = RidaBorder)
            Spacer(Modifier.height(12.dp))

            // Target destination address
            Row(verticalAlignment = Alignment.CenterVertically) {
                RidaIcon(
                    icon = if (phase == TripPhase.IN_TRIP) RidaIcons.SavedPlace else RidaIcons.Pickup,
                    contentDescription = null,
                    tint = if (phase == TripPhase.IN_TRIP) Color(0xFFEF4444) else RidaActionEnd,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (phase == TripPhase.IN_TRIP) "Dropoff Destination" else "Pickup Location",
                        fontSize = 11.sp,
                        color = RidaSecondary
                    )
                    Text(
                        text = trip.target.address,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RidaInk,
                        maxLines = 1
                    )
                }

                // Google Maps External Navigate Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onNavigateExternal),
                    color = Color(0xFFF3F4F6)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RidaIcon(icon = RidaIcons.Navigation, contentDescription = null, tint = RidaInk, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Advance Phase Primary Action Button
            val actionShape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(actionShape)
                    .background(Brush.horizontalGradient(listOf(RidaForest, RidaActionEnd)))
                    .clickable(role = Role.Button, onClick = onAdvancePhase),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DriverRules.primaryActionLabel(phase).uppercase(),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.width(8.dp))
                RidaIcon(icon = RidaIcons.Forward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            if (DriverRules.canCancel(phase)) {
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = onCancelClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel trip", color = RidaRed, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun TripCompletionDialog(
    record: TripRecord,
    onRate: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedScore by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(RidaPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    RidaIcon(icon = RidaIcons.Check, contentDescription = null, tint = RidaActionEnd, modifier = Modifier.size(28.dp))
                }

                Spacer(Modifier.height(14.dp))
                Text(text = "Trip Completed!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                Text(text = "Passenger: ${record.passengerName}", fontSize = 13.sp, color = RidaSecondary)

                Spacer(Modifier.height(16.dp))

                // Cash Collection Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (record.paymentMethod == "cash") Color(0xFFFEF3C7) else Color(0xFFE0F2FE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (record.paymentMethod == "cash") "COLLECT CASH FROM PASSENGER" else "PAID VIA MOBILE MONEY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (record.paymentMethod == "cash") Color(0xFF92400E) else Color(0xFF0369A1)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = TzMoney.format(record.fareTzs.toInt()),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RidaInk
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Fare Breakdown
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gross Fare", fontSize = 13.sp, color = RidaSecondary)
                        Text(TzMoney.format(record.fareTzs.toInt()), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = RidaInk)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rida Fee (15%)", fontSize = 13.sp, color = RidaSecondary)
                        Text("- ${TzMoney.format(record.commissionTzs.toInt())}", fontSize = 13.sp, color = RidaRed)
                    }
                    HorizontalDivider(color = RidaBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Your Net Earnings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                        Text(TzMoney.format(record.netTzs.toInt()), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RidaActionEnd)
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Rate Passenger Star Selector
                Text("Rate Passenger", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { selectedScore = star },
                            modifier = Modifier.size(36.dp)
                        ) {
                            RidaIcon(
                                icon = RidaIcons.Star,
                                contentDescription = "$star stars",
                                tint = if (star <= selectedScore) Color(0xFFF59E0B) else Color(0xFFD1D5DB),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val shape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(shape)
                    .background(Brush.horizontalGradient(listOf(RidaForest, RidaActionEnd)))
                    .clickable { onRate(selectedScore) },
                contentAlignment = Alignment.Center
            ) {
                Text("Done • Ready for Next Ride", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
}
