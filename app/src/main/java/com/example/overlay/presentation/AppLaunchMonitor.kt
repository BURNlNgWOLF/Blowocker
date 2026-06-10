package com.example.overlay.presentation

import android.content.Context
import android.util.Log
import com.example.overlay.presentation.WifiMonitor
import android.webkit.ConsoleMessage
import com.example.overlay.data.repository.SystemRepositoryImpl
import com.example.overlay.domain.usecase.MonitorAppLaunchUseCase
import com.example.overlay.domain.usecase.StartOverlayUseCase
import com.example.overlay.domain.usecase.ShouldStartOverlayUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppLaunchMonitor(context: Context) {
    private val repository = SystemRepositoryImpl.getInstance(context)
    private val monitorUseCase = MonitorAppLaunchUseCase(repository)
    private val startOverlayUseCase = StartOverlayUseCase(repository)
    private val scope = CoroutineScope(Dispatchers.Main)

    private val wifiMonitor = WifiMonitor(context)

    // Timestamp and package of the most recent switch event
    private var lastSwitchTimestamp: Long = 0L
    private var lastPackageName: String? = null

    // Map to accumulate total usage time per app to send them to db
    private val usageSeconds = mutableMapOf<String, Long>()

    private val db = com.example.overlay.data.database.AppDatabase.getInstance(context)

    fun startMonitoring(packageNames: List<String>) {
        // Ensure the in‑memory SSID list is up‑to‑date in case the user edited it in the UI.
        wifiMonitor.refreshSsids()
        scope.launch {
            repository.observeCurrentPackage().collect { packageName ->
                val ssid = wifiMonitor.getCurrentSsid()
                val timestamp = System.currentTimeMillis()
                lastPackageName?.let { prevPkg ->
                    val elapsedMs = timestamp - lastSwitchTimestamp
                    val elapsedSec = elapsedMs / 1000
                    usageSeconds[prevPkg] = (usageSeconds[prevPkg] ?: 0L) + elapsedSec
                    db.usageDao().upsert(prevPkg, elapsedSec)
                }
                lastPackageName = packageName
                lastSwitchTimestamp = timestamp
                Log.d(
                    "AppLaunchMonitor",
                    "Package switched to $packageName on SSID: $ssid at $timestamp"
                )
            }
        }

        scope.launch {
            monitorUseCase(packageNames) {
                // Ensure we have the latest SSID list before making a decision.
                wifiMonitor.refreshSsids()
                val shouldStart = ShouldStartOverlayUseCase(repository, wifiMonitor).invoke()
                Log.d("AppLaunchMonitor", "ShouldStartOverlayUseCase returned: $shouldStart")
                if (shouldStart) {
                    startOverlayUseCase(delayMs = 0)
                } else {
                    Log.d(
                        "AppLaunchMonitor",
                        "Overlay start blocked by ShouldStartOverlayUseCase"
                    )
                    return@monitorUseCase
                }
            }
        }
    }
}