package com.nagmo.app.ui.theme

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** Keeps status/navigation bar icons readable for the in-app theme (not just the system one). */
@Composable
fun ComponentActivity.SyncSystemBars() {
    val dark = LocalNagmo.current.dark
    LaunchedEffect(dark) {
        val style = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
