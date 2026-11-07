package com.example.ridedriver.ui.components.auth

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridedriver.ui.components.RidaIcon
import com.example.ridedriver.ui.components.RidaIcons
import com.example.ridedriver.ui.components.RidaLogo
import com.example.ridedriver.ui.theme.RidaActionEnd
import com.example.ridedriver.ui.theme.RidaField
import com.example.ridedriver.ui.theme.RidaFieldBorder
import com.example.ridedriver.ui.theme.RidaForest
import com.example.ridedriver.ui.theme.RidaHint
import com.example.ridedriver.ui.theme.RidaInk
import com.example.ridedriver.ui.theme.RidaPrimary
import com.example.ridedriver.ui.theme.RidaRed
import com.example.ridedriver.ui.theme.RidaSecondary

private val AuthDividerColor = Color(0xFFE5EAE7)

/** Stationary, edge-to-edge auth page. Header and footer stay put; the body fills the space between them. */
@Composable
fun AuthScreen(
    header: @Composable ColumnScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
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
            Spacer(Modifier.height(16.dp))
            header()
            Spacer(Modifier.height(28.dp))
            body()
            Spacer(Modifier.weight(1f))
            footer()
        }
    }
}

@Composable
fun AuthHeader(
    title: String,
    subtitle: String,
) {
    RidaLogo(green = RidaPrimary)
    Spacer(Modifier.height(28.dp))
    Text(
        text = title,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        color = RidaInk,
        letterSpacing = (-0.5).sp
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = subtitle,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = RidaSecondary
    )
}

@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    @DrawableRes icon: Int,
    error: String? = null,
    labelAction: String? = null,
    onLabelAction: (() -> Unit)? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RidaInk)
        if (labelAction != null && onLabelAction != null) {
            Text(
                text = labelAction,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = RidaPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onLabelAction)
                    .padding(4.dp)
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        placeholder = { Text(placeholder, color = RidaHint, fontSize = 14.sp) },
        leadingIcon = {
            RidaIcon(icon = icon, contentDescription = null, tint = RidaHint, modifier = Modifier.size(20.dp))
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    RidaIcon(
                        icon = if (passwordVisible) RidaIcons.HidePassword else RidaIcons.ShowPassword,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = RidaSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction?.invoke() },
            onDone = { onImeAction?.invoke() }
        ),
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
    if (error != null) {
        AuthMessage(error, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
    }
}

@Composable
fun AuthMessage(text: String, modifier: Modifier = Modifier) {
    Text(text = text, color = RidaRed, fontSize = 12.sp, modifier = modifier)
}

@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    loading: Boolean = false,
    showForwardIcon: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(stiffness = 700f),
        label = "auth_button_scale"
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
                enabled = !loading,
                role = Role.Button,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        if (showForwardIcon) {
            Spacer(Modifier.width(8.dp))
            RidaIcon(icon = RidaIcons.Forward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun AuthOrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = AuthDividerColor)
        Text(
            text = "OR",
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 12.sp,
            color = RidaSecondary,
            fontWeight = FontWeight.Medium
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = AuthDividerColor)
    }
}

@Composable
fun AuthGoogleButton(
    text: String,
    @DrawableRes icon: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = RidaInk),
        border = BorderStroke(1.dp, RidaFieldBorder)
    ) {
        Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AuthFooterLink(
    prompt: String,
    action: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = prompt, fontSize = 14.sp, color = RidaSecondary)
        Text(
            text = action,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = RidaPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(4.dp)
        )
    }
}
