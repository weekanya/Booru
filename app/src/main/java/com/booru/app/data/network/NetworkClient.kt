package com.booru.app.data.network

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClient {

    private val sharedConnectionPool = ConnectionPool(
        maxIdleConnections = 8,
        keepAliveDuration = 5,
        timeUnit = TimeUnit.MINUTES
    )

    private val sharedDispatcher = Dispatcher().apply {
        maxRequests = 64
        maxRequestsPerHost = 12
    }

    val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(sharedConnectionPool)
            .dispatcher(sharedDispatcher)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }
}
