package dev.feeddo.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Model representing the aggregated home dashboard data for a user
 */
data class HomeData(
    @SerializedName("hasConversation")
    val hasConversation: Boolean = false,

    @SerializedName("hasTicket")
    val hasTicket: Boolean = false,

    @SerializedName("hasTasks")
    val hasTasks: Boolean = false,

    @SerializedName("unreadMessageCount")
    val unreadMessageCount: Int = 0,

    @SerializedName("unreadNotificationCount")
    val unreadNotificationCount: Int = 0,

    @SerializedName("chatbotName")
    val chatbotName: String? = null,

    @SerializedName("recentTicket")
    val recentTicket: Ticket? = null,

    @SerializedName("recentTask")
    val recentTask: Task? = null,

    @SerializedName("recentConversation")
    val recentConversation: Conversation? = null
)

