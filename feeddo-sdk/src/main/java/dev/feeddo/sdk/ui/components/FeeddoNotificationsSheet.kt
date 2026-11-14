package dev.feeddo.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.feeddo.sdk.FeeddoInternal
import dev.feeddo.sdk.models.FeeddoNotification
import dev.feeddo.sdk.theme.FeeddoTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeddoNotificationsSheet(
    theme: FeeddoTheme,
    onDismiss: () -> Unit,
    onNotificationTap: ((FeeddoNotification) -> Unit)? = null
) {
    var notifications by remember { mutableStateOf<List<FeeddoNotification>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            val userId = FeeddoInternal.instance.cachedUserId
            if (userId != null) {
                val list = FeeddoInternal.instance.apiService.getNotifications(userId)
                notifications = list
            }
        } catch (_: Exception) {
            notifications = emptyList()
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
                .fillMaxHeight(0.8f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Notifications",
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
            } else if (notifications.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = theme.colors.textSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No notifications yet",
                            color = theme.colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    items(notifications!!) { notification ->
                        NotificationItemRow(
                            notification = notification,
                            theme = theme,
                            onTap = {
                                onDismiss()
                                onNotificationTap?.invoke(notification)
                            }
                        )
                        HorizontalDivider(
                            color = theme.colors.divider.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemRow(
    notification: FeeddoNotification,
    theme: FeeddoTheme,
    onTap: () -> Unit
) {
    val formattedTime = if (notification.createdAt > 0L) {
        val date = Date(notification.createdAt)
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
    } else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onTap() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, end = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(theme.colors.primary)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title ?: "Notification",
                    color = theme.colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                    text = formattedTime,
                    color = theme.colors.textSecondary,
                    fontSize = 11.sp
                )
            }

            notification.body?.let { body ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    color = theme.colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

