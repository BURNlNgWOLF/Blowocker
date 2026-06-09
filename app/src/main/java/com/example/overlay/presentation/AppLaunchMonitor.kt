package com.example.overlay.presentation

import android.content.Context
import android.util.Log
import com.example.overlay.presentation.WifiMonitor
import android.webkit.ConsoleMessage
import com.example.overlay.data.repository.SystemRepositoryImpl
import com.example.overlay.domain.usecase.MonitorAppLaunchUseCase
import com.example.overlay.domain.usecase.StartOverlayUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppLaunchMonitor(context: Context) {
    private val repository = SystemRepositoryImpl.getInstance(context)
    private val monitorUseCase = MonitorAppLaunchUseCase(repository)
    private val startOverlayUseCase = StartOverlayUseCase(repository)
    private val scope = CoroutineScope(Dispatchers.Main)

    private val wifiMonitor = WifiMonitor(context)
    // Map to store the timestamp of the last package switch for each app
    private val lastSwitchTimestamp = mutableMapOf<String, Long>()
    // Map to accumulate total usage time (in seconds) per app
    private val usageSeconds = mutableMapOf<String, Long>()

    fun startMonitoring(packageNames: List<String>) {
        // Log SSID on any package switch
        scope.launch {
            repository.observeCurrentPackage().collect { packageName ->
                val ssid = wifiMonitor.getCurrentSsid()
                // Record the timestamp of this package switch and update usage
                val timestamp = System.currentTimeMillis()
                // If we have a previous timestamp for this package, calculate the elapsed time
                lastSwitchTimestamp[packageName]?.let { previous ->
                    val elapsedMs = timestamp - previous
                    val elapsedSec = elapsedMs / 1000
                    usageSeconds[packageName] = (usageSeconds[packageName] ?: 0L) + elapsedSec
                }
                // Update the last switch timestamp for the package
                lastSwitchTimestamp[packageName] = timestamp
                Log.d(
                    "AppLaunchMonitor",
                    "Package switched to $packageName on SSID: $ssid at $timestamp"
                )
            }
        }

        // Monitor specific packages and apply blocking logic
        scope.launch {
            monitorUseCase(packageNames) {
                // Block if global block‑all is enabled
                if (BlockingState.blockAll.value) {
                    Log.d("AppLaunchMonitor", "Blocking overlay due to global block‑all switch")
                    return@monitorUseCase
                }
                // Only start overlay if current SSID is not in the blocked list
                val currentSsid = wifiMonitor.getCurrentSsid()
                if (!wifiMonitor.isTargetSsid(currentSsid)) {
                    startOverlayUseCase(delayMs = 0)
                } else {
                    Log.d("AppLaunchMonitor", "Blocking overlay on SSID $currentSsid")
                }
            }
        }
    }

    /**
     * Returns a map of package names to the total number of seconds the app has been used.
     * This is calculated from the recorded switch timestamps. Packages that have not yet
     * recorded a second switch will have a usage of 0 seconds.
     */
    fun getUsageSeconds(): Map<String, Long> = usageSeconds.toMap()
}