package com.fruko.usagestatsdisplayer.presentation.home

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fruko.usagestatsdisplayer.domain.model.SortOption
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame
import kotlinx.coroutines.delay

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
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
    val state by viewModel.state.collectAsStateWithLifecycle()
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
        LaunchedEffect(Unit) {
            while (true) {
                if (hasUsageStatsPermission(context)) {
                    viewModel.onEvent(HomeEvent.PermissionGranted)
                    viewModel.onEvent(HomeEvent.RefreshRequested)
                    break
                }
                delay(1000) // Poll permission every second
            }
        }

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
                val isOculus = android.os.Build.MANUFACTURER.contains("Oculus", ignoreCase = true) || 
                               android.os.Build.MANUFACTURER.contains("Meta", ignoreCase = true)
                
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                if (isOculus) {
                    intent.setComponent(android.content.ComponentName(
                        "com.android.settings", 
                        "com.android.settings.Settings\$UsageAccessSettingsActivity"
                    ))
                }
                
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to default behavior if explicit component targeting fails
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }) {
                Text("Grant Permission")
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        var query by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.onEvent(HomeEvent.SearchQueryChanged(it))
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search Apps") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TimeFrame.entries.forEach { timeFrame ->
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
            
            Box {
                var expanded by rememberSaveable { mutableStateOf(false) }
                
                TextButton(onClick = { expanded = true }) {
                    Text(state.sortOption.displayName)
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                viewModel.onEvent(HomeEvent.SortOptionChanged(option))
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.usageStats, key = { it.packageName }) { model ->
                    UsageStatItem(model = model)
                }
            }
        }
    }
}

@Composable
fun UsageStatItem(model: UsageStatUiModel) {
    val context = LocalContext.current
    val icon = remember(model.packageName) {
        try {
            context.packageManager.getApplicationIcon(model.packageName)
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
                    contentDescription = model.appName,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(modifier = Modifier.size(48.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(text = model.appName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = model.totalUsageText,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (model.averageUsageText != null) {
                    Text(
                        text = model.averageUsageText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (model.maxUsageText != null) {
                    Text(
                        text = model.maxUsageText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
