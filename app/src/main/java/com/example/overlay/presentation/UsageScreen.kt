package com.example.overlay.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overlay.data.database.AppDatabase

/**
 * Visualiser that shows how much time was spent in each monitored app.
 * It reads the usage map from [AppLaunchMonitor] and displays a label and a
 * horizontal progress bar for each package. The progress values are scaled so
 * that the sum of all bars represents a full bar (i.e. 100%).
 */
@Composable
fun UsageScreen(
    modifier: Modifier = Modifier,
    appLaunchMonitor: AppLaunchMonitor
) {
    // Retrieve usage records directly from the Room database for persistence safety.
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val usageRecords = db.usageDao().getAll()

    // Convert list to a map of package -> seconds for easier calculations.
    val usageMap = usageRecords.associate { it.packageName to it.timeSeconds }
    val totalTime = usageMap.values.sum().coerceAtLeast(1L)

    // Sort apps by usage descending for clearer visual hierarchy
    val sortedUsage = usageMap.entries.sortedByDescending { it.value }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "App Usage", fontSize = 20.sp)
        sortedUsage.forEach { (packageName, seconds) ->
            val proportion = seconds.toFloat() / totalTime.toFloat()
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(text = "$packageName: ${seconds}s", fontSize = 14.sp)
                LinearProgressIndicator(
                    progress = proportion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp) // thicker bar
                        .padding(top = 4.dp)
                )
            }
        }
    }
}
