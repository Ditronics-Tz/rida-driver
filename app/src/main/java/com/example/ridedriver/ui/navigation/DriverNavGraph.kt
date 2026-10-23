package com.example.ridedriver.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ridedriver.ui.components.DriverBottomBar
import com.example.ridedriver.ui.screens.DocumentsScreen
import com.example.ridedriver.ui.screens.DriverHomeScreen
import com.example.ridedriver.ui.screens.DriverProfileScreen
import com.example.ridedriver.ui.screens.EarningsScreen
import com.example.ridedriver.ui.screens.ForgotPasswordScreen
import com.example.ridedriver.ui.screens.SettingsScreen
import com.example.ridedriver.ui.screens.SignInScreen
import com.example.ridedriver.ui.screens.SignUpScreen
import com.example.ridedriver.ui.screens.SupportScreen
import com.example.ridedriver.ui.screens.TripDetailScreen
import com.example.ridedriver.ui.screens.TripsHistoryScreen
import com.example.ridedriver.ui.screens.VehicleEditScreen

@Composable
fun DriverNavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val mainTabs = listOf(
        DriverRoutes.HOME,
        DriverRoutes.EARNINGS,
        DriverRoutes.TRIPS,
        DriverRoutes.PROFILE
    )

    val showBottomBar = currentRoute in mainTabs

    val currentTab = when (currentRoute) {
        DriverRoutes.HOME -> "Home"
        DriverRoutes.EARNINGS -> "Earnings"
        DriverRoutes.TRIPS -> "Trips"
        DriverRoutes.PROFILE -> "Profile"
        else -> ""
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                DriverBottomBar(
                    selected = currentTab,
                    onHome = {
                        if (currentRoute != DriverRoutes.HOME) {
                            navController.navigate(DriverRoutes.HOME) {
                                popUpTo(DriverRoutes.HOME) { inclusive = true }
                            }
                        }
                    },
                    onEarnings = {
                        if (currentRoute != DriverRoutes.EARNINGS) {
                            navController.navigate(DriverRoutes.EARNINGS) {
                                popUpTo(DriverRoutes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onTrips = {
                        if (currentRoute != DriverRoutes.TRIPS) {
                            navController.navigate(DriverRoutes.TRIPS) {
                                popUpTo(DriverRoutes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onProfile = {
                        if (currentRoute != DriverRoutes.PROFILE) {
                            navController.navigate(DriverRoutes.PROFILE) {
                                popUpTo(DriverRoutes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                // Auth Routes
                composable(DriverRoutes.SIGN_IN) {
                    SignInScreen(
                        onSignInSuccess = {
                            navController.navigate(DriverRoutes.HOME) {
                                popUpTo(DriverRoutes.SIGN_IN) { inclusive = true }
                            }
                        },
                        onForgotPassword = { navController.navigate(DriverRoutes.FORGOT_PASSWORD) },
                        onSignUp = { navController.navigate(DriverRoutes.SIGN_UP) }
                    )
                }

                composable(DriverRoutes.SIGN_UP) {
                    SignUpScreen(
                        onSignUpSuccess = {
                            navController.navigate(DriverRoutes.HOME) {
                                popUpTo(DriverRoutes.SIGN_UP) { inclusive = true }
                            }
                        },
                        onBackToSignIn = { navController.popBackStack() }
                    )
                }

                composable(DriverRoutes.FORGOT_PASSWORD) {
                    ForgotPasswordScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                // Main Driver Routes
                composable(DriverRoutes.HOME) {
                    DriverHomeScreen(
                        onNavigateToEarnings = { navController.navigate(DriverRoutes.EARNINGS) },
                        onNavigateToProfile = { navController.navigate(DriverRoutes.PROFILE) }
                    )
                }

                composable(DriverRoutes.EARNINGS) {
                    EarningsScreen(
                        onTripClick = { tripId ->
                            navController.navigate(DriverRoutes.tripDetail(tripId))
                        }
                    )
                }

                composable(DriverRoutes.TRIPS) {
                    TripsHistoryScreen(
                        onTripClick = { tripId ->
                            navController.navigate(DriverRoutes.tripDetail(tripId))
                        }
                    )
                }

                composable(
                    route = DriverRoutes.TRIP_DETAIL,
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString("tripId").orEmpty()
                    TripDetailScreen(
                        tripId = tripId,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(DriverRoutes.PROFILE) {
                    DriverProfileScreen(
                        onNavigateToVehicle = { navController.navigate(DriverRoutes.VEHICLE_EDIT) },
                        onNavigateToDocuments = { navController.navigate(DriverRoutes.DOCUMENTS) },
                        onNavigateToSettings = { navController.navigate(DriverRoutes.SETTINGS) },
                        onNavigateToSupport = { navController.navigate(DriverRoutes.SUPPORT) }
                    )
                }

                composable(DriverRoutes.VEHICLE_EDIT) {
                    VehicleEditScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(DriverRoutes.DOCUMENTS) {
                    DocumentsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(DriverRoutes.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToSupport = { navController.navigate(DriverRoutes.SUPPORT) },
                        onLogout = {
                            navController.navigate(DriverRoutes.SIGN_IN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(DriverRoutes.SUPPORT) {
                    SupportScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
