package dev.feeddo.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.feeddo.sdk.models.Ticket
import dev.feeddo.sdk.theme.FeeddoTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TicketCard(
    ticket: Ticket,
    theme: FeeddoTheme,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        ticket.isResolved -> Color(0xFF10B981)
        ticket.priority.equals("high", ignoreCase = true) -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B)
    }

    val statusLabel = if (ticket.isResolved) "Resolved" else "Open"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onTap() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.colors.cardBackground
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Priority / status indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Date
                val formattedDate = rememberFormattedDate(ticket.createdAt)
                Text(
                    text = formattedDate,
                    color = theme.colors.textSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = ticket.title,
                color = theme.colors.cardText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (ticket.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ticket.description,
                    color = theme.colors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun rememberFormattedDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
}

