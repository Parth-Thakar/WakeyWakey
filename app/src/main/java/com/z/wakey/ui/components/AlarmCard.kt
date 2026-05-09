package com.z.wakey.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z.wakey.data.Alarm
import com.z.wakey.ui.theme.*

@Composable
fun AlarmCard(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (alarm.isEnabled) PurplePrimary.copy(alpha = 0.4f) else BorderColor,
        animationSpec = tween(300),
        label = "border"
    )
    val timeColor by animateColorAsState(
        targetValue = if (alarm.isEnabled) TextWhite else TextMuted,
        animationSpec = tween(300),
        label = "time"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onEdit() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatTime(alarm.hour, alarm.minute),
                        style = MaterialTheme.typography.displaySmall,
                        color = timeColor,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (alarm.hour < 12) "AM" else "PM",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (alarm.isEnabled) PurpleLight else TextMuted,
                        modifier = Modifier.padding(bottom = 7.dp)
                    )
                }

                if (alarm.label.isNotEmpty()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (alarm.isEnabled) TextGray else TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                val repeatLabel = getRepeatLabel(alarm.repeatDays)
                if (repeatLabel.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    RepeatChips(repeatLabel)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextWhite,
                        checkedTrackColor = PurplePrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant,
                        uncheckedBorderColor = BorderColor
                    )
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RepeatChips(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PurpleDeep.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PurpleLight,
            letterSpacing = 0.5.sp
        )
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return "%d:%02d".format(h, minute)
}

private fun getRepeatLabel(repeatDays: String): String {
    if (repeatDays.isBlank()) return "Once"
    val days = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    return when {
        days == setOf(1, 2, 3, 4, 5, 6, 7) -> "Every day"
        days == setOf(1, 2, 3, 4, 5) -> "Weekdays"
        days == setOf(6, 7) -> "Weekends"
        else -> days.sorted().joinToString(" · ") { dayAbbr(it) }
    }
}

private fun dayAbbr(day: Int) = when (day) {
    1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"
    5 -> "Fri"; 6 -> "Sat"; 7 -> "Sun"; else -> ""
}
