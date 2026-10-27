package com.booru.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {

    @Insert
    suspend fun insert(entry: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY at DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<HistoryEntity>

    @Query("SELECT DISTINCT mediaKey FROM history WHERE kind = :kind AND at >= :since AND mediaKey != '' ORDER BY at DESC LIMIT :limit")
    suspend fun recentKeys(kind: String, since: Long, limit: Int): List<String>

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int

    @Query("DELETE FROM history WHERE at < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM history WHERE id IN (SELECT id FROM history ORDER BY at ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Query("DELETE FROM history")
    suspend fun clear()
}
