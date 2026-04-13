package com.example.triorecorder.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.triorecorder.data.SettingsGameRepository
import com.example.triorecorder.data.createSettings

@Composable
fun TriominosMobileApp() {
    val repository = remember { SettingsGameRepository(createSettings()) }
    TriominosApp(repository = repository)
}
