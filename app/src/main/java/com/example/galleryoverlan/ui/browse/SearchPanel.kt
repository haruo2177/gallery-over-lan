package com.example.galleryoverlan.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.galleryoverlan.data.search.SearchHistoryEntry
import com.example.galleryoverlan.domain.model.SearchMode
import com.example.galleryoverlan.domain.model.SearchOptions

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchPanel(
    visible: Boolean,
    query: String,
    options: SearchOptions,
    history: List<SearchHistoryEntry>,
    onQueryChange: (String) -> Unit,
    onOptionsChange: (SearchOptions) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    onHistoryItemClick: (SearchHistoryEntry) -> Unit,
    onHistoryItemDelete: (String) -> Unit,
    onClearHistory: () -> Unit
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
            // Search input
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("検索キーワード") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "クリア")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Exclude keywords
            OutlinedTextField(
                value = options.excludeQuery,
                onValueChange = { onOptionsChange(options.copy(excludeQuery = it)) },
                placeholder = { Text("除外キーワード（NOT）") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Close, contentDescription = null)
                },
                trailingIcon = {
                    if (options.excludeQuery.isNotEmpty()) {
                        IconButton(onClick = { onOptionsChange(options.copy(excludeQuery = "")) }) {
                            Icon(Icons.Filled.Clear, contentDescription = "クリア")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // AND/OR toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SearchMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = options.mode == mode,
                        onClick = { onOptionsChange(options.copy(mode = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SearchMode.entries.size
                        )
                    ) {
                        Text(mode.label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Option chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = options.kanaUnify,
                    onClick = { onOptionsChange(options.copy(kanaUnify = !options.kanaUnify)) },
                    label = { Text("かな/カナ統一") }
                )
                FilterChip(
                    selected = !options.caseSensitive,
                    onClick = { onOptionsChange(options.copy(caseSensitive = !options.caseSensitive)) },
                    label = { Text("大小文字無視") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClear) {
                    Text("リセット")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onSearch) {
                    Text("検索")
                }
            }

            // Search history
            if (history.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "検索履歴",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClearHistory, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.DeleteSweep,
                            contentDescription = "履歴をすべて削除",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    history.forEach { entry ->
                        InputChip(
                            selected = false,
                            onClick = { onHistoryItemClick(entry) },
                            label = { Text(entry.query.ifEmpty { "(除外: ${entry.options.excludeQuery})" }) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onHistoryItemDelete(entry.query) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "削除",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
