package dev.feeddo.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.feeddo.sdk.theme.FeeddoTheme

@Composable
fun ChatInputArea(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachTap: (() -> Unit)? = null,
    isSending: Boolean = false,
    hintText: String = "Type a message...",
    theme: FeeddoTheme,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(theme.colors.surface)
            .border(
                width = 1.dp,
                color = theme.colors.border
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Input container with attach icon and text field
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(theme.colors.background)
                    .border(1.dp, theme.colors.border, RoundedCornerShape(24.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onAttachTap != null) {
                    IconButton(
                        onClick = onAttachTap,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach",
                            tint = theme.colors.iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    if (messageText.isEmpty()) {
                        Text(
                            text = hintText,
                            color = theme.colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = messageText,
                        onValueChange = onMessageChange,
                        textStyle = TextStyle(
                            color = theme.colors.textPrimary,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(theme.colors.primary),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Circular Send Button with Upward Arrow
            val canSend = messageText.isNotBlank() && !isSending
            IconButton(
                onClick = { if (canSend) onSend() },
                enabled = canSend,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) theme.colors.primary else theme.colors.primary.copy(alpha = 0.5f)
                    )
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = theme.colors.surface,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = theme.colors.surface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
