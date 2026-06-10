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

    // Public refresh
    fun refreshSsids() {
        loadSsidsFromPrefs()
    }

    // Loads from file to memory
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

    // Saves from memory to file
    private fun saveSsidsToPrefs() {
        try {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        // Use commit to ensure the data is written synchronously before the app may be killed
        prefs.edit().putStringSet(ssidKey, targetSsids.toSet()).commit()
        } catch (e: Exception) {
            Log.e("WifiMonitor", "Failed to save SSIDs to prefs", e)
        }
    }

    fun addSsid(ssid: String) {
        if (ssid.isNotBlank() && ssid !in targetSsids) {
            targetSsids.add(ssid)
            saveSsidsToPrefs()
        }
    }


    fun isTargetSsid(current: String): Boolean {
        val normalizedCurrent = current.trim().lowercase()
        return targetSsids.any { it.trim().lowercase() == normalizedCurrent }
    }


    fun getTargetSsids(): List<String> = targetSsids.toList()

    fun removeSsid(ssid: String) {
        if (targetSsids.remove(ssid)) {
            saveSsidsToPrefs()
        }
    }

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

    // Returns connected Wifi
    fun getCurrentSsid(): String {
        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wifiManager.connectionInfo.ssid?.trim('"') ?: ""
        } else {
            wifiManager.connectionInfo.ssid?.trim('"') ?: ""
        }
        return if (ssid.isBlank() || ssid == "<unknown ssid>") "Unknown" else ssid
    }
}
