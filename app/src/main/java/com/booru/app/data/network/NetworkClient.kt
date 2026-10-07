package com.booru.app.data.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object NetworkClient {

    private val sharedConnectionPool = ConnectionPool(
        maxIdleConnections = 24,
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

suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            cont.resume(response) { response.close() }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }
    })
    cont.invokeOnCancellation {
        runCatching { cancel() }
    }
}
