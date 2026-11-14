package dev.feeddo.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.feeddo.sdk.FeeddoInternal
import dev.feeddo.sdk.models.Ticket
import dev.feeddo.sdk.theme.FeeddoTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailsSheet(
    ticketId: String,
    theme: FeeddoTheme,
    onDismiss: () -> Unit,
    onNavigateToConversation: ((String) -> Unit)? = null
) {
    var ticket by remember { mutableStateOf<Ticket?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(ticketId) {
        try {
            isLoading = true
            val loaded = FeeddoInternal.instance.apiService.getTicket(
                ticketId = ticketId,
                userId = FeeddoInternal.instance.cachedUserId
            )
            ticket = loaded
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
                .fillMaxHeight(0.75f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ticket Details",
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
            } else if (ticket != null) {
                val currentTicket = ticket!!
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(vertical = 16.dp)
                ) {
                    // Status & Priority Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statusColor = if (currentTicket.isResolved) Color(0xFF10B981) else Color(0xFFF59E0B)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (currentTicket.isResolved) "Resolved" else "Open",
                                color = statusColor,
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
                                text = "Priority: ${currentTicket.priority.replaceFirstChar { it.uppercase() }}",
                                color = theme.colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title
                    Text(
                        text = currentTicket.title,
                        color = theme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val createdDate = if (currentTicket.createdAt > 0L) {
                        SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                            .format(Date(currentTicket.createdAt))
                    } else ""

                    if (createdDate.isNotEmpty()) {
                        Text(
                            text = "Created $createdDate",
                            color = theme.colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = theme.colors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.colors.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Description",
                                color = theme.colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentTicket.description,
                                color = theme.colors.cardText,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Conversation Jump
                    if (!currentTicket.conversationId.isNullOrEmpty() && onNavigateToConversation != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onNavigateToConversation(currentTicket.conversationId) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.colors.primary,
                                contentColor = theme.colors.background
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Open Related Conversation",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

