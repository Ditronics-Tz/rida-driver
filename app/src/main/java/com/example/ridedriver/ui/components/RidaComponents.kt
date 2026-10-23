package com.example.ridedriver.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridedriver.R
import com.example.ridedriver.ui.theme.*

// --------------------------------------------------------------------
// Logo: // + Rida text  (matches HTML double slash)
// --------------------------------------------------------------------
@Composable
fun RidaLogo(
    modifier: Modifier = Modifier,
    darkText: Boolean = false, // false = light bg (dark text), true = dark bg (white text)
    green: Color = RidaMintDark
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(22.dp)
                        .rotate(-15f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(green)
                )
            }
        }
        Text(
            text = "Rida",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (darkText) Color.White else RidaTitleDark,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
fun RidaLogoLargeDark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .width(7.dp)
                        .height(22.dp)
                        .rotate(-12f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RidaMint)
                )
            }
        }
        Text(
            text = "Rida",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = (-0.5).sp
        )
    }
}

// --------------------------------------------------------------------
// Primary / Secondary Buttons
// --------------------------------------------------------------------
@Composable
fun RidaPrimaryDarkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RidaDark,
            contentColor = Color.White,
            disabledContainerColor = RidaDark.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(8.dp))
        RidaIcon(RidaIcons.Forward, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun RidaMintButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RidaMint,
            contentColor = RidaOnboardingBg
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = RidaOnboardingBg)
        Spacer(modifier = Modifier.width(8.dp))
        RidaIcon(RidaIcons.Forward, contentDescription = null, modifier = Modifier.size(20.dp), tint = RidaOnboardingBg)
    }
}

@Composable
fun RidaGoogleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = RidaTitleDark
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, RidaBorder)
    ) {
        GoogleG()
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Continue with Google",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = RidaTitleDark
        )
    }
}

@Composable
private fun GoogleG() {
    Image(painter = painterResource(R.drawable.google_g), contentDescription = null, modifier = Modifier.size(18.dp))
}

// --------------------------------------------------------------------
// Text Field
// --------------------------------------------------------------------
@Composable
fun RidaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier,
    background: Color = RidaInputBg2,
    borderColor: Color = RidaBorder2
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = RidaTitleDark
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(text = placeholder, color = RidaPlaceholder, fontSize = 13.5.sp)
            },
            leadingIcon = leading?.let { { Box(modifier = Modifier.padding(start = 4.dp)) { it() } } },
            trailingIcon = trailing?.let { { Box(modifier = Modifier.padding(end = 4.dp)) { it() } } },
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = background,
                disabledContainerColor = background,
                focusedBorderColor = RidaMintDark,
                unfocusedBorderColor = borderColor,
                cursorColor = RidaMintDark
            ),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.5.sp, color = RidaTitleDark)
        )
    }
}

// --------------------------------------------------------------------
// Small field icons
// --------------------------------------------------------------------
@Composable
fun MailIcon(tint: Color = RidaMuted) {
    RidaIcon(RidaIcons.Mail, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
}

@Composable
fun LockIcon(tint: Color = RidaMuted) {
    RidaIcon(RidaIcons.Lock, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
}

@Composable
fun PhoneIcon(tint: Color = RidaMuted) {
    RidaIcon(RidaIcons.Phone, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
}

@Composable
fun PersonIcon(tint: Color = RidaMuted) {
    RidaIcon(RidaIcons.Account, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
}

@Composable
fun EyeIcon(visible: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        RidaIcon(
            icon = if (visible) RidaIcons.HidePassword else RidaIcons.ShowPassword,
            contentDescription = if (visible) "Hide password" else "Show password",
            modifier = Modifier.size(20.dp),
            tint = RidaMuted,
        )
    }
}

// Divider with OR
@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = RidaBorder)
        Text(
            text = "OR",
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 12.sp,
            color = RidaMuted,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = RidaBorder)
    }
}

// Home indicator bar
@Composable
fun HomeIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    width: Dp = 120.dp
) {
    Box(
        modifier = modifier
            .width(width)
            .height(5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
    )
}
