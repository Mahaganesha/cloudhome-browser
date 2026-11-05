package com.example.cloudhome

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cloudhome.data.PrefsManager
import com.example.cloudhome.data.SmbFile
import com.example.cloudhome.ui.ConnectScreen
import com.example.cloudhome.ui.FileBrowserViewModel
import com.example.cloudhome.ui.FileListScreen
import com.example.cloudhome.ui.UiState

class MainActivity : ComponentActivity() {

    private val viewModel: FileBrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                App(viewModel)
            }
        }
    }
}

@Composable
private fun App(viewModel: FileBrowserViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    val savedConfig = remember { prefs.load() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // File picker for uploads
    val uploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = displayName(context, uri) ?: "upload-${System.currentTimeMillis()}"
        val input = context.contentResolver.openInputStream(uri)
        if (input != null) {
            viewModel.upload(name, input)
        } else {
            viewModel.notify("Could not read the selected file")
        }
    }

    if (!connected || isConnecting) {
        ConnectScreen(
            saved = savedConfig,
            loading = isConnecting,
            error = message,
            onConnect = { config ->
                prefs.save(config)
                viewModel.connect(config)
            },
        )
    } else {
        // System back button pops one folder; at the root it exits the app.
        BackHandler {
            if (!viewModel.navigateUp()) {
                (context as? android.app.Activity)?.finish()
            }
        }
        FileListScreen(
            state = state,
            sortOrder = viewModel.sortOrder,
            snackbarHostState = snackbarHostState,
            onRefresh = viewModel::refresh,
            onSort = viewModel::setSortOrder,
            onOpenFolder = viewModel::openFolder,
            onDownload = viewModel::download,
            onDelete = viewModel::delete,
            onUpload = { uploadLauncher.launch("*/*") },
            onBack = {
                if (!viewModel.navigateUp()) {
                    (context as? android.app.Activity)?.finish()
                }
            },
            onDisconnect = viewModel::disconnect,
        )
    }
}

private fun displayName(context: android.content.Context, uri: Uri): String? =
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
