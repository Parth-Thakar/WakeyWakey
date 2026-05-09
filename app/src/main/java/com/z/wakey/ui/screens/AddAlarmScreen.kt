package com.z.wakey.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.z.wakey.data.Alarm
import com.z.wakey.data.DismissType
import com.z.wakey.ui.theme.*
import com.z.wakey.utils.HandModelProvider
import com.z.wakey.viewmodel.AlarmViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmScreen(
    viewModel: AlarmViewModel,
    alarmId: Int?,
    onDone: () -> Unit
) {
    val existing = remember(alarmId) { alarmId?.let { viewModel.getAlarmById(it) } }
    val now = Calendar.getInstance()

    var hour by remember { mutableIntStateOf(existing?.hour ?: now.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(existing?.minute ?: now.get(Calendar.MINUTE)) }
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var selectedDays by remember {
        mutableStateOf(
            existing?.repeatDays
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.toSet()
                ?: emptySet()
        )
    }
    var ringtonePath by remember { mutableStateOf(existing?.ringtonePath ?: "") }
    var ringtoneLabel by remember { mutableStateOf("Default Alarm") }
    var mathCount by remember { mutableIntStateOf(existing?.mathQuestionCount ?: 5) }
    var fingerRounds by remember { mutableIntStateOf(existing?.fingerRoundCount ?: 5) }
    var dismissType by remember { mutableStateOf(existing?.dismissType ?: DismissType.MATH) }
    var vibrate by remember { mutableStateOf(existing?.isVibrate ?: true) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var modelDownloading by remember { mutableStateOf(false) }
    var modelProgress by remember { mutableIntStateOf(0) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            dismissType = DismissType.FINGERS
            if (!HandModelProvider.isModelReady(context)) {
                modelDownloading = true
                scope.launch {
                    HandModelProvider.ensureModel(context) { p -> modelProgress = p }
                    modelDownloading = false
                }
            }
        }
    }

    val timePickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = false
    )

    LaunchedEffect(ringtonePath) {
        if (ringtonePath.isNotEmpty()) {
            runCatching {
                val ringtone = RingtoneManager.getRingtone(context, Uri.parse(ringtonePath))
                ringtoneLabel = ringtone?.getTitle(context) ?: "Custom"
            }
        }
    }

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            ringtonePath = uri?.toString() ?: ""
            if (ringtonePath.isEmpty()) ringtoneLabel = "Default Alarm"
        }
    }

    fun launchRingtonePicker() {
        val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            if (ringtonePath.isNotEmpty()) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(ringtonePath))
            }
        }
        ringtonePicker.launch(intent)
    }

    fun save() {
        val daysStr = selectedDays.sorted().joinToString(",")
        val alarm = Alarm(
            id = existing?.id ?: 0,
            hour = timePickerState.hour,
            minute = timePickerState.minute,
            label = label.trim(),
            isEnabled = true,
            repeatDays = daysStr,
            ringtonePath = ringtonePath,
            mathQuestionCount = mathCount,
            isVibrate = vibrate,
            dismissType = dismissType,
            fingerRoundCount = fingerRounds
        )
        if (existing != null) viewModel.updateAlarm(alarm)
        else viewModel.addAlarm(alarm)
        onDone()
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TopBar(
                title = if (existing == null) "New Alarm" else "Edit Alarm",
                onBack = onDone
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Time Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.padding(vertical = 8.dp),
                    colors = TimePickerDefaults.colors(
                        clockDialColor = DarkSurfaceVariant,
                        clockDialSelectedContentColor = TextWhite,
                        clockDialUnselectedContentColor = TextGray,
                        selectorColor = PurplePrimary,
                        containerColor = DarkSurface,
                        periodSelectorBorderColor = BorderColor,
                        timeSelectorSelectedContainerColor = PurpleDeep,
                        timeSelectorUnselectedContainerColor = DarkSurfaceVariant,
                        timeSelectorSelectedContentColor = TextWhite,
                        timeSelectorUnselectedContentColor = TextGray,
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Label
            SectionCard {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (optional)", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = BorderColor,
                        cursorColor = PurplePrimary,
                        focusedLabelColor = PurpleLight,
                        unfocusedLabelColor = TextGray,
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Repeat
            SectionCard {
                SectionLabel("Repeat")
                Spacer(modifier = Modifier.height(12.dp))
                RepeatPresets(selectedDays, onPreset = { selectedDays = it })
                Spacer(modifier = Modifier.height(12.dp))
                DaySelector(selectedDays = selectedDays, onToggle = { day ->
                    selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                })
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ringtone
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { launchRingtonePicker() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PurpleDeep.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = PurpleLight, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("Ringtone")
                        Text(ringtoneLabel, style = MaterialTheme.typography.bodyMedium, color = TextGray)
                    }
                    Text("›", color = TextGray, fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dismiss method + Vibrate
            SectionCard {
                SectionLabel("Dismiss Method")
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DismissOption(
                        icon = Icons.Default.Calculate,
                        title = "Math",
                        subtitle = "Solve questions",
                        selected = dismissType == DismissType.MATH,
                        modifier = Modifier.weight(1f),
                        onClick = { dismissType = DismissType.MATH }
                    )
                    DismissOption(
                        icon = Icons.Default.PanTool,
                        title = "Fingers",
                        subtitle = "Show fingers to camera",
                        selected = dismissType == DismissType.FINGERS,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (hasCameraPermission) {
                                dismissType = DismissType.FINGERS
                                if (!HandModelProvider.isModelReady(context)) {
                                    modelDownloading = true
                                    scope.launch {
                                        HandModelProvider.ensureModel(context) { p -> modelProgress = p }
                                        modelDownloading = false
                                    }
                                }
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (dismissType == DismissType.MATH) {
                    Text(
                        text = "Solve math questions to dismiss",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(5, 10).forEach { count ->
                            val selected = mathCount == count
                            ChipBox(
                                text = "$count Questions",
                                selected = selected,
                                onClick = { mathCount = count }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Show the right number of fingers to the back camera. Make sure the room is well-lit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(3, 5, 7).forEach { count ->
                            val selected = fingerRounds == count
                            ChipBox(
                                text = "$count Rounds",
                                selected = selected,
                                onClick = { fingerRounds = count }
                            )
                        }
                    }
                    if (modelDownloading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PurpleLight,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Downloading hand model… $modelProgress%",
                                color = TextGray,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    } else if (!hasCameraPermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Camera permission required",
                            color = ErrorRed,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("Vibrate")
                        Text("Phone vibrates with alarm", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                    }
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = PurplePrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceVariant,
                            uncheckedBorderColor = BorderColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save button
            Button(
                onClick = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurplePrimary,
                    contentColor = TextWhite
                )
            ) {
                Text(
                    text = if (existing == null) "Set Alarm" else "Update Alarm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextWhite,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = PurpleLight,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun RepeatPresets(selected: Set<Int>, onPreset: (Set<Int>) -> Unit) {
    val presets = listOf(
        "None" to emptySet(),
        "Daily" to setOf(1, 2, 3, 4, 5, 6, 7),
        "Weekdays" to setOf(1, 2, 3, 4, 5),
        "Weekends" to setOf(6, 7)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { (name, days) ->
            val active = selected == days
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) PurpleDeep.copy(alpha = 0.6f) else DarkSurfaceVariant)
                    .border(
                        BorderStroke(1.dp, if (active) PurplePrimary else BorderColor),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onPreset(days) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) PurpleLight else TextGray
                )
            }
        }
    }
}

@Composable
private fun DismissOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) PurpleDeep.copy(alpha = 0.6f) else DarkSurfaceVariant)
            .border(
                BorderStroke(1.dp, if (selected) PurplePrimary else BorderColor),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (selected) PurplePrimary else DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = if (selected) TextWhite else PurpleLight, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    color = TextWhite,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(subtitle, color = TextGray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ChipBox(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PurplePrimary else DarkSurfaceVariant)
            .border(
                BorderStroke(1.dp, if (selected) PurplePrimary else BorderColor),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) TextWhite else TextGray
        )
    }
}

@Composable
private fun DaySelector(selectedDays: Set<Int>, onToggle: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DAY_NAMES.forEachIndexed { index, name ->
            val day = index + 1
            val active = day in selectedDays
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if (active) PurplePrimary else DarkSurfaceVariant)
                    .border(BorderStroke(1.dp, if (active) PurplePrimary else BorderColor), CircleShape)
                    .clickable { onToggle(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) TextWhite else TextGray,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
