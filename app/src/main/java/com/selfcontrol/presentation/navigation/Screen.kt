package com.selfcontrol.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Apps : Screen("apps")
    data object AppDetails : Screen("apps/{packageName}") {
        fun createRoute(packageName: String) = "apps/$packageName"
    }
    data object Requests : Screen("requests")
    data object CreateRequest : Screen("requests/create")
    data object Violations : Screen("violations")
    data object Settings : Screen("settings")
    data object Urls : Screen("urls")
    data object Blocked : Screen("blocked/{packageName}") {
        fun createRoute(packageName: String) = "blocked/$packageName"
    }
}
