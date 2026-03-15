package com.example.galleryoverlan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.galleryoverlan.ui.navigation.AppNavigation
import com.example.galleryoverlan.ui.theme.GalleryOverLanTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GalleryOverLanTheme {
                AppNavigation()
            }
        }
    }
}
