package dev.feeddo.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Model representing an end user in the Feeddo system
 */
data class EndUser(
    @SerializedName("userId")
    val userId: String? = null,

    @SerializedName("externalUserId")
    val externalUserId: String? = null,

    @SerializedName("userName")
    val userName: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("country")
    val country: String? = null,

    @SerializedName("locale")
    val locale: String? = null,

    @SerializedName("appVersion")
    val appVersion: String? = null,

    @SerializedName("platform")
    val platform: String? = "android",

    @SerializedName("deviceModel")
    val deviceModel: String? = null,

    @SerializedName("osVersion")
    val osVersion: String? = null,

    @SerializedName("userSegment")
    val userSegment: String? = null,

    @SerializedName("subscriptionStatus")
    val subscriptionStatus: String? = null,

    @SerializedName("customAttributes")
    val customAttributes: Map<String, Any?>? = null,

    @SerializedName("pushToken")
    val pushToken: String? = null,

    @SerializedName("pushProvider")
    val pushProvider: String? = null,

    @SerializedName("firstSeenAt")
    val firstSeenAt: Long? = null
)

data class UpsertEndUserResponse(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("action")
    val action: String? = null,

    @SerializedName("externalUserId")
    val externalUserId: String? = null,

    @SerializedName("recentConversation")
    val recentConversation: Map<String, Any?>? = null
)

