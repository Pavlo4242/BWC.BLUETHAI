package com.bwc.bluethai.networking

import android.content.Context
import com.bwc.bluethai.data.local.LogDao
import com.bwc.bluethai.data.model.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException

class LoggingInterceptor(private val logDao: LogDao) : Interceptor {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val message = "--> ${request.method} ${request.url}\n${request.headers}"
        
        scope.launch {
            logDao.insert(
                LogEntry(
                    timestamp = System.currentTimeMillis(),
                    level = "NETWORK",
                    tag = "OkHttp",
                    message = message
                )
            )
        }

        return chain.proceed(request)
    }
}