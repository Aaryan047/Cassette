package com.example.cassettev01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cassettev01.player.PlayerViewModel
import com.example.cassettev01.ui.CassetteTheme
import com.example.cassettev01.ui.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val playerViewModel: PlayerViewModel = viewModel()

            val currentTheme by playerViewModel.currentTheme.collectAsState()
            val appearance by playerViewModel.appearance.collectAsState()

            CassetteTheme(palette = currentTheme, appearance = appearance) {
                HomeScreen(viewModel = playerViewModel)
            }
        }
    }
}