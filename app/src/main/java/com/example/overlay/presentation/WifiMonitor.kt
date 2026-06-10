package com.example.overlay.presentation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
// Using a simple mutable list for SSID storage; no Compose state needed here
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Simple helper that watches the current Wi‑Fi SSID and triggers a callback when the device
 * connects to a specific network. The class is intentionally lightweight – it does not
 * persist state or expose a Flow – because the current project only needs a one‑shot
 * notification to decide whether to scan for apps.
 */
class WifiMonitor(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val scope = CoroutineScope(Dispatchers.Main)

    // List of SSIDs to monitor. Using a mutableStateList so Compose recomposes when it changes.
    private val targetSsids = mutableStateListOf<String>()

    // Use SharedPreferences for persisting SSIDs across app restarts.
    private val prefsName = "wifi_monitor_prefs"
    private val ssidKey = "saved_ssids"

    init {
        loadSsidsFromPrefs()
    }

    /**
     * Public method to refresh the in‑memory list from SharedPreferences.
     * Call this when the UI page that displays the SSID list becomes visible
     * (e.g., in onResume) to ensure it reflects any changes that might have
     * been made while the app was not running.
     */
    fun refreshSsids() {
        loadSsidsFromPrefs()
    }

    /**
     * Loads SSIDs stored in SharedPreferences into [targetSsids].
     */
    private fun loadSsidsFromPrefs() {
        try {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val saved = prefs.getStringSet(ssidKey, emptySet()) ?: emptySet()
            targetSsids.clear()
            targetSsids.addAll(saved.filter { it.isNotBlank() })
            Log.d("WifiMonitor", "Adding to list from file $targetSsids")
        } catch (e: Exception) {
            Log.e("WifiMonitor", "Failed to load SSIDs from prefs", e)
        }
    }

    /**
     * Saves the current [targetSsids] to SharedPreferences.
     */
    private fun saveSsidsToPrefs() {
        try {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        // Use commit to ensure the data is written synchronously before the app may be killed
        prefs.edit().putStringSet(ssidKey, targetSsids.toSet()).commit()
        } catch (e: Exception) {
            Log.e("WifiMonitor", "Failed to save SSIDs to prefs", e)
        }
    }

    /**
     * Adds a new SSID to the watch list and persists the change.
     */
    fun addSsid(ssid: String) {
        if (ssid.isNotBlank() && ssid !in targetSsids) {
            targetSsids.add(ssid)
            saveSsidsToPrefs()
        }
    }

    /**
     * Returns true if the current SSID matches any of the target SSIDs.
     */
    /**
     * Returns true if the provided SSID matches any of the target SSIDs, ignoring case.
     * The comparison is defensive: both the stored SSIDs and the current SSID are trimmed
     * of surrounding whitespace and compared case‑insensitively. This prevents mismatches
     * caused by differences in capitalization that are irrelevant for Wi‑Fi network names.
     */
    fun isTargetSsid(current: String): Boolean {
        val normalizedCurrent = current.trim().lowercase()
        return targetSsids.any { it.trim().lowercase() == normalizedCurrent }
    }

    /**
     * Returns the current list of target SSIDs.
     */
    /**
     * Returns a snapshot of the current SSID list. Because `targetSsids` is a
     * `mutableStateListOf`, any composable that reads this list will recompose
     * automatically when items are added or removed.
     */
    fun getTargetSsids(): List<String> = targetSsids.toList()

    /**
     * Removes an SSID from the watch list and persists the change.
     */
    fun removeSsid(ssid: String) {
        if (targetSsids.remove(ssid)) {
            saveSsidsToPrefs()
        }
    }

    /**
     * Starts monitoring the Wi‑Fi connection. When the device connects to a network whose SSID
     * matches any of the provided [targetSsids] the [onConnected] callback is invoked.
     */
    fun startMonitoring(targetSsids: List<String>, onConnected: () -> Unit) {
        scope.launch {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    val ssid = getCurrentSsid()
                    if (ssid in targetSsids) {
                        onConnected()
                    }
                }
            }
            val request = android.net.NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
        }
    }

    /**
     * Returns the SSID of the currently connected Wi‑Fi network. If Wi‑Fi is disabled or the
     * SSID cannot be determined, an empty string is returned.
     */
    fun getCurrentSsid(): String {
        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wifiManager.connectionInfo.ssid?.trim('"') ?: ""
        } else {
            wifiManager.connectionInfo.ssid?.trim('"') ?: ""
        }
        return if (ssid.isBlank() || ssid == "<unknown ssid>") "Unknown" else ssid
    }
}
