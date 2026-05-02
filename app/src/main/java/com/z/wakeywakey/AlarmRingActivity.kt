package com.z.wakeywakey

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z.wakeywakey.alarm.AlarmService
import com.z.wakeywakey.data.AlarmStorage
import com.z.wakeywakey.ui.theme.*
import com.z.wakeywakey.utils.MathQuestion
import com.z.wakeywakey.utils.MathQuestionGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val alarmId = intent.getIntExtra("alarm_id", -1)

        setContent {
            WakeyWakeyTheme {
                AlarmRingScreen(
                    alarmId = alarmId,
                    onDismiss = { stopAlarmAndFinish() }
                )
            }
        }
    }

    private fun stopAlarmAndFinish() {
        startService(Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        })
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Blocked — user must solve math to dismiss
    }
}

@Composable
private fun AlarmRingScreen(alarmId: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var alarmLabel by remember { mutableStateOf("Alarm") }
    var questions by remember { mutableStateOf<List<MathQuestion>>(emptyList()) }
    var questionIndex by remember { mutableIntStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    var feedbackState by remember { mutableStateOf<FeedbackState>(FeedbackState.None) }
    var isDismissed by remember { mutableStateOf(false) }

    var currentTime by remember { mutableStateOf("") }
    var currentAmPm by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val h = now.get(Calendar.HOUR_OF_DAY)
            val m = now.get(Calendar.MINUTE)
            val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
            currentTime = "%d:%02d".format(h12, m)
            currentAmPm = if (h < 12) "AM" else "PM"
            delay(1000)
        }
    }

    LaunchedEffect(alarmId) {
        val alarm = if (alarmId != -1) AlarmStorage.get(context).getById(alarmId) else null
        alarmLabel = alarm?.label?.ifEmpty { "Alarm" } ?: "Alarm"
        questions = MathQuestionGenerator.generateBatch(alarm?.mathQuestionCount ?: 5)
    }

    if (isDismissed) {
        DismissedScreen(onDismiss = onDismiss)
        return
    }

    val currentQuestion = questions.getOrNull(questionIndex)

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 0.85f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Box(
            modifier = Modifier
                .size(420.dp)
                .scale(pulseScale)
                .align(Alignment.TopCenter)
                .offset(y = (-60).dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            PurpleGlow.copy(alpha = 0.4f),
                            PurpleFuchsia.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.displayMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = currentAmPm,
                    style = MaterialTheme.typography.headlineMedium,
                    color = PurpleLight,
                    modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
                )
            }

            Text(
                text = alarmLabel.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = PurpleFuchsia,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "WAKE UP! ⚡",
                style = MaterialTheme.typography.headlineLarge,
                color = TextWhite,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (questions.isNotEmpty()) {
                ProgressDots(current = questionIndex, total = questions.size)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Question ${questionIndex + 1} of ${questions.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentQuestion != null) {
                MathCard(
                    question = currentQuestion,
                    userInput = userInput,
                    feedbackState = feedbackState
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            NumberPad(
                onDigit = { if (userInput.length < 6) userInput += it },
                onBackspace = { if (userInput.isNotEmpty()) userInput = userInput.dropLast(1) },
                onSubmit = {
                    val answer = userInput.toIntOrNull()
                    if (answer == currentQuestion?.answer) {
                        feedbackState = FeedbackState.Correct
                        scope.launch {
                            delay(500)
                            if (questionIndex + 1 >= questions.size) {
                                isDismissed = true
                            } else {
                                questionIndex++
                                userInput = ""
                                feedbackState = FeedbackState.None
                            }
                        }
                    } else {
                        feedbackState = FeedbackState.Wrong
                        scope.launch {
                            delay(700)
                            userInput = ""
                            feedbackState = FeedbackState.None
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MathCard(
    question: MathQuestion,
    userInput: String,
    feedbackState: FeedbackState
) {
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(feedbackState) {
        if (feedbackState == FeedbackState.Wrong) {
            repeat(4) { i ->
                shakeAnim.animateTo(if (i % 2 == 0) 14f else -14f, tween(75))
            }
            shakeAnim.animateTo(0f, tween(75))
        }
    }

    val borderColor = when (feedbackState) {
        FeedbackState.Correct -> SuccessGreen
        FeedbackState.Wrong -> ErrorRed
        FeedbackState.None -> BorderColor
    }
    val cardColor = when (feedbackState) {
        FeedbackState.Correct -> SuccessGreen.copy(alpha = 0.1f)
        FeedbackState.Wrong -> ErrorRed.copy(alpha = 0.1f)
        FeedbackState.None -> CardBg
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = shakeAnim.value.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "solve to dismiss",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "${question.display} = ?",
                style = MaterialTheme.typography.displaySmall,
                color = TextWhite,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceVariant)
                    .border(
                        BorderStroke(1.dp, when (feedbackState) {
                            FeedbackState.Correct -> SuccessGreen
                            FeedbackState.Wrong -> ErrorRed
                            else -> PurplePrimary.copy(alpha = 0.5f)
                        }),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (feedbackState) {
                        FeedbackState.Correct -> "✓  Correct!"
                        FeedbackState.Wrong -> "✗  Try again"
                        else -> if (userInput.isEmpty()) "_ _ _" else userInput
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = when (feedbackState) {
                        FeedbackState.Correct -> SuccessGreen
                        FeedbackState.Wrong -> ErrorRed
                        else -> if (userInput.isEmpty()) TextMuted else TextWhite
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("⌫", "0", "✓")
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    val isBack = key == "⌫"
                    val isOk = key == "✓"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.6f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    isOk -> PurplePrimary
                                    isBack -> DarkSurfaceVariant
                                    else -> CardBg
                                }
                            )
                            .border(
                                BorderStroke(1.dp, if (isOk) PurplePrimary else BorderColor),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                when {
                                    isBack -> onBackspace()
                                    isOk -> onSubmit()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBack) {
                            Icon(Icons.Default.Backspace, null, tint = TextGray, modifier = Modifier.size(22.dp))
                        } else {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextWhite,
                                fontWeight = if (isOk) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .size(if (i == current) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            i < current -> SuccessGreen
                            i == current -> PurplePrimary
                            else -> BorderColor
                        }
                    )
            )
        }
    }
}

@Composable
private fun DismissedScreen(onDismiss: () -> Unit) {
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        delay(1800)
        onDismiss()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(DeepBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.15f))
                    .border(BorderStroke(2.dp, SuccessGreen), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", fontSize = 52.sp, color = SuccessGreen)
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text("All done!", style = MaterialTheme.typography.headlineLarge, color = TextWhite, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Rise & grind 🔥", style = MaterialTheme.typography.bodyLarge, color = TextGray)
        }
    }
}

private sealed class FeedbackState {
    object None : FeedbackState()
    object Correct : FeedbackState()
    object Wrong : FeedbackState()
}
