package com.example.ridedriver.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val BarBg = Color.White
private val BarGreen = Color(0xFF008F64)
private val BarInactive = Color(0xFF667085)

/**
 * Driver bottom navigation bar.
 * Tabs: Home (Map) | Earnings | Trips | Profile
 */
@Composable
fun DriverBottomBar(
    selected: String,
    onHome: () -> Unit,
    onEarnings: () -> Unit,
    onTrips: () -> Unit,
    onSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = BarGreen,
        selectedTextColor = BarGreen,
        indicatorColor = Color.Transparent,
        unselectedIconColor = BarInactive,
        unselectedTextColor = BarInactive
    )

    NavigationBar(
        modifier = modifier,
        containerColor = BarBg
    ) {
        NavigationBarItem(
            selected = selected == "Home",
            onClick = onHome,
            label = { Text("Home") },
            icon = { RidaIcon(icon = RidaIcons.Vehicle, contentDescription = "Home") },
            colors = colors
        )
        NavigationBarItem(
            selected = selected == "Earnings",
            onClick = onEarnings,
            label = { Text("Earnings") },
            icon = { RidaIcon(icon = RidaIcons.Earnings, contentDescription = "Earnings") },
            colors = colors
        )
        NavigationBarItem(
            selected = selected == "Rides",
            onClick = onTrips,
            label = { Text("Rides") },
            icon = { RidaIcon(icon = RidaIcons.Calendar, contentDescription = "Rides") },
            colors = colors
        )
        NavigationBarItem(
            selected = selected == "Support",
            onClick = onSupport,
            label = { Text("Support") },
            icon = { RidaIcon(icon = RidaIcons.Help, contentDescription = "Support") },
            colors = colors
        )
    }
}
