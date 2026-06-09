package com.example.overlay.presentation

import android.content.Intent
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.BottomAppBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedTextField
import androidx.core.net.toUri
import com.example.overlay.data.repository.SystemRepositoryImpl
import com.example.overlay.domain.usecase.StartOverlayUseCase
import com.example.overlay.presentation.theme.OverlayTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.focus.focusRequester

class MainActivity : ComponentActivity() {
    companion object {
        const val LOCATION_PERMISSION_REQUEST = 1001
    }

    private lateinit var startOverlayUseCase: StartOverlayUseCase
    private lateinit var appLaunchMonitor: AppLaunchMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request location permission for Wi‑Fi SSID access
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
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
                MainScaffold(repository = repository, appLaunchMonitor = appLaunchMonitor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    repository: com.example.overlay.domain.repository.SystemRepository,
    appLaunchMonitor: AppLaunchMonitor
)
{
    var currentScreen by remember { mutableStateOf(Screen.Selection) }

    val monitoredPackagesState = remember { mutableStateOf(repository.getMonitoredPackages()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { androidx.compose.material3.Text(text = "Overlay Monitor") },
            )
        },
        // Add a bottom navigation bar to switch between pages
        bottomBar = {
            androidx.compose.material3.BottomAppBar(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = androidx.compose.ui.graphics.Color.Unspecified
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
                ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { currentScreen = Screen.Selection },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Apps")
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { currentScreen = Screen.Monitored },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Monitored Apps")
                }
                }
            }
        }
    ) { innerPadding ->
            when (currentScreen) {
            Screen.Selection -> SelectionScreen(
                modifier = Modifier.padding(innerPadding),
                repository = repository,
                monitoredPackages = monitoredPackagesState.value,
                onAppSelected = { packageName ->
                    repository.addMonitoredPackage(packageName)
                    monitoredPackagesState.value = repository.getMonitoredPackages()
                    appLaunchMonitor.startMonitoring(repository.getMonitoredPackages())
                }
            )
            Screen.Monitored -> MonitoredAppsScreen(
                modifier = Modifier.padding(innerPadding),
                repository = repository,
                monitoredPackages = monitoredPackagesState.value,
                onAppRemoved = { packageName ->
                    repository.removeMonitoredPackage(packageName)
                    monitoredPackagesState.value = repository.getMonitoredPackages()
                    appLaunchMonitor.startMonitoring(repository.getMonitoredPackages())
                }
            )
        }
    }
}

enum class Screen {
    Selection, Monitored
}

@Composable
fun SelectionScreen(
    modifier: Modifier = Modifier,
    repository: com.example.overlay.domain.repository.SystemRepository,
    monitoredPackages: List<String>,
    onAppSelected: (String) -> Unit
)
{
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    val apps = remember { repository.getInstalledApps() }
    // Search text state
    var searchText by remember { mutableStateOf("") }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    // Filter apps based on search text and exclude monitored ones
    val filteredApps = apps.filter { app ->
        app.packageName !in monitoredPackages &&
        (searchText.isBlank() ||
            app.name.contains(searchText, ignoreCase = true) ||
            app.packageName.contains(searchText, ignoreCase = true))
    }

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

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { androidx.compose.material3.Text("Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .focusRequester(focusRequester)
            )
            LazyColumn {
                items(filteredApps) { app ->
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

@Composable
fun MonitoredAppsScreen(
    modifier: Modifier = Modifier,
    repository: com.example.overlay.domain.repository.SystemRepository,
    monitoredPackages: List<String>,
    onAppRemoved: (String) -> Unit
)
{
    // Use passed list instead of recomputing
    val monitoredPackages = monitoredPackages
    
    // Refresh the list when composition occurs
    val apps = remember { repository.getInstalledApps() }
    val monitoredApps = apps.filter { it.packageName in monitoredPackages }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monitored Apps (${monitoredApps.size}):",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (monitoredApps.isEmpty()) {
                Text(
                    text = "No apps are currently being monitored.",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                LazyColumn {
                    items(monitoredApps) { app ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = app.name, fontSize = 18.sp)
                                    Text(text = app.packageName, fontSize = 12.sp)
                                }
                                IconButton(onClick = { onAppRemoved(app.packageName) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove ${app.name}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
