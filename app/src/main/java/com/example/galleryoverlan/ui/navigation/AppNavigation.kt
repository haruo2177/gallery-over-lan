package com.example.galleryoverlan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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

        composable(Routes.BROWSE) { backStackEntry ->
            val goBackToFolder = backStackEntry.savedStateHandle
                .getStateFlow("goBackToFolder", false)
                .collectAsState()

            BrowseScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { folderPath, startIndex, autoSlideshow ->
                    navController.navigate(Routes.viewer(folderPath, startIndex, autoSlideshow))
                },
                goBackToFolder = goBackToFolder.value,
                onGoBackToFolderHandled = {
                    backStackEntry.savedStateHandle["goBackToFolder"] = false
                }
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("folderPath") { type = NavType.StringType },
                navArgument("startIndex") { type = NavType.IntType },
                navArgument("autoSlideshow") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) {
            ViewerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateBackToFolder = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("goBackToFolder", true)
                    navController.popBackStack()
                }
            )
        }
    }
}
