package dev.feeddo.sdk.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.feeddo.sdk.theme.FeeddoTheme

@Composable
fun RatingWidget(
    currentRating: Int = 0,
    readOnly: Boolean = false,
    onRate: (Int) -> Unit = {},
    theme: FeeddoTheme? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { starIndex ->
            val isSelected = starIndex <= currentRating
            Icon(
                imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.StarOutline,
                contentDescription = "$starIndex stars",
                tint = Color(0xFFFFB300),
                modifier = Modifier
                    .size(28.dp)
                    .clickable(enabled = !readOnly) {
                        onRate(starIndex)
                    }
            )
        }
    }
}
