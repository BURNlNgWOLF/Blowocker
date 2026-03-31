package com.example.overlay

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class HomeAccessibilityService : AccessibilityService() {

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
        Log.d(TAG, "Service connected")
    }

    override fun onInterrupt() {
        instance = null
        Log.d(TAG, "Service interrupted")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed for global actions
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}