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

    // Timestamp and package of the most recent switch event
    private var lastSwitchTimestamp: Long = 0L
    private var lastPackageName: String? = null

    // Map to accumulate total usage time (in seconds) per app
    private val usageSeconds = mutableMapOf<String, Long>()

    // Room database instance for persisting usage
    private val db = com.example.overlay.data.database.AppDatabase.getInstance(context)

    fun startMonitoring(packageNames: List<String>) {
        // Ensure the in‑memory SSID list is up‑to‑date in case the user edited it in the UI.
        wifiMonitor.refreshSsids()
        // Log SSID on any package switch
        scope.launch {
            repository.observeCurrentPackage().collect { packageName ->
                val ssid = wifiMonitor.getCurrentSsid()
                // Record the timestamp of this package switch and update usage for the previous app
                val timestamp = System.currentTimeMillis()
                // If we have a previously recorded package, calculate its elapsed time
                lastPackageName?.let { prevPkg ->
                    val elapsedMs = timestamp - lastSwitchTimestamp
                    val elapsedSec = elapsedMs / 1000
                    usageSeconds[prevPkg] = (usageSeconds[prevPkg] ?: 0L) + elapsedSec
                    // Persist usage data to the Room database for the previous package
                    db.usageDao().upsert(prevPkg, elapsedSec)
                }
                // Update the last switch info for the current package
                lastPackageName = packageName
                lastSwitchTimestamp = timestamp
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
                // Determine whether to start overlay based on global block‑all and optional Wi‑Fi mode
                // If the Wi‑Fi based mode is enabled, only start overlay when the current SSID matches one of the target SSIDs.
                // If the Wi‑Fi based mode is disabled, start overlay unconditionally (subject to global block‑all above).
                // Determine whether to start the overlay based on the global block‑all flag
                // and the optional Wi‑Fi‑based mode. The logic is now explicit and defensive:
                //   • If block‑all is enabled, we never start the overlay (handled earlier).
                //   • If Wi‑Fi‑based mode is enabled, we only start the overlay when the
                //     current SSID matches one of the user‑defined target SSIDs.
                //   • Otherwise (Wi‑Fi‑based mode disabled) we start the overlay unconditionally.
                // Refresh SSID list and log current state for debugging
                wifiMonitor.refreshSsids()
                val currentSsid = wifiMonitor.getCurrentSsid()
                Log.d("AppLaunchMonitor", "Current SSID: $currentSsid, targets: ${wifiMonitor.getTargetSsids()}")
                if (BlockingState.wifiBasedMode.value) {
                    // Wi‑Fi mode active: start overlay only if the current SSID matches a target.
                    if (wifiMonitor.isTargetSsid(currentSsid)) {
                        startOverlayUseCase(delayMs = 0)
                    } else {
                        Log.d(
                            "AppLaunchMonitor",
                            "Blocking overlay on SSID $currentSsid (Wi‑Fi mode active)"
                        )
                        return@monitorUseCase
                    }
                } else {
                    // Wi‑Fi mode not active, allow overlay regardless of SSID
                    startOverlayUseCase(delayMs = 0)
                }
            }
        }

        /**
         * Debug helper: logs all usage records currently stored in the database.
         * Call this method from a UI action or directly after a short delay to verify
         * that data is being persisted.
         */
        fun logAllUsageRecords() {
            // Run on a background thread to avoid blocking the main thread
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val records = db.usageDao().getAll()
                android.util.Log.d("AppLaunchMonitor", "Current usage records: $records")
            }
        }

        /**
         * Returns a map of package names to the total number of seconds the app has been used.
         * This is calculated from the recorded switch timestamps. Packages that have not yet
         * recorded a second switch will have a usage of 0 seconds.
         */
        fun getUsageSeconds(): Map<String, Long> = usageSeconds.toMap()
    }
}