package com.example.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat
import com.example.overlay.R
import android.view.Gravity
import android.view.ViewGroup

class OverlayService : Service() {

    companion object {
        private const val TAG = "Overlay" // ✅ Use your own tag
    }
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val channelId = "overlay_channel"
    private val notificationId = 1

    private val parentViewGroup: ViewGroup? = null
    private val DELAY_BEFORE_SHOW_MS: Long = 5_000  // Wait 5 seconds before showing overlay
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create and show notification (REQUIRED for foreground service)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Overlay Service")
            .setContentText("Overlay is active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        startForeground(notificationId, notification)

        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Delay complete - showing overlay now")  // Added
            showOverlay()
            }, DELAY_BEFORE_SHOW_MS)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Overlay Service Channel",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Channel for overlay service"
            enableLights(false)
            lightColor = Color.TRANSPARENT
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    private fun showOverlay() {
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.layout_overlay, null)

        val btnHome = overlayView?.findViewById<Button>(R.id.btnHome)



        btnHome?.setOnClickListener {
            Log.d(TAG, "Home button clicked")

            removeOverlay()

            Handler(Looper.getMainLooper()).postDelayed({
                if (HomeAccessibilityService.isAvailable()) {
                    Log.d(TAG, "Using AccessibilityService for Home navigation")
                    HomeAccessibilityService.goToHome()
                } else {
                    Log.d(TAG, "AccessibilityService unavailable, using Intent fallback")
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(homeIntent)
                }

                stopSelf()
                Log.d(TAG, "Service stopped")

            }, 200)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            this.windowManager.addView(this.overlayView, params)
            Log.d(TAG, "Overlay added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        Log.d(TAG, "removeOverlay() called")

        overlayView?.let { view ->
            try {
                if (view.parent != null) {
                    windowManager.removeViewImmediate(view)
                    Log.d(TAG, "View removed successfully from WindowManager")
                } else {
                    Log.d(TAG, "View was not attached to WindowManager")
                }
                overlayView = null
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay: ${e.message}", e)
            }
        } ?: run {
            Log.d(TAG, "overlayView was null - nothing to remove")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        Log.d(TAG, "OverlayService: onDestroy")
        }
}