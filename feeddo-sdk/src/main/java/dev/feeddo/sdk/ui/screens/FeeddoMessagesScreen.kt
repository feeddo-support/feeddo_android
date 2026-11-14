package dev.feeddo.sdk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.feeddo.sdk.FeeddoInternal
import dev.feeddo.sdk.models.Conversation
import dev.feeddo.sdk.theme.FeeddoTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeddoMessagesScreen(
    theme: FeeddoTheme,
    onBack: () -> Unit,
    onConversationTap: (Conversation) -> Unit,
    onNewConversation: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val conversations by FeeddoInternal.instance.conversationService.conversations.collectAsState()
    val isLoading by FeeddoInternal.instance.conversationService.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        FeeddoInternal.instance.ensureUserInitialized()
        val userId = FeeddoInternal.instance.cachedUserId
        if (userId != null) {
            FeeddoInternal.instance.conversationService.loadConversations(userId)
        }
    }

    val onPrimaryColor = if (theme.isDark) Color.Black else Color.White

    Scaffold(
        containerColor = theme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Messages",
                        color = theme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.colors.iconColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.colors.appBarBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewConversation,
                containerColor = theme.colors.primary,
                contentColor = onPrimaryColor,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "New Message",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading && conversations.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = theme.colors.primary
                )
            } else if (conversations.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = theme.colors.textSecondary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No messages yet",
                        color = theme.colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Start a conversation to get instant help from our team.",
                        color = theme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onNewConversation,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.colors.primary,
                            contentColor = onPrimaryColor
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Send a message", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        ConversationListItem(
                            conversation = conv,
                            theme = theme,
                            onTap = { onConversationTap(conv) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationListItem(
    conversation: Conversation,
    theme: FeeddoTheme,
    onTap: () -> Unit
) {
    val formattedTime = remember(conversation.lastMessageAt) {
        formatDate(conversation.lastMessageAt)
    }

    val name = conversation.displayName ?: conversation.title ?: "Support"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onTap() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = theme.colors.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(theme.colors.surface)
                    .border(1.dp, theme.colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    color = theme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        color = theme.colors.cardText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedTime,
                        color = theme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessagePreview ?: "No messages",
                        color = theme.colors.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadMessages > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Red),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conversation.unreadMessages > 99) "99+" else "${conversation.unreadMessages}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (conversation.status == "resolved" || conversation.hasTicket || conversation.hasTask) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (conversation.status == "resolved") {
                            StatusChip(
                                label = "Resolved",
                                color = theme.colors.success,
                                icon = Icons.Default.CheckCircleOutline
                            )
                        }
                        if (conversation.hasTicket) {
                            StatusChip(
                                label = "Ticket: ${conversation.ticketStatus ?: "Open"}",
                                color = Color(0xFF2196F3),
                                icon = Icons.Default.ConfirmationNumber
                            )
                        }
                        if (conversation.hasTask) {
                            StatusChip(
                                label = "Task",
                                color = Color(0xFFFF9800),
                                icon = Icons.Default.TaskAlt
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    color: Color,
    icon: ImageVector
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { time = date }

    return if (now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
    ) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        val diffDays = (now.timeInMillis - timestamp) / (1000 * 60 * 60 * 24)
        if (diffDays < 7) {
            SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        } else {
            SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(date)
        }
    }
}
