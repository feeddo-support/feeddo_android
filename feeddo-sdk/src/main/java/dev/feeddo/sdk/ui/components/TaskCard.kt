package dev.feeddo.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import dev.feeddo.sdk.models.Task
import dev.feeddo.sdk.theme.FeeddoTheme

@Composable
fun TaskCard(
    task: Task,
    theme: FeeddoTheme,
    onTap: () -> Unit,
    onVote: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUpvoted = task.myVote == "up"

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
                // Type badge (Feature / Bug)
                val isFeature = task.type.equals("feature", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isFeature) Color(0xFF6366F1).copy(alpha = 0.15f)
                            else Color(0xFFEF4444).copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isFeature) "Feature" else "Bug",
                        color = if (isFeature) Color(0xFF818CF8) else Color(0xFFF87171),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(theme.colors.border.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = task.safeStatus.replaceFirstChar { it.uppercase() },
                        color = theme.colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = task.title,
                color = theme.colors.cardText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (task.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    color = theme.colors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions row: Upvote & Comment counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Upvote button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isUpvoted) theme.colors.primary.copy(alpha = 0.2f)
                            else theme.colors.border.copy(alpha = 0.4f)
                        )
                        .border(
                            1.dp,
                            if (isUpvoted) theme.colors.primary else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = onVote != null) {
                            onVote?.invoke(if (isUpvoted) "none" else "up")
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Upvote",
                            tint = if (isUpvoted) theme.colors.primary else theme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${task.upvoteCount}",
                            color = if (isUpvoted) theme.colors.primary else theme.colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Comments count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = theme.colors.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${task.commentCount}",
                        color = theme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

