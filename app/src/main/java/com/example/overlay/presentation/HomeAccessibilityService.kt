package com.example.overlay.presentation

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.overlay.data.repository.SystemRepositoryImpl

class HomeAccessibilityService : AccessibilityService() {

    private lateinit var repository: SystemRepositoryImpl

    companion object {
        private const val TAG = "HomeAccessibility"
        private var instance: HomeAccessibilityService? = null

        fun goToHome(): Boolean {
            val service = instance
            return if (service != null) {
                val result = service.performGlobalAction(GLOBAL_ACTION_HOME)
                Log.d(TAG, "GLOBAL_ACTION_HOME executed: $result")
                result
            } else {
                Log.w(TAG, "AccessibilityService not available")
                false
            }
        }

        fun isAvailable(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        instance = this
        repository = SystemRepositoryImpl.getInstance(applicationContext)
        val monitoredPackages = repository.getMonitoredPackages()
        AppLaunchMonitor(applicationContext).startMonitoring(monitoredPackages)
        Log.d(TAG, "Service connected and monitoring started for ${monitoredPackages.size} packages")
    }

    override fun onInterrupt() {
        instance = null
        Log.d(TAG, "Service interrupted")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            Log.d(TAG, "Detected window change: $packageName")
            if (packageName != null) {
                repository.updateCurrentPackage(packageName)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}