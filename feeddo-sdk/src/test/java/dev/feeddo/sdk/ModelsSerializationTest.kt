package dev.feeddo.sdk

import com.google.gson.Gson
import dev.feeddo.sdk.models.*
import org.junit.Assert.*
import org.junit.Test

class ModelsSerializationTest {

    private val gson = Gson()

    @Test
    fun testEndUserSerialization() {
        val user = EndUser(
            userId = "usr_12345",
            externalUserId = "ext_999",
            userName = "Alice Wonderland",
            email = "alice@example.com",
            country = "US",
            locale = "en-US",
            appVersion = "2.1.0",
            platform = "android",
            deviceModel = "Google Pixel 8",
            osVersion = "14",
            userSegment = "enterprise",
            subscriptionStatus = "active",
            customAttributes = mapOf("plan" to "pro", "tier" to 3),
            pushToken = "fcm_token_sample",
            pushProvider = "fcm",
            firstSeenAt = 1700000000000L
        )

        val json = gson.toJson(user)
        assertTrue(json.contains("\"userId\":\"usr_12345\""))
        assertTrue(json.contains("\"userName\":\"Alice Wonderland\""))
        assertTrue(json.contains("\"platform\":\"android\""))

        val deserialized = gson.fromJson(json, EndUser::class.java)
        assertEquals(user.userId, deserialized.userId)
        assertEquals(user.userName, deserialized.userName)
        assertEquals(user.email, deserialized.email)
        assertEquals(user.customAttributes?.get("plan"), deserialized.customAttributes?.get("plan"))
    }

    @Test
    fun testConversationParsing() {
        val json = """
            {
                "id": "conv_abc123",
                "appId": "app_feeddo",
                "status": "active",
                "title": "Need help with login",
                "displayName": "Support Bot",
                "lastMessagePreview": "We have sent a reset link.",
                "autoReply": true,
                "startedAt": 1700000000000,
                "lastMessageAt": 1700000050000,
                "unreadMessages": 2,
                "hasTicket": false,
                "hasTask": true,
                "taskStatus": "in_progress"
            }
        """.trimIndent()

        val conv = gson.fromJson(json, Conversation::class.java)
        assertEquals("conv_abc123", conv.id)
        assertEquals("active", conv.status)
        assertEquals("Support Bot", conv.displayName)
        assertEquals(2, conv.unreadMessages)
        assertTrue(conv.hasTask)
        assertEquals("in_progress", conv.taskStatus)

        // Verify copy does not throw NPE when tags or other fields are omitted in JSON
        val updated = conv.copy(unreadMessages = 0)
        assertEquals(0, updated.unreadMessages)
    }

    @Test
    fun testTaskCommentAndVoting() {
        val comment = TaskComment(
            id = "comm_1",
            content = "Great feature, looking forward to it!",
            userType = "end_user",
            authorName = "John",
            createdAt = 1700000100000L,
            hasAttachments = false
        )

        val task = Task(
            id = "task_99",
            title = "Dark Mode Support",
            description = "Add dark mode to the application",
            status = "in_progress",
            priority = "high",
            type = "feature",
            upvoteCount = 42,
            commentCount = 1,
            myVote = "up",
            comments = listOf(comment),
            isOwner = true
        )

        assertEquals("task_99", task.id)
        assertEquals(42, task.upvoteCount)
        assertEquals("up", task.myVote)
        assertEquals(1, task.safeComments.size)
        assertEquals("Great feature, looking forward to it!", task.safeComments[0].content)
    }

    @Test
    fun testMessageRoleAndIsUser() {
        val userMsg = Message(
            id = "msg_1",
            conversationId = "conv_1",
            role = "user",
            content = "Hello there!"
        )
        assertTrue(userMsg.isUser)

        val botMsg = Message(
            id = "msg_2",
            conversationId = "conv_1",
            role = "assistant",
            content = "Hello! How can I assist you today?"
        )
        assertFalse(botMsg.isUser)
    }

    @Test
    fun testPushProviderParsing() {
        assertEquals(FeeddoPushProvider.FCM, FeeddoPushProvider.fromString("fcm"))
        assertEquals(FeeddoPushProvider.FCM, FeeddoPushProvider.fromString("FCM"))
        assertEquals(FeeddoPushProvider.APNS, FeeddoPushProvider.fromString("apns"))
        assertEquals(FeeddoPushProvider.ONESIGNAL, FeeddoPushProvider.fromString("onesignal"))
        assertNull(FeeddoPushProvider.fromString("unknown_provider"))
    }
}

