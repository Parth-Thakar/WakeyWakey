package com.z.wakey.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.z.wakey.ui.components.AlarmCard
import com.z.wakey.ui.theme.*
import com.z.wakey.viewmodel.AlarmViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: AlarmViewModel,
    onAddAlarm: () -> Unit,
    onEditAlarm: (Int) -> Unit
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var currentAmPm by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val minute = now.get(Calendar.MINUTE)
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            currentTime = "%d:%02d".format(h, minute)
            currentAmPm = if (hour < 12) "AM" else "PM"
            currentDate = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now.time)
            delay(1000)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                ClockHeader(currentTime, currentAmPm, currentDate)
            }

            item {
                AlarmListHeader(
                    activeCount = alarms.count { it.isEnabled },
                    total = alarms.size
                )
            }

            if (alarms.isEmpty()) {
                item { EmptyState() }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
                    ) {
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { enabled -> viewModel.toggleAlarm(alarm, enabled) },
                            onEdit = { onEditAlarm(alarm.id) },
                            onDelete = { viewModel.deleteAlarm(alarm) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddAlarm,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            shape = CircleShape,
            containerColor = PurplePrimary,
            contentColor = TextWhite,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add alarm",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ClockHeader(time: String, amPm: String, date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkSurfaceVariant, DeepBlack),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
            .padding(top = 48.dp, bottom = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "WakeyWakey",
                style = MaterialTheme.typography.labelLarge,
                color = PurplePrimary,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.displayLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = amPm,
                    style = MaterialTheme.typography.headlineMedium,
                    color = PurpleLight,
                    modifier = Modifier.padding(start = 8.dp, bottom = 14.dp)
                )
            }

            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, PurplePrimary, Color.Transparent)
                        )
                    )
            )
        }
    }
}

@Composable
private fun AlarmListHeader(activeCount: Int, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Alarms",
            style = MaterialTheme.typography.headlineSmall,
            color = TextWhite
        )
        Spacer(modifier = Modifier.weight(1f))
        if (total > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PurpleDeep.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$activeCount active",
                    style = MaterialTheme.typography.labelMedium,
                    color = PurpleLight
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "⏰", fontSize = 48.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No alarms yet",
            style = MaterialTheme.typography.headlineMedium,
            color = TextWhite
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap + to set your first alarm",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center
        )
    }
}
