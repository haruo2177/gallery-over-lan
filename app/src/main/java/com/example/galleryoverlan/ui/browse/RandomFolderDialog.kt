package com.example.galleryoverlan.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.galleryoverlan.domain.model.FolderItem
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomFolderDialog(
    visible: Boolean,
    folders: List<FolderItem>,
    count: Int,
    onCountChange: (Int) -> Unit,
    onReshuffle: () -> Unit,
    onFolderClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "ランダムフォルダ",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Count input + reshuffle
            var countText by remember { mutableStateOf(count.toString()) }
            LaunchedEffect(count) {
                if (countText.toIntOrNull() != count) {
                    countText = count.toString()
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = countText,
                    onValueChange = { text ->
                        val filtered = text.filter { it.isDigit() }
                        countText = filtered
                        val num = filtered.toIntOrNull()
                        if (num != null) {
                            onCountChange(num)
                        }
                    },
                    label = { Text("表示件数") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(120.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onReshuffle) {
                    Text("再抽選")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()

            // Folder list
            if (folders.isEmpty()) {
                Text(
                    text = "フォルダがありません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                folders.forEach { folder ->
                    val twoLineHeight = with(LocalDensity.current) {
                        MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2
                    }
                    ListItem(
                        headlineContent = {
                            Box(
                                modifier = Modifier.heightIn(min = twoLineHeight),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = folder.name,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        supportingContent = folder.imageCount?.let {
                            { Text("${it}枚") }
                        },
                        leadingContent = {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable { onFolderClick(folder.path) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
