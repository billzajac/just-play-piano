package com.windupairships.airshippiano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.windupairships.airshippiano.ui.MainScreen
import com.windupairships.airshippiano.ui.theme.AirshipPianoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirshipPianoTheme {
                MainScreen()
            }
        }
    }
}
