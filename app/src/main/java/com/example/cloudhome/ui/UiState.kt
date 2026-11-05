package com.example.cloudhome.ui

import com.example.cloudhome.data.SmbFile
import com.example.cloudhome.data.SortOrder

sealed interface UiState {
    data object NeedsConnect : UiState
    data class Ready(val path: String, val files: List<SmbFile>, val sortOrder: SortOrder) : UiState
}
