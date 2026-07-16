package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_media")
data class HiddenMedia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalName: String,
    val localFileName: String, // name of the file saved inside internal storage
    val fileType: String, // "IMAGE" or "VIDEO"
    val fileSize: Long,
    val duration: Long = 0L, // only for videos
    val timestamp: Long = System.currentTimeMillis()
)
