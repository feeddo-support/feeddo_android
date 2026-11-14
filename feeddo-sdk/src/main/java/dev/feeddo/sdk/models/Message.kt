package dev.feeddo.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Model representing a chat message in Feeddo
 */
data class Message(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("conversationId")
    val conversationId: String = "",

    @SerializedName("role")
    val role: String = "assistant", // 'user', 'assistant', 'human', 'function'

    @SerializedName("content")
    val content: String? = null,

    @SerializedName("hasAttachments")
    val hasAttachments: Boolean = false,

    @SerializedName("attachments")
    val attachments: Any? = null,

    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @SerializedName("displayName")
    val displayName: String? = null
) {
    val isUser: Boolean
        get() = role == "user"
}

data class GetMessagesResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("messages")
    val messages: List<Message> = emptyList()
)

