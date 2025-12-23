package sophon.desktop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sophon.desktop.ui.theme.Dimens

@Composable
fun DefaultListItem(
    title: String,
    description: String = "",
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    trailingContent: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .clickable(onClick = onClick, indication = null, interactionSource = null),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF1A1A1A)
            )
            if (description.isNotBlank()) {
                Spacer(Modifier.height(Dimens.spacerSmall))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))

        trailingContent()
    }
}

@Composable
fun TrailingTextListItem(
    title: String,
    description: String = "",
    actionText: String = "",
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit = {},
) {
    DefaultListItem(
        title = title,
        description = description,
        icon = icon,
        modifier = modifier,
        onClick = onClick,
        trailingContent = {
            TextButton(onClick = onClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
fun SwitchListItem(
    title: String,
    description: String = "",
    checked: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    DefaultListItem(
        title = title,
        description = description,
        icon = icon,
        modifier = modifier,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color(0xFFBDBDBD),
                    uncheckedTrackColor = Color(0xFFE0E0E0)
                )
            )
        }
    )
}