package com.example.prajarakshak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.prajarakshak.ui.FindBackApp
import com.example.prajarakshak.ui.theme.PrajaRakshakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrajaRakshakTheme {
                FindBackApp()
            }
        }
    }
}