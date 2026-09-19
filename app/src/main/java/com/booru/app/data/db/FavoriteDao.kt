package com.booru.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    suspend fun getAllFavoritesList(): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaKey = :key OR url = :key)")
    fun isFavorite(key: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaKey = :key OR url = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM favorites WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("DELETE FROM favorites")
    suspend fun clearAll()
}
