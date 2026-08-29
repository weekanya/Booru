package com.booru.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.booru.app.RemoteMedia

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val url: String,
    val id: String = "",
    val preview: String = "",
    val sample: String = "",
    val tags: String = "",
    val score: Int = 0,
    val source: String = "",
    val rating: String = "s",
    val width: Int = 0,
    val height: Int = 0,
    val createdAt: Long = 0L,
    val savedAt: Long = System.currentTimeMillis()
) {
    fun toRemoteMedia(): RemoteMedia {
        return RemoteMedia(
            id = id,
            url = url,
            preview = preview,
            sample = sample,
            tags = tags,
            score = score,
            source = source,
            rating = rating,
            width = width,
            height = height,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromRemoteMedia(media: RemoteMedia): FavoriteEntity {
            return FavoriteEntity(
                url = media.url,
                id = media.id,
                preview = media.preview,
                sample = media.sample,
                tags = media.tags,
                score = media.score,
                source = media.source,
                rating = media.rating,
                width = media.width,
                height = media.height,
                createdAt = media.createdAt
            )
        }
    }
}
