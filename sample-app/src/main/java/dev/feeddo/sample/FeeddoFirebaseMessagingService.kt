package dev.feeddo.sample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.feeddo.sdk.Feeddo
import dev.feeddo.sdk.models.FeeddoPushProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service to handle Firebase Cloud Messaging (FCM) tokens and incoming push notifications.
 */
class FeeddoFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FeeddoFCM"
        const val CHANNEL_ID = "feeddo_notifications_channel"
        const val CHANNEL_NAME = "Feeddo Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Feeddo.registerPushToken(token, FeeddoPushProvider.FCM)
                Log.d(TAG, "FCM token successfully registered with Feeddo SDK")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register FCM token with Feeddo SDK: ${e.message}", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Push received from: ${remoteMessage.from}")
        Log.d(TAG, "Push data: ${remoteMessage.data}")
        Log.d(TAG, "Push notification: title=${remoteMessage.notification?.title}, body=${remoteMessage.notification?.body}")

        val data = remoteMessage.data
        val title = remoteMessage.notification?.title ?: data["title"] ?: "Feeddo"
        val body = remoteMessage.notification?.body ?: data["body"] ?: ""

        CoroutineScope(Dispatchers.Main).launch {
            val foreground = Feeddo.isAppInForeground && Feeddo.currentActivity != null
            Log.d(TAG, "isAppInForeground: $foreground (activity: ${Feeddo.currentActivity})")
            if (foreground) {
                // When app is in foreground: Suppress system notification and show Feeddo in-app overlay banner
                Feeddo.handleForegroundNotification(
                    data = data,
                    title = title,
                    message = body
                )
            } else {
                // When app is in background or closed: Show system notification
                showSystemNotification(title, body, data)
            }
        }
    }

    private fun showSystemNotification(
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Feeddo notifications for messages and community tasks"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
