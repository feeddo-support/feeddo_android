package dev.feeddo.sample

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dev.feeddo.sdk.Feeddo
import dev.feeddo.sdk.models.FeeddoPushProvider
import dev.feeddo.sdk.theme.FeeddoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "FeeddoSampleMain"
    }

    override fun onResume() {
        super.onResume()
        Feeddo.setNotificationActivity(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle notification click if launched from push
        handlePushIntent(intent)

        val apiKey = "YOUR_FEEDDO_API_KEY"

        setContent {
            val coroutineScope = rememberCoroutineScope()
            var isDarkMode by remember { mutableStateOf(true) }
            val unreadCountState = Feeddo.unreadMessageCountFlow?.collectAsState()
            val unreadCount = unreadCountState?.value ?: Feeddo.unreadMessageCount
            var userId by remember { mutableStateOf<String?>(null) }
            var isInitialized by remember { mutableStateOf(false) }
            var fcmToken by remember { mutableStateOf<String?>(null) }
            var hasNotificationPermission by remember { mutableStateOf(true) }

            // Permission launcher for Android 13+ (API 33+)
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasNotificationPermission = isGranted
                if (isGranted) {
                    Toast.makeText(this@MainActivity, "Notification permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Notification permission denied", Toast.LENGTH_SHORT).show()
                }
            }

            LaunchedEffect(Unit) {
                // Check notification permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    hasNotificationPermission = granted
                    if (!granted) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Initialize Feeddo SDK
                try {
                    val uid = Feeddo.init(
                        context = this@MainActivity,
                        apiKey = apiKey,
                        userName = "Jane Doe",
                        email = "jane@example.com",
                        userSegment = "premium",
                        theme = if (isDarkMode) FeeddoTheme.dark() else FeeddoTheme.light()
                    )
                    userId = uid
                    isInitialized = true

                    // Fetch FCM Token and register with Feeddo SDK if Firebase is initialized
                    try {
                        if (FirebaseApp.getApps(this@MainActivity).isNotEmpty()) {
                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val token = task.result
                                    Log.d(TAG, "Fetched FCM Token: $token")
                                    fcmToken = token
                                    coroutineScope.launch {
                                        try {
                                            Feeddo.registerPushToken(token, FeeddoPushProvider.FCM)
                                            Log.d(TAG, "FCM Token registered with Feeddo successfully")
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to register FCM token: ${e.message}", e)
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "Fetching FCM token failed", task.exception)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Firebase not configured: ${e.message}")
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Init error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            val currentTheme = if (isDarkMode) FeeddoTheme.dark() else FeeddoTheme.light()
            val scrollState = rememberScrollState()

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = currentTheme.colors.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Feeddo Android SDK",
                        color = currentTheme.colors.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Native Kotlin + Jetpack Compose",
                        color = currentTheme.colors.textSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (userId != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = currentTheme.colors.cardBackground),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "User ID: $userId",
                                    color = currentTheme.colors.textSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Unread Messages: $unreadCount",
                                    color = currentTheme.colors.cardText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Push Notification / FCM Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = currentTheme.colors.cardBackground),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = currentTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FCM Push Notifications",
                                    color = currentTheme.colors.cardText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = when {
                                    fcmToken != null -> "Status: Registered with Feeddo"
                                    FirebaseApp.getApps(this@MainActivity).isNotEmpty() -> "Status: Fetching token..."
                                    else -> "Status: Add google-services.json to enable FCM"
                                },
                                color = if (fcmToken != null) Color(0xFF10B981) else currentTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            if (fcmToken != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Token: ${fcmToken!!.take(24)}...${fcmToken!!.takeLast(10)}",
                                    color = currentTheme.colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("FCM Token", fcmToken)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(this@MainActivity, "FCM Token copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = currentTheme.colors.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy FCM Token", fontSize = 13.sp, color = currentTheme.colors.primary)
                                }
                            }

                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.colors.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Grant Notification Permission", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Buttons
                    Button(
                        onClick = {
                            Feeddo.show(this@MainActivity, theme = currentTheme)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentTheme.colors.primary,
                            contentColor = currentTheme.colors.background
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Open Feeddo Support", fontWeight = FontWeight.Bold)
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$unreadCount",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            Feeddo.showCommunityBoard(this@MainActivity, theme = currentTheme)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = currentTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Community Board",
                            color = currentTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Simulate In-App Foreground Push Notification
                    OutlinedButton(
                        onClick = {
                            Feeddo.handleForegroundNotification(
                                data = mapOf(
                                    "type" to "chat_message",
                                    "conversationId" to "sample_conv_123",
                                    "title" to "Feeddo Support",
                                    "body" to "Hello! We have received your request and are actively working on it."
                                ),
                                title = "Feeddo Support",
                                message = "Hello! We have received your request and are actively working on it.",
                                theme = currentTheme
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = currentTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simulate In-App Push",
                            color = currentTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    Feeddo.updateUser(
                                        userName = "Alex Smith",
                                        email = "alex@example.com"
                                    )
                                    Toast.makeText(this@MainActivity, "User profile updated!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(this@MainActivity, "Update error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = currentTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Update User Profile",
                            color = currentTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Theme toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDarkMode) "Dark Theme" else "Light Theme",
                            color = currentTheme.colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { isDarkMode = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    private fun handlePushIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val dataMap = mutableMapOf<String, Any?>()
        for (key in extras.keySet()) {
            dataMap[key] = extras.getString(key)
        }
        if (dataMap.containsKey("conversationId") || dataMap.containsKey("taskId") || dataMap.containsKey("type")) {
            lifecycleScope.launch {
                Feeddo.handleNotificationTap(this@MainActivity, dataMap)
            }
        }
    }
}
