package com.fruko.usagestatsdisplayer.presentation.home

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fruko.usagestatsdisplayer.domain.model.SortOption
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame
import com.fruko.usagestatsdisplayer.domain.model.UsageStatInfo
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasUsageStatsPermission(context)) {
                    viewModel.onEvent(HomeEvent.PermissionGranted)
                    viewModel.onEvent(HomeEvent.RefreshRequested)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!state.hasPermission) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "We need Usage Access permissions to display app statistics.",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }) {
                Text("Grant Permission")
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onEvent(HomeEvent.SearchQueryChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search Apps") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TimeFrame.values().forEach { timeFrame ->
                ElevatedFilterChip(
                    selected = state.timeFrame == timeFrame,
                    onClick = { viewModel.onEvent(HomeEvent.TimeFrameChanged(timeFrame)) },
                    label = { Text(timeFrame.name) }
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Sort by: ")
            TextButton(onClick = { 
                viewModel.onEvent(HomeEvent.SortOptionChanged(SortOption.toggle(state.sortOption))) 
            }) {
                Text(state.sortOption.name)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.usageStats, key = { it.packageName }) { info ->
                    UsageStatItem(info = info, timeFrame = state.timeFrame)
                }
            }
        }
    }
}

@Composable
fun UsageStatItem(info: UsageStatInfo, timeFrame: TimeFrame) {
    val context = LocalContext.current
    val icon = remember(info.packageName) {
        try {
            context.packageManager.getApplicationIcon(info.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(icon)
                        .crossfade(true)
                        .build(),
                    contentDescription = info.appName,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(modifier = Modifier.size(48.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(text = info.appName, style = MaterialTheme.typography.titleMedium)
                Text(text = "Total Usage: ${formatTime(info.totalTimeInForeground)}", style = MaterialTheme.typography.bodyMedium)
                if (timeFrame != TimeFrame.DAILY) {
                    Text(text = "Avg per Day: ${formatTime(info.averageTime)}", style = MaterialTheme.typography.bodySmall)
                    val date = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(info.maxUsageDayTimestamp))
                    Text(text = "Max Usage: ${formatTime(info.maxUsageInDay)} (on $date)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
