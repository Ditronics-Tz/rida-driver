package com.example.ridedriver

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.remote.RidaApiClient
import com.example.ridedriver.data.repository.UserRepository
import com.example.ridedriver.ui.navigation.DriverNavGraph
import com.example.ridedriver.ui.navigation.DriverRoutes
import com.example.ridedriver.ui.screens.RidaSplashScreen
import com.example.ridedriver.ui.theme.RidaDriverTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        RidaApiClient.init(applicationContext)
        setContent {
            RidaDriverTheme {
                var startDestination by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    startDestination = try {
                        withContext(Dispatchers.IO) {
                            val loggedIn = SessionManager.isLoggedIn(this@MainActivity)
                            if (loggedIn) {
                                UserRepository.refreshSessionFromApi(this@MainActivity)
                            }
                            if (loggedIn) DriverRoutes.HOME else DriverRoutes.SIGN_IN
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        DriverRoutes.SIGN_IN
                    }
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    val destination = startDestination
                    if (destination == null) {
                        RidaSplashScreen()
                    } else {
                        DriverNavGraph(startDestination = destination)
                    }
                }
            }
        }
    }
}
