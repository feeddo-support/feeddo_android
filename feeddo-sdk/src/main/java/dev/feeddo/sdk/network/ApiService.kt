package dev.feeddo.sdk.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import dev.feeddo.sdk.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

class FeeddoApiException(
    message: String,
    val statusCode: Int? = null,
    val details: Any? = null
) : Exception(
    "FeeddoApiException: $message" +
            (if (statusCode != null) " (Status: $statusCode)" else "") +
            (if (details != null) " - $details" else "")
)

class ApiService(
    val apiUrl: String,
    val apiKey: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Lenient Gson instance
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Boolean::class.java, object : JsonDeserializer<Boolean> {
            override fun deserialize(
                json: JsonElement?,
                typeOfT: Type?,
                context: JsonDeserializationContext?
            ): Boolean {
                if (json == null || json.isJsonNull) return false
                return try {
                    if (json.asJsonPrimitive.isBoolean) {
                        json.asBoolean
                    } else if (json.asJsonPrimitive.isNumber) {
                        json.asInt == 1
                    } else {
                        json.asString.equals("true", ignoreCase = true) || json.asString == "1"
                    }
                } catch (_: Exception) {
                    false
                }
            }
        })
        .create()

    private fun newRequestBuilder(url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
    }

    suspend fun upsertEndUser(user: EndUser): UpsertEndUserResponse = withContext(Dispatchers.IO) {
        val url = "$apiUrl/end-users/upsert"
        val jsonBody = gson.toJson(user)
        val request = newRequestBuilder(url)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                gson.fromJson(responseBody, UpsertEndUserResponse::class.java)
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    suspend fun getHomeData(userId: String): HomeData = withContext(Dispatchers.IO) {
        val url = "$apiUrl/end-users/home?userId=$userId"
        val request = newRequestBuilder(url).get().build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                gson.fromJson(responseBody, HomeData::class.java)
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    suspend fun getUnreadData(userId: String): UnreadData? = withContext(Dispatchers.IO) {
        val url = "$apiUrl/end-users/unread?userId=$userId"
        val request = newRequestBuilder(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string().orEmpty()
                    gson.fromJson(responseBody, UnreadData::class.java)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getNotifications(
        userId: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<FeeddoNotification> = withContext(Dispatchers.IO) {
        val url = "$apiUrl/end-users/notifications?userId=$userId&limit=$limit&offset=$offset"
        val request = newRequestBuilder(url).get().build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val resp = gson.fromJson(responseBody, GetNotificationsResponse::class.java)
                resp.notifications
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    suspend fun getConversations(userId: String): List<Conversation> = withContext(Dispatchers.IO) {
        val url = "$apiUrl/conversations?userId=$userId"
        val request = newRequestBuilder(url).get().build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val resp = gson.fromJson(responseBody, GetConversationsResponse::class.java)
                resp.conversations
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    suspend fun getConversation(conversationId: String): Conversation = withContext(Dispatchers.IO) {
        val url = "$apiUrl/conversations/$conversationId"
        val request = newRequestBuilder(url).get().build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val json = gson.fromJson(responseBody, JsonObject::class.java)
                if (json.get("success")?.asBoolean == true && json.has("conversation")) {
                    gson.fromJson(json.get("conversation"), Conversation::class.java)
                } else {
                    throw FeeddoApiException("Conversation not found or invalid response")
                }
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    suspend fun createConversation(
        userId: String,
        agentName: String = "support_agent"
    ): Conversation = withContext(Dispatchers.IO) {
        val url = "$apiUrl/conversations"
        val body = mapOf("userId" to userId, "agentName" to agentName)
        val request = newRequestBuilder(url)
            .post(gson.toJson(body).toRequestBody(jsonMediaType))
            .build()

        val conversationId: String = client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val json = gson.fromJson(responseBody, JsonObject::class.java)
                json.get("conversationId")?.asString
                    ?: throw FeeddoApiException("Missing conversationId in response")
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }

        getConversation(conversationId)
    }

    suspend fun getMessages(conversationId: String): List<Message> = withContext(Dispatchers.IO) {
        val url = "$apiUrl/conversations/$conversationId/messages"
        val request = newRequestBuilder(url).get().build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val resp = gson.fromJson(responseBody, GetMessagesResponse::class.java)
                resp.messages
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    suspend fun getTask(taskId: String, userId: String? = null): Task = withContext(Dispatchers.IO) {
        val httpUrl = "$apiUrl/tasks/$taskId".toHttpUrlOrNull()!!.newBuilder().apply {
            if (userId != null) addQueryParameter("userId", userId)
        }.build()

        val request = newRequestBuilder(httpUrl.toString()).get().build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val json = gson.fromJson(responseBody, JsonObject::class.java)
                if (json.get("success")?.asBoolean == true && json.has("task")) {
                    gson.fromJson(json.get("task"), Task::class.java)
                } else {
                    throw FeeddoApiException("Task not found or invalid response")
                }
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    suspend fun getTicket(ticketId: String, userId: String? = null): Ticket = withContext(Dispatchers.IO) {
        val httpUrl = "$apiUrl/tickets/$ticketId".toHttpUrlOrNull()!!.newBuilder().apply {
            if (userId != null) addQueryParameter("userId", userId)
        }.build()

        val request = newRequestBuilder(httpUrl.toString()).get().build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val json = gson.fromJson(responseBody, JsonObject::class.java)
                if (json.get("success")?.asBoolean == true && json.has("ticket")) {
                    gson.fromJson(json.get("ticket"), Ticket::class.java)
                } else {
                    throw FeeddoApiException("Ticket not found or invalid response")
                }
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    suspend fun getTickets(userId: String): List<Ticket> = withContext(Dispatchers.IO) {
        val url = "$apiUrl/tickets?userId=$userId"
        val request = newRequestBuilder(url).get().build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val resp = gson.fromJson(responseBody, GetTicketsResponse::class.java)
                resp.tickets
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    suspend fun getTasks(
        userId: String? = null,
        type: String? = null,
        priority: String? = null,
        sortBy: String? = null,
        sortOrder: String? = null,
        page: Int? = null,
        limit: Int? = null,
        createdByMe: Boolean? = null
    ): List<Task> = withContext(Dispatchers.IO) {
        val httpUrl = "$apiUrl/tasks".toHttpUrlOrNull()!!.newBuilder().apply {
            if (userId != null) addQueryParameter("userId", userId)
            if (type != null) addQueryParameter("type", type)
            if (priority != null) addQueryParameter("priority", priority)
            if (sortBy != null) addQueryParameter("sortBy", sortBy)
            if (sortOrder != null) addQueryParameter("sortOrder", sortOrder)
            if (page != null) addQueryParameter("page", page.toString())
            if (limit != null) addQueryParameter("limit", limit.toString())
            if (createdByMe != null) addQueryParameter("createdByMe", createdByMe.toString())
        }.build()

        val request = newRequestBuilder(httpUrl.toString()).get().build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val resp = gson.fromJson(responseBody, GetTasksResponse::class.java)
                resp.tasks
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    suspend fun addTaskComment(
        taskId: String,
        content: String,
        userId: String? = null,
        attachments: List<Map<String, Any?>>? = null
    ): TaskComment = withContext(Dispatchers.IO) {
        val httpUrl = "$apiUrl/tasks/$taskId/comments".toHttpUrlOrNull()!!.newBuilder().apply {
            if (userId != null) addQueryParameter("userId", userId)
        }.build()

        val bodyMap = mutableMapOf<String, Any?>("content" to content)
        if (!attachments.isNullOrEmpty()) {
            bodyMap["attachments"] = attachments
        }

        val request = newRequestBuilder(httpUrl.toString())
            .post(gson.toJson(bodyMap).toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val json = gson.fromJson(responseBody, JsonObject::class.java)
                if (json.get("success")?.asBoolean == true && json.has("comment")) {
                    gson.fromJson(json.get("comment"), TaskComment::class.java)
                } else {
                    throw FeeddoApiException("Failed to add comment")
                }
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    suspend fun voteTask(
        taskId: String,
        voteType: String,
        userId: String? = null
    ): Map<String, Any?> = withContext(Dispatchers.IO) {
        val httpUrl = "$apiUrl/tasks/$taskId/vote".toHttpUrlOrNull()!!.newBuilder().apply {
            if (userId != null) addQueryParameter("userId", userId)
        }.build()

        val bodyMap = mapOf("voteType" to voteType)

        val request = newRequestBuilder(httpUrl.toString())
            .post(gson.toJson(bodyMap).toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val type = object : TypeToken<Map<String, Any?>>() {}.type
                gson.fromJson(responseBody, type)
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    suspend fun rateConversation(
        conversationId: String,
        rating: Int
    ): Unit = withContext(Dispatchers.IO) {
        val url = "$apiUrl/conversations/$conversationId/satisfaction"
        val bodyMap = mapOf("rating" to rating)

        val request = newRequestBuilder(url)
            .put(gson.toJson(bodyMap).toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val responseBody = response.body?.string().orEmpty()
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    suspend fun uploadMedia(
        bytes: ByteArray,
        fileName: String,
        mimeType: String = "image/jpeg",
        userId: String
    ): Map<String, Any?> = withContext(Dispatchers.IO) {
        val url = "$apiUrl/media/upload?userId=$userId"

        val fileRequestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileRequestBody)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .post(multipartBody)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val json = gson.fromJson(responseBody, JsonObject::class.java)
                if (json.get("success")?.asBoolean == true && json.has("media")) {
                    val type = object : TypeToken<Map<String, Any?>>() {}.type
                    gson.fromJson(json.get("media"), type)
                } else {
                    throw FeeddoApiException("Upload failed: $responseBody")
                }
            } else {
                val errorMsg = parseErrorMessage(responseBody)
                throw FeeddoApiException(errorMsg, response.code, responseBody)
            }
        }
    }

    private fun parseErrorMessage(body: String): String {
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            json.get("error")?.asString ?: "Unknown API error"
        } catch (_: Exception) {
            body.ifEmpty { "Unknown API error" }
        }
    }
}

