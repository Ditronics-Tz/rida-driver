package com.example.ridedriver.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridedriver.R
import com.example.ridedriver.ui.theme.PoppinsFontFamily

@Composable
fun RidaSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Match the native splash's centered 288dp icon to avoid a jump at handoff.
        Image(
            painter = painterResource(R.drawable.rida_splash_mark),
            contentDescription = null,
            modifier = Modifier.size(288.dp)
        )
        Text(
            text = "Rida",
            color = Color(0xFF061D3D),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            letterSpacing = (-1).sp,
            modifier = Modifier.offset(y = 88.dp)
        )
        CircularProgressIndicator(
            color = Color(0xFF00B875),
            trackColor = Color(0xFFE8ECE9),
            strokeWidth = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = 40.dp)
                .size(20.dp)
        )
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun RidaSplashPreview() {
    RidaSplashScreen()
}
