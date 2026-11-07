package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.OptiHauntMainScreen
import com.example.ui.OptiHauntViewModel
import com.example.ui.theme.OptiHauntTheme

class MainActivity : ComponentActivity() {
    private val viewModel: OptiHauntViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
            OptiHauntTheme(selectedTheme = currentTheme) {
                OptiHauntMainScreen(viewModel = viewModel)
            }
        }
    }
}
