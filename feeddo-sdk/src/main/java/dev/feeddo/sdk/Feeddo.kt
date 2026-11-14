package dev.feeddo.sdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import dev.feeddo.sdk.models.Conversation
import dev.feeddo.sdk.models.FeeddoPushProvider
import dev.feeddo.sdk.models.InitResult
import dev.feeddo.sdk.theme.FeeddoTheme
import dev.feeddo.sdk.ui.FeeddoActivity
import dev.feeddo.sdk.ui.FeeddoChatActivity
import dev.feeddo.sdk.ui.FeeddoCommunityActivity
import dev.feeddo.sdk.ui.FeeddoInAppNotificationManager
import dev.feeddo.sdk.utils.StorageHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

/**
 * Feeddo SDK - AI-powered customer support and feedback for Android
 */
object Feeddo {
    private const val TAG = "Feeddo"
    private var currentActivityRef: WeakReference<Activity>? = null
    private var isLifecycleRegistered = false
    private var startedActivityCount = 0

    val currentActivity: Activity?
        get() {
            val act = currentActivityRef?.get()
            return if (act != null && !act.isFinishing && !act.isDestroyed) act else null
        }

    val isAppInForeground: Boolean
        get() = currentActivity != null || startedActivityCount > 0

    val activeActivity: Activity?
        get() = currentActivity

    /**
     * Explicitly set the active activity for in-app overlay notifications.
     */
    fun setNotificationActivity(activity: Activity?) {
        currentActivityRef = activity?.let { WeakReference(it) }
        if (activity != null) {
            startedActivityCount = maxOf(1, startedActivityCount)
        }
    }

    /**
     * Initialize Feeddo SDK and create/update user
     *
     * Call this method at app startup or when user data changes.
     * Automatically handles user ID management, device info collection,
     * and in-app notifications.
     *
     * @return The persistent user ID
     */
    suspend fun init(
        context: Context,
        apiKey: String,
        isInAppNotificationOn: Boolean = true,
        theme: FeeddoTheme? = null,
        notificationDurationMs: Long = 10000L,
        externalUserId: String? = null,
        userName: String? = null,
        email: String? = null,
        userSegment: String? = null,
        subscriptionStatus: String? = null,
        customAttributes: Map<String, Any?>? = null,
        pushToken: String? = null,
        pushProvider: FeeddoPushProvider? = null,
    ): String = withContext(Dispatchers.Main) {
        val appContext = context.applicationContext

        // If context is an active Activity, immediately track it
        if (context is Activity && !context.isFinishing && !context.isDestroyed) {
            currentActivityRef = WeakReference(context)
            startedActivityCount = maxOf(1, startedActivityCount)
        }

        // Register Activity Lifecycle Callbacks to track top activity for in-app notifications
        if (!isLifecycleRegistered && appContext is Application) {
            appContext.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    currentActivityRef = WeakReference(activity)
                }
                override fun onActivityStarted(activity: Activity) {
                    startedActivityCount++
                    currentActivityRef = WeakReference(activity)
                }
                override fun onActivityResumed(activity: Activity) {
                    currentActivityRef = WeakReference(activity)
                }
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {
                    startedActivityCount = maxOf(0, startedActivityCount - 1)
                    if (currentActivityRef?.get() == activity && startedActivityCount == 0) {
                        currentActivityRef = null
                    }
                }
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    if (currentActivityRef?.get() == activity) {
                        currentActivityRef = null
                    }
                }
            })
            isLifecycleRegistered = true
        }

        val storage = StorageHelper.getInstance(appContext)

        // 1. Record 1st app open timestamp locally (sets once, never overwritten)
        storage.recordFirstAppOpenAt()

        // 2. Initialize internal singleton
        val internal = if (!FeeddoInternal.isInitialized) {
            FeeddoInternal.Builder(appContext, apiKey).build()
        } else {
            FeeddoInternal.instance
        }

        internal.isInAppNotificationOn = isInAppNotificationOn
        internal.notificationTheme = theme ?: FeeddoTheme.dark()
        internal.notificationDurationMs = notificationDurationMs

        // 3. Check if user identity or attributes changed
        val existingExternalId = storage.getExternalUserId()
        val existingUserName = storage.getUserName()
        val existingEmail = storage.getEmail()
        val existingPushToken = storage.getPendingPushToken()

        val hasChanges = (externalUserId != null && externalUserId != existingExternalId) ||
                (userName != null && userName != existingUserName) ||
                (email != null && email != existingEmail) ||
                (pushToken != null && pushToken != existingPushToken)

        if (hasChanges) {
            storage.setSyncedWithBackend(false)
        }

        // Save user data to local storage
        storage.saveUserData(
            externalUserId = externalUserId,
            userName = userName,
            email = email,
            userSegment = userSegment,
            subscriptionStatus = subscriptionStatus,
            customAttributes = customAttributes
        )

        if (pushToken != null) {
            storage.savePendingPushToken(pushToken, pushProvider?.value)
        }

        // 4. Retrieve existing user ID or generate persistent local user ID
        val userId = storage.getOrCreateLocalUserId()

        // Setup real-time notification listener if enabled
        if (isInAppNotificationOn) {
            internal.setupNotificationListener { currentActivity }
        }

        // Check for unread notifications if the user has an active conversation
        val hasActiveConv = storage.hasActiveConversation()
        if (hasActiveConv && isInAppNotificationOn) {
            try {
                withTimeoutOrNull(3000L) {
                    val unreadData = internal.apiService.getUnreadData(userId)
                    if (unreadData != null) {
                        storage.setHasActiveConversation(unreadData.hasActiveConversation)
                        unreadData.recentConversation?.let { conv ->
                            internal.conversationService.updateConversation(conv)
                            if (conv.unreadMessages > 0) {
                                currentActivity?.let { act ->
                                    FeeddoInAppNotificationManager.showNotification(
                                        activity = act,
                                        conversation = conv,
                                        theme = internal.notificationTheme,
                                        durationMs = internal.notificationDurationMs
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check unread notifications: ${e.message}")
            }
        }

        userId
    }

    /**
     * Open the Feeddo support home screen
     */
    fun show(
        context: Context,
        theme: FeeddoTheme? = null
    ) {
        if (!FeeddoInternal.isInitialized) {
            Log.e(TAG, "Feeddo not initialized. Call Feeddo.init() first.")
            return
        }

        FeeddoThemeHolder.customTheme = theme

        val intent = Intent(context, FeeddoActivity::class.java).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    }

    /**
     * Open the Community Board screen (Feature Requests & Bug Reports)
     */
    fun showCommunityBoard(
        context: Context,
        theme: FeeddoTheme? = null
    ) {
        if (!FeeddoInternal.isInitialized) {
            Log.e(TAG, "Feeddo not initialized. Call Feeddo.init() first.")
            return
        }

        FeeddoThemeHolder.customTheme = theme

        val intent = Intent(context, FeeddoCommunityActivity::class.java).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    }

    /**
     * Show an in-app banner notification
     */
    fun showInAppNotification(
        activity: Activity,
        title: String,
        message: String,
        data: Map<String, Any?>? = null,
        theme: FeeddoTheme? = null,
        durationMs: Long = 10000L
    ) {
        if (data != null && data["type"] == "chat_message") {
            val convId = data["conversationId"] as? String
            if (convId != null && FeeddoInternal.isInitialized) {
                if (FeeddoInternal.instance.conversationService.activeConversationId == convId) {
                    return
                }
            }
        }

        FeeddoInAppNotificationManager.showSimpleNotification(
            activity = activity,
            title = title,
            body = message,
            theme = theme ?: FeeddoThemeHolder.getTheme(),
            durationMs = durationMs,
            onTap = {
                if (data != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        handleNotificationTap(activity, data, theme)
                    }
                }
            }
        )
    }

    /**
     * Handle a foreground push notification to display an in-app banner and update the unread badge,
     * suppressing the native system notification.
     */
    fun handleForegroundNotification(
        data: Map<String, Any?>,
        activity: Activity? = null,
        title: String? = null,
        message: String? = null,
        theme: FeeddoTheme? = null
    ) {
        try {
            val conversationId = data["conversationId"] as? String
            if (conversationId != null && FeeddoInternal.isInitialized) {
                FeeddoInternal.instance.conversationService.incrementUnreadCount(conversationId)
            } else {
                val badgeStr = data["badge"]?.toString()
                val badge = badgeStr?.toIntOrNull()
                if (badge != null && FeeddoInternal.isInitialized) {
                    FeeddoInternal.instance.conversationService.setTotalUnreadCount(badge)
                }
            }

            val targetActivity = activity ?: currentActivity
            if (targetActivity != null && !targetActivity.isFinishing && !targetActivity.isDestroyed && FeeddoInternal.isInitialized && FeeddoInternal.instance.isInAppNotificationOn) {
                val notifTitle = title ?: (data["title"] as? String) ?: FeeddoInternal.instance.chatBotName
                val notifBody = message ?: (data["body"] as? String) ?: ""
                Log.d(TAG, "Showing foreground in-app notification: $notifTitle - $notifBody on activity: $targetActivity")

                showInAppNotification(
                    activity = targetActivity,
                    title = notifTitle,
                    message = notifBody,
                    data = data,
                    theme = theme ?: FeeddoInternal.instance.notificationTheme,
                    durationMs = FeeddoInternal.instance.notificationDurationMs
                )
            } else {
                Log.w(TAG, "Cannot show foreground in-app notification: targetActivity=$targetActivity, initialized=${FeeddoInternal.isInitialized}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling foreground notification: ${e.message}")
        }
    }

    /**
     * Handle notification tap action based on payload data
     */
    suspend fun handleNotificationTap(
        context: Context,
        data: Map<String, Any?>,
        theme: FeeddoTheme? = null
    ) = withContext(Dispatchers.Main) {
        if (!FeeddoInternal.isInitialized) return@withContext
        FeeddoInternal.instance.ensureUserInitialized()

        val type = data["type"] as? String
        val effectiveTheme = theme ?: FeeddoThemeHolder.getTheme()

        if (type == "task_released" || type == "task_status_changed" || type == "task_comment" || type == "task_created" || (type == null && data.containsKey("taskId"))) {
            val taskId = data["taskId"] as? String
            if (taskId != null) {
                FeeddoCommunityActivity.launchWithTask(context, taskId)
            }
        } else if (type == "chat_message" || (type == null && data.containsKey("conversationId"))) {
            val conversationId = data["conversationId"] as? String
            if (conversationId != null) {
                if (FeeddoInternal.instance.conversationService.activeConversationId == conversationId) {
                    Log.d(TAG, "Already on conversation $conversationId")
                    return@withContext
                }
                try {
                    val conversation = FeeddoInternal.instance.apiService.getConversation(conversationId)
                    FeeddoChatActivity.launch(context, conversation)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load conversation details, launching with conversationId: ${e.message}")
                    FeeddoChatActivity.launch(context, Conversation(id = conversationId))
                }
            }
        }
    }

    /**
     * Update user information (e.g., username, email, etc.)
     */
    suspend fun updateUser(
        externalUserId: String? = null,
        userName: String? = null,
        email: String? = null,
        userSegment: String? = null,
        subscriptionStatus: String? = null,
        customAttributes: Map<String, Any?>? = null,
        pushToken: String? = null,
        pushProvider: FeeddoPushProvider? = null,
    ): String {
        return FeeddoInternal.instance.updateUser(
            externalUserId = externalUserId,
            userName = userName,
            email = email,
            userSegment = userSegment,
            subscriptionStatus = subscriptionStatus,
            customAttributes = customAttributes,
            pushToken = pushToken,
            pushProvider = pushProvider
        )
    }

    /**
     * Register push token for the current user
     */
    suspend fun registerPushToken(
        pushToken: String,
        pushProvider: FeeddoPushProvider
    ) {
        FeeddoInternal.instance.registerPushToken(pushToken, pushProvider)
    }

    /**
     * Get current unread message count
     */
    val unreadMessageCount: Int
        get() = try {
            FeeddoInternal.instance.conversationService.unreadMessageCount.value
        } catch (_: Exception) {
            0
        }

    /**
     * Flow observing unread message count changes
     */
    val unreadMessageCountFlow: StateFlow<Int>?
        get() = try {
            FeeddoInternal.instance.conversationService.unreadMessageCount
        } catch (_: Exception) {
            null
        }

    /**
     * Clean up SDK resources
     */
    fun dispose() {
        FeeddoInternal.dispose()
    }
}

/**
 * Holder for theme configuration across activities
 */
object FeeddoThemeHolder {
    var customTheme: FeeddoTheme? = null

    fun getTheme(): FeeddoTheme = customTheme ?: FeeddoTheme.dark()
}

