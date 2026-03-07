package com.fruko.usagestatsdisplayer.presentation.details

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fruko.usagestatsdisplayer.domain.model.TimeFrame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    onBackClick: () -> Unit,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Usage Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // ── App header ───────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.icon != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(state.icon).crossfade(true).build(),
                            contentDescription = state.appName,
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        Box(Modifier.size(64.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(state.appName, style = MaterialTheme.typography.headlineSmall)
                        if (state.averageUsageText.isNotEmpty()) {
                            Text(
                                text = "Average: ${state.averageUsageText}/day",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (state.totalUsageText.isNotEmpty()) {
                            Text(
                                text = "Total: ${state.totalUsageText}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        val peakDayText = state.peakDayText
                        if (peakDayText != null) {
                            Text(
                                text = peakDayText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── TimeFrame chips ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimeFrame.entries.forEach { tf ->
                        ElevatedFilterChip(
                            selected = state.timeFrame == tf,
                            onClick = { viewModel.onEvent(DetailsEvent.TimeFrameChanged(tf)) },
                            label = { Text(tf.name) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Per-day session groups ───────────────────────────────────────
            if (state.dailyUsages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No sessions more than 5 seconds recorded for this timeframe.")
                    }
                }
            } else {
                state.dailyUsages.forEach { dailyUsage ->
                    item(key = dailyUsage.dayLabel) {
                        Text(
                            text = dailyUsage.dayLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(
                        items = dailyUsage.sessions,
                        key = { "${dailyUsage.dayLabel}/${it.startTime}" }
                    ) { session ->
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = session.timeRange,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = session.duration,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
