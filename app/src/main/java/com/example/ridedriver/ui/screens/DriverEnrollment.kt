package com.example.ridedriver.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ridedriver.data.remote.*
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

@Composable
fun DriverEnrollment(documents: Boolean, back: () -> Unit) {
    val context = LocalContext.current
    val api = RidaApiClient.driverApi
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<Verification?>(null) }
    var vehicle by remember { mutableStateOf(VehicleIdentifiers()) }
    var capacity by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("driving_licence") }
    var expiry by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    suspend fun refresh() {
        val response = api.verification()
        detail = response
        vehicle = response.profile
        capacity = response.profile.passenger_seats.takeIf { it > 0 }?.toString().orEmpty()
    }
    fun change(work: suspend () -> String) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val message = work()
                refresh()
                info = message
            } catch (e: Exception) {
                info = transportError(e)
            } finally {
                busy = false
            }
        }
    }
    LaunchedEffect(Unit) {
        try {
            refresh()
        } catch (e: Exception) {
            info = transportError(e)
        }
    }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                change {
                    uploadProof(context, uri, kind, expiry)
                    "Document uploaded."
                }
        }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = back) { Text("Back") }
        Text(
            if (documents) "Driver documents" else "Registered vehicle",
            style = MaterialTheme.typography.headlineSmall,
        )
        if (detail == null)
            TextButton(onClick = { change { "" } }) { Text("Reload vehicle information") }
        else if (!documents) {
            Row {
                listOf("bajaji", "car", "xl", "boda").forEach { type ->
                    TextButton(onClick = { vehicle = vehicle.copy(vehicle_type = type) }) {
                        Text(if (vehicle.vehicle_type == type) "✓ $type" else type)
                    }
                }
            }
            OutlinedTextField(
                vehicle.plate_number,
                { vehicle = vehicle.copy(plate_number = it) },
                label = { Text("Registration plate") },
            )
            OutlinedTextField(
                vehicle.licence_number,
                { vehicle = vehicle.copy(licence_number = it) },
                label = { Text("Driving licence number") },
            )
            OutlinedTextField(
                vehicle.vehicle_card_number,
                { vehicle = vehicle.copy(vehicle_card_number = it) },
                label = { Text("Vehicle card number") },
            )
            OutlinedTextField(
                capacity,
                { capacity = it.filter(Char::isDigit) },
                label = { Text("Passenger seats, excluding driver") },
            )
            Button(
                enabled =
                    !busy &&
                        (capacity.toIntOrNull() ?: 0) in 1..100 &&
                        vehicle.vehicle_type.isNotEmpty() &&
                        vehicle.plate_number.isNotBlank() &&
                        vehicle.licence_number.isNotBlank() &&
                        vehicle.vehicle_card_number.isNotBlank(),
                onClick = {
                    change {
                        api.identifiers(vehicle.copy(passenger_seats = capacity.toInt()))
                        "Vehicle saved."
                    }
                },
            ) {
                Text("Save vehicle")
            }
        } else {
            detail?.required_types?.forEach { type ->
                val status = detail?.documents?.firstOrNull { it.kind == type }?.status ?: "missing"
                TextButton(
                    onClick = {
                        kind = type
                        expiry = ""
                    }
                ) {
                    Text("${if(kind == type) "✓ " else ""}${type.replace('_',' ')} · $status")
                }
            }
            if (kind == "driving_licence")
                OutlinedTextField(
                    expiry,
                    { expiry = it },
                    label = { Text("Expiry date (YYYY-MM-DD)") },
                )
            Button(
                enabled =
                    !busy &&
                        (kind != "driving_licence" ||
                            expiry.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))),
                onClick = {
                    picker.launch(
                        arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")
                    )
                },
            ) {
                Text("Choose document (up to 8 MB)")
            }
            OutlinedButton(
                enabled = !busy,
                onClick = {
                    change {
                        api.submitDocuments()
                        "Submitted for review."
                    }
                },
            ) {
                Text("Submit for review")
            }
        }
        if (busy) LinearProgressIndicator()
        if (info.isNotBlank()) Text(info)
    }
}

private suspend fun uploadProof(context: Context, uri: Uri, type: String, expiry: String) =
    withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: error("Unknown document type")
        val bytes =
            resolver.openInputStream(uri)!!.use { stream ->
                val buffer = ByteArray(8192)
                val out = ByteArrayOutputStream()
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    require(out.size() + n <= 8 * 1024 * 1024) { "Document exceeds 8 MB" }
                    out.write(buffer, 0, n)
                }
                out.toByteArray()
            }
        val body = bytes.toRequestBody(mime.toMediaType())
        val api = RidaApiClient.driverApi
        val ticket =
            try {
                api.presign(DocumentUpload(type, mime, bytes.size.toLong(), expiry))
            } catch (e: HttpException) {
                if (e.code() != 501) throw e
                api.upload(
                    MultipartBody.Part.createFormData("file", "document", body),
                    type.toRequestBody(),
                    expiry.toRequestBody(),
                )
                return@withContext
            }
        // Signed object-store requests must never carry the app bearer token.
        val request = Request.Builder().url(ticket.upload.url).put(body)
        ticket.upload.headers.forEach { (name, value) -> request.header(name, value) }
        OkHttpClient().newCall(request.build()).execute().use {
            check(it.isSuccessful) { "Document transfer failed" }
        }
        api.confirm(ticket.document.id)
        Unit
    }
