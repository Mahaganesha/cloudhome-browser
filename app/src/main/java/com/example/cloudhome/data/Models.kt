package com.example.cloudhome.data

/** A file or folder entry on the SMB share. [path] is relative to the share root ("" = root). */
data class SmbFile(
    val name: String,
    val path: String,
    val size: Long,
    val modified: Long, // epoch millis
    val isDirectory: Boolean,
)

enum class SortOrder { NAME, SIZE, DATE }

/** Connection details entered by the user. */
data class ServerConfig(
    val host: String,
    val username: String,
    val password: String,
    val share: String,
)
