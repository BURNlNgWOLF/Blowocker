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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.example.overlay.presentation.theme.onPrimaryContainerLight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Arrangement
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
import com.example.overlay.presentation.MonitoredAppsManager
import com.example.overlay.presentation.UsageScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.emptyList
import kotlin.collections.filter

class MainActivity : ComponentActivity() {
    companion object {
        const val LOCATION_PERMISSION_REQUEST = 1001
    }

    private lateinit var startOverlayUseCase: StartOverlayUseCase
    private lateinit var appLaunchMonitor: AppLaunchMonitor
    private lateinit var wifiMonitor: WifiMonitor
    private lateinit var appsManager: MonitoredAppsManager

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
        wifiMonitor = WifiMonitor(this)
        appsManager = MonitoredAppsManager(this)

                // Use Case handles the check and start logic
                if (!repository.hasOverlayPermission()) {
                    val overlPermIntent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:$packageName".toUri()
                    )
                    startActivity(overlPermIntent)
                }

                // Start monitoring for all SSIDs added via the UI
                wifiMonitor.startMonitoring(wifiMonitor.getTargetSsids()) {
                    // No‑op callback – actual blocking is handled in AppLaunchMonitor
                }

                enableEdgeToEdge()
                setContent {
                    OverlayTheme {
                        MainScaffold(
                            repository = repository,
                            appLaunchMonitor = appLaunchMonitor,
                            appsManager = appsManager
                        )
                    }
                }
    }

//    override fun onStart() {
//        super.onStart()
//        // Refresh the SSID list from SharedPreferences each time the activity starts
//        if (::wifiMonitor.isInitialized) {
//            wifiMonitor.refreshSsids()
//        }
//    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    repository: com.example.overlay.domain.repository.SystemRepository,
    appLaunchMonitor: AppLaunchMonitor,
    appsManager: MonitoredAppsManager
) {
    var currentScreen by remember { mutableStateOf(Screen.Selection) }

    // Load persisted monitored apps
    val persistedApps = remember { appsManager.getMonitoredApps() }

    // Initialize the state as empty (or with whatever is currently in the repo)
    val monitoredPackagesState = remember { mutableStateOf(repository.getMonitoredPackages()) }

    // FIX: Sync repository AND explicitly update the Compose state inside the launch block
    LaunchedEffect(Unit) {
        // 1. Put the file items into your repository database/memory
        persistedApps.forEach { repository.addMonitoredPackage(it) }

        // 2. CRITICAL FIX: Update the Compose state so the screens refresh on launch!
        monitoredPackagesState.value = repository.getMonitoredPackages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { androidx.compose.material3.Text(text = "Overlay Monitor") },
            )
        },
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
                    Icon(Icons.Default.Add, contentDescription = "Add Apps", tint = onPrimaryContainerLight)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { currentScreen = Screen.Monitored },
                        contentAlignment = Alignment.Center
                    ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Monitored Apps", tint = onPrimaryContainerLight)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { currentScreen = Screen.Settings },
                        contentAlignment = Alignment.Center
                    ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = com.example.overlay.presentation.theme.onPrimaryContainerLight)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { currentScreen = Screen.Usage },
                        contentAlignment = Alignment.Center
                    ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Usage", tint = com.example.overlay.presentation.theme.onPrimaryContainerLight)
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
                        appsManager.addApp(packageName)
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
                        appsManager.removeApp(packageName)
                        monitoredPackagesState.value = repository.getMonitoredPackages()
                        appLaunchMonitor.startMonitoring(repository.getMonitoredPackages())
                    }
                )
                Screen.WifiSettings -> {
                    // Wi‑Fi settings handled by activity; no composable needed
                }
                Screen.Settings -> {
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                Screen.Usage -> {
                    UsageScreen(
                        modifier = Modifier.padding(innerPadding),
                        appLaunchMonitor = appLaunchMonitor
                    )
                }
            }
    }
}


    enum class Screen {
        Selection, Monitored,
        WifiSettings,
        Settings,
        Usage
    }
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
){
    // Wifi mode
    var switchState by BlockingState.wifiBasedMode
    var textFieldValue by remember { mutableStateOf("") }

    // List of Wifi's
    val ssidList = remember { mutableStateListOf<String>() }
    var ssidListIsLoading by remember { mutableStateOf(true) }
    var wifiDetected by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val wifiMonitor = remember { WifiMonitor(context) }
    val blockAllState = remember { BlockingState.blockAll }

    LaunchedEffect(Unit) {
        wifiMonitor.refreshSsids()

        val savedSsids = wifiMonitor.getTargetSsids()
        ssidList.addAll(savedSsids)

        ssidListIsLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Block on wifi", modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = switchState,
                onCheckedChange = { isOn ->
                    // Update the global Wi‑Fi based mode flag
                    BlockingState.wifiBasedMode.value = isOn
                }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
        Text("Temporarily turn block off", modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(
                        checked = blockAllState.value,
                        onCheckedChange = { isOn ->
                            // Toggle global block‑all without affecting the Wi‑Fi block switch
                            blockAllState.value = isOn
                        }
                    )
        }

        // TextField for adding SSIDs
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it.replace("\n", "").replace("\r", "") },
            label = { androidx.compose.material3.Text("Enter SSID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Show status when Wi‑Fi detected and mode active DEBUG (or not)
        if (switchState && wifiDetected) {
           Text("Blocking apps – target Wi‑Fi detected", color = MaterialTheme.colorScheme.error)
        }

        // Button to add item to list of SSIDs
        androidx.compose.material3.Button(
            onClick = {
                if (textFieldValue.isNotBlank()) {
                    val cleaned = textFieldValue.replace("\n", "").replace("\r", "").trim()
                    if (cleaned.isNotEmpty()) {
                        wifiMonitor.addSsid(cleaned) // Saves to file/internal memory
                        ssidList.add(cleaned)        // Updates UI
                    }
                    textFieldValue = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add SSID")
        }

        if (ssidListIsLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            // Display added SSIDs as cards
            LazyColumn {
                items(ssidList) { ssid ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                ssid,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                wifiMonitor.removeSsid(ssid)
                                ssidList.remove(ssid)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove $ssid"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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
                onValueChange = { searchText = it.replace("\n", "").replace("\r", "") },
                label = { Text("Search") },
                singleLine = true,
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
) {
    val apps = remember { repository.getInstalledApps() }

    val monitoredApps = apps.filter { app ->
        app.packageName in monitoredPackages
    }

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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
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

