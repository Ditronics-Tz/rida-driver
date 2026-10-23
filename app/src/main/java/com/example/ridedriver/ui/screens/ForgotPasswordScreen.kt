package com.example.ridedriver.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridedriver.data.local.AuthValidation
import com.example.ridedriver.ui.components.RidaIcon
import com.example.ridedriver.ui.components.RidaIcons

private val RidaPrimary = Color(0xFF00C878)
private val RidaForest = Color(0xFF063F32)
private val RidaActionEnd = Color(0xFF008F64)
private val RidaInk = Color(0xFF111827)
private val RidaSecondary = Color(0xFF667085)
private val RidaHint = Color(0xFF98A6A0)
private val RidaField = Color(0xFFFAFCFB)
private val RidaFieldBorder = Color(0xFFDCE8E3)

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }

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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                RidaIcon(icon = RidaIcons.Back, contentDescription = "Back", tint = RidaInk, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Back to Sign In", color = RidaInk, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Reset password",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = RidaInk,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (sent) "We sent a password reset link to $email. Please check your inbox."
                else "Enter your registered driver email address and we'll send you instructions to reset your password.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = RidaSecondary
            )

            Spacer(Modifier.height(28.dp))

            if (!sent) {
                Text("Email address", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; error = null },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    placeholder = { Text("driver@example.com", color = RidaHint, fontSize = 14.sp) },
                    leadingIcon = { RidaIcon(icon = RidaIcons.Mail, contentDescription = null, tint = RidaHint, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RidaField, unfocusedContainerColor = RidaField,
                        focusedBorderColor = RidaPrimary, unfocusedBorderColor = RidaFieldBorder
                    )
                )
                if (error != null) Text(error.orEmpty(), color = Color(0xFFD92D20), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

                Spacer(Modifier.height(24.dp))

                val shape = RoundedCornerShape(18.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(7.dp, shape, ambientColor = RidaPrimary.copy(alpha = 0.15f), spotColor = RidaPrimary.copy(alpha = 0.25f))
                    .clip(shape)
                    .background(Brush.horizontalGradient(listOf(RidaForest, RidaActionEnd)))
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            val err = AuthValidation.emailError(email)
                            if (err != null) {
                                error = err
                            } else {
                                sent = true
                            }
                        }
                    ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Send Reset Link", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            } else {
                val shape = RoundedCornerShape(18.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(shape)
                        .background(Brush.horizontalGradient(listOf(RidaForest, RidaActionEnd)))
                        .clickable(role = Role.Button, onClick = onBack),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Return to Sign In", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}
