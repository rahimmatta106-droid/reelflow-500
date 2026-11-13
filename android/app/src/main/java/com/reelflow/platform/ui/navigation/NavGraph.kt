package com.reelflow.platform.ui.navigation

sealed class Screen(val route: String, val label: String) {
    data object Home : Screen("home", "Home")
    data object Library : Screen("library", "Library")
    data object Create : Screen("create", "Create")
    data object Review : Screen("review", "Review")
    data object Pages : Screen("pages", "Pages")
    data object Schedule : Screen("schedule", "Schedule")
    data object Analytics : Screen("analytics", "Analytics")
    data object Settings : Screen("settings", "Settings")
    data object ThumbnailEditor : Screen("thumbnail/{jobId}", "Thumbnail") {
        fun routeFor(jobId: String) = "thumbnail/$jobId"
    }
    data object VideoDetail : Screen("video/{jobId}", "Video") {
        fun routeFor(jobId: String) = "video/$jobId"
    }
}

val BOTTOM_NAV_ITEMS = listOf(
    Screen.Home,
    Screen.Library,
    Screen.Create,
    Screen.Review,
    Screen.Pages
)
