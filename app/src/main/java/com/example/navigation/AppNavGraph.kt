package com.example.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.SavingsViewModel

@Composable
fun AppNavGraph(
    viewModel: FinanceViewModel
) {
    val savingsViewModel: SavingsViewModel = viewModel()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    // Local onboarding / authentication selector states
    var showSplash by remember { mutableStateOf(true) }
    var showOnboarding by remember { mutableStateOf(true) }
    var currentAuthScreen by remember { mutableStateOf("login") } // login or register

    if (showSplash) {
        SplashScreen(
            onSplashFinished = { showSplash = false }
        )
    } else if (showOnboarding) {
        OnboardingScreen(
            onFinishOnboarding = { showOnboarding = false }
        )
    } else if (currentUser == null) {
        if (currentAuthScreen == "login") {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { currentAuthScreen = "register" },
                onLoginSuccess = {}
            )
        } else {
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { currentAuthScreen = "login" },
                onRegisterSuccess = {}
            )
        }
    } else {
        // Main App Layout with Modern Bottom Navigation
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route
        val premiumUpgradeFeature by viewModel.premiumUpgradeFeature.collectAsStateWithLifecycle()

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = SurfaceElevation,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(80.dp)
                        .testTag("app_bottom_navigation")
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                ) {
                    val items = listOf(
                        Triple(Screen.Dashboard.route, "Home", Icons.Default.Home),
                        Triple(Screen.Transactions.route, "Transazioni", Icons.Default.FormatListBulleted),
                        Triple(Screen.SplitBill.route, "Split Bill", Icons.Default.Group),
                        Triple(Screen.Analytics.route, "Analisi", Icons.Default.BarChart),
                        Triple(Screen.Settings.route, "Impostazioni", Icons.Default.Settings)
                    )
 
                    items.forEach { (route, label, icon) ->
                        val isSelected = currentRoute == route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
                            label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00E676),
                                selectedTextColor = Color(0xFFA8FF60),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Color(0xFF00E676).copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                        onNavigateToAiCoach = {
                            navController.navigate(Screen.AiCoach.route)
                        },
                        onNavigateToTransactions = {
                            navController.navigate(Screen.Transactions.route)
                        },
                        onNavigateToSplitBill = {
                            navController.navigate(Screen.SplitBill.route)
                        },
                        onNavigateToAnalytics = {
                            navController.navigate(Screen.Analytics.route)
                        },
                        onNavigateToSavingsGoals = {
                            navController.navigate(Screen.SavingsGoals.route)
                        },
                        onNavigateToAccounts = {
                            navController.navigate(Screen.Accounts.route)
                        }
                    )
                }

                composable(Screen.Transactions.route) {
                    TransactionsScreen(
                        viewModel = viewModel,
                        onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                        onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) }
                    )
                }

                composable(Screen.SplitBill.route) {
                    SplitBillScreen(
                        viewModel = viewModel,
                        onPremiumUpgradeRequired = { feature ->
                            viewModel.triggerPremiumUpgradeSheet(feature)
                        }
                    )
                }

                composable(Screen.Analytics.route) {
                    AnalyticsScreen(
                        viewModel = viewModel
                    )
                }

                composable(Screen.AiCoach.route) {
                    AiCoachScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onLogoutSuccess = {},
                        onNavigateToAutomaticRules = { navController.navigate(Screen.AutomaticRules.route) },
                        onNavigateToRecurringTransactions = { navController.navigate(Screen.RecurringTransactions.route) },
                        onNavigateToPaywall = { navController.navigate(Screen.PremiumPaywall.route) }
                    )
                }

                composable(Screen.RecurringTransactions.route) {
                    RecurringTransactionsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AddTransaction.route) {
                    AddTransactionScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Accounts.route) {
                    AccountsScreen(
                        financeViewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onPremiumUpgradeRequired = { feature ->
                            viewModel.triggerPremiumUpgradeSheet(feature)
                        }
                    )
                }

                composable(Screen.AutomaticRules.route) {
                    AutomaticRulesScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.SavingsGoals.route) {
                    SavingsGoalsScreen(
                        viewModel = savingsViewModel,
                        onNavigateToDetail = { goalId ->
                            navController.navigate(Screen.SavingsGoalDetail.createRoute(goalId))
                        },
                        onNavigateBack = { navController.popBackStack() },
                        onPremiumUpgradeRequired = { feature ->
                            viewModel.triggerPremiumUpgradeSheet(feature)
                        }
                    )
                }

                composable(
                    route = Screen.SavingsGoalDetail.route,
                    arguments = listOf(navArgument("goalId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val goalId = backStackEntry.arguments?.getInt("goalId") ?: 0
                    SavingsGoalDetailScreen(
                        goalId = goalId,
                        viewModel = savingsViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.PremiumPaywall.route) {
                    PremiumPaywallScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            if (premiumUpgradeFeature != null) {
                com.example.ui.components.PremiumUpgradeBottomSheet(
                    feature = premiumUpgradeFeature!!,
                    onDismiss = { viewModel.dismissPremiumUpgradeSheet() },
                    onNavigateToPaywall = {
                        navController.navigate(Screen.PremiumPaywall.route)
                    },
                    onUpgradeSuccess = {
                        com.example.premium.PremiumManager.setPlan(viewModel.getApplication(), com.example.premium.PremiumPlan.PREMIUM)
                        viewModel.togglePremiumStatus(true)
                    }
                )
            }
        }
    }
}
