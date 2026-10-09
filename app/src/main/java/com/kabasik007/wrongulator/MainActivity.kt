package com.kabasik007.wrongulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kabasik007.wrongulator.ui.CalculatorViewModel
import com.kabasik007.wrongulator.ui.WrongulatorScreen
import com.kabasik007.wrongulator.ui.WrongulatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WrongulatorTheme {
                val calculator: CalculatorViewModel = viewModel()
                WrongulatorScreen(
                    viewModel = calculator,
                    allowDebugPreview = BuildConfig.DEBUG,
                )
            }
        }
    }
}
