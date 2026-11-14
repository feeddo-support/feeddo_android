package dev.feeddo.sdk.service

import android.util.Log
import dev.feeddo.sdk.models.Conversation
import dev.feeddo.sdk.network.ApiService
import dev.feeddo.sdk.network.WebSocketService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class ConversationService(
    private val apiService: ApiService,
    private val webSocketService: WebSocketService
) {
    companion object {
        private const val TAG = "FeeddoConvService"
        private val JSON_CODE_BLOCK_REGEX = Pattern.compile("```json\\s*(\\{[\\s\\S]*?\\})\\s*```")
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var _activeConversationId: String? = null
    val activeConversationId: String?
        get() = _activeConversationId

    private val _unreadMessageCount = MutableStateFlow(0)
    val unreadMessageCount: StateFlow<Int> = _unreadMessageCount.asStateFlow()

    private val unreadCounts = ConcurrentHashMap<String, Int>()

    var onNewMessage: ((Conversation) -> Unit)? = null

    init {
        scope.launch {
            webSocketService.messages.collect { data ->
                handleWebSocketMessage(data)
            }
        }
    }

    fun setInitialUnreadCount(conversationId: String, count: Int) {
        unreadCounts[conversationId] = count
        updateTotalUnreadCount()
    }

    fun setActiveConversationId(id: String?) {
        _activeConversationId = id
        if (id != null) {
            markAsRead(id)
        }
    }

    suspend fun loadConversations(userId: String) = withContext(Dispatchers.Main) {
        _isLoading.value = true
        _error.value = null

        try {
            val convs = apiService.getConversations(userId)
            val sorted = convs.sortedByDescending { it.lastMessageAt }
            _conversations.value = sorted

            for (c in sorted) {
                unreadCounts[c.id] = c.unreadMessages
            }
            updateTotalUnreadCount()
        } catch (e: Exception) {
            _error.value = e.message
            Log.e(TAG, "Failed to load conversations: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun createConversation(
        userId: String,
        agentName: String = "support_agent"
    ): Conversation = withContext(Dispatchers.Main) {
        val conversation = apiService.createConversation(userId, agentName)
        val current = _conversations.value.toMutableList()
        current.add(0, conversation)
        _conversations.value = current
        conversation
    }

    private fun handleWebSocketMessage(data: Map<String, Any?>) {
        val role = data["role"] as? String
        if (role == "assistant" || role == "human" || role == "user") {
            val conversationId = data["conversationId"] as? String ?: return

            val content = data["content"] as? String
            val createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            val displayName = data["displayName"] as? String

            var preview = content
            if (content != null) {
                val matcher = JSON_CODE_BLOCK_REGEX.matcher(content)
                if (matcher.find()) {
                    preview = matcher.replaceAll("").trim()
                }
            }

            val currentList = _conversations.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == conversationId }

            if (index != -1) {
                var conv = currentList[index]
                var newDisplayName = conv.displayName
                var unreadCount = conv.unreadMessages

                if (role == "assistant" || role == "human") {
                    if (displayName != null) {
                        newDisplayName = displayName
                    }
                    if (_activeConversationId != conversationId) {
                        unreadCount++
                        unreadCounts[conversationId] = (unreadCounts[conversationId] ?: 0) + 1
                    }
                }

                conv = conv.copy(
                    lastMessagePreview = preview,
                    lastMessageAt = createdAt,
                    displayName = newDisplayName,
                    unreadMessages = unreadCount
                )

                currentList.removeAt(index)
                currentList.add(0, conv)
                _conversations.value = currentList
                updateTotalUnreadCount()

                if (_activeConversationId != conversationId && (role == "assistant" || role == "human")) {
                    onNewMessage?.invoke(conv)
                }
            } else {
                if (role == "assistant" || role == "human") {
                    val tempConv = Conversation(
                        id = conversationId,
                        appId = "",
                        status = "active",
                        autoReply = false,
                        startedAt = createdAt,
                        lastMessageAt = createdAt,
                        displayName = displayName ?: "Support",
                        lastMessagePreview = preview,
                        unreadMessages = 1
                    )

                    if (_activeConversationId != conversationId) {
                        unreadCounts[conversationId] = (unreadCounts[conversationId] ?: 0) + 1
                        updateTotalUnreadCount()
                        onNewMessage?.invoke(tempConv)
                    }
                }
            }
        }
    }

    fun markAsRead(conversationId: String) {
        unreadCounts[conversationId] = 0
        updateTotalUnreadCount()

        val currentList = _conversations.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == conversationId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(unreadMessages = 0)
            _conversations.value = currentList
        }
    }

    fun sendReadReceipt(conversationId: String) {
        scope.launch {
            webSocketService.send(
                mapOf(
                    "type" to "read_receipt",
                    "conversationId" to conversationId
                )
            )
            Log.d(TAG, "Sent read receipt for $conversationId")
            markAsRead(conversationId)
        }
    }

    suspend fun rateConversation(conversationId: String, rating: Int) = withContext(Dispatchers.Main) {
        apiService.rateConversation(conversationId, rating)

        val currentList = _conversations.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == conversationId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(userSatisfaction = rating)
            _conversations.value = currentList
        }
    }

    fun updateConversation(conversation: Conversation) {
        val currentList = _conversations.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == conversation.id }
        if (index != -1) {
            currentList[index] = conversation
        } else {
            currentList.add(conversation)
        }
        currentList.sortByDescending { it.lastMessageAt }
        _conversations.value = currentList

        unreadCounts[conversation.id] = conversation.unreadMessages
        updateTotalUnreadCount()
    }

    /**
     * Increment unread count for a conversation when a background or foreground notification arrives
     */
    fun incrementUnreadCount(conversationId: String) {
        unreadCounts[conversationId] = (unreadCounts[conversationId] ?: 0) + 1
        val currentList = _conversations.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == conversationId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(
                unreadMessages = unreadCounts[conversationId] ?: 1
            )
            _conversations.value = currentList
        }
        updateTotalUnreadCount()
    }

    /**
     * Directly set the total unread badge count from external push payload
     */
    fun setTotalUnreadCount(count: Int) {
        if (_unreadMessageCount.value != count) {
            _unreadMessageCount.value = count
            Log.d(TAG, "Total unread count set to $count")
        }
    }

    private fun updateTotalUnreadCount() {
        val total = unreadCounts.values.sum()
        if (_unreadMessageCount.value != total) {
            _unreadMessageCount.value = total
            Log.d(TAG, "Unread count updated to $total")
        }
    }

    fun dispose() {
        scope.cancel()
        unreadCounts.clear()
        _conversations.value = emptyList()
        _unreadMessageCount.value = 0
        onNewMessage = null
    }
}

