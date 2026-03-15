package com.example.galleryoverlan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.galleryoverlan.ui.browser.BrowserScreen
import com.example.galleryoverlan.ui.settings.SettingsScreen
import com.example.galleryoverlan.ui.viewer.ImageListScreen
import com.example.galleryoverlan.ui.viewer.ViewerScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SETTINGS
    ) {
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateToImageList = {
                    navController.navigate(Routes.BROWSER)
                }
            )
        }

        composable(Routes.BROWSER) {
            BrowserScreen(
                onNavigateBack = { navController.popBackStack() },
                onFolderClick = { path ->
                    // Navigate within browser (handled by ViewModel)
                },
                onViewImages = { folderPath ->
                    navController.navigate(Routes.imageList(folderPath))
                }
            )
        }

        composable(
            route = Routes.IMAGE_LIST,
            arguments = listOf(
                navArgument("folderPath") { type = NavType.StringType }
            )
        ) {
            ImageListScreen(
                onNavigateBack = { navController.popBackStack() },
                onImageClick = { index ->
                    val folderPath = it.arguments?.getString("folderPath")?.trim() ?: ""
                    navController.navigate(Routes.viewer(folderPath, index))
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
