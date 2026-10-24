package com.example.ridedriver.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.repository.UserRepository
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridedriver.data.driver.DocumentStatus
import com.example.ridedriver.data.driver.DriverDocument
import com.example.ridedriver.data.driver.DriverRepositoryProvider
import com.example.ridedriver.data.driver.Vehicle
import com.example.ridedriver.data.driver.VehicleType
import com.example.ridedriver.data.driver.VerificationStatus
import com.example.ridedriver.ui.components.RidaIcon
import com.example.ridedriver.ui.components.RidaIcons
import com.example.ridedriver.ui.components.UserAvatar
import com.example.ridedriver.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun VehicleEditScreen(
    onBack: () -> Unit
) {
    val repo = remember { DriverRepositoryProvider.get() }
    val state by repo.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedType by remember { mutableStateOf(state.vehicle.type) }
    var make by remember { mutableStateOf(state.vehicle.make) }
    var model by remember { mutableStateOf(state.vehicle.model) }
    var color by remember { mutableStateOf(state.vehicle.color) }
    var plate by remember { mutableStateOf(state.vehicle.plate) }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Back Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack).padding(4.dp)
        ) {
            RidaIcon(icon = RidaIcons.Back, contentDescription = "Back", tint = RidaInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Vehicle Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RidaInk)
        }

        Spacer(Modifier.height(24.dp))

        // Vehicle Category
        Text("Vehicle Type", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VehicleType.entries.forEach { vType ->
                val isSelected = selectedType == vType
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedType = vType }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) RidaPrimary else RidaFieldBorder,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    color = if (isSelected) RidaPrimary.copy(alpha = 0.08f) else RidaField
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RidaIcon(
                            icon = RidaIcons.Vehicle,
                            contentDescription = null,
                            tint = if (isSelected) RidaActionEnd else RidaSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = vType.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) RidaInk else RidaSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Make & Model
        Text("Make / Brand", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = make,
            onValueChange = { make = it },
            placeholder = { Text("e.g. Toyota", color = RidaSecondary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = RidaField, unfocusedContainerColor = RidaField,
                focusedBorderColor = RidaPrimary, unfocusedBorderColor = RidaFieldBorder
            )
        )

        Spacer(Modifier.height(14.dp))

        Text("Model", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            placeholder = { Text("e.g. IST, Vitz, Boxer", color = RidaSecondary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = RidaField, unfocusedContainerColor = RidaField,
                focusedBorderColor = RidaPrimary, unfocusedBorderColor = RidaFieldBorder
            )
        )

        Spacer(Modifier.height(14.dp))

        // Color & Plate
        Text("Vehicle Color", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = color,
            onValueChange = { color = it },
            placeholder = { Text("e.g. Silver, White, Blue", color = RidaSecondary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = RidaField, unfocusedContainerColor = RidaField,
                focusedBorderColor = RidaPrimary, unfocusedBorderColor = RidaFieldBorder
            )
        )

        Spacer(Modifier.height(14.dp))

        Text("Registration Plate Number", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = plate,
            onValueChange = { plate = it.uppercase() },
            placeholder = { Text("e.g. T 421 DKL", color = RidaSecondary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = RidaField, unfocusedContainerColor = RidaField,
                focusedBorderColor = RidaPrimary, unfocusedBorderColor = RidaFieldBorder
            )
        )

        Spacer(Modifier.height(28.dp))

        // Save Button
        val shape = RoundedCornerShape(16.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(RidaForest, RidaActionEnd)))
                .clickable(
                    enabled = !isSaving,
                    role = Role.Button,
                    onClick = {
                        scope.launch {
                            isSaving = true
                            val updated = Vehicle(
                                type = selectedType,
                                make = make.trim(),
                                model = model.trim(),
                                color = color.trim(),
                                plate = plate.trim()
                            )
                            repo.updateVehicle(updated)
                            isSaving = false
                            Toast.makeText(context, "Vehicle updated successfully", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text("Save Vehicle Details", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DocumentsScreen(
    onBack: () -> Unit
) {
    val repo = remember { DriverRepositoryProvider.get() }
    val state by repo.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack).padding(4.dp)
        ) {
            RidaIcon(icon = RidaIcons.Back, contentDescription = "Back", tint = RidaInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Driver Documents", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RidaInk)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Regulatory compliance mandated by LATRA and local traffic authorities in Tanzania.",
            fontSize = 13.sp,
            color = RidaSecondary
        )

        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.documents.forEach { doc ->
                DocumentCard(
                    doc = doc,
                    onUpload = {
                        scope.launch {
                            repo.uploadDocument(doc.id, doc.title)
                            Toast.makeText(context, "${doc.title} submitted for verification", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DocumentCard(
    doc: DriverDocument,
    onUpload: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = doc.title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = RidaInk)
                DocStatusBadge(doc.status)
            }

            Spacer(Modifier.height(6.dp))
            Text(text = doc.description, fontSize = 12.5.sp, color = RidaSecondary)

            if (doc.expiresAt != null) {
                Spacer(Modifier.height(4.dp))
                Text(text = "Valid until: ${doc.expiresAt}", fontSize = 11.5.sp, color = RidaSecondary)
            }

            Spacer(Modifier.height(14.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onUpload),
                color = Color(0xFFF3F4F6)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RidaIcon(icon = RidaIcons.Upload, contentDescription = null, tint = RidaInk, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (doc.status == DocumentStatus.APPROVED) "Update Document" else "Upload Document",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RidaInk
                    )
                }
            }
        }
    }
}

@Composable
private fun DocStatusBadge(status: DocumentStatus) {
    val (bg, fg) = when (status) {
        DocumentStatus.APPROVED -> Pair(RidaPrimary.copy(alpha = 0.15f), RidaActionEnd)
        DocumentStatus.PENDING -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
        DocumentStatus.REJECTED, DocumentStatus.EXPIRED -> Pair(Color(0xFFFEE2E2), RidaRed)
        DocumentStatus.MISSING -> Pair(Color(0xFFE5E7EB), RidaSecondary)
    }

    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = status.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
