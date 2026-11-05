package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.LiveInterpreterScreen
import com.example.ui.theme.HamZabanTheme
import com.example.util.configureAppCheck
import com.example.viewmodel.LiveInterpreterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        // STEP 2: Enable edge-to-edge display and initialize UI
        enableEdgeToEdge()
        setContent {
            HamZabanTheme {
                val interpreterViewModel: LiveInterpreterViewModel = viewModel()
                LiveInterpreterScreen(viewModel = interpreterViewModel)
            }
        }
    }
}
