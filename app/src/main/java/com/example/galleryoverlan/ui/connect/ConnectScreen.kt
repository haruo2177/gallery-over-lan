package com.example.galleryoverlan.ui.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.galleryoverlan.ui.components.PasswordTextField
import com.example.galleryoverlan.ui.settings.DeviceDiscoveryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    onNavigateToBrowse: () -> Unit,
    viewModel: ConnectViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToBrowse.collect {
            onNavigateToBrowse()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("接続") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.pcName,
                    onValueChange = viewModel::onPcNameChange,
                    label = { Text("PC名") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = viewModel::startDiscovery) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "ネットワーク探索"
                    )
                }
            }

            OutlinedTextField(
                value = state.ipAddress,
                onValueChange = viewModel::onIpAddressChange,
                label = { Text("IPアドレス") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.userName,
                onValueChange = viewModel::onUserNameChange,
                label = { Text("ユーザー名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            PasswordTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "パスワード",
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier.fillMaxWidth(fraction = 5f / 6f),
                contentAlignment = Alignment.CenterEnd
            ) {
            Button(
                onClick = viewModel::connect,
                enabled = !state.isConnecting &&
                    (state.ipAddress.isNotBlank() || state.pcName.isNotBlank()) &&
                    state.userName.isNotBlank(),
                modifier = Modifier.width(96.dp)
            ) {
                Text("接続")
            }
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (state.showDiscoveryDialog) {
        DeviceDiscoveryDialog(
            scanState = state.discoveryState,
            onDeviceSelected = viewModel::onDeviceSelected,
            onDismiss = viewModel::cancelDiscovery,
            onRetry = viewModel::startDiscovery
        )
    }
}
