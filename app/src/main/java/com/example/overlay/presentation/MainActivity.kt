package com.example.overlay.presentation

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.overlay.data.repository.SystemRepositoryImpl
import com.example.overlay.domain.usecase.StartOverlayUseCase
import com.example.overlay.presentation.theme.OverlayTheme

class MainActivity : ComponentActivity() {

    private lateinit var startOverlayUseCase: StartOverlayUseCase
    private lateinit var appLaunchMonitor: AppLaunchMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Overlay", "MainActivity onCreate called")

        // Manual Injection
        val repository = SystemRepositoryImpl.getInstance(applicationContext)
        startOverlayUseCase = StartOverlayUseCase(repository)
        appLaunchMonitor = AppLaunchMonitor(applicationContext)

        // Use Case handles the check and start logic
        if (!repository.hasOverlayPermission()) {
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
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        repository = repository,
                        onAppSelected = { packageName ->
                            repository.addMonitoredPackage(packageName)
                            appLaunchMonitor.startMonitoring(repository.getMonitoredPackages())
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    repository: com.example.overlay.domain.repository.SystemRepository,
    onAppSelected: (String) -> Unit
) {
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    val apps = remember { repository.getInstalledApps() }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Select app to monitor:",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (selectedPackage != null) {
                Text(
                    text = "Monitoring: $selectedPackage",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            LazyColumn {
                items(apps) { app ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedPackage = app.packageName
                                onAppSelected(app.packageName)
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = app.name, fontSize = 18.sp)
                            Text(text = app.packageName, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

