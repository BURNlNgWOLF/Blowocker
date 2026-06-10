package com.example.overlay.domain.repository

import kotlinx.coroutines.flow.Flow

data class AppInfo(val name: String, val packageName: String)

interface SystemRepository {
    fun isAccessibilityServiceAvailable(): Boolean
    fun navigateToHomeViaAccessibility(): Boolean
    fun navigateToHomeViaIntent()
    fun hasOverlayPermission(): Boolean
    fun startOverlayService(delayMs: Int)
    fun updateCurrentPackage(packageName: String)
    fun observeCurrentPackage(): Flow<String>
    fun getMonitoredPackages(): List<String>
    fun addMonitoredPackage(packageName: String)
    fun removeMonitoredPackage(packageName: String)
    fun isOverlayShowing(): Boolean
    fun setOverlayShowing(showing: Boolean)
    fun getInstalledApps(): List<AppInfo>
    fun getBlockedWifis(): List<String>
    fun addBlockedWifi(name: String)
    fun removeBlockedWifi(name: String)
    fun setWifiDetectionEnabled(enabled: Boolean)
    fun isWifiDetectionEnabled(): Boolean
}
