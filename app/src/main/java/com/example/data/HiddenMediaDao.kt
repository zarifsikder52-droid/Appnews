package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenMediaDao {
    @Query("SELECT * FROM hidden_media ORDER BY timestamp DESC")
    fun getAllMedia(): Flow<List<HiddenMedia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: HiddenMedia): Long

    @Delete
    suspend fun deleteMedia(media: HiddenMedia)

    @Query("DELETE FROM hidden_media WHERE id = :id")
    suspend fun deleteMediaById(id: Long)
}
