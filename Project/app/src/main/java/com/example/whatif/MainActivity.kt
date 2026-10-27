package com.example.whatif

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.whatif.navigation.WhatIfApp
import com.example.whatif.ui.theme.WhatIfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhatIfTheme {
                WhatIfApp()
            }
        }
    }
}
