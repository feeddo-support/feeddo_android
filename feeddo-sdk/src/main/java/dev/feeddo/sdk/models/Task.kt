package dev.feeddo.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Model representing a community board task (Feature Request or Bug Report)
 */
data class Task(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("title")
    val title: String = "",

    @SerializedName("description")
    val description: String = "",

    @SerializedName("status")
    val status: String? = "todo",

    @SerializedName("priority")
    val priority: String? = "medium",

    @SerializedName("type")
    val type: String? = "feature",

    @SerializedName("upvoteCount")
    val upvoteCount: Int = 0,

    @SerializedName("commentCount")
    val commentCount: Int = 0,

    @SerializedName("myVote")
    val myVote: String? = null,

    @SerializedName("comments")
    val comments: List<TaskComment>? = null,

    @SerializedName("isOwner")
    val isOwner: Boolean = false,

    @SerializedName("conversationId")
    val conversationId: String? = null
) {
    val safeStatus: String
        get() = status ?: "todo"

    val safePriority: String
        get() = priority ?: "medium"

    val safeType: String
        get() = type ?: "feature"

    val safeComments: List<TaskComment>
        get() = comments ?: emptyList()
}

data class GetTasksResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("tasks")
    val tasks: List<Task> = emptyList()
)
