package com.example.overlay

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.tooling.preview.Preview
import com.example.overlay.ui.theme.OverlayTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startOverlayServiceWithDelay(5_000)
        setContentView(R.layout.activity_main)
        Log.d("Overlay", "MainActivity onCreate called")
        //check permission for overlay
        if (!Settings.canDrawOverlays(this)) {
            // send user to settings
            val overlPermIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivity(overlPermIntent)
        }

        enableEdgeToEdge()
        setContent {
            OverlayTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }


    private fun startOverlayService() {
        val overlPermIntent = Intent(this, OverlayService::class.java)
        startForegroundService(overlPermIntent)
    }
    private fun startOverlayServiceWithDelay(delay: Int = 0) {
        Log.d(TAG, "Starting overlay service with 5-second delay...")
        val intent = Intent(this, OverlayService::class.java)
        intent.putExtra("DELAY_MS", delay)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OverlayTheme {
        Greeting("Android")
    }
}