package com.example.ridedriver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridedriver.data.driver.DriverRepositoryProvider
import com.example.ridedriver.data.driver.TripRecord
import com.example.ridedriver.data.driver.TripStatus
import com.example.ridedriver.data.remote.MapServiceFactory
import com.example.ridedriver.data.remote.osrmCoords
import com.example.ridedriver.ui.common.TzMoney
import com.example.ridedriver.ui.components.RidaIcon
import com.example.ridedriver.ui.components.RidaIcons
import com.example.ridedriver.ui.components.UserAvatar
import com.example.ridedriver.ui.map.RidaMapView
import com.example.ridedriver.ui.theme.RidaActionEnd
import com.example.ridedriver.ui.theme.RidaBorder
import com.example.ridedriver.ui.theme.RidaForest
import com.example.ridedriver.ui.theme.RidaInk
import com.example.ridedriver.ui.theme.RidaPrimary
import com.example.ridedriver.ui.theme.RidaRed
import com.example.ridedriver.ui.theme.RidaSecondary
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripsHistoryScreen(
    onTripClick: (String) -> Unit
) {
    val repo = remember { DriverRepositoryProvider.get() }
    val state by repo.state.collectAsState()
    var filter by remember { mutableStateOf<TripStatus?>(null) } // null = All

    val filteredTrips = state.history.filter {
        filter == null || it.status == filter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text("Trips & History", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = RidaInk)
        Text("All completed and cancelled trips in Dar es Salaam", fontSize = 13.sp, color = RidaSecondary)

        Spacer(Modifier.height(16.dp))

        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(label = "All (${state.history.size})", selected = filter == null, onClick = { filter = null })
            FilterChip(
                label = "Completed (${state.history.count { it.status == TripStatus.COMPLETED }})",
                selected = filter == TripStatus.COMPLETED,
                onClick = { filter = TripStatus.COMPLETED }
            )
            FilterChip(
                label = "Cancelled (${state.history.count { it.status == TripStatus.CANCELLED }})",
                selected = filter == TripStatus.CANCELLED,
                onClick = { filter = TripStatus.CANCELLED }
            )
        }

        Spacer(Modifier.height(18.dp))

        if (filteredTrips.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No trips found", fontSize = 14.sp, color = RidaSecondary)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filteredTrips.forEach { trip ->
                    TripHistoryCard(trip = trip, onClick = { onTripClick(trip.id) })
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) RidaPrimary.copy(alpha = 0.15f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) RidaPrimary else RidaBorder)
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) RidaActionEnd else RidaInk,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun TripHistoryCard(
    trip: TripRecord,
    onClick: () -> Unit
) {
    val dateStr = remember(trip.endedAtMillis) {
        SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(trip.endedAtMillis))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(name = trip.passengerName, photoUrl = null, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(text = trip.passengerName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                        Text(text = dateStr, fontSize = 11.sp, color = RidaSecondary)
                    }
                }

                if (trip.status == TripStatus.COMPLETED) {
                    Text(
                        text = TzMoney.format(trip.netTzs.toInt()),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RidaForest
                    )
                } else {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEE2E2)) {
                        Text(
                            text = "Cancelled",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RidaRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = RidaBorder)
            Spacer(Modifier.height(10.dp))

            // Route points
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(RidaPrimary))
                    Spacer(Modifier.width(8.dp))
                    Text(text = trip.pickup.address, fontSize = 12.5.sp, color = RidaInk, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                    Spacer(Modifier.width(8.dp))
                    Text(text = trip.dropoff.address, fontSize = 12.5.sp, color = RidaSecondary, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun TripDetailScreen(
    tripId: String,
    onBack: () -> Unit
) {
    val repo = remember { DriverRepositoryProvider.get() }
    val state by repo.state.collectAsState()
    val trip = state.history.firstOrNull { it.id == tripId }

    if (trip == null) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Trip not found", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RidaInk)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack) { Text("Back to trips") }
        }
        return
    }

    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    LaunchedEffect(trip.id) {
        try {
            val coords = osrmCoords(trip.pickup.lng, trip.pickup.lat, trip.dropoff.lng, trip.dropoff.lat)
            val resp = MapServiceFactory.osrm.route(coords = coords)
            val list = resp.routes.firstOrNull()?.geometry?.coordinates
            if (!list.isNullOrEmpty()) {
                routePoints = list.map { GeoPoint(it[1], it[0]) }
            }
        } catch (_: Exception) {
            routePoints = listOf(GeoPoint(trip.pickup.lat, trip.pickup.lng), GeoPoint(trip.dropoff.lat, trip.dropoff.lng))
        }
    }

    val pickupPoint = GeoPoint(trip.pickup.lat, trip.pickup.lng)
    val dropoffPoint = GeoPoint(trip.dropoff.lat, trip.dropoff.lng)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Back Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RidaIcon(icon = RidaIcons.Back, contentDescription = "Back", tint = RidaInk, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Text("Trip Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RidaInk)
        }

        // Map Route Preview
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            RidaMapView(
                modifier = Modifier.fillMaxSize(),
                center = pickupPoint,
                zoom = 13.0,
                currentLocation = pickupPoint,
                destination = dropoffPoint,
                routePoints = routePoints
            )
        }

        Column(modifier = Modifier.padding(20.dp)) {
            // Passenger Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(name = trip.passengerName, photoUrl = null, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(trip.passengerName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                        Text(
                            text = if (trip.status == TripStatus.COMPLETED) "Trip completed successfully" else "Cancelled: ${trip.cancelReason ?: "Passenger cancelled"}",
                            fontSize = 12.sp,
                            color = if (trip.status == TripStatus.COMPLETED) RidaActionEnd else RidaRed
                        )
                    }
                    if (trip.ratingGiven != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RidaIcon(icon = RidaIcons.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${trip.ratingGiven}★", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Route Breakdown
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row {
                        RidaIcon(icon = RidaIcons.Pickup, contentDescription = null, tint = RidaActionEnd, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Pickup", fontSize = 11.sp, color = RidaSecondary)
                            Text(trip.pickup.address, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
                        }
                    }
                    Row {
                        RidaIcon(icon = RidaIcons.SavedPlace, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Dropoff", fontSize = 11.sp, color = RidaSecondary)
                            Text(trip.dropoff.address, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Fare & Payment Receipt
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Payment & Receipt", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                    Spacer(Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Fare", fontSize = 13.sp, color = RidaSecondary)
                        Text(TzMoney.format(trip.fareTzs.toInt()), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rida Commission (15%)", fontSize = 13.sp, color = RidaSecondary)
                        Text("- ${TzMoney.format(trip.commissionTzs.toInt())}", fontSize = 13.sp, color = RidaRed)
                    }
                    HorizontalDivider(color = RidaBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Your Net Payout", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                        Text(TzMoney.format(trip.netTzs.toInt()), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = RidaActionEnd)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Payment Method", fontSize = 12.sp, color = RidaSecondary)
                        Text(if (trip.paymentMethod == "cash") "Cash (Collected)" else "Mobile Money", fontSize = 12.sp, color = RidaInk)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}
