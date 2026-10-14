package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.DocumentType
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StoredFileInfo(
    val internalUriString: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val documentType: DocumentType
)

object FileUtils {

    fun copyUriToAppStorage(context: Context, sourceUri: Uri, isImage: Boolean): StoredFileInfo? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = if (isImage) "photo_${System.currentTimeMillis()}.jpg" else "document_${System.currentTimeMillis()}"
            var fileSize = 0L

            contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val mimeType = contentResolver.getType(sourceUri) ?: getMimeType(fileName)
            val subFolder = if (isImage) "pictures" else "documents"
            val targetDir = File(context.filesDir, subFolder).apply { if (!exists()) mkdirs() }

            // Ensure safe unique filename
            val targetFile = File(targetDir, "${System.currentTimeMillis()}_$fileName")
            contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (fileSize <= 0) {
                fileSize = targetFile.length()
            }

            val ext = fileName.substringAfterLast('.', "")
            val docType = if (isImage) DocumentType.IMAGE else DocumentType.fromExtension(ext)

            StoredFileInfo(
                internalUriString = targetFile.absolutePath,
                fileName = fileName,
                fileSizeBytes = fileSize,
                mimeType = mimeType,
                documentType = docType
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: when (extension) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    fun getShareableUri(context: Context, pathOrUri: String): Uri? {
        return try {
            if (pathOrUri.startsWith("content://")) {
                Uri.parse(pathOrUri)
            } else {
                val file = File(pathOrUri)
                if (file.exists()) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } else {
                    Uri.parse(pathOrUri)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Send picture or document to others via Android Share Sheet (like WhatsApp, Drive, Mail, etc.)
     */
    fun shareFileToOthers(context: Context, filePathOrUri: String?, mimeType: String?, caption: String? = null) {
        if (filePathOrUri.isNullOrEmpty()) {
            Toast.makeText(context, "No file to share", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val shareUri = getShareableUri(context, filePathOrUri) ?: Uri.parse(filePathOrUri)
            val effectiveMime = mimeType ?: "application/octet-stream"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = effectiveMime
                putExtra(Intent.EXTRA_STREAM, shareUri)
                if (!caption.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_TEXT, caption)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Send to Others like WhatsApp")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to share file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open picture or document in external viewer or reader app
     */
    fun openFile(context: Context, filePathOrUri: String?, mimeType: String?) {
        if (filePathOrUri.isNullOrEmpty()) {
            Toast.makeText(context, "File path is empty", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri = getShareableUri(context, filePathOrUri) ?: Uri.parse(filePathOrUri)
            val effectiveMime = mimeType ?: "*/*"

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, effectiveMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(viewIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateHeader(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val oneDay = 24 * 60 * 60 * 1000L
        return when {
            diff < oneDay -> "Today"
            diff < 2 * oneDay -> "Yesterday"
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun encodeFileToBase64(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists() || file.length() > 800 * 1024L) return null // Max 800KB for inline Firestore
            val bytes = file.readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveBase64ToAppStorage(context: Context, base64Str: String, fileName: String, isImage: Boolean): StoredFileInfo? {
        return try {
            val bytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
            val subFolder = if (isImage) "pictures" else "documents"
            val targetDir = File(context.filesDir, subFolder).apply { if (!exists()) mkdirs() }
            val targetFile = File(targetDir, "${System.currentTimeMillis()}_$fileName")
            FileOutputStream(targetFile).use { it.write(bytes) }

            val ext = fileName.substringAfterLast('.', "")
            val docType = if (isImage) DocumentType.IMAGE else DocumentType.fromExtension(ext)
            val mimeType = getMimeType(fileName)

            StoredFileInfo(
                internalUriString = targetFile.absolutePath,
                fileName = fileName,
                fileSizeBytes = targetFile.length(),
                mimeType = mimeType,
                documentType = docType
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
