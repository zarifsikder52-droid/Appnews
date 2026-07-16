package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class HiddenMediaRepository(
    private val context: Context,
    private val dao: HiddenMediaDao
) {
    val allMedia: Flow<List<HiddenMedia>> = dao.getAllMedia()

    suspend fun deleteMedia(media: HiddenMedia) = withContext(Dispatchers.IO) {
        // First delete the physical file
        val file = File(context.filesDir, "vault_media/${media.localFileName}")
        if (file.exists()) {
            file.delete()
        }
        dao.deleteMedia(media)
    }

    suspend fun hideMedia(uri: Uri, mediaType: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val (originalName, size) = getFileNameAndSize(uri)
            val uniqueName = "hidden_${System.currentTimeMillis()}_$originalName"
            
            val vaultDir = File(context.filesDir, "vault_media")
            if (!vaultDir.exists()) {
                vaultDir.mkdirs()
            }
            
            val destFile = File(vaultDir, uniqueName)
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (destFile.exists()) {
                val entity = HiddenMedia(
                    originalName = originalName,
                    localFileName = uniqueName,
                    fileType = mediaType,
                    fileSize = size,
                    timestamp = System.currentTimeMillis()
                )
                dao.insertMedia(entity)
                
                // Attempt to delete original file if possible (usually not possible directly in modern Android without user prompts,
                // but we try, and notify user that files are copied securely to the vault)
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    // Safe to ignore on newer Android versions because of scoped storage restriction
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun unhideMedia(media: HiddenMedia): Boolean = withContext(Dispatchers.IO) {
        val srcFile = File(context.filesDir, "vault_media/${media.localFileName}")
        if (!srcFile.exists()) return@withContext false

        val success = try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, media.originalName)
                if (media.fileType == "IMAGE") {
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CalculatorVault")
                    }
                } else {
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/CalculatorVault")
                    }
                }
            }

            val collectionUri = if (media.fileType == "IMAGE") {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val uri = context.contentResolver.insert(collectionUri, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    srcFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to Downloads directory
            try {
                val pubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!pubDir.exists()) {
                    pubDir.mkdirs()
                }
                val destFile = File(pubDir, media.originalName)
                srcFile.copyTo(destFile, overwrite = true)
                true
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }
        }

        if (success) {
            // Delete from database and local app storage
            if (srcFile.exists()) {
                srcFile.delete()
            }
            dao.deleteMedia(media)
        }
        success
    }

    private fun getFileNameAndSize(uri: Uri): Pair<String, Long> {
        var name = "file_${System.currentTimeMillis()}"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(name, size)
    }
}
