package com.example.ridedriver.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.ridedriver.R
import com.example.ridedriver.data.local.AuthValidation
import com.example.ridedriver.data.local.GoogleAuthManager
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.remote.RegisterRequest
import com.example.ridedriver.data.remote.RidaApiClient
import com.example.ridedriver.data.remote.RidaApiErrors
import com.example.ridedriver.data.repository.UserRepository
import com.example.ridedriver.ui.components.RidaIcons
import com.example.ridedriver.ui.components.auth.AuthFooterLink
import com.example.ridedriver.ui.components.auth.AuthGoogleButton
import com.example.ridedriver.ui.components.auth.AuthHeader
import com.example.ridedriver.ui.components.auth.AuthMessage
import com.example.ridedriver.ui.components.auth.AuthOrDivider
import com.example.ridedriver.ui.components.auth.AuthPrimaryButton
import com.example.ridedriver.ui.components.auth.AuthScreen
import com.example.ridedriver.ui.components.auth.AuthTextField
import com.example.ridedriver.ui.theme.RidaDriverTheme
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onBackToSignIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
                UserRepository.refreshSessionFromApi(context)
                onSignUpSuccess()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                formError = RidaApiErrors.message(e, "Registration failed. Please check your details and try again.")
            } finally {
                isLoading = false
            }
        }
    }

    fun doGoogleSignUp() {
        if (isLoading) return
        isLoading = true
        scope.launch {
            formError = null
            try {
                val result = GoogleAuthManager.register(context)
                if (result.isSuccess) {
                    UserRepository.refreshSessionFromApi(context)
                    onSignUpSuccess()
                } else {
                    val err = result.exceptionOrNull()
                    if (err !is GetCredentialCancellationException) {
                        formError = RidaApiErrors.message(
                            err ?: IllegalStateException("Google sign-up failed"),
                            err?.message ?: "Google sign-up is unavailable right now. Try again or register with email."
                        )
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }

    AuthScreen(
        header = {
            AuthHeader(
                title = "Create account",
                subtitle = "Register with email or Google."
            )
        },
        footer = {
            AuthFooterLink(
                prompt = "Already have an account? ",
                action = "Sign in",
                onClick = onBackToSignIn
            )
        },
        body = {
            AuthTextField(
                label = "Email address",
                value = email,
                onValueChange = { email = it; emailError = null },
                placeholder = "juma@example.com",
                icon = RidaIcons.Mail,
                error = emailError,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
            Spacer(Modifier.height(14.dp))
            AuthTextField(
                label = "Password",
                value = password,
                onValueChange = { password = it; pwError = null },
                placeholder = "At least 10 characters",
                icon = RidaIcons.Lock,
                error = pwError,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = ::doSignUp
            )
            if (formError != null) {
                AuthMessage(formError.orEmpty(), modifier = Modifier.padding(top = 8.dp, start = 4.dp))
            }
            Spacer(Modifier.height(24.dp))
            AuthPrimaryButton(
                text = "Create account",
                onClick = ::doSignUp,
                loading = isLoading,
                showForwardIcon = false
            )
            Spacer(Modifier.height(16.dp))
            AuthOrDivider()
            Spacer(Modifier.height(16.dp))
            AuthGoogleButton(
                text = "Continue with Google",
                icon = R.drawable.google_g,
                enabled = !isLoading,
                onClick = ::doGoogleSignUp
            )
        }
    )
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
