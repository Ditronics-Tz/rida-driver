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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridedriver.data.driver.DayEarning
import com.example.ridedriver.data.driver.DriverRepositoryProvider
import com.example.ridedriver.data.driver.EarningsPeriod
import com.example.ridedriver.data.driver.EarningsSummary
import com.example.ridedriver.data.driver.TripRecord
import com.example.ridedriver.ui.common.TzMoney
import com.example.ridedriver.ui.components.RidaIcon
import com.example.ridedriver.ui.components.RidaIcons
import com.example.ridedriver.ui.theme.RidaActionEnd
import com.example.ridedriver.ui.theme.RidaBorder
import com.example.ridedriver.ui.theme.RidaForest
import com.example.ridedriver.ui.theme.RidaInk
import com.example.ridedriver.ui.theme.RidaPrimary
import com.example.ridedriver.ui.theme.RidaSecondary

@Composable
fun EarningsScreen(
    onTripClick: (String) -> Unit = {}
) {
    val repo = remember { DriverRepositoryProvider.get() }
    var selectedPeriod by remember { mutableStateOf(EarningsPeriod.TODAY) }
    var summary by remember { mutableStateOf<EarningsSummary?>(null) }

    LaunchedEffect(selectedPeriod) {
        summary = repo.getEarnings(selectedPeriod)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Title
        Text(
            text = "Earnings",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = RidaInk
        )
        Text(
            text = "Track your net income, tips and 15% platform fee",
            fontSize = 13.sp,
            color = RidaSecondary
        )

        Spacer(Modifier.height(18.dp))

        // Period Segmented Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EarningsPeriod.entries.forEach { period ->
                val isSelected = selectedPeriod == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { selectedPeriod = period }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) RidaInk else RidaSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Main Net Earnings Card
        val curSummary = summary
        val net = curSummary?.netTzs ?: 0L
        val gross = curSummary?.grossTzs ?: 0L
        val commission = curSummary?.commissionTzs ?: 0L
        val trips = curSummary?.trips ?: 0

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "TOTAL NET EARNINGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RidaSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = TzMoney.format(net.toInt()),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RidaForest
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = RidaBorder)
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Gross Fare", fontSize = 12.sp, color = RidaSecondary)
                        Text(TzMoney.format(gross.toInt()), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Rida Fee (15%)", fontSize = 12.sp, color = RidaSecondary)
                        Text("- ${TzMoney.format(commission.toInt())}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD92D20))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Trips", fontSize = 12.sp, color = RidaSecondary)
                        Text("$trips completed", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Daily Activity Bar Chart
        if (!curSummary?.daily.isNullOrEmpty()) {
            Text("Activity Overview", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RidaInk)
            Spacer(Modifier.height(10.dp))
            DailyBarChart(daily = curSummary!!.daily)
            Spacer(Modifier.height(20.dp))
        }

        // Recent Trips Section
        Text("Trips in this Period", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RidaInk)
        Spacer(Modifier.height(10.dp))

        val records = curSummary?.records.orEmpty()
        if (records.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No trips recorded in this period", fontSize = 13.sp, color = RidaSecondary)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                records.forEach { trip ->
                    EarningsTripItem(trip = trip, onClick = { onTripClick(trip.id) })
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DailyBarChart(daily: List<DayEarning>) {
    val maxEarnings = daily.maxOfOrNull { it.netTzs }?.coerceAtLeast(1000L) ?: 1000L

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            daily.forEach { item ->
                val fraction = (item.netTzs.toFloat() / maxEarnings).coerceIn(0.08f, 1f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height((fraction * 90).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .then(
                                if (item.netTzs > 0) {
                                    Modifier.background(Brush.verticalGradient(listOf(RidaPrimary, RidaActionEnd)))
                                } else {
                                    Modifier.background(Color(0xFFE5E7EB))
                                }
                            )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(text = item.label, fontSize = 11.sp, color = RidaSecondary, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun EarningsTripItem(
    trip: TripRecord,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RidaPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                RidaIcon(icon = RidaIcons.Rides, contentDescription = null, tint = RidaActionEnd, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = trip.passengerName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                Text(
                    text = "${trip.pickup.address.take(18)}... → ${trip.dropoff.address.take(18)}...",
                    fontSize = 12.sp,
                    color = RidaSecondary,
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = TzMoney.format(trip.netTzs.toInt()),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RidaForest
                )
                Text(
                    text = if (trip.paymentMethod == "cash") "Cash" else "Mobile",
                    fontSize = 11.sp,
                    color = RidaSecondary
                )
            }
        }
    }
}
