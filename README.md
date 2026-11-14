# Feeddo Android SDK

Native Android SDK (Kotlin + Jetpack Compose) for **Feeddo** — AI-powered customer support, feedback, and community engagement.

Maintains 100% architectural and feature parity with [`feeddo_flutter`](../feeddo_flutter).

---

## Features

- **Customer Support Chat**: Real-time bidirectional WebSocket communication (`wss://api.feeddo.dev/api/chat/ws`), typing indicators, message delivery, and read receipts.
- **Rich Media & Attachments**: Image attachments with upload and preview.
- **Embedded Task & Ticket Cards**: Direct support for interactive task cards and support ticket references within chat messages.
- **Satisfaction Ratings**: Post-resolution conversation rating widget (1-5 stars).
- **Community Board**: Public feature requests and bug reports with upvoting, comment threads, attachments, filtering (All, Features, Bugs), and sorting (Most Upvoted, Recent).
- **Support Tickets**: Support ticket list and detail sheet with conversation linking.
- **In-App Notification Banners**: Slide-in top banners for unread messages and system updates with auto-dismiss and tap handling.
- **Unread Message Counter**: Live `StateFlow<Int>` counter for badges.
- **Theming**: Dark and Light themes with full custom color overrides.
- **Offline & Identity Management**: Persistent UUID v4 user identification, automatic device info collection, and backend synchronization.

---

## Installation

Add the Feeddo SDK module or AAR to your Gradle build:

### Gradle (Kotlin DSL)
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// build.gradle.kts (app)
dependencies {
    implementation(project(":feeddo-sdk"))
    // Or if using Maven / AAR:
    // implementation("dev.feeddo:feeddo-sdk:1.0.0")
}
```

---

## Quickstart

### 1. Initialize Feeddo

Initialize the SDK at app launch (e.g. in your `Application` class or main `Activity`):

```kotlin
import dev.feeddo.sdk.Feeddo
import dev.feeddo.sdk.models.FeeddoPushProvider
import dev.feeddo.sdk.theme.FeeddoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val userId = Feeddo.init(
                context = this@MainActivity,
                apiKey = "YOUR_FEEDDO_API_KEY",
                userName = "Jane Doe",
                email = "jane@example.com",
                userSegment = "premium",
                theme = FeeddoTheme.dark(), // or FeeddoTheme.light()
                isInAppNotificationOn = true
            )
            println("Feeddo initialized with user ID: $userId")
        }
    }
}
```

### 2. Launch the Support Home Screen

Open the full support interface with greeting, recent activity, quick actions, and chat:

```kotlin
Feeddo.show(context)
```

### 3. Open Community Board

Directly open the Community Board (Feature Requests & Bug Reports):

```kotlin
Feeddo.showCommunityBoard(context)
```

### 4. Show In-App Banner Notification

Show a banner at the top of the current screen:

```kotlin
Feeddo.showInAppNotification(
    activity = this,
    title = "New message from Support",
    message = "We have replied to your inquiry.",
    theme = FeeddoTheme.dark()
)
```

### 5. Handle Push Notification Tap

Handle background or push notification clicks (e.g. FCM payload):

```kotlin
Feeddo.handleNotificationTap(
    context = context,
    data = remoteMessage.data
)
```

### 6. Register Push Token (FCM / OneSignal)

```kotlin
Feeddo.registerPushToken(
    pushToken = token,
    pushProvider = FeeddoPushProvider.FCM
)
```

### 7. Update User Profile

```kotlin
Feeddo.updateUser(
    userName = "Jane Smith",
    email = "jane.smith@example.com",
    customAttributes = mapOf("plan" to "enterprise")
)
```

### 8. Live Unread Message Count

Observe unread messages in real-time in Jetpack Compose:

```kotlin
val unreadCountState = Feeddo.unreadMessageCountFlow?.collectAsState()
val unreadCount = unreadCountState?.value ?: Feeddo.unreadMessageCount

if (unreadCount > 0) {
    Badge { Text("$unreadCount") }
}
```

---

## Theming

Feeddo supports dark and light modes out of the box, as well as full color customization:

```kotlin
val customTheme = FeeddoTheme(
    colors = FeeddoColors(
        background = Color(0xFF0F172A),
        cardBackground = Color(0xFF1E293B),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        primary = Color(0xFF38BDF8),
        border = Color(0xFF334155),
        divider = Color(0xFF1E293B),
        appBarBackground = Color(0xFF0F172A)
    ),
    isDark = true
)

Feeddo.show(context, theme = customTheme)
```

---

## Project Structure

```
feeddo_android/
├── feeddo-sdk/                 # Core Android Library (AAR)
│   ├── src/main/java/dev/feeddo/sdk/
│   │   ├── Feeddo.kt           # Public API facade
│   │   ├── FeeddoInternal.kt   # Core SDK coordinator
│   │   ├── config/             # Config & endpoint URLs
│   │   ├── models/             # Data models & JSON serialization
│   │   ├── network/            # ApiService & WebSocketService
│   │   ├── service/            # ConversationService & state
│   │   ├── theme/              # FeeddoTheme & FeeddoColors
│   │   ├── ui/
│   │   │   ├── FeeddoActivity.kt
│   │   │   ├── FeeddoCommunityActivity.kt
│   │   │   ├── FeeddoChatActivity.kt
│   │   │   ├── FeeddoInAppNotificationManager.kt
│   │   │   ├── screens/        # Compose Screens (Home, Chat, Messages, Tickets, Tasks)
│   │   │   └── components/     # Reusable UI widgets (Bubbles, Cards, Sheets, Input)
│   │   └── utils/              # StorageHelper & DeviceInfoHelper
│   └── src/test/java/          # Unit tests
├── sample-app/                 # Demo Application
│   └── src/main/java/dev/feeddo/sample/
│       └── MainActivity.kt
└── gradle/                     # Gradle wrapper & Version catalog
```

---

## Build & Test

```bash
# Run unit tests
./gradlew test

# Assemble Debug APK & AAR
./gradlew assembleDebug

# Assemble Release AAR
./gradlew :feeddo-sdk:assembleRelease
```

