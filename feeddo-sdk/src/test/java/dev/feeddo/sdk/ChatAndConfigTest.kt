package dev.feeddo.sdk

import dev.feeddo.sdk.config.FeeddoConfig
import dev.feeddo.sdk.ui.components.parseMessageContent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.Assert.*
import org.junit.Test

class ChatAndConfigTest {

    @Test
    fun testDefaultEndpoints() {
        assertEquals("https://api.feeddo.dev/api", FeeddoConfig.DEFAULT_API_URL)
    }

    @Test
    fun testParseMessageContentPlainText() {
        val raw = "Hello, how can I help you today?"
        val parsed = parseMessageContent(raw)
        assertEquals(raw, parsed.content)
        assertNull(parsed.taskId)
        assertNull(parsed.ticketId)
    }

    @Test
    fun testParseMessageContentWithTaskId() {
        val raw = """
            Here is the feature request you requested:
            ```json
            {
              "taskId": "FEATURE-3383-001"
            }
            ```
            Feel free to check it out!
        """.trimIndent()

        val parsed = parseMessageContent(raw)
        assertEquals("FEATURE-3383-001", parsed.taskId)
        assertNull(parsed.ticketId)
        assertFalse(parsed.content.contains("FEATURE-3383-001"))
        assertTrue(parsed.content.contains("Here is the feature request you requested:"))
        assertTrue(parsed.content.contains("Feel free to check it out!"))
    }

    @Test
    fun testParseMessageContentWithTicketId() {
        val raw = """
            We created a ticket for you:
            ```json
            {"ticketId": "TICKET-789"}
            ```
            Our team will reach out soon.
        """.trimIndent()

        val parsed = parseMessageContent(raw)
        assertEquals("TICKET-789", parsed.ticketId)
        assertNull(parsed.taskId)
        assertFalse(parsed.content.contains("TICKET-789"))
        assertTrue(parsed.content.contains("We created a ticket for you:"))
    }

    @Test
    fun testWebSocketUrlConstruction() {
        val httpUrl = FeeddoConfig.DEFAULT_API_URL.toHttpUrlOrNull()!!.newBuilder()
            .addPathSegment("chat")
            .addPathSegment("ws")
            .addQueryParameter("apiKey", "test_key_123")
            .addQueryParameter("userId", "usr_abc_456")
            .addQueryParameter("conversationId", "conv_xyz_789")
            .build()

        val fullUrl = httpUrl.toString()
        assertTrue(fullUrl.startsWith("https://api.feeddo.dev/api/chat/ws"))
        assertTrue(fullUrl.contains("apiKey=test_key_123"))
        assertTrue(fullUrl.contains("userId=usr_abc_456"))
        assertTrue(fullUrl.contains("conversationId=conv_xyz_789"))
    }

    @Test
    fun testParseMessagesResponse() {
        val jsonStr = """
            {"success":true,"conversationId":"8d670414-c371-4638-947a-0c9657da4074","autoReply":false,"messages":[{"id":"aecd78f3-9fe9-4b3b-8f8f-16f191793c5d","conversationId":"8d670414-c371-4638-947a-0c9657da4074","role":"user","content":"hi","displayName":"Jane Doe","hasAttachments":false,"attachments":[],"createdAt":1794647490847},{"id":"992f389b-08da-47ae-bf8e-845dee2249ad","conversationId":"8d670414-c371-4638-947a-0c9657da4074","role":"human","content":"gi","displayName":"Dev","hasAttachments":false,"attachments":{"developerId":"dev_15f6e5a2189d4233"},"createdAt":1794647774322}]}
        """.trimIndent()
        val gson = com.google.gson.Gson()
        val resp = gson.fromJson(jsonStr, dev.feeddo.sdk.models.GetMessagesResponse::class.java)
        assertTrue(resp.success)
        assertEquals(2, resp.messages.size)
        assertEquals("hi", resp.messages[0].content)
        assertEquals("gi", resp.messages[1].content)
    }

    @Test
    @org.junit.Ignore("Requires live API key and network")
    fun testRealGetMessages() = kotlinx.coroutines.runBlocking {
        val apiService = dev.feeddo.sdk.network.ApiService(
            apiUrl = "https://api.feeddo.dev/api",
            apiKey = "YOUR_FEEDDO_API_KEY"
        )
        val messages = apiService.getMessages("sample_conversation_id")
        println("Messages count: ${messages.size}")
    }
}
