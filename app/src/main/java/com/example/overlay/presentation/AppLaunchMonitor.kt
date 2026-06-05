package com.example.overlay.presentation

import android.content.Context
import android.util.Log
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

    fun startMonitoring(packageNames: List<String>) {
        scope.launch {
            monitorUseCase(packageNames) {
                startOverlayUseCase(delayMs = 0)
            }
        }
    }
}