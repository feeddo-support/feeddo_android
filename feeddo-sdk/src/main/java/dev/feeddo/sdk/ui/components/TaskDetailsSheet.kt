package dev.feeddo.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.feeddo.sdk.FeeddoInternal
import dev.feeddo.sdk.models.Task
import dev.feeddo.sdk.models.TaskComment
import dev.feeddo.sdk.theme.FeeddoTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsSheet(
    taskId: String,
    theme: FeeddoTheme,
    onDismiss: () -> Unit,
    onNavigateToConversation: ((String) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var task by remember { mutableStateOf<Task?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    var isSubmittingComment by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        try {
            isLoading = true
            val loaded = FeeddoInternal.instance.apiService.getTask(
                taskId = taskId,
                userId = FeeddoInternal.instance.cachedUserId
            )
            task = loaded
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.colors.background,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(theme.colors.textSecondary.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Feedback Details",
                    color = theme.colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = theme.colors.closeButtonColor
                    )
                }
            }

            HorizontalDivider(color = theme.colors.divider)

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = theme.colors.primary)
                }
            } else if (task != null) {
                val currentTask = task!!
                val isUpvoted = currentTask.myVote == "up"

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp)
                ) {
                    item {
                        // Badges row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isFeature = currentTask.type.equals("feature", ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isFeature) Color(0xFF6366F1).copy(alpha = 0.15f)
                                        else Color(0xFFEF4444).copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isFeature) "Feature" else "Bug",
                                    color = if (isFeature) Color(0xFF818CF8) else Color(0xFFF87171),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(theme.colors.border.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = currentTask.safeStatus.replaceFirstChar { it.uppercase() },
                                    color = theme.colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Title
                        Text(
                            text = currentTask.title,
                            color = theme.colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Description
                        Text(
                            text = currentTask.description,
                            color = theme.colors.textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Upvote Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isUpvoted) theme.colors.primary.copy(alpha = 0.2f)
                                    else theme.colors.cardBackground
                                )
                                .border(
                                    1.dp,
                                    if (isUpvoted) theme.colors.primary else theme.colors.border,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        try {
                                            val newVote = if (isUpvoted) "none" else "up"
                                            FeeddoInternal.instance.apiService.voteTask(
                                                taskId = currentTask.id,
                                                voteType = newVote,
                                                userId = FeeddoInternal.instance.cachedUserId
                                            )
                                            task = currentTask.copy(
                                                myVote = if (newVote == "none") null else "up",
                                                upvoteCount = currentTask.upvoteCount + if (newVote == "up") 1 else -1
                                            )
                                        } catch (_: Exception) {}
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Upvote",
                                    tint = if (isUpvoted) theme.colors.primary else theme.colors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Upvote (${currentTask.upvoteCount})",
                                    color = if (isUpvoted) theme.colors.primary else theme.colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Linked conversation link if any
                        if (!currentTask.conversationId.isNullOrEmpty() && onNavigateToConversation != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "💬 View Related Conversation",
                                color = theme.colors.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clickable { onNavigateToConversation(currentTask.conversationId) }
                                    .padding(vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = theme.colors.divider)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Comments header
                        Text(
                            text = "Comments (${currentTask.safeComments.size})",
                            color = theme.colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (currentTask.safeComments.isEmpty()) {
                        item {
                            Text(
                                text = "No comments yet. Start the conversation!",
                                color = theme.colors.textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(currentTask.safeComments) { comment ->
                            CommentItem(comment = comment, theme = theme)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }

                // Add comment input box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(theme.colors.cardBackground)
                            .border(1.dp, theme.colors.border, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (commentText.isEmpty()) {
                            Text(
                                text = "Leave a comment...",
                                color = theme.colors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            textStyle = TextStyle(
                                color = theme.colors.textPrimary,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(theme.colors.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val canSubmit = commentText.isNotBlank() && !isSubmittingComment
                    IconButton(
                        onClick = {
                            if (canSubmit) {
                                isSubmittingComment = true
                                val text = commentText.trim()
                                coroutineScope.launch {
                                    try {
                                        val added = FeeddoInternal.instance.apiService.addTaskComment(
                                            taskId = currentTask.id,
                                            content = text,
                                            userId = FeeddoInternal.instance.cachedUserId
                                        )
                                        commentText = ""
                                        task = currentTask.copy(
                                            comments = currentTask.safeComments + added,
                                            commentCount = currentTask.commentCount + 1
                                        )
                                    } catch (_: Exception) {
                                    } finally {
                                        isSubmittingComment = false
                                    }
                                }
                            }
                        },
                        enabled = canSubmit,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (canSubmit) theme.colors.primary else theme.colors.border.copy(alpha = 0.5f))
                    ) {
                        if (isSubmittingComment) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = theme.colors.background,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (canSubmit) theme.colors.background else theme.colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: TaskComment, theme: FeeddoTheme) {
    val author = comment.authorName ?: if (comment.userType == "agent") "Team" else "User"
    val formattedTime = if (comment.createdAt > 0L) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(comment.createdAt))
    } else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = theme.colors.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.colors.border)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = author,
                    color = theme.colors.cardText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formattedTime,
                    color = theme.colors.textSecondary,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.content,
                color = theme.colors.textPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

