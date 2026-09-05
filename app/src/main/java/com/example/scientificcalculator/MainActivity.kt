package com.example.scientificcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.scientificcalculator.ui.ScientificCalculatorApp
import com.example.scientificcalculator.ui.theme.ScientificCalculatorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Allow the user to toggle between light and dark theme manually;
            // the default follows the system setting.
            var darkTheme by remember { mutableStateOf(isSystemInDarkTheme()) }

            ScientificCalculatorTheme(darkTheme = darkTheme) {
                ScientificCalculatorApp(
                    darkTheme = darkTheme,
                    onDarkThemeChange = { darkTheme = it }
                )
            }
        }
    }
}
