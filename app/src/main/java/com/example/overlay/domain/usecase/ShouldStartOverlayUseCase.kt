package com.example.overlay.domain.usecase

import com.example.overlay.domain.repository.SystemRepository
import com.example.overlay.presentation.WifiMonitor
import com.example.overlay.presentation.BlockingState

class ShouldStartOverlayUseCase(
    private val repository: SystemRepository,
    private val wifiMonitor: WifiMonitor
) {
    operator fun invoke(): Boolean {
        // If the global block‑all switch is enabled, never start the overlay.
        if (BlockingState.blockAll.value) return false

        // When Wi‑Fi‑based mode is active, only start the overlay if the current SSID matches a target.
        if (BlockingState.wifiBasedMode.value) {
            val currentSsid = wifiMonitor.getCurrentSsid()
            return wifiMonitor.isTargetSsid(currentSsid)
        }

        // Allow overlay
        return true
    }
}
