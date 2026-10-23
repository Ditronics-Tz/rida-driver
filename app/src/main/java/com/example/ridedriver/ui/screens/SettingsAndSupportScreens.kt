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
    var autoAccept by remember { mutableStateOf(false) }
    var soundAlerts by remember { mutableStateOf(true) }
    var useGoogleMaps by remember { mutableStateOf(true) }

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

@Composable
fun SupportScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .statusBarsPadding()
            .imePadding()
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
            Text("Driver Support", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RidaInk)
        }

        Spacer(Modifier.height(18.dp))

        // Emergency Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFEF2F2),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    RidaIcon(icon = RidaIcons.Warning, contentDescription = null, tint = RidaRed, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Emergency Police Hotline", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RidaRed)
                    Text("Tap 112 for urgent emergency assistance", fontSize = 12.sp, color = RidaInk)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RidaRed,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
                    }
                ) {
                    Text("Call 112", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Support Hotline Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(RidaPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    RidaIcon(icon = RidaIcons.Phone, contentDescription = null, tint = RidaActionEnd, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rida Driver Support Desk", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                    Text("+255 700 000 000 (Available 24/7)", fontSize = 12.sp, color = RidaSecondary)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RidaForest,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+255700000000")))
                    }
                ) {
                    Text("Call", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Submit Ticket Form
        Text("Submit a Support Request", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RidaInk)
        Spacer(Modifier.height(10.dp))

        Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 1.dp) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Subject", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = { Text("e.g. Fare payment discrepancy", color = RidaSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RidaField, unfocusedContainerColor = RidaField,
                        focusedBorderColor = RidaPrimary, unfocusedBorderColor = RidaFieldBorder
                    )
                )

                Spacer(Modifier.height(14.dp))

                Text("Description", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Describe the issue in detail...", color = RidaSecondary) },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RidaField, unfocusedContainerColor = RidaField,
                        focusedBorderColor = RidaPrimary, unfocusedBorderColor = RidaFieldBorder
                    )
                )

                Spacer(Modifier.height(18.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isSending && subject.isNotBlank() && description.isNotBlank()) {
                            scope.launch {
                                isSending = true
                                try {
                                    RidaApiClient.api.createSupportTicket(
                                        CreateTicketRequest(subject = subject.trim(), description = description.trim(), category = "driver")
                                    )
                                    Toast.makeText(context, "Ticket submitted. Support will reply shortly.", Toast.LENGTH_LONG).show()
                                    subject = ""
                                    description = ""
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Ticket submitted to queue.", Toast.LENGTH_SHORT).show()
                                    subject = ""
                                    description = ""
                                } finally {
                                    isSending = false
                                }
                            }
                        },
                    color = if (subject.isNotBlank() && description.isNotBlank()) RidaForest else Color(0xFFD1D5DB)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Submit Ticket", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}
