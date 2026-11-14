package dev.feeddo.sdk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.feeddo.sdk.Feeddo
import dev.feeddo.sdk.FeeddoInternal
import dev.feeddo.sdk.models.Conversation
import dev.feeddo.sdk.models.HomeData
import dev.feeddo.sdk.theme.FeeddoTheme
import dev.feeddo.sdk.ui.components.FeeddoNotificationsSheet
import dev.feeddo.sdk.ui.components.TaskCard
import dev.feeddo.sdk.ui.components.TicketCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeddoHomeScreen(
    theme: FeeddoTheme,
    onClose: () -> Unit,
    onSendMessageTap: () -> Unit,
    onConversationTap: (Conversation) -> Unit,
    onMessagesTap: () -> Unit,
    onTicketsTap: () -> Unit,
    onCommunityBoardTap: () -> Unit,
    onTaskTap: (String) -> Unit,
    onTicketTap: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var homeData by remember { mutableStateOf<HomeData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }

    val conversations by FeeddoInternal.instance.conversationService.conversations.collectAsState()
    val unreadMessageCount by FeeddoInternal.instance.conversationService.unreadMessageCount.collectAsState()

    // Determine the most active/recent non-resolved conversation
    val displayConversation = remember(conversations, homeData) {
        val activeServiceConv = conversations.firstOrNull { it.status != "resolved" }
        activeServiceConv ?: homeData?.recentConversation?.takeIf { it.status != "resolved" }
    }

    val hasRecentActivity = homeData?.recentTask != null ||
            homeData?.recentTicket != null ||
            displayConversation != null

    suspend fun loadHomeData() {
        try {
            val data = FeeddoInternal.instance.getHomeData()
            data.recentConversation?.let { conv ->
                FeeddoInternal.instance.conversationService.updateConversation(conv)
            }
            homeData = data
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadHomeData()
        val currentName = FeeddoInternal.instance.storageHelper.getUserName()
        if (currentName.isNullOrBlank()) {
            showNameDialog = true
        }
    }

    LaunchedEffect(Unit) {
        FeeddoInternal.instance.notifications.collect {
            loadHomeData()
        }
    }

    if (showNotificationsSheet) {
        FeeddoNotificationsSheet(
            theme = theme,
            onDismiss = {
                showNotificationsSheet = false
                coroutineScope.launch { loadHomeData() }
            },
            onNotificationTap = { notification ->
                showNotificationsSheet = false
                val data = notification.data
                if (data != null) {
                    val type = data["type"] as? String
                    val taskId = data["taskId"] as? String
                    if (type in listOf("task_released", "task_status_changed", "task_comment", "task_created") && taskId != null) {
                        onTaskTap(taskId)
                    } else {
                        coroutineScope.launch {
                            Feeddo.handleNotificationTap(context, data, theme)
                        }
                    }
                }
            }
        )
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { /* Non-dismissible matching Flutter barrierDismissible: false */ },
            containerColor = theme.colors.surface,
            title = {
                Text(
                    text = "Welcome!",
                    color = theme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Please enter your name to get started",
                        color = theme.colors.textSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        placeholder = { Text("Your name", color = theme.colors.textSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.colors.textPrimary,
                            unfocusedTextColor = theme.colors.textPrimary,
                            focusedContainerColor = theme.colors.background,
                            unfocusedContainerColor = theme.colors.background,
                            focusedBorderColor = theme.colors.primary,
                            unfocusedBorderColor = theme.colors.border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = inputName.trim()
                        if (name.isNotEmpty()) {
                            showNameDialog = false
                            coroutineScope.launch {
                                FeeddoInternal.instance.storageHelper.saveUserData(userName = name)
                                val externalUserId = FeeddoInternal.instance.storageHelper.getExternalUserId()
                                val email = FeeddoInternal.instance.storageHelper.getEmail()
                                val userSegment = FeeddoInternal.instance.storageHelper.getUserSegment()
                                val subscriptionStatus = FeeddoInternal.instance.storageHelper.getSubscriptionStatus()
                                val customAttributes = FeeddoInternal.instance.storageHelper.getCustomAttributes()

                                FeeddoInternal.instance.updateUser(
                                    externalUserId = externalUserId,
                                    userName = name,
                                    email = email,
                                    userSegment = userSegment,
                                    subscriptionStatus = subscriptionStatus,
                                    customAttributes = customAttributes
                                )
                            }
                        }
                    }
                ) {
                    Text("Continue", color = theme.colors.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    val onPrimaryColor = if (theme.isDark) Color.Black else Color.White

    Scaffold(
        containerColor = theme.colors.background,
        floatingActionButton = {
            if (hasRecentActivity) {
                FloatingActionButton(
                    onClick = onSendMessageTap,
                    containerColor = theme.colors.primary,
                    contentColor = onPrimaryColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New Message",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                // Greeting Section
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Hi there 👋",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = theme.colors.textPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.0).sp,
                        lineHeight = 38.sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "How can we help you today?",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = theme.colors.textSecondary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.2).sp
                    )
                )
                Spacer(modifier = Modifier.height(36.dp))

                // Primary Action Card (Only if !hasRecentActivity)
                if (!hasRecentActivity) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSendMessageTap() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = theme.colors.primary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(onPrimaryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = onPrimaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Send us a message",
                                    color = onPrimaryColor,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "We're here to help",
                                    color = onPrimaryColor.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = (-0.1).sp
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = onPrimaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Quick Actions Section
                Text(
                    text = "Quick Actions",
                    color = theme.colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                HomeMenuCard(
                    title = "Messages",
                    subtitle = "View your conversations",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    color = Color(0xFF2196F3),
                    badgeCount = unreadMessageCount,
                    theme = theme,
                    onTap = onMessagesTap
                )

                Spacer(modifier = Modifier.height(14.dp))

                HomeMenuCard(
                    title = "Community Board",
                    subtitle = "Suggest features or report bugs",
                    icon = Icons.Default.Lightbulb,
                    color = Color(0xFFFFB300),
                    theme = theme,
                    onTap = onCommunityBoardTap
                )

                Spacer(modifier = Modifier.height(14.dp))

                HomeMenuCard(
                    title = "Tickets",
                    subtitle = "Track your support tickets",
                    icon = Icons.Default.ConfirmationNumber,
                    color = Color(0xFF9C27B0),
                    theme = theme,
                    onTap = onTicketsTap
                )

                // Recent Activity Section
                if (hasRecentActivity) {
                    Spacer(modifier = Modifier.height(36.dp))
                    Text(
                        text = "Recent Activity",
                        color = theme.colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (displayConversation != null) {
                        HomeConversationCard(
                            conversation = displayConversation,
                            theme = theme,
                            onTap = { onConversationTap(displayConversation) }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    homeData?.recentTicket?.let { ticket ->
                        TicketCard(
                            ticket = ticket,
                            theme = theme,
                            onTap = { onTicketTap(ticket.id) }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    homeData?.recentTask?.let { task ->
                        TaskCard(
                            task = task,
                            theme = theme,
                            onTap = { onTaskTap(task.id) }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Powered by
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Powered by ",
                        color = theme.colors.textSecondary.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        letterSpacing = (-0.1).sp
                    )
                    Text(
                        text = FeeddoInternal.instance.chatBotName,
                        color = theme.colors.textSecondary.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.1).sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Top Buttons: Notifications & Close
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Notifications button
                val notificationCount = homeData?.unreadNotificationCount ?: 0
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.colors.cardBackground)
                        .border(1.dp, theme.colors.divider.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { showNotificationsSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = theme.colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    if (notificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Red),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (notificationCount > 99) "99+" else "$notificationCount",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Close Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.colors.cardBackground)
                        .border(1.dp, theme.colors.divider.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = theme.colors.closeButtonColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    theme: FeeddoTheme,
    onTap: () -> Unit,
    badgeCount: Int? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onTap() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = theme.colors.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.colors.divider.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = theme.colors.cardText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    if (badgeCount != null && badgeCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Red)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (badgeCount > 99) "99+" else "$badgeCount",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = theme.colors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.1).sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = theme.colors.textSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun HomeConversationCard(
    conversation: Conversation,
    theme: FeeddoTheme,
    onTap: () -> Unit
) {
    val timeAgo = remember(conversation.lastMessageAt) {
        formatTimeAgo(conversation.lastMessageAt)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onTap() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = theme.colors.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.colors.divider.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2196F3).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conversation.displayName ?: conversation.title ?: "Conversation",
                            color = theme.colors.cardText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (conversation.unreadMessages > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Red)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (conversation.unreadMessages > 99) "99+" else "${conversation.unreadMessages}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = timeAgo,
                        color = theme.colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = theme.colors.textSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }

            if (!conversation.lastMessagePreview.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = conversation.lastMessagePreview!!,
                    color = theme.colors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp,
                    letterSpacing = (-0.1).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        hours < 1 -> "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
        days < 1 -> "$hours ${if (hours == 1L) "hour" else "hours"} ago"
        days < 7 -> "$days ${if (days == 1L) "day" else "days"} ago"
        else -> {
            val weeks = days / 7
            "$weeks ${if (weeks == 1L) "week" else "weeks"} ago"
        }
    }
}
