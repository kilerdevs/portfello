package com.portfello.data.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpClientProvider @Inject constructor(
    private val networkLog: NetworkLog
) {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Portfello/0.1 Android")
                .build()
            val url = request.url.run {
                // ponytail: strip query params for brevity, keep host+path
                "${host}${encodedPath}"
            }
            try {
                val response = chain.proceed(request)
                val ms = response.receivedResponseAtMillis - response.sentRequestAtMillis
                if (response.isSuccessful) {
                    networkLog.log("OK ${response.code} $url (${ms}ms)")
                } else {
                    networkLog.log("ERR ${response.code} $url (${ms}ms)")
                }
                response
            } catch (e: Exception) {
                networkLog.log("FAIL $url — ${e.javaClass.simpleName}: ${e.message}")
                throw e
            }
        }
        .build()
}
