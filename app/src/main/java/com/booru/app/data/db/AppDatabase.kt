package com.booru.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FavoriteEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS favorites_new (
                        mediaKey TEXT NOT NULL PRIMARY KEY,
                        url TEXT NOT NULL,
                        id TEXT NOT NULL,
                        preview TEXT NOT NULL,
                        sample TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        rating TEXT NOT NULL,
                        width INTEGER NOT NULL,
                        height INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        savedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO favorites_new (mediaKey, url, id, preview, sample, tags, score, source, rating, width, height, createdAt, savedAt)
                    SELECT 
                        CASE WHEN length(source) > 0 AND length(id) > 0 THEN lower(source) || '_' || id ELSE url END,
                        url, id, preview, sample, tags, score, source, rating, width, height, createdAt, savedAt
                    FROM favorites
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE favorites")
                db.execSQL("ALTER TABLE favorites_new RENAME TO favorites")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "booru_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
