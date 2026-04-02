package com.rk

import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.rk.libcommons.ubuntuHomeDir
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Ubuntu Document Provider for Android Storage Access Framework integration
 * Provides access to Ubuntu filesystem through Android's document picker
 */
class UbuntuDocumentProvider : DocumentsProvider() {

    companion object {
        private const val ROOT_ID = "ubuntu_root"
        private const val ROOT_DOCUMENT_ID = "ubuntu_home"

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
        )

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
        )
    }

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        val row = result.newRow()
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID)
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
        row.add(DocumentsContract.Root.COLUMN_SUMMARY, "Ubuntu Linux Environment")
        row.add(DocumentsContract.Root.COLUMN_FLAGS, 
            DocumentsContract.Root.FLAG_SUPPORTS_CREATE or
            DocumentsContract.Root.FLAG_SUPPORTS_SEARCH or
            DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD)
        row.add(DocumentsContract.Root.COLUMN_TITLE, "Ubuntu")
        row.add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
        row.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, ubuntuHomeDir().freeSpace)
        row.add(DocumentsContract.Root.COLUMN_ICON, com.rk.resources.R.drawable.terminal)
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        includeFile(result, documentId, null)
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocId(parentDocumentId)
        
        parent.listFiles()?.forEach { file ->
            includeFile(result, null, file)
        }
        
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val file = getFileForDocId(documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        
        val isWrite = mode.indexOf('w') != -1
        if (isWrite) {
            return ParcelFileDescriptor.open(file, accessMode)
        } else {
            return ParcelFileDescriptor.open(file, accessMode)
        }
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String
    ): String? {
        val parent = getFileForDocId(parentDocumentId)
        val file = File(parent, displayName)
        
        try {
            if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) {
                if (!file.mkdirs()) {
                    throw IOException("Failed to create directory")
                }
            } else {
                if (!file.createNewFile()) {
                    throw IOException("Failed to create file")
                }
            }
        } catch (e: IOException) {
            throw FileNotFoundException("Failed to create document with name $displayName and documentId $parentDocumentId")
        }
        
        return getDocIdForFile(file)
    }

    override fun deleteDocument(documentId: String) {
        val file = getFileForDocId(documentId)
        if (!file.delete()) {
            throw FileNotFoundException("Failed to delete document with id $documentId")
        }
    }

    override fun getDocumentType(documentId: String): String {
        val file = getFileForDocId(documentId)
        return getTypeForFile(file)
    }

    private fun getFileForDocId(docId: String): File {
        var target = ubuntuHomeDir()
        if (docId == ROOT_DOCUMENT_ID) {
            return target
        }
        
        val splitIndex = docId.indexOf(':', 1)
        if (splitIndex < 0) {
            throw FileNotFoundException("Missing root for $docId")
        } else {
            val path = docId.substring(splitIndex + 1)
            target = File(target, path)
        }
        
        if (!target.exists()) {
            throw FileNotFoundException("Missing file for $docId at $target")
        }
        
        return target
    }

    private fun getDocIdForFile(file: File): String {
        var path = file.absolutePath
        val rootPath = ubuntuHomeDir().absolutePath
        
        if (rootPath == path) {
            path = ""
        } else if (rootPath.endsWith("/")) {
            path = path.substring(rootPath.length)
        } else {
            path = path.substring(rootPath.length + 1)
        }
        
        return "ubuntu_root:$path"
    }

    private fun includeFile(result: MatrixCursor, docId: String?, file: File?) {
        var documentId = docId
        var targetFile = file
        
        if (documentId == null) {
            documentId = getDocIdForFile(targetFile!!)
        } else {
            targetFile = getFileForDocId(documentId)
        }
        
        var flags = 0
        if (targetFile.canWrite()) {
            if (targetFile.isDirectory) {
                flags = flags or DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
            } else {
                flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_WRITE
            }
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_DELETE
        }
        
        val displayName = if (documentId == ROOT_DOCUMENT_ID) "Ubuntu Home" else targetFile.name
        val mimeType = getTypeForFile(targetFile)
        
        if (mimeType.startsWith("image/")) {
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
        }
        
        val row = result.newRow()
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, documentId)
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName)
        row.add(DocumentsContract.Document.COLUMN_SIZE, targetFile.length())
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType)
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, targetFile.lastModified())
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags)
    }

    private fun getTypeForFile(file: File): String {
        return if (file.isDirectory) {
            DocumentsContract.Document.MIME_TYPE_DIR
        } else {
            getTypeForName(file.name)
        }
    }

    private fun getTypeForName(name: String): String {
        val lastDot = name.lastIndexOf('.')
        if (lastDot >= 0) {
            val extension = name.substring(lastDot + 1).lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime != null) {
                return mime
            }
        }
        return "application/octet-stream"
    }

    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point,
        signal: CancellationSignal?
    ): AssetFileDescriptor? {
        val file = getFileForDocId(documentId)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            .let { AssetFileDescriptor(it, 0, AssetFileDescriptor.UNKNOWN_LENGTH) }
    }
}
