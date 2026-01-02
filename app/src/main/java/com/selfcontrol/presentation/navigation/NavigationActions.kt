package com.selfcontrol.presentation.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

class NavigationActions(private val navController: NavHostController) {

    fun navigateToHome() {
        navController.navigate(Screen.Home.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToApps() {
        navController.navigate(Screen.Apps.route) {
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToAppDetails(packageName: String) {
        navController.navigate(Screen.AppDetails.createRoute(packageName))
    }

    fun navigateToRequests() {
        navController.navigate(Screen.Requests.route)
    }

    fun navigateToCreateRequest() {
        navController.navigate(Screen.CreateRequest.route)
    }

    fun navigateToViolations() {
        navController.navigate(Screen.Violations.route)
    }

    fun navigateToSettings() {
        navController.navigate(Screen.Settings.route)
    }
    
    fun navigateToBlocked(packageName: String) {
        navController.navigate(Screen.Blocked.createRoute(packageName)) {
             // Clear back stack so they can't go back easily? 
             // Or maybe just standard nav for now.
             launchSingleTop = true
        }
    }

    fun navigateToUrls() {
        navController.navigate(Screen.Urls.route) {
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateBack() {
        navController.popBackStack()
    }
}
