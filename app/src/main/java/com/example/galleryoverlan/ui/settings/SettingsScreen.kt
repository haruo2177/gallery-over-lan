package com.example.galleryoverlan.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.galleryoverlan.ui.components.PasswordTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToImageList: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("接続設定") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = state.hostName,
                onValueChange = viewModel::onHostNameChange,
                label = { Text("PC名またはIP") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.shareName,
                onValueChange = viewModel::onShareNameChange,
                label = { Text("共有名") },
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

            OutlinedTextField(
                value = state.baseFolderPath,
                onValueChange = viewModel::onBaseFolderPathChange,
                label = { Text("ベースフォルダ (任意)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Test connection button
            OutlinedButton(
                onClick = viewModel::testConnection,
                enabled = !state.isTesting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("接続テスト")
            }

            // Test result
            state.testResult?.let { result ->
                when (result) {
                    is TestResult.Success -> {
                        Text(
                            text = "接続成功",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is TestResult.Failure -> {
                        Text(
                            text = result.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save and browse buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = viewModel::saveSettings,
                    enabled = !state.isSaving && state.hostName.isNotBlank() && state.shareName.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }

                Button(
                    onClick = onNavigateToImageList,
                    enabled = state.isSaved,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("画像を見る")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
