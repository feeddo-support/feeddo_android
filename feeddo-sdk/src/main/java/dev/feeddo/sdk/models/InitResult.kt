package dev.feeddo.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Result from Feeddo initialization
 */
data class InitResult(
    val userId: String,
    val recentConversation: Conversation? = null
) {
    val hasUnreadMessages: Boolean
        get() = recentConversation != null && recentConversation.unreadMessages > 0
}

/**
 * Response for lightweight unread check
 */
data class UnreadData(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("hasActiveConversation")
    val hasActiveConversation: Boolean = false,

    @SerializedName("unreadMessageCount")
    val unreadMessageCount: Int = 0,

    @SerializedName("recentConversation")
    val recentConversation: Conversation? = null
)

