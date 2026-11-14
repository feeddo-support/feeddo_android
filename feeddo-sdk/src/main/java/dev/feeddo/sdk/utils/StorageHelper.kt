package dev.feeddo.sdk.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Helper class for managing local storage using Android SharedPreferences
 */
class StorageHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "dev.feeddo.sdk.preferences"
        private const val KEY_USER_ID = "feeddo_user_id"
        private const val KEY_EXTERNAL_USER_ID = "feeddo_external_user_id"
        private const val KEY_USER_NAME = "feeddo_user_name"
        private const val KEY_EMAIL = "feeddo_email"
        private const val KEY_USER_SEGMENT = "feeddo_user_segment"
        private const val KEY_SUBSCRIPTION_STATUS = "feeddo_subscription_status"
        private const val KEY_CUSTOM_ATTRIBUTES = "feeddo_custom_attributes"
        private const val KEY_FIRST_APP_OPEN_AT = "feeddo_first_app_open_at"
        private const val KEY_IS_SYNCED_WITH_BACKEND = "feeddo_is_synced_with_backend"
        private const val KEY_PENDING_PUSH_TOKEN = "feeddo_pending_push_token"
        private const val KEY_PENDING_PUSH_PROVIDER = "feeddo_pending_push_provider"
        private const val KEY_HAS_ACTIVE_CONVERSATION = "feeddo_has_active_conversation"

        @Volatile
        private var instance: StorageHelper? = null

        fun getInstance(context: Context): StorageHelper {
            return instance ?: synchronized(this) {
                instance ?: StorageHelper(context).also { instance = it }
            }
        }
    }

    /**
     * Get stored user ID or create and persist a new local UUID v4
     */
    fun getOrCreateLocalUserId(): String {
        val existingId = prefs.getString(KEY_USER_ID, null)
        if (!existingId.isNullOrEmpty()) {
            return existingId
        }
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_USER_ID, newId).apply()
        return newId
    }

    /**
     * Record 1st app open timestamp in local storage (only sets once)
     */
    fun recordFirstAppOpenAt(): Long {
        val existing = prefs.getLong(KEY_FIRST_APP_OPEN_AT, -1L)
        if (existing != -1L) {
            return existing
        }
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_FIRST_APP_OPEN_AT, now).apply()
        return now
    }

    fun getFirstAppOpenAt(): Long? {
        val value = prefs.getLong(KEY_FIRST_APP_OPEN_AT, -1L)
        return if (value != -1L) value else null
    }

    fun hasActiveConversation(): Boolean {
        return prefs.getBoolean(KEY_HAS_ACTIVE_CONVERSATION, false)
    }

    fun setHasActiveConversation(hasActive: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_ACTIVE_CONVERSATION, hasActive).apply()
    }

    fun isSyncedWithBackend(): Boolean {
        return prefs.getBoolean(KEY_IS_SYNCED_WITH_BACKEND, false)
    }

    fun setSyncedWithBackend(synced: Boolean) {
        prefs.edit().putBoolean(KEY_IS_SYNCED_WITH_BACKEND, synced).apply()
    }

    fun savePendingPushToken(pushToken: String?, pushProvider: String?) {
        val editor = prefs.edit()
        if (pushToken != null) {
            editor.putString(KEY_PENDING_PUSH_TOKEN, pushToken)
        } else {
            editor.remove(KEY_PENDING_PUSH_TOKEN)
        }
        if (pushProvider != null) {
            editor.putString(KEY_PENDING_PUSH_PROVIDER, pushProvider)
        } else {
            editor.remove(KEY_PENDING_PUSH_PROVIDER)
        }
        editor.apply()
    }

    fun getPendingPushToken(): String? = prefs.getString(KEY_PENDING_PUSH_TOKEN, null)

    fun getPendingPushProvider(): String? = prefs.getString(KEY_PENDING_PUSH_PROVIDER, null)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getExternalUserId(): String? = prefs.getString(KEY_EXTERNAL_USER_ID, null)

    fun saveExternalUserId(externalUserId: String?) {
        val editor = prefs.edit()
        if (externalUserId != null) {
            editor.putString(KEY_EXTERNAL_USER_ID, externalUserId)
        } else {
            editor.remove(KEY_EXTERNAL_USER_ID)
        }
        editor.apply()
    }

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    fun saveUserName(userName: String?) {
        val editor = prefs.edit()
        if (userName != null) {
            editor.putString(KEY_USER_NAME, userName)
        } else {
            editor.remove(KEY_USER_NAME)
        }
        editor.apply()
    }

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun saveEmail(email: String?) {
        val editor = prefs.edit()
        if (email != null) {
            editor.putString(KEY_EMAIL, email)
        } else {
            editor.remove(KEY_EMAIL)
        }
        editor.apply()
    }

    fun getUserSegment(): String? = prefs.getString(KEY_USER_SEGMENT, null)

    fun saveUserSegment(userSegment: String?) {
        val editor = prefs.edit()
        if (userSegment != null) {
            editor.putString(KEY_USER_SEGMENT, userSegment)
        } else {
            editor.remove(KEY_USER_SEGMENT)
        }
        editor.apply()
    }

    fun getSubscriptionStatus(): String? = prefs.getString(KEY_SUBSCRIPTION_STATUS, null)

    fun saveSubscriptionStatus(subscriptionStatus: String?) {
        val editor = prefs.edit()
        if (subscriptionStatus != null) {
            editor.putString(KEY_SUBSCRIPTION_STATUS, subscriptionStatus)
        } else {
            editor.remove(KEY_SUBSCRIPTION_STATUS)
        }
        editor.apply()
    }

    fun getCustomAttributes(): Map<String, Any?>? {
        val json = prefs.getString(KEY_CUSTOM_ATTRIBUTES, null) ?: return null
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            gson.fromJson(json, type)
        } catch (_: Exception) {
            null
        }
    }

    fun saveCustomAttributes(customAttributes: Map<String, Any?>?) {
        val editor = prefs.edit()
        if (customAttributes != null) {
            editor.putString(KEY_CUSTOM_ATTRIBUTES, gson.toJson(customAttributes))
        } else {
            editor.remove(KEY_CUSTOM_ATTRIBUTES)
        }
        editor.apply()
    }

    fun saveUserData(
        externalUserId: String? = null,
        userName: String? = null,
        email: String? = null,
        userSegment: String? = null,
        subscriptionStatus: String? = null,
        customAttributes: Map<String, Any?>? = null
    ) {
        val editor = prefs.edit()
        if (externalUserId != null) editor.putString(KEY_EXTERNAL_USER_ID, externalUserId)
        if (userName != null) editor.putString(KEY_USER_NAME, userName)
        if (email != null) editor.putString(KEY_EMAIL, email)
        if (userSegment != null) editor.putString(KEY_USER_SEGMENT, userSegment)
        if (subscriptionStatus != null) editor.putString(KEY_SUBSCRIPTION_STATUS, subscriptionStatus)
        if (customAttributes != null) {
            editor.putString(KEY_CUSTOM_ATTRIBUTES, gson.toJson(customAttributes))
        }
        editor.apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

