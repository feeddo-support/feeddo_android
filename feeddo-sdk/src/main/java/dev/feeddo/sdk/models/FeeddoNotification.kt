package dev.feeddo.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Model representing an in-app notification in Feeddo
 */
data class FeeddoNotification(
    @SerializedName("id")
    val id: String,

    @SerializedName("app_id")
    val appId: String = "",

    @SerializedName("end_user_id")
    val endUserId: String = "",

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("body")
    val body: String? = null,

    @SerializedName("data")
    val data: Map<String, Any?>? = null,

    @SerializedName("is_read")
    val isRead: Boolean = false,

    @SerializedName("created_at")
    val createdAt: Long = 0L
)

data class GetNotificationsResponse(
    @SerializedName("notifications")
    val notifications: List<FeeddoNotification> = emptyList()
)

