package com.example.cloudhome.ui

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloudhome.data.ServerConfig
import com.example.cloudhome.data.SmbFile
import com.example.cloudhome.data.SmbRepository
import com.example.cloudhome.data.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class FileBrowserViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<UiState>(UiState.NeedsConnect)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    var sortOrder = SortOrder.NAME
        private set

    private var repo: SmbRepository? = null
    private var currentPath = ""
    private val navStack = ArrayDeque<String>()

    fun connect(config: ServerConfig) {
        viewModelScope.launch {
            _isConnecting.value = true
            runCatching {
                withContext(Dispatchers.IO) {
                    SmbRepository(config.host, config.username, config.password, config.share)
                }
            }.onSuccess { r ->
                repo = r
                currentPath = ""
                navStack.clear()
                sortOrder = SortOrder.NAME
                _connected.value = true
                refresh()
            }.onFailure { e ->
                _message.value = "Connection failed: ${e.message ?: e.javaClass.simpleName}"
            }
            _isConnecting.value = false
        }
    }

    fun disconnect() {
        viewModelScope.launch(Dispatchers.IO) { runCatching { repo?.close() } }
        repo = null
        _connected.value = false
        _uiState.value = UiState.NeedsConnect
    }

    /** Reloads the current folder in place; on failure the old list is kept. */
    fun refresh() {
        val r = repo ?: return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { r.list(currentPath, sortOrder) } }
                .onSuccess { files -> _uiState.value = UiState.Ready(currentPath, files, sortOrder) }
                .onFailure { e -> _message.value = "Failed to list folder: ${e.message ?: "unknown error"}" }
        }
    }

    fun setSortOrder(order: SortOrder) {
        sortOrder = order
        refresh()
    }

    fun openFolder(name: String) {
        navStack.addLast(currentPath)
        currentPath = if (currentPath.isEmpty()) name else "$currentPath/$name"
        refresh()
    }

    /** Pops one level. Returns false if already at the share root. */
    fun navigateUp(): Boolean {
        if (navStack.isEmpty()) return false
        currentPath = navStack.removeLast()
        refresh()
        return true
    }

    fun download(file: SmbFile) {
        val r = repo ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val dir = getApplication<Application>()
                        .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                    val target = File(dir, file.name)
                    target.outputStream().use { out -> r.download(file.path, out) }
                    target.absolutePath
                }
            }.onSuccess { path -> _message.value = "Downloaded: $path" }
                .onFailure { e -> _message.value = "Download failed: ${e.message ?: "unknown error"}" }
        }
    }

    fun upload(fileName: String, input: InputStream) {
        val r = repo ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val dest = if (currentPath.isEmpty()) fileName else "$currentPath/$fileName"
                    input.use { r.upload(dest, it) }
                }
            }.onSuccess { _message.value = "Uploaded $fileName" }
                .onFailure { e -> _message.value = "Upload failed: ${e.message ?: "unknown error"}" }
            refresh()
        }
    }

    fun delete(file: SmbFile) {
        val r = repo ?: return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { r.delete(file.path, file.isDirectory) } }
                .onSuccess { _message.value = "Deleted ${file.name}" }
                .onFailure { e -> _message.value = "Delete failed: ${e.message ?: "unknown error"}" }
            refresh()
        }
    }

    fun notify(msg: String) {
        _message.value = msg
    }

    fun clearMessage() {
        _message.value = null
    }
}
