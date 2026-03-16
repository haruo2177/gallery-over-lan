package com.example.galleryoverlan.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.galleryoverlan.data.network.LanScanState
import com.example.galleryoverlan.domain.model.DiscoveredDevice

@Composable
fun DeviceDiscoveryDialog(
    scanState: LanScanState?,
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ネットワーク探索") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val isScanning = scanState == null || scanState is LanScanState.Scanning
                val progress = (scanState as? LanScanState.Scanning)?.progress ?: 0f
                val devices = when (scanState) {
                    null -> emptyList()
                    is LanScanState.Scanning -> scanState.devicesFound
                    is LanScanState.Completed -> scanState.devices
                    is LanScanState.Error -> emptyList()
                }.sortedBy { it.displayName.lowercase() }

                // Status text
                when {
                    scanState is LanScanState.Error -> {
                        Text(
                            text = scanState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    isScanning -> {
                        Text(
                            text = "SMBデバイスをスキャン中... (${(progress * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    devices.isEmpty() -> {
                        Text(
                            text = "SMBデバイスが見つかりませんでした",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {
                        Text(
                            text = "${devices.size}台のデバイスが見つかりました",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Progress bar - always reserve space to prevent jitter
                Spacer(modifier = Modifier.height(8.dp))
                if (isScanning) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Device list - always reserve space to prevent jitter
                if (devices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DeviceList(devices = devices, onDeviceSelected = onDeviceSelected)
                }
            }
        },
        confirmButton = {
            when (scanState) {
                is LanScanState.Completed, is LanScanState.Error -> {
                    TextButton(onClick = onRetry) {
                        Text("再スキャン")
                    }
                }
                else -> {}
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

@Composable
private fun DeviceList(
    devices: List<DiscoveredDevice>,
    onDeviceSelected: (DiscoveredDevice) -> Unit
) {
    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
        items(devices, key = { it.ipAddress }) { device ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeviceSelected(device) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (device.hostName != null) {
                        Text(
                            text = device.ipAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}
