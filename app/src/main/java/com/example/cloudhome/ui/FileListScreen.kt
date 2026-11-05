package com.example.cloudhome.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cloudhome.data.SmbFile
import com.example.cloudhome.data.SortOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    state: UiState,
    sortOrder: SortOrder,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onSort: (SortOrder) -> Unit,
    onOpenFolder: (SmbFile) -> Unit,
    onDownload: (SmbFile) -> Unit,
    onDelete: (SmbFile) -> Unit,
    onUpload: () -> Unit,
    onBack: () -> Unit,
    onDisconnect: () -> Unit,
) {
    var deleteTarget by remember { mutableStateOf<SmbFile?>(null) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (state as? UiState.Ready)?.path?.ifEmpty { "/" } ?: "CloudHome",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.name.lowercase().replaceFirstChar(Char::uppercase)) },
                                    onClick = { sortMenuOpen = false; onSort(order) },
                                    leadingIcon = {
                                        if (order == sortOrder) Icon(Icons.Default.Check, contentDescription = null)
                                    },
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { overflowOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Disconnect") },
                                onClick = { overflowOpen = false; onDisconnect() },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onUpload) {
                Icon(Icons.Default.Upload, contentDescription = "Upload file")
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is UiState.Ready -> {
                    if (s.files.isEmpty()) {
                        Text("This folder is empty", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(s.files, key = { it.path }) { file ->
                                FileRow(
                                    file = file,
                                    onClick = {
                                        if (file.isDirectory) onOpenFolder(file) else onDownload(file)
                                    },
                                    onLongClick = { deleteTarget = file },
                                )
                            }
                        }
                    }
                }
                UiState.NeedsConnect -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${target.name}?") },
            text = {
                Text(
                    if (target.isDirectory) "The folder and everything inside it will be permanently deleted."
                    else "This file will be permanently deleted."
                )
            },
            confirmButton = {
                TextButton(onClick = { deleteTarget = null; onDelete(target) }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(file: SmbFile, onClick: () -> Unit, onLongClick: () -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        headlineContent = {
            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            if (!file.isDirectory) Text("${formatSize(file.size)}  ·  ${formatDate(file.modified)}")
        },
        leadingContent = {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = "B"
    for (u in units) {
        if (value < 1024) break
        value /= 1024
        unit = u
    }
    return "%.1f %s".format(value, unit)
}

private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

private fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))
