package com.example.overlay.data.repository

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.overlay.presentation.HomeAccessibilityService
import com.example.overlay.presentation.OverlayService
import com.example.overlay.domain.repository.AppInfo
import com.example.overlay.domain.repository.SystemRepository
// Database imports removed as the database has been deleted
// Coroutine imports retained for other uses
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SystemRepositoryImpl private constructor(private val context: Context) : SystemRepository {

    companion object {
        @Volatile
        private var INSTANCE: SystemRepositoryImpl? = null

        fun getInstance(context: Context): SystemRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SystemRepositoryImpl(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _currentPackage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private var isOverlayShowing = false
    private val monitoredPackages = mutableSetOf<String>()
    // Wi‑Fi blocking state (in‑memory for demo purposes)
    private val blockedWifis = mutableSetOf<String>()
    private var wifiDetectionEnabled = false

    // ---------- Usage tracking state (simplified, no database) ----------
    private var lastPackage: String? = null
    private var packageStartTime: Long = System.currentTimeMillis()

    override fun isAccessibilityServiceAvailable(): Boolean = HomeAccessibilityService.isAvailable()

    override fun navigateToHomeViaAccessibility(): Boolean = HomeAccessibilityService.goToHome()

    override fun navigateToHomeViaIntent() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(homeIntent)
    }

    override fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    override fun startOverlayService(delayMs: Int) {
        val intent = Intent(context, OverlayService::class.java).apply {
            putExtra("DELAY_MS", delayMs)
        }
        context.startForegroundService(intent)
    }

    override fun updateCurrentPackage(packageName: String) {
        // Simple tracking without persistence
        lastPackage = packageName
        packageStartTime = System.currentTimeMillis()
        _currentPackage.tryEmit(packageName)
    }

    override fun observeCurrentPackage(): Flow<String> = _currentPackage.asSharedFlow()

    override fun getMonitoredPackages(): List<String> = monitoredPackages.toList()

    override fun addMonitoredPackage(packageName: String) {
        monitoredPackages.add(packageName)
    }

    override fun removeMonitoredPackage(packageName: String) {
        monitoredPackages.remove(packageName)
    }

    override fun isOverlayShowing(): Boolean = isOverlayShowing

    override fun setOverlayShowing(showing: Boolean) {
        isOverlayShowing = showing
    }

    override fun getInstalledApps(): List<AppInfo> {
        return context.packageManager.getInstalledApplications(0).map { app ->
            AppInfo(
                name = app.loadLabel(context.packageManager).toString(),
                packageName = app.packageName
            )
        }.sortedBy { it.name }
    }

    // ----- Wi‑Fi blocking helpers -----
    override fun getBlockedWifis(): List<String> = blockedWifis.toList()

    override fun addBlockedWifi(name: String) {
        blockedWifis.add(name)
    }

    override fun removeBlockedWifi(name: String) {
        blockedWifis.remove(name)
    }

    override fun setWifiDetectionEnabled(enabled: Boolean) {
        wifiDetectionEnabled = enabled
    }

    override fun isWifiDetectionEnabled(): Boolean = wifiDetectionEnabled
}
