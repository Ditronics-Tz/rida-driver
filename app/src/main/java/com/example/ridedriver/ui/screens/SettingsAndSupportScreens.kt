package com.example.ridedriver.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridedriver.data.driver.DriverRepositoryProvider
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.remote.CreateTicketRequest
import com.example.ridedriver.data.remote.RidaApiClient
import com.example.ridedriver.ui.components.RidaIcon
import com.example.ridedriver.ui.components.RidaIcons
import com.example.ridedriver.ui.theme.RidaActionEnd
import com.example.ridedriver.ui.theme.RidaBorder
import com.example.ridedriver.ui.theme.RidaField
import com.example.ridedriver.ui.theme.RidaFieldBorder
import com.example.ridedriver.ui.theme.RidaForest
import com.example.ridedriver.ui.theme.RidaInk
import com.example.ridedriver.ui.theme.RidaPrimary
import com.example.ridedriver.ui.theme.RidaRed
import com.example.ridedriver.ui.theme.RidaSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences("driver_preferences", android.content.Context.MODE_PRIVATE) }
    var autoAccept by remember { mutableStateOf(preferences.getBoolean("auto_accept", false)) }
    var soundAlerts by remember { mutableStateOf(preferences.getBoolean("sound_alerts", true)) }
    var useGoogleMaps by remember { mutableStateOf(preferences.getBoolean("google_maps", true)) }
    androidx.compose.runtime.LaunchedEffect(autoAccept, soundAlerts, useGoogleMaps) {
        preferences.edit().putBoolean("auto_accept", autoAccept).putBoolean("sound_alerts", soundAlerts).putBoolean("google_maps", useGoogleMaps).apply()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Back Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack).padding(4.dp)
        ) {
            RidaIcon(icon = RidaIcons.Back, contentDescription = "Back", tint = RidaInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RidaInk)
        }

        Spacer(Modifier.height(20.dp))

        // Driving Preferences
        Text("Driving Preferences", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RidaInk)
        Spacer(Modifier.height(8.dp))

        Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 1.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingSwitchRow(
                    title = "Auto-accept ride requests",
                    subtitle = "Automatically accept offers matching your category",
                    checked = autoAccept,
                    onCheckedChange = { autoAccept = it }
                )
                HorizontalDivider(color = RidaBorder, modifier = Modifier.padding(vertical = 12.dp))
                SettingSwitchRow(
                    title = "Ride alert sounds",
                    subtitle = "Play loud chime when receiving new trip requests",
                    checked = soundAlerts,
                    onCheckedChange = { soundAlerts = it }
                )
                HorizontalDivider(color = RidaBorder, modifier = Modifier.padding(vertical = 12.dp))
                SettingSwitchRow(
                    title = "External Google Maps",
                    subtitle = "Open navigation directly in Google Maps application",
                    checked = useGoogleMaps,
                    onCheckedChange = { useGoogleMaps = it }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Support & Safety
        Text("Assistance & Legal", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RidaInk)
        Spacer(Modifier.height(8.dp))

        Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 1.dp) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingNavRow(
                    title = "Driver Support Center",
                    subtitle = "Submit tickets & view answers",
                    onClick = onNavigateToSupport
                )
                HorizontalDivider(color = RidaBorder)
                SettingNavRow(
                    title = "Terms & LATRA Regulation",
                    subtitle = "Driver terms of service in Tanzania",
                    onClick = {
                        Toast.makeText(context, "Rida Driver terms (LATRA registered)", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = RidaBorder)
                SettingNavRow(
                    title = "Privacy & Location Policy",
                    subtitle = "How background GPS and trip telemetry are protected",
                    onClick = {
                        Toast.makeText(context, "Location data encrypted and used solely for dispatch", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Logout Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    scope.launch {
                        DriverRepositoryProvider.get().setOnline(false)
                        try { com.example.ridedriver.data.remote.RidaApiClient.api.logout() } catch (_: Exception) { }
                        SessionManager.clear(context)
                        onLogout()
                    }
                },
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFEE2E2)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RidaIcon(icon = RidaIcons.Logout, contentDescription = null, tint = RidaRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Log Out of Driver Account", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = RidaRed)
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
            Text(subtitle, fontSize = 12.sp, color = RidaSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RidaPrimary
            )
        )
    }
}

@Composable
private fun SettingNavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
            Text(subtitle, fontSize = 12.sp, color = RidaSecondary)
        }
        RidaIcon(icon = RidaIcons.ChevronRight, contentDescription = null, tint = RidaSecondary, modifier = Modifier.size(18.dp))
    }
}
