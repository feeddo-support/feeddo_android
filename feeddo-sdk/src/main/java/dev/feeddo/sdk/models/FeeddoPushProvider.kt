package dev.feeddo.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Supported push notification providers
 */
enum class FeeddoPushProvider(val value: String) {
    @SerializedName("fcm")
    FCM("fcm"),

    @SerializedName("apns")
    APNS("apns"),

    @SerializedName("onesignal")
    ONESIGNAL("onesignal");

    companion object {
        fun fromString(value: String?): FeeddoPushProvider? {
            if (value == null) return null
            return when (value.lowercase()) {
                "fcm" -> FCM
                "apns" -> APNS
                "onesignal" -> ONESIGNAL
                else -> null
            }
        }
    }
}

