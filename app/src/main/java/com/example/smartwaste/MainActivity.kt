package com.example.smartwaste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.smartwaste.ui.SmartWasteApp
import com.example.smartwaste.ui.theme.SmartWasteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartWasteTheme {
                SmartWasteApp()
            }
        }
    }
}
