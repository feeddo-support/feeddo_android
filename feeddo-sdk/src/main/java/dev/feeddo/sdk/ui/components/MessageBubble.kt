package dev.feeddo.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.feeddo.sdk.models.Message
import dev.feeddo.sdk.theme.FeeddoTheme
import java.util.*
import java.util.regex.Pattern

private val JSON_BLOCK_PATTERN = Pattern.compile("```json\\s*(\\{[\\s\\S]*?\\})\\s*```")

data class ParsedMessageContent(
    val content: String,
    val taskId: String? = null,
    val ticketId: String? = null
)

fun parseMessageContent(rawContent: String?): ParsedMessageContent {
    if (rawContent.isNullOrBlank()) return ParsedMessageContent(content = "")

    val matcher = JSON_BLOCK_PATTERN.matcher(rawContent)
    if (matcher.find()) {
        val jsonStr = matcher.group(1)
        val cleanText = matcher.replaceAll("").trim()

        try {
            val json = com.google.gson.Gson().fromJson(jsonStr, com.google.gson.JsonObject::class.java)
            val taskId = json.get("taskId")?.asString
            val ticketId = json.get("ticketId")?.asString

            if (taskId != null || ticketId != null) {
                return ParsedMessageContent(content = cleanText, taskId = taskId, ticketId = ticketId)
            }
        } catch (_: Exception) {}
    }

    return ParsedMessageContent(content = rawContent)
}

@Composable
fun MessageBubble(
    message: Message,
    theme: FeeddoTheme,
    taskCard: (@Composable () -> Unit)? = null,
    ticketCard: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser
    val parsedContent = remember(message.content) {
        parseMessageContent(message.content)
    }

    val formattedTime = remember(message.createdAt) {
        formatMessageTime(message.createdAt)
    }

    val richContent = remember(parsedContent.content) {
        buildRichText(parsedContent.content)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp,
                        topStart = if (isUser) 16.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) theme.colors.primary else theme.colors.surface
                )
                .border(
                    width = if (isUser) 0.dp else 1.dp,
                    color = theme.colors.border,
                    shape = RoundedCornerShape(
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp,
                        topStart = if (isUser) 16.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                // Assistant header inside bubble
                if (!isUser) {
                    val isHuman = message.role == "human"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isHuman) Color(0xFF6451F4) else theme.colors.primary.copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isHuman) Icons.Default.Code else Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = if (isHuman) Color.White else theme.colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message.displayName ?: "Feeddo",
                            color = theme.colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Text body with bold markdown support
                if (parsedContent.content.isNotEmpty()) {
                    Text(
                        text = richContent,
                        color = if (isUser) {
                            if (theme.isDark) Color.Black else Color.White
                        } else {
                            theme.colors.textPrimary
                        },
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }

                // Attachments preview
                message.attachments?.let { att ->
                    MessageAttachments(
                        attachments = att,
                        theme = theme,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Embedded Task card
                if (taskCard != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    taskCard()
                }

                // Embedded Ticket card
                if (ticketCard != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ticketCard()
                }

                // Timestamp
                if (formattedTime.isNotEmpty()) {
                    Text(
                        text = formattedTime,
                        color = if (isUser) {
                            if (theme.isDark) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
                        } else {
                            theme.colors.textSecondary
                        },
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageAttachments(
    attachments: Any,
    theme: FeeddoTheme,
    modifier: Modifier = Modifier
) {
    val urls = remember(attachments) {
        val list = mutableListOf<String>()
        try {
            if (attachments is List<*>) {
                for (item in attachments) {
                    if (item is Map<*, *>) {
                        val url = item["url"] as? String
                        if (!url.isNullOrEmpty()) list.add(url)
                    } else if (item is String && item.startsWith("http")) {
                        list.add(item)
                    }
                }
            }
        } catch (_: Exception) {}
        list
    }

    if (urls.isNotEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            urls.forEach { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Attachment",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, theme.colors.border, RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

private fun buildRichText(text: String): AnnotatedString {
    val parts = text.split("**")
    if (parts.size <= 1) return AnnotatedString(text)

    return buildAnnotatedString {
        for (i in parts.indices) {
            if (i % 2 == 0) {
                append(parts[i])
            } else {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(parts[i])
                }
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }

    val hour = String.format(Locale.getDefault(), "%02d", msgCal.get(Calendar.HOUR_OF_DAY))
    val minute = String.format(Locale.getDefault(), "%02d", msgCal.get(Calendar.MINUTE))
    val timeStr = "$hour:$minute"

    val isSameDay = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
    if (isSameDay) return timeStr

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
    if (isYesterday) return "Yesterday, $timeStr"

    val daysDiff = ((now.timeInMillis - msgCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    if (daysDiff in 1..6) {
        val dayName = when (msgCal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "Sun"
        }
        return "$dayName, $timeStr"
    }

    val day = msgCal.get(Calendar.DAY_OF_MONTH)
    val month = msgCal.get(Calendar.MONTH) + 1
    val year = msgCal.get(Calendar.YEAR)
    return "$day/$month/$year, $timeStr"
}
