package com.example.prajarakshak

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.prajarakshak.ui.FindBackApp
import com.example.prajarakshak.ui.theme.PrajaRakshakTheme

class MainActivity : AppCompatActivity() {
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