package com.selfcontrol.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.selfcontrol.presentation.apps.AppDetailsScreen
import com.selfcontrol.presentation.apps.AppsScreen
import com.selfcontrol.presentation.blocked.BlockedScreen
import com.selfcontrol.presentation.home.HomeScreen
import com.selfcontrol.presentation.requests.CreateRequestScreen
import com.selfcontrol.presentation.requests.RequestsScreen
import com.selfcontrol.presentation.settings.SettingsScreen
import com.selfcontrol.presentation.violations.ViolationsScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route,
    navigationActions: NavigationActions = NavigationActions(navController)
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navigationActions = navigationActions)
        }
        
        composable(Screen.Apps.route) {
            AppsScreen(navigationActions = navigationActions)
        }
        
        composable(
            route = Screen.AppDetails.route,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            AppDetailsScreen(
                packageName = packageName,
                navigationActions = navigationActions
            )
        }
        
        composable(Screen.Requests.route) {
            RequestsScreen(navigationActions = navigationActions)
        }
        
        composable(Screen.CreateRequest.route) {
            CreateRequestScreen(navigationActions = navigationActions)
        }
        
        composable(Screen.Violations.route) {
            ViolationsScreen(navigationActions = navigationActions)
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(navigationActions = navigationActions)
        }
        
        composable(
            route = Screen.Blocked.route,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            BlockedScreen(
                packageName = packageName,
                navigationActions = navigationActions
            )
        }
    }
}
