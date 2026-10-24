package com.example.ridedriver.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.ridedriver.R
import com.example.ridedriver.data.local.AuthValidation
import com.example.ridedriver.data.local.GoogleAuthManager
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.remote.LoginRequest
import com.example.ridedriver.data.remote.RidaApiClient
import com.example.ridedriver.data.remote.RidaApiErrors
import com.example.ridedriver.data.repository.UserRepository
import com.example.ridedriver.ui.components.RidaIcon
import com.example.ridedriver.ui.components.RidaIcons
import com.example.ridedriver.ui.components.RidaLogo
import com.example.ridedriver.ui.theme.*
import kotlinx.coroutines.launch

private val RidaDivider = Color(0xFFE5EAE7)

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var pwError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun doSignIn() {
        if (isLoading) return
        emailError = AuthValidation.emailError(email)
        pwError = AuthValidation.passwordError(password)
        if (emailError != null || pwError != null) return

        scope.launch {
            isLoading = true
            formError = null
            try {
                val loginReq = LoginRequest(email = email.trim(), password = password)
                val sessionResp = RidaApiClient.api.login(loginReq)
                SessionManager.saveGoogleSession(context, sessionResp)
                UserRepository.refreshSessionFromApi(context)
                onSignInSuccess()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                formError = RidaApiErrors.message(e, "Sign in failed. Check your email and password.")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Brand Header with Driver Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                RidaLogo(green = RidaPrimary)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = RidaPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "DRIVER",
                        color = RidaActionEnd,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Welcome back, Driver",
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = RidaInk,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Sign in to go online, accept rides, and track your daily earnings in Dar es Salaam.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = RidaSecondary
            )

            Spacer(Modifier.height(28.dp))

            // Email Field
            Text(
                text = "Email address",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = RidaInk
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                placeholder = { Text("driver@example.com", color = RidaHint, fontSize = 14.sp) },
                leadingIcon = {
                    RidaIcon(icon = RidaIcons.Mail, contentDescription = null, tint = RidaHint, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                isError = emailError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = RidaInk,
                    unfocusedTextColor = RidaInk,
                    focusedContainerColor = RidaField,
                    unfocusedContainerColor = RidaField,
                    focusedBorderColor = RidaPrimary,
                    unfocusedBorderColor = RidaFieldBorder,
                    cursorColor = RidaPrimary
                )
            )
            if (emailError != null) {
                Text(text = emailError.orEmpty(), color = Color(0xFFD92D20), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }

            Spacer(Modifier.height(14.dp))

            // Password Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Password", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
                Text(
                    text = "Forgot password?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RidaPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onForgotPassword).padding(4.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; pwError = null },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                placeholder = { Text("Enter your password", color = RidaHint, fontSize = 14.sp) },
                leadingIcon = {
                    RidaIcon(icon = RidaIcons.Lock, contentDescription = null, tint = RidaHint, modifier = Modifier.size(20.dp))
                },
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
                keyboardActions = KeyboardActions(onDone = { doSignIn() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = RidaInk,
                    unfocusedTextColor = RidaInk,
                    focusedContainerColor = RidaField,
                    unfocusedContainerColor = RidaField,
                    focusedBorderColor = RidaPrimary,
                    unfocusedBorderColor = RidaFieldBorder,
                    cursorColor = RidaPrimary
                )
            )
            if (pwError != null) {
                Text(text = pwError.orEmpty(), color = Color(0xFFD92D20), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }
            if (formError != null) {
                Text(text = formError.orEmpty(), color = Color(0xFFD92D20), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Primary Sign-in Button
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val buttonScale by animateFloatAsState(
                targetValue = if (pressed) 0.985f else 1f,
                animationSpec = spring(stiffness = 700f),
                label = "btn_scale"
            )
            val shape = RoundedCornerShape(18.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(buttonScale)
                    .shadow(7.dp, shape, ambientColor = RidaPrimary.copy(alpha = 0.15f), spotColor = RidaPrimary.copy(alpha = 0.25f))
                    .clip(shape)
                    .background(Brush.horizontalGradient(listOf(RidaForest, RidaActionEnd)))
                    .clickable(
                        enabled = !isLoading,
                        role = Role.Button,
                        interactionSource = interaction,
                        indication = null,
                        onClick = ::doSignIn
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Text("Sign In as Driver", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.width(8.dp))
                RidaIcon(icon = RidaIcons.Forward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = RidaDivider)
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 12.sp,
                    color = RidaSecondary,
                    fontWeight = FontWeight.Medium
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = RidaDivider)
            }

            Spacer(Modifier.height(16.dp))

            // Google Sign-In Button
            OutlinedButton(
                onClick = {
                    if (isLoading) return@OutlinedButton
                    scope.launch {
                        isLoading = true
                        try {
                            val result = GoogleAuthManager.signIn(context)
                            if (result.isSuccess) {
                                UserRepository.refreshSessionFromApi(context)
                                onSignInSuccess()
                            } else {
                                val err = result.exceptionOrNull()
                                if (err !is GetCredentialCancellationException) {
                                    formError = err?.let { RidaApiErrors.message(it, "Google Sign-In unavailable. Try again.") }
                                }
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = RidaInk),
                border = androidx.compose.foundation.BorderStroke(1.dp, RidaFieldBorder)
            ) {
                Image(painter = painterResource(R.drawable.google_g), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Continue with Google", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(28.dp))

            // Sign Up link
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Want to drive with Rida? ", fontSize = 14.sp, color = RidaSecondary)
                Text(
                    text = "Register Now",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RidaPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onSignUp).padding(4.dp)
                )
            }
        }
    }
}
