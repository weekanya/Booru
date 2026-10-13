package com.booru.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {

    @Insert
    suspend fun insert(entry: HistoryEntity)

    @Query("SELECT * FROM history WHERE at >= :since ORDER BY at DESC")
    suspend fun since(since: Long): List<HistoryEntity>

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int

    @Query("DELETE FROM history WHERE at < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM history WHERE id IN (SELECT id FROM history ORDER BY at ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Query("DELETE FROM history")
    suspend fun clear()
}
