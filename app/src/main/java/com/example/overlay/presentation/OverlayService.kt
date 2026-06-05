package com.example.overlay.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.overlay.data.repository.SystemRepositoryImpl
import com.example.overlay.domain.repository.SystemRepository
import com.example.overlay.domain.usecase.NavigateHomeUseCase
import com.example.overlay.presentation.theme.OverlayTheme

class OverlayService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val localViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = localViewModelStore

    companion object {
        private const val TAG = "Overlay"
    }
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val channelId = "overlay_channel"
    private val notificationId = 1

    private lateinit var repository: SystemRepository
    private lateinit var navigateHomeUseCase: NavigateHomeUseCase

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        // Manual Dependency Injection
        repository = SystemRepositoryImpl.getInstance(applicationContext)
        navigateHomeUseCase = NavigateHomeUseCase(repository)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Create and show notification (REQUIRED for foreground service)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Overlay Service")
            .setContentText("Overlay is active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        startForeground(notificationId, notification)

        val delay = intent?.getIntExtra("DELAY_MS", 0)?.toLong() ?: 0L
        
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Delay of $delay ms complete - showing overlay now")
            showOverlay()
        }, delay)

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
            lightColor = android.graphics.Color.TRANSPARENT
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    private fun showOverlay() {
        if (overlayView != null || repository.isOverlayShowing()) {
            Log.d(TAG, "Overlay already showing, ignoring request")
            return
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setContent {
                OverlayTheme {
                    OverlayContent(
                        onHomeClick = {
                            Log.d(TAG, "Home button clicked")
                            removeOverlay()
                            Handler(Looper.getMainLooper()).postDelayed({
                                navigateHomeUseCase()
                                stopSelf()
                                Log.d(TAG, "Service stopped")
                            }, 200)
                        }
                    )
                }
            }
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
            windowManager.addView(composeView, params)
            overlayView = composeView
            repository.setOverlayShowing(true)
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
                    repository.setOverlayShowing(false)
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
            repository.setOverlayShowing(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        Log.d(TAG, "OverlayService: onDestroy")
    }
}

@Composable
fun OverlayContent(onHomeClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x59006D00))
    ) {
        Text(
            text = "Overlay Active",
            color = Color.White,
            fontSize = 40.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.Red)
        )
        Button(
            onClick = onHomeClick,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(text = "Go to Home", fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OverlayContentPreview() {
    OverlayTheme {
        OverlayContent(onHomeClick = {})
    }
}
