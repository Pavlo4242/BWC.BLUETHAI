package com.bwc.bluethai.data.websocket

import android.content.Context
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class WebSocketInterceptor(
    private val context: Context,
    private val baseUrl: String
) : Interceptor {
    private val logger = WebSocketLogger(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (isWebSocketUpgrade(request)) {
            val listener = WebSocketLoggingListener(
                logger = logger,
                url = request.url.toString()
            )

            return chain.proceed(request.newBuilder()
                .header("Connection", "Upgrade")
                .header("Upgrade", "websocket")
                .eventListener(listener)
                .build())
        }

        return chain.proceed(request)
    }

    private fun isWebSocketUpgrade(request: Request): Boolean {
        return request.header("Connection")?.equals("Upgrade", ignoreCase = true) == true &&
                request.header("Upgrade")?.equals("websocket", ignoreCase = true) == true
    }
    inner class WebSocketLoggingListener(
        private val logger: WebSocketLogger,
        private val url: String
    ) : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            if (!WebSocketLogger.isAudioMessage(text)) {
                logger.logReceivedMessage(url, text)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            if (!WebSocketLogger.isAudioMessage(bytes)) {
                logger.logReceivedMessage(url, bytes.hex())
            }
        }

}

class WebSocketLoggingListener(
    private val logger: WebSocketLogger,
    private val url: String
) : WebSocketListener() {
    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        logger.logReceivedMessage(url, text, isAudioMessage(text))
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)
        val message = bytes.utf8()
        if (message != null) {
            logger.logReceivedMessage(url, message, isAudioMessage(message))
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        logger.logError(url, "Connection closed: $code - $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        logger.logError(url, "Failure: ${t.message}", response?.toString())
    }

    private fun isAudioMessage(message: String): Boolean {
        return message.contains("\"audio\"") ||
                message.contains("content-type: audio") ||
                message.contains("Content-Type: audio")
    }
}