package com.example.galleryoverlan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.galleryoverlan.ui.settings.SettingsScreen
import com.example.galleryoverlan.ui.viewer.ImageListScreen

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
                    navController.navigate(Routes.IMAGE_LIST)
                }
            )
        }

        composable(Routes.IMAGE_LIST) {
            ImageListScreen(
                onNavigateBack = { navController.popBackStack() },
                onImageClick = { /* P1: full-screen viewer */ }
            )
        }
    }
}
