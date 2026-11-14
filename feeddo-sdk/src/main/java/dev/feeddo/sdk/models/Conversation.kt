package dev.feeddo.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Model representing a conversation in Feeddo
 */
data class Conversation(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("appId")
    val appId: String? = null,

    @SerializedName("status")
    val status: String? = "active",

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("displayName")
    val displayName: String? = null,

    @SerializedName("lastMessagePreview")
    val lastMessagePreview: String? = null,

    @SerializedName("autoReply")
    val autoReply: Boolean = false,

    @SerializedName("startedAt")
    val startedAt: Long = 0L,

    @SerializedName("lastMessageAt")
    val lastMessageAt: Long = 0L,

    @SerializedName("resolvedAt")
    val resolvedAt: Long? = null,

    @SerializedName("resolvedBy")
    val resolvedBy: String? = null,

    @SerializedName("userSatisfaction")
    val userSatisfaction: Int? = null,

    @SerializedName("tags")
    val tags: List<String>? = null,

    @SerializedName("customData")
    val customData: Map<String, Any?>? = null,

    @SerializedName("unreadMessages")
    val unreadMessages: Int = 0,

    @SerializedName("hasTicket")
    val hasTicket: Boolean = false,

    @SerializedName("ticketStatus")
    val ticketStatus: String? = null,

    @SerializedName("hasTask")
    val hasTask: Boolean = false,

    @SerializedName("taskStatus")
    val taskStatus: String? = null
) {
    val safeTags: List<String>
        get() = tags ?: emptyList()

    val safeCustomData: Map<String, Any?>
        get() = customData ?: emptyMap()
}

data class GetConversationsResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("conversations")
    val conversations: List<Conversation> = emptyList()
)
