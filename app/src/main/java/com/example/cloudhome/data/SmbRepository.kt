package com.example.cloudhome.data

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import java.io.InputStream
import java.io.OutputStream
import java.util.EnumSet

/**
 * Thin wrapper around SMBJ for one SMB session against the WD My Cloud Home.
 * All methods are blocking — call them from Dispatchers.IO.
 */
class SmbRepository(
    host: String,
    username: String,
    password: String,
    private val shareName: String,
) {
    private val client = SMBClient()
    private val session: Session = client.connect(host).authenticate(
        AuthenticationContext(username, password.toCharArray(), "")
    )

    /** Lists [path] ("" = share root) sorted per [sortOrder]; folders always first. */
    fun list(path: String, sortOrder: SortOrder): List<SmbFile> = useShare { share ->
        val comparator: Comparator<SmbFile> =
            compareByDescending<SmbFile> { it.isDirectory }.then(
                when (sortOrder) {
                    SortOrder.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortOrder.SIZE -> compareByDescending { it.size }
                    SortOrder.DATE -> compareByDescending { it.modified }
                }
            )
        share.list(path)
            .filter { it.fileName != "." && it.fileName != ".." }
            .map { fi ->
                SmbFile(
                    name = fi.fileName,
                    path = if (path.isEmpty()) fi.fileName else "$path/${fi.fileName}",
                    size = fi.endOfFile,
                    modified = fi.lastWriteTime.toEpochMillis(),
                    isDirectory = fi.fileAttributes.contains(FileAttributes.FILE_ATTRIBUTE_DIRECTORY),
                )
            }
            .sortedWith(comparator)
    }

    /** Streams a remote file into [out]. */
    fun download(remotePath: String, out: OutputStream) = useShare { share ->
        share.openFile(
            remotePath,
            EnumSet.of(AccessMask.FILE_READ_DATA),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            null,
        ).use { file -> file.inputStream.use { it.copyTo(out) } }
    }

    /** Streams [input] to a remote file, creating it or overwriting if it exists. */
    fun upload(remotePath: String, input: InputStream) = useShare { share ->
        share.openFile(
            remotePath,
            EnumSet.of(AccessMask.FILE_WRITE_DATA),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OVERWRITE_IF,
            null,
        ).use { file -> file.outputStream.use { input.copyTo(it) } }
    }

    fun delete(remotePath: String, isDirectory: Boolean) = useShare { share ->
        if (isDirectory) share.rmdir(remotePath, true) else share.rm(remotePath)
    }

    fun close() {
        session.close()
    }

    private fun <T> useShare(block: (DiskShare) -> T): T {
        val share = session.connectShare(shareName) as DiskShare
        return share.use(block)
    }
}
