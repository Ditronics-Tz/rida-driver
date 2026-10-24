package com.example.ridedriver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.repository.UserRepository
import com.example.ridedriver.ui.common.TzPhone
import com.example.ridedriver.ui.components.ProfilePhotoEditor
import kotlinx.coroutines.launch

@Composable
fun EditDriverProfileScreen(onSaved: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val session by SessionManager.observe(context).collectAsState(initial = SessionManager.Session())
    val scope = rememberCoroutineScope()
    var name by remember(session.name) { mutableStateOf(session.name) }
    var phone by remember(session.phone) { mutableStateOf(session.phone) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var otp by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    var verified by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("Personal information", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))
        ProfilePhotoEditor()
        OutlinedTextField(name, { name = it; error = null }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(phone, { phone = it; error = null; codeSent = false; verified = false }, label = { Text("Tanzania phone number") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone))
        Text("Email: ${session.email}", modifier = Modifier.padding(vertical = 12.dp))
        Text("Your email is used to sign in. A changed phone number must be verified before it can be used as a verified contact.", style = MaterialTheme.typography.bodySmall)
        if (verified) Text("Phone number verified")
        else {
            TextButton(enabled = !verifying && !saving, onClick = {
                val normalized = TzPhone.normalize(phone)
                if (normalized == null) error = "Enter a valid Tanzania phone number."
                else scope.launch {
                    verifying = true
                    error = null
                    try {
                        com.example.ridedriver.data.remote.RidaApiClient.api.requestOtp(com.example.ridedriver.data.remote.RequestOtpRequest(normalized))
                        codeSent = true
                        otp = ""
                    } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (e: Exception) { error = com.example.ridedriver.data.remote.RidaApiErrors.message(e, "Could not send a verification code. Try again.") }
                    finally { verifying = false }
                }
            }) { Text(if (codeSent) "Resend verification code" else "Verify phone number") }
            if (codeSent) {
                OutlinedTextField(otp, { otp = it.filter(Char::isDigit).take(6) }, label = { Text("6-digit code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(enabled = !verifying && !saving && otp.length == 6, onClick = {
                    scope.launch {
                        verifying = true
                        error = null
                        try {
                            val response = com.example.ridedriver.data.remote.RidaApiClient.api.verifyOtp(com.example.ridedriver.data.remote.VerifyOtpRequest(otp))
                            SessionManager.updateProfile(context, session.name, response.user.displayPhone(), session.email)
                            verified = true
                            codeSent = false
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (e: Exception) { error = com.example.ridedriver.data.remote.RidaApiErrors.message(e, "Could not verify that code. Try again.") }
                        finally { verifying = false }
                    }
                }) { Text("Confirm code") }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !saving && !verifying, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), onClick = {
            val normalized = TzPhone.normalize(phone)
            if (name.trim().length !in 2..100) error = "Enter your full name (2–100 characters)."
            else if (normalized == null) error = "Enter a valid Tanzania phone number."
            else scope.launch {
                saving = true
                try {
                    UserRepository.updateProfileOnServer(context, name, normalized)
                        .onSuccess { onSaved() }
                        .onFailure { error = it.message }
                } finally { saving = false }
            }
        }) { Text(if (saving) "Saving…" else "Save profile") }
    }
}
