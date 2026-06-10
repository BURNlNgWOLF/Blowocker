package com.example.overlay.presentation

import android.content.Context
import android.util.Log


class MonitoredAppsManager(private val context: Context) {
    private val prefsName = "monitored_apps_prefs"
    private val appsKey = "saved_apps"

    private val cachedApps = mutableSetOf<String>()

    init {
        loadAppsFromPrefs()
    }

    private fun loadAppsFromPrefs() {
        try {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val saved = prefs.getStringSet(appsKey, emptySet()) ?: emptySet()
            cachedApps.clear()
            cachedApps.addAll(saved.filter { it.isNotBlank() })
            Log.d("MonitoredAppsMgr", "Loaded apps: $cachedApps")
        } catch (e: Exception) {
            Log.e("MonitoredAppsMgr", "Failed to load apps", e)
        }
    }

    private fun saveAppsToPrefs() {
        try {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            // Ensure immediate write
            prefs.edit().putStringSet(appsKey, cachedApps).commit()
        } catch (e: Exception) {
            Log.e("MonitoredAppsMgr", "Failed to save apps", e)
        }
    }

    fun getMonitoredApps(): List<String> = cachedApps.toList()

    fun addApp(packageName: String) {
        if (packageName.isNotBlank() && cachedApps.add(packageName)) {
            saveAppsToPrefs()
        }
    }

    fun removeApp(packageName: String) {
        if (cachedApps.remove(packageName)) {
            saveAppsToPrefs()
        }
    }
}
