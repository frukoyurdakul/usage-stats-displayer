package com.fruko.usagestatsdisplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fruko.usagestatsdisplayer.presentation.main.MainScreen
import com.fruko.usagestatsdisplayer.ui.theme.UsageStatsDisplayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UsageStatsDisplayerTheme {
                MainScreen()
            }
        }
    }
}