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

    fun startMonitoring(packageNames: List<String>) {
        // Log SSID on any package switch
        scope.launch {
            repository.observeCurrentPackage().collect { packageName ->
                val ssid = wifiMonitor.getCurrentSsid()
                Log.d("AppLaunchMonitor", "Package switched to $packageName on SSID: $ssid")
            }
        }
        // Existing monitoring for specific packages
        scope.launch {
            monitorUseCase(packageNames) {
                startOverlayUseCase(delayMs = 0)
            }
        }
    }
}