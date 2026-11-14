package dev.feeddo.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Model representing a comment on a community board task
 */
data class TaskComment(
    @SerializedName("id")
    val id: String,

    @SerializedName("content")
    val content: String = "",

    @SerializedName("userType")
    val userType: String = "end_user",

    @SerializedName("authorName")
    val authorName: String? = null,

    @SerializedName("createdAt")
    val createdAt: Long = 0L,

    @SerializedName("hasAttachments")
    val hasAttachments: Boolean = false,

    @SerializedName("attachments")
    val attachments: List<Map<String, Any?>>? = null
)

