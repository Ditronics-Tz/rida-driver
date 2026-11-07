package com.example.ridedriver.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import com.example.ridedriver.ui.navigation.openHomeAfterAuthentication
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController


class ScreenStateRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun resetEmailCannotClaimDeliveryWithoutAnEndpoint() {
        compose.setContent { ForgotPasswordScreen({}) }
        compose.onNode(hasSetTextAction()).performTextInput("driver@example.test")
        compose.onNodeWithText("Send Reset Link").performClick()
        compose.onAllNodesWithText("We sent a password reset link", substring = true).assertCountEquals(0)
    }
    @Test fun successfulSignupCannotGoBackToAnEarlierLoginScreen() {
        lateinit var controller: NavHostController
        compose.setContent {
            controller = rememberNavController()
            NavHost(controller, startDestination = com.example.ridedriver.ui.navigation.DriverRoutes.SIGN_IN) {
                composable(com.example.ridedriver.ui.navigation.DriverRoutes.SIGN_IN) { androidx.compose.material3.Text("Login fixture") }
                composable(com.example.ridedriver.ui.navigation.DriverRoutes.SIGN_UP) { androidx.compose.material3.Text("Signup fixture") }
                composable(com.example.ridedriver.ui.navigation.DriverRoutes.HOME) { androidx.compose.material3.Text("Home fixture") }
            }
        }
        compose.runOnIdle {
            controller.navigate(com.example.ridedriver.ui.navigation.DriverRoutes.SIGN_UP)
            controller.openHomeAfterAuthentication()
            org.junit.Assert.assertEquals(com.example.ridedriver.ui.navigation.DriverRoutes.HOME, controller.currentDestination?.route)
            org.junit.Assert.assertNull(controller.previousBackStackEntry)
        }
    }

}
