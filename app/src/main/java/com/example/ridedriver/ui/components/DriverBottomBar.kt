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
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = BarGreen,
        selectedTextColor = BarGreen,
        indicatorColor = BarGreen.copy(alpha = 0.12f),
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
            icon = { RidaIcon(icon = RidaIcons.Home, contentDescription = "Home") },
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
            selected = selected == "Trips",
            onClick = onTrips,
            label = { Text("Trips") },
            icon = { RidaIcon(icon = RidaIcons.History, contentDescription = "Trips") },
            colors = colors
        )
        NavigationBarItem(
            selected = selected == "Profile",
            onClick = onProfile,
            label = { Text("Profile") },
            icon = { RidaIcon(icon = RidaIcons.Account, contentDescription = "Profile") },
            colors = colors
        )
    }
}
