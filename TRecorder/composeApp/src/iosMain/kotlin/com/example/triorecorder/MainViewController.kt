package com.example.triorecorder

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import com.example.triorecorder.ui.TriominosMobileApp

fun MainViewController(): UIViewController = ComposeUIViewController {
    TriominosMobileApp()
}
