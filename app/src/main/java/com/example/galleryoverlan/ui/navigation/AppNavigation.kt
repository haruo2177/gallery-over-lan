package com.example.galleryoverlan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.galleryoverlan.ui.browse.BrowseScreen
import com.example.galleryoverlan.ui.connect.ConnectScreen
import com.example.galleryoverlan.ui.viewer.ViewerScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.CONNECT
    ) {
        composable(Routes.CONNECT) {
            ConnectScreen(
                onNavigateToBrowse = {
                    navController.navigate(Routes.BROWSE) {
                        popUpTo(Routes.CONNECT) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.BROWSE) {
            BrowseScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { folderPath, startIndex ->
                    navController.navigate(Routes.viewer(folderPath, startIndex))
                }
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("folderPath") { type = NavType.StringType },
                navArgument("startIndex") { type = NavType.IntType }
            )
        ) {
            ViewerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
