package dev.feeddo.sdk.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.feeddo.sdk.FeeddoInternal
import dev.feeddo.sdk.models.Conversation
import dev.feeddo.sdk.models.Message
import dev.feeddo.sdk.models.Task
import dev.feeddo.sdk.models.Ticket
import dev.feeddo.sdk.theme.FeeddoTheme
import dev.feeddo.sdk.ui.components.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeddoChatScreen(
    conversation: Conversation?,
    theme: FeeddoTheme,
    onBack: () -> Unit,
    initialMessage: String? = null,
    onTaskTap: ((String) -> Unit)? = null,
    onTicketTap: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var currentConversation by remember { mutableStateOf(conversation) }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var isLoading by remember { mutableStateOf(conversation != null) }
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }
    var systemStatus by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val taskCache = remember { mutableStateMapOf<String, Task>() }
    val ticketCache = remember { mutableStateMapOf<String, Ticket>() }

    fun buildWelcomeMessage(convId: String?): Message {
        val welcomeContent = initialMessage
            ?: "How can I help you?\nYou can ask any question about the app, report a bug, or request a new feature."
        return Message(
            id = "welcome-${convId ?: "new"}",
            conversationId = convId ?: "new",
            role = "assistant",
            content = welcomeContent,
            createdAt = System.currentTimeMillis(),
            displayName = FeeddoInternal.instance.chatBotName,
            hasAttachments = false
        )
    }

    // Connect to WebSocket and load messages once
    LaunchedEffect(Unit) {
        val convId = currentConversation?.id
        FeeddoInternal.instance.conversationService.setActiveConversationId(convId)
        FeeddoInternal.instance.connectWebSocket(conversationId = convId)

        if (!convId.isNullOrBlank() && !convId.startsWith("temp")) {
            try {
                isLoading = true
                errorText = null
                val loadedMessages = FeeddoInternal.instance.apiService.getMessages(convId)
                // Reverse to display newest first at index 0, followed by welcome message
                messages = loadedMessages.reversed() + listOf(buildWelcomeMessage(convId))
                FeeddoInternal.instance.conversationService.sendReadReceipt(convId)
            } catch (e: Exception) {
                android.util.Log.e("FeeddoChatScreen", "Failed to load messages", e)
                errorText = e.message ?: "Failed to load messages"
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
            messages = listOf(buildWelcomeMessage(null))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentConversation?.let { conv ->
                FeeddoInternal.instance.conversationService.updateConversation(conv)
            }
            FeeddoInternal.instance.conversationService.setActiveConversationId(null)
        }
    }

    fun scrollToBottom(animate: Boolean = true) {
        coroutineScope.launch {
            try {
                if (animate) {
                    listState.animateScrollToItem(0, 0)
                } else {
                    listState.scrollToItem(0, 0)
                }
            } catch (_: Exception) {}
        }
    }

    // Auto-scroll to bottom after sending a message or when new incoming messages arrive
    LaunchedEffect(messages.firstOrNull()?.id) {
        if (messages.isNotEmpty()) {
            val first = messages.first()
            if (first.isUser) {
                listState.animateScrollToItem(0, 0)
            } else if (listState.firstVisibleItemIndex <= 1) {
                listState.animateScrollToItem(0, 0)
            }
        }
    }

    LaunchedEffect(isTyping) {
        if (isTyping && listState.firstVisibleItemIndex <= 1) {
            try {
                listState.animateScrollToItem(0, 0)
            } catch (_: Exception) {}
        }
    }

    // Helper to fetch task
    fun loadTask(taskId: String) {
        if (!taskCache.containsKey(taskId)) {
            coroutineScope.launch {
                try {
                    val task = FeeddoInternal.instance.apiService.getTask(
                        taskId,
                        userId = FeeddoInternal.instance.cachedUserId
                    )
                    taskCache[taskId] = task
                } catch (_: Exception) {}
            }
        }
    }

    // Helper to fetch ticket
    fun loadTicket(ticketId: String) {
        if (!ticketCache.containsKey(ticketId)) {
            coroutineScope.launch {
                try {
                    val ticket = FeeddoInternal.instance.apiService.getTicket(
                        ticketId,
                        userId = FeeddoInternal.instance.cachedUserId
                    )
                    ticketCache[ticketId] = ticket
                } catch (_: Exception) {}
            }
        }
    }

    // Image Picker Launcher: Immediately uploads and sends image attachment message (matching Flutter)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isSending = true
                try {
                    val userId = FeeddoInternal.instance.ensureUserInitialized()
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val media = FeeddoInternal.instance.apiService.uploadMedia(
                            bytes = bytes,
                            fileName = "attachment_${System.currentTimeMillis()}.jpg",
                            mimeType = mimeType,
                            userId = userId
                        )

                        val tempMessage = Message(
                            id = "temp_${System.currentTimeMillis()}",
                            conversationId = currentConversation?.id ?: "temp",
                            role = "user",
                            content = "Sent an attachment",
                            hasAttachments = true,
                            attachments = listOf(media),
                            createdAt = System.currentTimeMillis()
                        )

                        messages = listOf(tempMessage) + messages
                        scrollToBottom()

                        val messageData = mutableMapOf<String, Any?>(
                            "type" to "message",
                            "message" to "Sent an attachment",
                            "id" to tempMessage.id,
                            "attachments" to listOf(media)
                        )

                        currentConversation?.let { conv ->
                            messageData["conversationId"] = conv.id
                            val updatedConv = conv.copy(
                                lastMessagePreview = "Sent an attachment",
                                lastMessageAt = tempMessage.createdAt
                            )
                            currentConversation = updatedConv
                            FeeddoInternal.instance.conversationService.updateConversation(updatedConv)
                        }

                        FeeddoInternal.instance.webSocketService.send(messageData)
                        FeeddoInternal.instance.storageHelper.setHasActiveConversation(true)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isSending = false
                }
            }
        }
    }

    // Listen to WebSocket messages
    LaunchedEffect(Unit) {
        FeeddoInternal.instance.webSocketService.messages.collect { data ->
            val type = data["type"] as? String
            val role = data["role"] as? String

            if (type == "system") {
                return@collect
            }

            // Assign new conversation ID if new conversation started
            val incomingConvId = data["conversationId"] as? String
            if (currentConversation == null && incomingConvId != null) {
                coroutineScope.launch {
                    try {
                        val fetchedConv = FeeddoInternal.instance.apiService.getConversation(incomingConvId)
                        currentConversation = fetchedConv
                        FeeddoInternal.instance.conversationService.updateConversation(fetchedConv)
                        FeeddoInternal.instance.conversationService.setActiveConversationId(incomingConvId)
                    } catch (_: Exception) {}
                }
            }

            // Filter messages for current conversation if conversation is active
            if (currentConversation != null && incomingConvId != null && incomingConvId != currentConversation?.id) {
                return@collect
            }

            if (role == "assistant" || role == "human" || role == "user") {
                val newMsg = Message(
                    id = data["id"] as? String ?: UUID.randomUUID().toString(),
                    conversationId = incomingConvId ?: currentConversation?.id ?: "",
                    role = role,
                    content = data["content"] as? String ?: data["message"] as? String,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    displayName = data["displayName"] as? String ?: FeeddoInternal.instance.chatBotName,
                    hasAttachments = data["hasAttachments"] == true || data["attachments"] != null,
                    attachments = data["attachments"]
                )

                // Invalidate task/ticket cache if message contains references
                newMsg.content?.let { c ->
                    val parsed = parseMessageContent(c)
                    parsed.taskId?.let { tid ->
                        taskCache.remove(tid)
                        loadTask(tid)
                    }
                    parsed.ticketId?.let { tid ->
                        ticketCache.remove(tid)
                        loadTicket(tid)
                    }
                }

                messages = listOf(newMsg) + messages
                isTyping = false
                systemStatus = null

                // Update conversation preview
                currentConversation?.let { conv ->
                    val parsed = parseMessageContent(newMsg.content)
                    val updated = conv.copy(
                        lastMessagePreview = if (parsed.content.isNotEmpty()) parsed.content else newMsg.content,
                        lastMessageAt = newMsg.createdAt,
                        displayName = newMsg.displayName ?: conv.displayName
                    )
                    currentConversation = updated
                    FeeddoInternal.instance.conversationService.updateConversation(updated)
                    FeeddoInternal.instance.conversationService.sendReadReceipt(conv.id)
                }
            } else if (type == "typing") {
                isTyping = (data["status"] as? String) == "on"
            } else if (type == "system_status") {
                systemStatus = data["content"] as? String ?: data["status"] as? String
            } else if (type == "error" || data["success"] == false) {
                val errMsg = data["error"] as? String ?: data["message"] as? String ?: "Unknown error"
                Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val displayTitle = currentConversation?.displayName ?: FeeddoInternal.instance.chatBotName
    val isResolved = currentConversation?.status == "resolved"

    Scaffold(
        containerColor = theme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(theme.colors.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayTitle.trim().take(1).uppercase().ifEmpty { "F" },
                                color = if (theme.isDark) Color.Black else Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = displayTitle,
                                color = theme.colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.5).sp
                            )
                            if (currentConversation?.status == "active") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(theme.colors.success)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Online",
                                        color = theme.colors.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.colors.iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.colors.appBarBackground
                )
            )
        },
        bottomBar = {
            if (isResolved) {
                // Resolved rating widget matching Flutter
                val conv = currentConversation
                val hasRating = (conv?.userSatisfaction ?: 0) > 0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.colors.surface)
                        .border(1.dp, theme.colors.border)
                        .padding(16.dp)
                        .imePadding()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "This conversation has been resolved.",
                            color = theme.colors.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (hasRating) "Your rating:" else "How would you rate your experience?",
                            color = theme.colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RatingWidget(
                            currentRating = conv?.userSatisfaction ?: 0,
                            readOnly = hasRating,
                            onRate = { rating ->
                                if (conv != null) {
                                    coroutineScope.launch {
                                        try {
                                            FeeddoInternal.instance.conversationService.rateConversation(conv.id, rating)
                                            currentConversation = conv.copy(userSatisfaction = rating)
                                            Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to submit rating: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            theme = theme
                        )
                        if (hasRating) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Thank you for your feedback!",
                                color = theme.colors.success,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            } else {
                ChatInputArea(
                    messageText = inputText,
                    onMessageChange = { inputText = it },
                    onSend = {
                        val text = inputText.trim()
                        if (text.isEmpty()) return@ChatInputArea

                        isSending = true
                        inputText = ""

                        val tempMessage = Message(
                            id = "temp_${System.currentTimeMillis()}",
                            conversationId = currentConversation?.id ?: "temp",
                            role = "user",
                            content = text,
                            createdAt = System.currentTimeMillis(),
                            hasAttachments = false
                        )

                        // Optimistically add user message at the front of reversed list
                        messages = listOf(tempMessage) + messages
                        scrollToBottom()

                        coroutineScope.launch {
                            try {
                                FeeddoInternal.instance.ensureUserInitialized()

                                val messageData = mutableMapOf<String, Any?>(
                                    "type" to "message",
                                    "message" to text,
                                    "id" to tempMessage.id
                                )

                                currentConversation?.let { conv ->
                                    messageData["conversationId"] = conv.id
                                    val updatedConv = conv.copy(
                                        lastMessagePreview = text,
                                        lastMessageAt = tempMessage.createdAt
                                    )
                                    currentConversation = updatedConv
                                    FeeddoInternal.instance.conversationService.updateConversation(updatedConv)
                                }

                                val sent = FeeddoInternal.instance.webSocketService.send(messageData)
                                if (!sent) {
                                    Toast.makeText(context, "Message queued, waiting for connection...", Toast.LENGTH_SHORT).show()
                                }
                                FeeddoInternal.instance.storageHelper.setHasActiveConversation(true)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error sending message: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSending = false
                            }
                        }
                    },
                    onAttachTap = {
                        imagePickerLauncher.launch("image/*")
                    },
                    isSending = isSending,
                    theme = theme
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = theme.colors.primary
                )
            } else if (errorText != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorText ?: "Failed to load messages",
                        color = theme.colors.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val convId = currentConversation?.id
                                if (!convId.isNullOrBlank() && !convId.startsWith("temp")) {
                                    try {
                                        isLoading = true
                                        errorText = null
                                        val loaded = FeeddoInternal.instance.apiService.getMessages(convId)
                                        messages = loaded.reversed() + listOf(buildWelcomeMessage(convId))
                                    } catch (e: Exception) {
                                        android.util.Log.e("FeeddoChatScreen", "Failed to load messages", e)
                                        errorText = e.message ?: "Failed to load messages"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Retry", color = theme.colors.primary)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Item at index 0 when typing or system status active
                    if (isTyping || systemStatus != null) {
                        item(key = "status_indicator") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (systemStatus != null) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = theme.colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = systemStatus!!,
                                        color = theme.colors.textSecondary,
                                        fontSize = 12.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                } else if (isTyping) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(theme.colors.surface)
                                            .border(1.dp, theme.colors.border, RoundedCornerShape(16.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        TypingIndicator(theme = theme)
                                    }
                                }
                            }
                        }
                    }

                    items(messages, key = { it.id }) { message ->
                        val parsed = remember(message.content) { parseMessageContent(message.content) }
                        if (parsed.taskId != null) {
                            loadTask(parsed.taskId)
                        }
                        if (parsed.ticketId != null) {
                            loadTicket(parsed.ticketId)
                        }

                        MessageBubble(
                            message = message,
                            theme = theme,
                            taskCard = parsed.taskId?.let { taskId ->
                                val task = taskCache[taskId]
                                if (task != null) {
                                    {
                                        TaskCard(
                                            task = task,
                                            theme = theme,
                                            onTap = {
                                                onTaskTap?.invoke(taskId)
                                            }
                                        )
                                    }
                                } else null
                            },
                            ticketCard = parsed.ticketId?.let { ticketId ->
                                val ticket = ticketCache[ticketId]
                                if (ticket != null) {
                                    {
                                        TicketCard(
                                            ticket = ticket,
                                            theme = theme,
                                            onTap = {
                                                onTicketTap?.invoke(ticketId)
                                            }
                                        )
                                    }
                                } else null
                            }
                        )
                    }
                }
            }
        }
    }
}
