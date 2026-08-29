package com.booru.app.data.model

sealed interface GalleryError {
    data object Network : GalleryError
    data class Unauthorized(val source: String, val code: Int? = null) : GalleryError
    data class RateLimited(val source: String, val retryAfterSec: Int? = null) : GalleryError
    data class Server(val source: String, val code: Int) : GalleryError
    data class Generic(val message: String) : GalleryError
}
