package com.example.overlay.presentation

import androidx.compose.runtime.mutableStateOf

object BlockingState {
    var blockAll = mutableStateOf(false)
    // Flag controlled by SettingsScreen switch: when true, overlay only shows on target SSIDs
    var wifiBasedMode = mutableStateOf(false)
}
