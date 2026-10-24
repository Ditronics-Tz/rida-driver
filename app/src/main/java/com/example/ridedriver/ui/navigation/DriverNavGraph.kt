package com.example.ridedriver.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.ui.screens.EditDriverProfileScreen
import com.example.ridedriver.ui.screens.EarningsBreakdownScreen
import com.example.ridedriver.ui.screens.WalletScreen
import com.example.ridedriver.ui.screens.PrivacySecurityScreen
import com.example.ridedriver.ui.screens.SupportCaseScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun authenticated() {
        scope.launch {
            val session = SessionManager.observe(context).first()
            navController.navigate(if (session.isProfileComplete) DriverRoutes.HOME else DriverRoutes.EDIT_PROFILE) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    LaunchedEffect(Unit) {
        SessionManager.observe(context).collect { session ->
            val route = navController.currentBackStackEntry?.destination?.route
            if (!session.loggedIn && route !in listOf(DriverRoutes.SIGN_IN, DriverRoutes.SIGN_UP, DriverRoutes.FORGOT_PASSWORD, null)) {
                navController.navigate(DriverRoutes.SIGN_IN) { popUpTo(0) { inclusive = true } }
            }
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val earningsRoutes = listOf(DriverRoutes.EARNINGS, DriverRoutes.BREAKDOWN, DriverRoutes.WALLET)
    val ridesRoutes = listOf(DriverRoutes.TRIPS, DriverRoutes.TRIP_DETAIL)
    val supportRoutes = listOf(DriverRoutes.SUPPORT, DriverRoutes.SUPPORT_CASE, DriverRoutes.RIDE_SUPPORT)
    val showBottomBar = currentRoute in earningsRoutes + ridesRoutes + listOf(DriverRoutes.SUPPORT, DriverRoutes.RIDE_SUPPORT, DriverRoutes.HOME, DriverRoutes.PROFILE)
    val currentTab = when(currentRoute) {
        in earningsRoutes -> "Earnings"
        in ridesRoutes -> "Rides"
        in supportRoutes -> "Support"
        else -> "Home"
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
                    onSupport = {
                        if (currentRoute != DriverRoutes.SUPPORT) {
                            navController.navigate(DriverRoutes.SUPPORT) {
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
                        onSignInSuccess = { authenticated() },
                        onForgotPassword = { navController.navigate(DriverRoutes.FORGOT_PASSWORD) },
                        onSignUp = { navController.navigate(DriverRoutes.SIGN_UP) }
                    )
                }

                composable(DriverRoutes.SIGN_UP) {
                    SignUpScreen(
                        onSignUpSuccess = { authenticated() },
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
                        onBack = { navController.popBackStack() },
                        onBreakdown = { navController.navigate(DriverRoutes.BREAKDOWN) },
                        onWallet = { navController.navigate(DriverRoutes.WALLET) },
                        onTripClick = { tripId ->
                            navController.navigate(DriverRoutes.tripDetail(tripId))
                        }
                    )
                }

                composable(DriverRoutes.TRIPS) {
                    TripsHistoryScreen(
                        onBack = { navController.popBackStack() },
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
                        onHelp = { navController.navigate(DriverRoutes.rideSupport(tripId)) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(DriverRoutes.PROFILE) {
                    DriverProfileScreen(
                        onBack = { navController.popBackStack() },
                        onPrivacy = { navController.navigate(DriverRoutes.PRIVACY) },
                        onEditProfile = { navController.navigate(DriverRoutes.EDIT_PROFILE) },
                        onNavigateToVehicle = { navController.navigate(DriverRoutes.VEHICLE_EDIT) },
                        onNavigateToDocuments = { navController.navigate(DriverRoutes.DOCUMENTS) },
                        onNavigateToSettings = { navController.navigate(DriverRoutes.SETTINGS) },
                        onNavigateToSupport = { navController.navigate(DriverRoutes.SUPPORT) }
                    )
                }

                composable(DriverRoutes.EDIT_PROFILE) {
                    EditDriverProfileScreen(
                        onSaved = { navController.navigate(DriverRoutes.HOME) { popUpTo(0) { inclusive = true } } },
                        onBack = { navController.popBackStack() }
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

                composable(DriverRoutes.BREAKDOWN) {
                    EarningsBreakdownScreen(onBack = { navController.popBackStack() }, onTrips = { navController.navigate(DriverRoutes.TRIPS) })
                }
                composable(DriverRoutes.WALLET) { WalletScreen(onBack = { navController.popBackStack() }) }
                composable(DriverRoutes.PRIVACY) {
                    PrivacySecurityScreen(onBack = { navController.popBackStack() },
                        onSupport = { navController.navigate(DriverRoutes.SUPPORT) },
                        onDeleted = { navController.navigate(DriverRoutes.SIGN_IN) { popUpTo(0) { inclusive = true } } })
                }
                composable(DriverRoutes.SUPPORT_CASE, arguments = listOf(navArgument("caseId") { type = NavType.StringType })) { entry ->
                    SupportCaseScreen(entry.arguments?.getString("caseId").orEmpty(), onBack = { navController.popBackStack() })
                }
                composable(DriverRoutes.RIDE_SUPPORT, arguments = listOf(navArgument("rideId") { type = NavType.StringType })) { entry ->
                    SupportScreen(onBack = { navController.popBackStack() }, rideId = entry.arguments?.getString("rideId"), onCase = { navController.navigate(DriverRoutes.supportCase(it)) })
                }
                composable(DriverRoutes.SUPPORT) {
                    SupportScreen(
                        onCase = { navController.navigate(DriverRoutes.supportCase(it)) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
