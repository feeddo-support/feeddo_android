package dev.feeddo.sdk.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.feeddo.sdk.FeeddoInternal
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.TimeUnit

class WebSocketService(
    val baseUrl: String,
    val apiKey: String
) {
    companion object {
        private const val TAG = "FeeddoWebSocket"
        private const val MAX_RETRIES = 3
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var webSocket: WebSocket? = null
    private var userId: String? = null
    private var currentConversationId: String? = null

    @Volatile
    private var isConnected = false
    private var isDisposed = false
    private var retryCount = 0
    private var connectDeferred: CompletableDeferred<Boolean>? = null

    private val _messages = MutableSharedFlow<Map<String, Any?>>(extraBufferCapacity = 64)
    val messages: SharedFlow<Map<String, Any?>> = _messages.asSharedFlow()

    @Synchronized
    fun connect(
        userId: String,
        conversationId: String? = null,
        isRetry: Boolean = false
    ) {
        isDisposed = false
        this.userId = userId

        if (!isRetry) {
            retryCount = 0
        }

        if (isConnected && webSocket != null) {
            if (currentConversationId == conversationId) {
                connectDeferred = CompletableDeferred(true)
                return
            }
            Log.d(TAG, "Switching conversation from $currentConversationId to $conversationId")
            webSocket?.close(1000, "Switching conversation")
            webSocket = null
            isConnected = false
        }

        currentConversationId = conversationId
        val deferred = CompletableDeferred<Boolean>()
        connectDeferred = deferred

        try {
            val httpUrl = baseUrl.toHttpUrlOrNull()
                ?: throw IllegalArgumentException("Invalid base URL: $baseUrl")

            val urlBuilder = httpUrl.newBuilder()
                .addPathSegment("chat")
                .addPathSegment("ws")
                .addQueryParameter("apiKey", apiKey)
                .addQueryParameter("userId", userId)

            if (conversationId != null) {
                urlBuilder.addQueryParameter("conversationId", conversationId)
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .build()

            Log.d(TAG, "Connecting to ${request.url}")

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "Connected successfully")
                    isConnected = true
                    retryCount = 0
                    deferred.complete(true)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    retryCount = 0
                    try {
                        Log.d(TAG, "Received message: $text")
                        val type = object : TypeToken<Map<String, Any?>>() {}.type
                        val decoded: Map<String, Any?> = gson.fromJson(text, type)
                        scope.launch {
                            _messages.emit(decoded)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error decoding message: ${e.message}")
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "Closing: $code / $reason")
                    isConnected = false
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "Closed: $code / $reason")
                    isConnected = false
                    deferred.complete(false)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "Failure: ${t.message}")
                    isConnected = false
                    deferred.complete(false)
                    scheduleReconnect()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}")
            isConnected = false
            deferred.complete(false)
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (isDisposed || userId == null) return
        if (retryCount >= MAX_RETRIES) {
            Log.d(TAG, "Max retries ($MAX_RETRIES) reached. Giving up.")
            return
        }

        retryCount++
        Log.d(TAG, "Reconnecting (Attempt $retryCount/$MAX_RETRIES)...")

        scope.launch {
            delay(5000L)
            if (!isDisposed && !isConnected && userId != null) {
                connect(
                    userId = userId!!,
                    conversationId = currentConversationId,
                    isRetry = true
                )
            }
        }
    }

    suspend fun send(message: Map<String, Any?>): Boolean = withContext(Dispatchers.IO) {
        val uId = userId ?: try { FeeddoInternal.instance.cachedUserId } catch (_: Exception) { null }
        if (uId == null) {
            Log.w(TAG, "Cannot send message - userId not set")
            return@withContext false
        }

        if (!isConnected || webSocket == null) {
            Log.d(TAG, "Not connected, connecting before sending message...")
            connect(userId = uId, conversationId = currentConversationId)
            val connected = withTimeoutOrNull(6000L) {
                connectDeferred?.await()
            } ?: false

            if (!connected && !isConnected) {
                Log.w(TAG, "Failed to connect, cannot send message")
                return@withContext false
            }
        }

        val ws = webSocket
        if (ws != null) {
            val encoded = gson.toJson(message)
            Log.d(TAG, "WebSocket Sending: $encoded")
            val enqueued = ws.send(encoded)
            if (!enqueued) {
                Log.w(TAG, "WebSocket.send returned false, reconnecting and retrying...")
                isConnected = false
                connect(userId = uId, conversationId = currentConversationId)
                val reconnected = withTimeoutOrNull(6000L) {
                    connectDeferred?.await()
                } ?: false
                if (reconnected && webSocket != null) {
                    return@withContext webSocket!!.send(encoded)
                }
                return@withContext false
            }
            return@withContext true
        } else {
            Log.w(TAG, "WebSocket is null, cannot send message")
            return@withContext false
        }
    }

    fun disconnect() {
        isDisposed = true
        isConnected = false
        connectDeferred?.complete(false)
        connectDeferred = null
        webSocket?.close(1000, "Disconnect requested")
        webSocket = null
    }

    fun dispose() {
        disconnect()
        scope.cancel()
    }
}

