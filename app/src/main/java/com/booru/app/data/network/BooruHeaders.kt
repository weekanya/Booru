package com.booru.app.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object BooruHeaders {
    const val BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    private val KNOWN_REFERERS = listOf(
        "gelbooru.com" to "https://gelbooru.com/",
        "rule34.xxx" to "https://rule34.xxx/",
        "realbooru.com" to "https://realbooru.com/",
        "tbib.org" to "https://tbib.org/",
        "safebooru.org" to "https://safebooru.org/",
        "yande.re" to "https://yande.re/",
        "konachan.com" to "https://konachan.com/",
        "konachan.net" to "https://konachan.net/"
    )

    fun refererFor(url: String): String {
        val parsed = url.toHttpUrlOrNull() ?: return ""
        val host = parsed.host.lowercase()
        KNOWN_REFERERS.firstOrNull { (domain, _) -> host == domain || host.endsWith(".$domain") }
            ?.let { return it.second }
        return "${parsed.scheme}://${parsed.host}/"
    }
}
