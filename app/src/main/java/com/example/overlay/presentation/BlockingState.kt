package com.example.overlay.presentation

import androidx.compose.runtime.mutableStateOf

/**
 * Simple singleton to hold the global blocking state.
 * When `blockAll` is true, the overlay will be blocked regardless of Wi‑Fi.
 */
object BlockingState {
    var blockAll = mutableStateOf(false)
}
