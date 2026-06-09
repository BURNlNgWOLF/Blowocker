package com.example.overlay.presentation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
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

    /**
     * Starts monitoring the Wi‑Fi connection. When the device connects to a network whose SSID
     * matches [targetSsid] the [onConnected] callback is invoked.
     */
    fun startMonitoring(targetSsid: String, onConnected: () -> Unit) {
        scope.launch {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    val ssid = getCurrentSsid()
                    if (ssid == targetSsid) {
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
