package com.example.ridedriver.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridedriver.data.driver.DriverRepositoryProvider
import com.example.ridedriver.data.driver.Vehicle
import com.example.ridedriver.data.driver.VehicleType
import com.example.ridedriver.data.local.AuthValidation
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.remote.RegisterRequest
import com.example.ridedriver.data.remote.RidaApiClient
import com.example.ridedriver.data.remote.RidaApiErrors
import com.example.ridedriver.data.repository.UserRepository
import com.example.ridedriver.ui.common.TzPhone
import com.example.ridedriver.ui.components.RidaIcon
import com.example.ridedriver.ui.components.RidaIcons
import com.example.ridedriver.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onBackToSignIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var pwError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun doSignUp() {
        if (isLoading) return
        emailError = AuthValidation.emailError(email)
        pwError = AuthValidation.registrationPasswordError(password)

        if (emailError != null || pwError != null) return

        isLoading = true
        scope.launch {
            formError = null
            try {
                // Register driver account with the backend
                val regReq = RegisterRequest(
                    email = email.trim(),
                    password = password,
                    role = "driver"
                )
                val sessionResp = RidaApiClient.api.register(regReq)
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val user = sessionResp.user

                SessionManager.save(
                    context = context,
                    name = user.displayName(),
                    handle = email.substringBefore("@").let { "@$it" },
                    phone = user.displayPhone(),
                    email = email.trim(),
                    profileCompleted = sessionResp.isProfileComplete(),
                    accessToken = sessionResp.getEffectiveAccessToken(),
                    refreshToken = sessionResp.getEffectiveRefreshToken()
                )

                // Vehicle identifiers and documents use the canonical enrollment screen after login.
                UserRepository.refreshSessionFromApi(context)
                onSignUpSuccess()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled } catch (e: Exception) {
                formError = RidaApiErrors.message(e, "Registration failed. Please check your details and try again.")
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Back button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBackToSignIn)
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                RidaIcon(icon = RidaIcons.Back, contentDescription = "Back", tint = RidaInk, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Back to Sign In", color = RidaInk, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Drive with Rida",
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = RidaInk,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Earn competitive fares with low 15% platform commission and instant cash payments in Dar es Salaam.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = RidaSecondary
            )

            Spacer(Modifier.height(24.dp))

            Text("Create your email login, then register your vehicle and documents from Profile. Personal name and phone editing is currently unavailable.", fontSize = 13.sp, color = RidaSecondary)
            Spacer(Modifier.height(14.dp))

            // Email
            Text("Email address", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                placeholder = { Text("juma@example.com", color = RidaHint, fontSize = 14.sp) },
                leadingIcon = { RidaIcon(icon = RidaIcons.Mail, contentDescription = null, tint = RidaHint, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                isError = emailError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = RidaField, unfocusedContainerColor = RidaField,
                    focusedBorderColor = RidaPrimary, unfocusedBorderColor = RidaFieldBorder
                )
            )
            if (emailError != null) Text(emailError.orEmpty(), color = Color(0xFFD92D20), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

            Spacer(Modifier.height(14.dp))

            // Password
            Text("Password", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; pwError = null },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                placeholder = { Text("At least 10 characters", color = RidaHint, fontSize = 14.sp) },
                leadingIcon = { RidaIcon(icon = RidaIcons.Lock, contentDescription = null, tint = RidaHint, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        RidaIcon(
                            icon = if (passwordVisible) RidaIcons.HidePassword else RidaIcons.ShowPassword,
                            contentDescription = null,
                            tint = RidaSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = pwError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = RidaField, unfocusedContainerColor = RidaField,
                    focusedBorderColor = RidaPrimary, unfocusedBorderColor = RidaFieldBorder
                )
            )
            if (pwError != null) Text(pwError.orEmpty(), color = Color(0xFFD92D20), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

            Spacer(Modifier.height(20.dp))

            if (formError != null) {
                Text(text = formError.orEmpty(), color = Color(0xFFD92D20), fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp, start = 4.dp))
            }

            Spacer(Modifier.height(28.dp))

            // Submit Button
            val shape = RoundedCornerShape(18.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(7.dp, shape, ambientColor = RidaPrimary.copy(alpha = 0.15f), spotColor = RidaPrimary.copy(alpha = 0.25f))
                    .clip(shape)
                    .background(Brush.horizontalGradient(listOf(RidaForest, RidaActionEnd)))
                    .clickable(
                        enabled = !isLoading,
                        role = Role.Button,
                        onClick = ::doSignUp
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Text("Complete Driver Registration", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    RidaDriverTheme {
        SignUpScreen(
            onSignUpSuccess = {},
            onBackToSignIn = {}
        )
    }
}

