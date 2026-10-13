package com.booru.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.booru.app.RemoteMedia

@Entity(tableName = "history", indices = [Index(value = ["at"])])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kind: String,
    val mediaKey: String = "",
    val url: String = "",
    val preview: String = "",
    val tags: String = "",
    val source: String = "",
    val rating: String = "u",
    val score: Int = 0,
    val query: String = "",
    val at: Long = System.currentTimeMillis()
) {
    companion object {
        const val KIND_VIEW = "view"
        const val KIND_DOWNLOAD = "download"
        const val KIND_SEARCH = "search"

        fun fromMedia(kind: String, media: RemoteMedia) = HistoryEntity(
            kind = kind,
            mediaKey = media.mediaKey,
            url = media.url,
            preview = media.preview.ifBlank { media.sample },
            tags = media.tags,
            source = media.source,
            rating = media.rating,
            score = media.score
        )

        fun search(query: String) = HistoryEntity(kind = KIND_SEARCH, query = query)
    }
}
