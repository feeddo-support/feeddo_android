package dev.feeddo.sdk

import android.app.Activity
import android.content.Context
import android.util.Log
import dev.feeddo.sdk.config.FeeddoConfig
import dev.feeddo.sdk.models.*
import dev.feeddo.sdk.network.ApiService
import dev.feeddo.sdk.network.FeeddoApiException
import dev.feeddo.sdk.network.WebSocketService
import dev.feeddo.sdk.service.ConversationService
import dev.feeddo.sdk.theme.FeeddoTheme
import dev.feeddo.sdk.ui.FeeddoInAppNotificationManager
import dev.feeddo.sdk.utils.DeviceInfoHelper
import dev.feeddo.sdk.utils.StorageHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FeeddoInternal private constructor(
    val context: Context,
    val apiKey: String,
    val apiUrl: String = FeeddoConfig.DEFAULT_API_URL
) {
    companion object {
        private const val TAG = "FeeddoInternal"

        @Volatile
        private var _instance: FeeddoInternal? = null

        val instance: FeeddoInternal
            get() = _instance ?: throw IllegalStateException(
                "Feeddo not initialized. Call Feeddo.init() first."
            )

        val isInitialized: Boolean
            get() = _instance != null

        fun dispose() {
            _instance?.let { inst ->
                inst.conversationService.onNewMessage = null
                inst.activityProvider = null
                inst.scope.cancel()
                inst.disconnectWebSocket()
                inst.webSocketService.dispose()
                inst.conversationService.dispose()
                FeeddoInAppNotificationManager.clearAll()
                _instance = null
            }
        }
    }

    val apiService: ApiService = ApiService(apiUrl = apiUrl, apiKey = apiKey)
    val webSocketService: WebSocketService = WebSocketService(baseUrl = apiUrl, apiKey = apiKey)
    val conversationService: ConversationService = ConversationService(apiService, webSocketService)
    val storageHelper: StorageHelper = StorageHelper.getInstance(context)

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val initMutex = Mutex()
    private var activityProvider: (() -> Activity?)? = null

    private val _notifications = MutableSharedFlow<Map<String, Any?>>(extraBufferCapacity = 64)
    val notifications: SharedFlow<Map<String, Any?>> = _notifications.asSharedFlow()

    var cachedUserId: String? = null
        private set

    var chatBotName: String = "Feeddo"
        private set

    var recentConversation: Conversation? = null
        private set

    var isInAppNotificationOn: Boolean = true
    var notificationTheme: FeeddoTheme = FeeddoTheme.dark()
    var notificationDurationMs: Long = 10000L

    init {
        scope.launch {
            webSocketService.messages.collect { data ->
                if (data["type"] == "notification") {
                    handleWebSocketNotification(data)
                }
            }
        }
    }

    fun setupNotificationListener(activityProvider: () -> Activity?) {
        this.activityProvider = activityProvider
        conversationService.onNewMessage = { conversation ->
            if (conversationService.activeConversationId != conversation.id && isInAppNotificationOn) {
                this.activityProvider?.invoke()?.let { activity ->
                    FeeddoInAppNotificationManager.showNotification(
                        activity = activity,
                        conversation = conversation,
                        theme = notificationTheme,
                        durationMs = notificationDurationMs
                    )
                }
            }
        }
    }

    private fun handleWebSocketNotification(data: Map<String, Any?>) {
        _notifications.tryEmit(data)
        if (!isInAppNotificationOn) return
        val activity = activityProvider?.invoke() ?: return

        val title = data["title"] as? String ?: "Notification"
        val body = data["body"] as? String ?: ""
        val rawData = data["data"] as? Map<*, *>
        val payloadData: Map<String, Any?>? = rawData?.entries?.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            key to v
        }?.toMap()

        Feeddo.showInAppNotification(
            activity = activity,
            title = title,
            message = body,
            data = payloadData,
            theme = notificationTheme,
            durationMs = notificationDurationMs
        )
    }

    suspend fun ensureUserInitialized(): String {
        return initMutex.withLock {
            val isSynced = storageHelper.isSyncedWithBackend()
            if (cachedUserId == null) {
                cachedUserId = storageHelper.getUserId()
            }

            if (isSynced && !cachedUserId.isNullOrEmpty()) {
                cachedUserId!!
            } else {
                upsertUser()
            }
        }
    }

    suspend fun upsertUser(
        externalUserId: String? = null,
        userName: String? = null,
        email: String? = null,
        userSegment: String? = null,
        subscriptionStatus: String? = null,
        customAttributes: Map<String, Any?>? = null,
        pushToken: String? = null,
        pushProvider: FeeddoPushProvider? = null
    ): String = withContext(Dispatchers.IO) {
        val effectiveExternalUserId = externalUserId ?: storageHelper.getExternalUserId()
        val effectiveUserName = userName ?: storageHelper.getUserName()
        val effectiveEmail = email ?: storageHelper.getEmail()
        val effectiveUserSegment = userSegment ?: storageHelper.getUserSegment()
        val effectiveSubscriptionStatus = subscriptionStatus ?: storageHelper.getSubscriptionStatus()
        val effectiveCustomAttributes = customAttributes ?: storageHelper.getCustomAttributes()

        var effectivePushToken = pushToken ?: storageHelper.getPendingPushToken()
        var effectivePushProvider = pushProvider?.value ?: storageHelper.getPendingPushProvider()

        val firstSeenAt = storageHelper.getFirstAppOpenAt()
        val deviceInfo = try {
            DeviceInfoHelper.getDeviceInfo(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to collect device info: ${e.message}")
            null
        }

        val endUser = EndUser(
            userId = cachedUserId ?: storageHelper.getUserId(),
            externalUserId = effectiveExternalUserId,
            userName = effectiveUserName,
            email = effectiveEmail,
            country = deviceInfo?.country,
            locale = deviceInfo?.locale,
            appVersion = deviceInfo?.appVersion,
            platform = deviceInfo?.platform ?: "android",
            deviceModel = deviceInfo?.deviceModel,
            osVersion = deviceInfo?.osVersion,
            userSegment = effectiveUserSegment,
            subscriptionStatus = effectiveSubscriptionStatus,
            customAttributes = effectiveCustomAttributes,
            pushToken = effectivePushToken,
            pushProvider = effectivePushProvider,
            firstSeenAt = firstSeenAt
        )

        try {
            val response = apiService.upsertEndUser(endUser)
            val returnedUserId = response.userId
            cachedUserId = returnedUserId

            if (response.recentConversation != null) {
                try {
                    val gson = com.google.gson.Gson()
                    val convJson = gson.toJson(response.recentConversation)
                    val conv = gson.fromJson(convJson, Conversation::class.java)
                    recentConversation = conv
                    conversationService.updateConversation(conv)
                    if (conv.status != "resolved") {
                        storageHelper.setHasActiveConversation(true)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse recent conversation: ${e.message}")
                }
            }

            storageHelper.saveUserId(returnedUserId)
            storageHelper.saveUserData(
                externalUserId = effectiveExternalUserId ?: response.externalUserId,
                userName = effectiveUserName,
                email = effectiveEmail,
                userSegment = effectiveUserSegment,
                subscriptionStatus = effectiveSubscriptionStatus,
                customAttributes = effectiveCustomAttributes
            )
            storageHelper.setSyncedWithBackend(true)

            Log.d(TAG, "User ${response.action} - ID: $returnedUserId")
            returnedUserId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize/upsert user: ${e.message}")
            throw e
        }
    }

    suspend fun updateUser(
        externalUserId: String? = null,
        userName: String? = null,
        email: String? = null,
        userSegment: String? = null,
        subscriptionStatus: String? = null,
        customAttributes: Map<String, Any?>? = null,
        pushToken: String? = null,
        pushProvider: FeeddoPushProvider? = null
    ): String {
        storageHelper.saveUserData(
            externalUserId = externalUserId,
            userName = userName,
            email = email,
            userSegment = userSegment,
            subscriptionStatus = subscriptionStatus,
            customAttributes = customAttributes
        )
        if (pushToken != null) {
            storageHelper.savePendingPushToken(pushToken, pushProvider?.value)
        }

        val isSynced = storageHelper.isSyncedWithBackend()
        return if (isSynced) {
            upsertUser(
                externalUserId = externalUserId,
                userName = userName,
                email = email,
                userSegment = userSegment,
                subscriptionStatus = subscriptionStatus,
                customAttributes = customAttributes,
                pushToken = pushToken,
                pushProvider = pushProvider
            )
        } else {
            cachedUserId ?: storageHelper.getOrCreateLocalUserId().also { cachedUserId = it }
        }
    }

    suspend fun registerPushToken(
        pushToken: String,
        pushProvider: FeeddoPushProvider
    ) {
        storageHelper.savePendingPushToken(pushToken, pushProvider.value)

        val isSynced = storageHelper.isSyncedWithBackend()
        if (isSynced) {
            try {
                upsertUser(pushToken = pushToken, pushProvider = pushProvider)
                Log.d(TAG, "Push token registered successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register push token: ${e.message}")
            }
        } else {
            Log.d(TAG, "Push token saved locally, will sync when UI opens")
        }
    }

    suspend fun getHomeData(): HomeData {
        ensureUserInitialized()
        val uid = cachedUserId ?: throw FeeddoApiException("User not identified")
        val homeData = apiService.getHomeData(uid)

        homeData.chatbotName?.let { chatBotName = it }

        if (homeData.recentConversation != null && homeData.recentConversation.status != "resolved") {
            storageHelper.setHasActiveConversation(true)
        } else {
            storageHelper.setHasActiveConversation(false)
        }

        return homeData
    }

    fun connectWebSocket(conversationId: String? = null) {
        scope.launch {
            try {
                ensureUserInitialized()
                val uid = cachedUserId
                if (uid != null) {
                    webSocketService.connect(userId = uid, conversationId = conversationId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to WebSocket: ${e.message}")
            }
        }
    }

    fun disconnectWebSocket() {
        webSocketService.disconnect()
    }

    internal class Builder(private val context: Context, private val apiKey: String) {
        fun build(): FeeddoInternal {
            val inst = FeeddoInternal(context.applicationContext, apiKey)
            _instance = inst
            return inst
        }
    }
}

