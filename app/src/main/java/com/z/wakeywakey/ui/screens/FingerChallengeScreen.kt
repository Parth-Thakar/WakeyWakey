package com.z.wakeywakey.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.z.wakeywakey.ui.theme.*
import com.z.wakeywakey.utils.FingerCounter
import com.z.wakeywakey.utils.HandLandmarkerHelper
import com.z.wakeywakey.utils.HandModelProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun FingerChallengeScreen(
    label: String,
    rounds: Int,
    timeText: String,
    amPm: String,
    onSolved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val hasPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    var modelReady by remember { mutableStateOf(HandModelProvider.isModelReady(context)) }
    var modelDownloadProgress by remember { mutableStateOf(0) }
    var modelError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!modelReady) {
            val ok = HandModelProvider.ensureModel(context) { p -> modelDownloadProgress = p }
            if (ok) modelReady = true else modelError = "Could not download hand model. Connect to internet and try again."
        }
    }

    if (!hasPermission) {
        FingerNoticeScreen(
            title = "Camera permission needed",
            message = "This alarm needs camera access to detect your fingers. Please re-create the alarm and grant camera permission.",
            actionText = "Dismiss anyway",
            onAction = onSolved
        )
        return
    }

    if (modelError != null) {
        FingerNoticeScreen(
            title = "Hand model unavailable",
            message = modelError!!,
            actionText = "Dismiss alarm",
            onAction = onSolved
        )
        return
    }

    if (!modelReady) {
        FingerLoadingScreen(progress = modelDownloadProgress)
        return
    }

    var roundIndex by remember { mutableIntStateOf(0) }
    var target by remember { mutableIntStateOf(FingerCounter.randomTarget()) }
    var detected by remember { mutableIntStateOf(-1) }
    var hasHand by remember { mutableStateOf(false) }
    var roundComplete by remember { mutableStateOf(false) }
    var allDone by remember { mutableStateOf(false) }

    // Re-launches whenever detected or hasHand changes, which cancels any in-progress hold timer.
    LaunchedEffect(detected, hasHand) {
        if (!roundComplete && hasHand && detected == target) {
            delay(1500L)
            // Re-check after the hold window — user must still be showing the right count
            if (!roundComplete && hasHand && detected == target) {
                roundComplete = true
                delay(600)
                if (roundIndex + 1 >= rounds) {
                    allDone = true
                    delay(900)
                    onSolved()
                } else {
                    roundIndex++
                    target = FingerCounter.randomTarget(target)
                    roundComplete = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        // Camera preview as background
        CameraPreviewWithDetection(
            onResult = { c, h ->
                detected = c
                hasHand = h
            }
        )

        // Dark gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DeepBlack.copy(alpha = 0.85f),
                            DeepBlack.copy(alpha = 0.55f),
                            DeepBlack.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displayMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = amPm,
                    style = MaterialTheme.typography.headlineMedium,
                    color = PurpleLight,
                    modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
                )
            }
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = PurpleFuchsia,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SHOW YOUR FINGERS ✋",
                style = MaterialTheme.typography.headlineSmall,
                color = TextWhite,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProgressDotsRow(current = roundIndex, total = rounds)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Round ${roundIndex + 1} of $rounds",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray
            )

            Spacer(modifier = Modifier.weight(1f))

            // Target display
            TargetCard(target = target, detected = detected, hasHand = hasHand, success = roundComplete)

            Spacer(modifier = Modifier.height(20.dp))

            HintText(hasHand = hasHand, detected = detected, target = target, success = roundComplete)

            Spacer(modifier = Modifier.height(48.dp))
        }

        AnimatedVisibility(visible = allDone) {
            Box(
                modifier = Modifier.fillMaxSize().background(DeepBlack),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.15f))
                            .border(BorderStroke(2.dp, SuccessGreen), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("✓", fontSize = 52.sp, color = SuccessGreen) }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Nicely done!", style = MaterialTheme.typography.headlineLarge, color = TextWhite, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun TargetCard(target: Int, detected: Int, hasHand: Boolean, success: Boolean) {
    val pulse = rememberInfiniteTransition(label = "p")
    val s by pulse.animateFloat(
        initialValue = 0.96f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "s"
    )

    val border = when {
        success -> SuccessGreen
        hasHand && detected == target -> SuccessGreen
        else -> PurplePrimary
    }
    val accent = when {
        success || (hasHand && detected == target) -> SuccessGreen
        else -> PurpleLight
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (success) 1.05f else s),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.85f)),
        border = BorderStroke(1.5.dp, border)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "show this many fingers",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$target",
                    fontSize = 96.sp,
                    color = accent,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (target == 1) "finger" else "fingers",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextGray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (hasHand) SuccessGreen else ErrorRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasHand) "Detecting: $detected" else "No hand seen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite
                )
            }
        }
    }
}

@Composable
private fun HintText(hasHand: Boolean, detected: Int, target: Int, success: Boolean) {
    val msg = when {
        success -> "Hold steady…"
        !hasHand -> "Turn on the lights and point camera at your hand"
        detected == target -> "Hold it…"
        detected < target -> "Show ${target - detected} more"
        else -> "Show fewer fingers"
    }
    Text(
        text = msg,
        color = TextGray,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ProgressDotsRow(current: Int, total: Int) {
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
private fun CameraPreviewWithDetection(onResult: (Int, Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val helper = remember {
        HandLandmarkerHelper(
            context = context,
            onResult = { count, hasHand -> onResult(count, hasHand) },
            onError = { /* swallow */ }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            helper.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
                    ).build()

                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build().also {
                        it.setAnalyzer(executor) { image -> helper.analyze(image) }
                    }

                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
private fun FingerLoadingScreen(progress: Int) {
    Box(
        modifier = Modifier.fillMaxSize().background(DeepBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PurplePrimary, strokeWidth = 4.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text("Preparing finger detection…", color = TextWhite, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("$progress%", color = TextGray, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun FingerNoticeScreen(title: String, message: String, actionText: String, onAction: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(DeepBlack).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = TextWhite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = TextGray, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary, contentColor = TextWhite)
            ) { Text(actionText) }
        }
    }
}
