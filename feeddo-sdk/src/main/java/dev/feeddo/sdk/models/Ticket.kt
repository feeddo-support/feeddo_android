package dev.feeddo.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Model representing a support ticket in Feeddo
 */
data class Ticket(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("priority")
    val priority: String = "medium",

    @SerializedName("isResolved")
    val isResolved: Boolean = false,

    @SerializedName("createdAt")
    val createdAt: Long = 0L,

    @SerializedName("updatedAt")
    val updatedAt: Long = 0L,

    @SerializedName("isOwner")
    val isOwner: Boolean = false,

    @SerializedName("conversationId")
    val conversationId: String? = null
)

data class GetTicketsResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("tickets")
    val tickets: List<Ticket> = emptyList()
)

