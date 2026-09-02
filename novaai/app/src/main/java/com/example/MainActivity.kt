package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.NovaAiViewModel
import com.example.ui.screens.NovaMainScreen
import com.example.ui.theme.NovaAITheme

class MainActivity : ComponentActivity() {
    private val viewModel: NovaAiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovaAITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NovaMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
