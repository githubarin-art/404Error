package com.runanywhere.startup_hackathon20.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.ProtocolQuestion
import com.runanywhere.startup_hackathon20.SafetyViewModel
import com.runanywhere.startup_hackathon20.ui.theme.*
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.*
import com.runanywhere.startup_hackathon20.AlertRecord
import com.runanywhere.startup_hackathon20.EmergencyPath
import com.runanywhere.startup_hackathon20.SafePlace
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun EmergencyScreen(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val isAlarmActive by viewModel.isAlarmActive.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val timeRemaining by viewModel.questionTimeRemaining.collectAsState()
    val secondQuestion by viewModel.secondQuestion.collectAsState()
    val secondTimeRemaining by viewModel.secondQuestionTimeRemaining.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isModelLoaded by viewModel.isModelLoaded.collectAsState()
    val contacts by viewModel.emergencyContacts.collectAsState()
    val alertHistory by viewModel.alertHistory.collectAsState()
    val emergencyPath by viewModel.emergencyPath.collectAsState()
    val nearestSafePlaces by viewModel.nearestSafePlaces.collectAsState()
    val isLoudAlarmActive by viewModel.isLoudAlarmActive.collectAsState()
    val isRecordingActive by viewModel.isRecordingActive.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    val isFakeCallActive by viewModel.isFakeCallActive.collectAsState()
    val isBreathingActive by viewModel.isBreathingActive.collectAsState()
    val showPoliceConfirmation by viewModel.showPoliceConfirmation.collectAsState()
    val showArrivalConfirmation by viewModel.showArrivalConfirmation.collectAsState()
    val currentDestination by viewModel.currentDestination.collectAsState()
    val interactionSignal by viewModel.interactionTimestamp.collectAsState()

    // Track if user manually requested stealth mode
    var userRequestedStealthMode by remember { mutableStateOf(false) }
    var camouflageActive by rememberSaveable { mutableStateOf(false) }
    var autoCamouflageStealth by rememberSaveable { mutableStateOf(false) }
    var storedStealthState by rememberSaveable { mutableStateOf(false) }


    // Auto-hide after alerts sent and question answered (or timed out)
    LaunchedEffect(isAlarmActive, currentQuestion, alertHistory) {
        if (isAlarmActive && currentQuestion == null && alertHistory.isNotEmpty()) {
            // Alerts sent AND question answered/timed out
            // Auto-hide after 3 seconds to show monitoring message
            delay(3000)
            if (isAlarmActive && currentQuestion == null) {
                // Auto-activate stealth mode
                userRequestedStealthMode = true
                viewModel.enterStealthMode()
            }
        }
    }

    // Reset stealth mode when alarm is cancelled
    LaunchedEffect(isAlarmActive) {
        if (!isAlarmActive) {
            userRequestedStealthMode = false
        }
    }

    // Handle back button press - ONLY way to activate stealth mode manually
    BackHandler(enabled = isAlarmActive && !userRequestedStealthMode) {
        // User pressed back - activate stealth mode
        userRequestedStealthMode = true
        viewModel.enterStealthMode()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isAlarmActive, camouflageActive) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        if (isAlarmActive && !camouflageActive) {
                            viewModel.registerUserInteraction()
                        }
                    }
                }
            }
            .background(
                if (isAlarmActive && !userRequestedStealthMode) {
                    // Simple clean background for emergency mode
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF5F5F5),
                            Color(0xFFE8E8E8)
                        )
                    )
                } else {
                    // Normal 404 error theme - Light beige
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF5F1E8),
                            Color(0xFFF5F1E8)
                        )
                    )
                }
            )
    ) {
        if (!isAlarmActive || userRequestedStealthMode) {
            // Show normal UI (when no emergency OR user hid the emergency screen)
            NormalModeUI(
                isModelLoaded = isModelLoaded,
                statusMessage = if (isAlarmActive && userRequestedStealthMode) "System initialized" else statusMessage,
                contacts = contacts,
                onEmergencyTrigger = { viewModel.triggerEmergencyAlarm() },
                onLoadModel = { viewModel.loadAIModel("Qwen 2.5 0.5B Instruct Q6_K") },
                isStealthMode = isAlarmActive && userRequestedStealthMode
            )
        } else {
            // Emergency is active and user has NOT hidden it - show emergency UI
            SimpleEmergencyUI(
                currentQuestion = currentQuestion,
                currentQuestionTimeRemaining = timeRemaining,
                secondQuestion = secondQuestion,
                secondQuestionTimeRemaining = secondTimeRemaining,
                statusMessage = statusMessage,
                emergencyPath = emergencyPath,
                alertHistory = alertHistory,
                nearestSafePlaces = nearestSafePlaces,
                isLoudAlarmActive = isLoudAlarmActive,
                onToggleLoudAlarm = { viewModel.toggleLoudAlarm() },
                isRecordingActive = isRecordingActive,
                recordingDuration = recordingDuration,
                onToggleRecording = { viewModel.toggleRecording() },
                isFakeCallActive = isFakeCallActive,
                onToggleFakeCall = {
                    if (isFakeCallActive) viewModel.stopFakeCall() else viewModel.startFakeCall()
                },
                isBreathingActive = isBreathingActive,
                onToggleBreathing = {
                    if (isBreathingActive) viewModel.stopBreathingExercise() else viewModel.startBreathingExercise()
                },
                onNavigateToPlace = { viewModel.navigateToPlace(it) },
                onRequestPolice = { viewModel.requestCallPolice() },
                showPoliceConfirmation = showPoliceConfirmation,
                onPoliceConfirmation = { viewModel.confirmCallPolice(it) },
                currentDestination = currentDestination,
                showArrivalConfirmation = showArrivalConfirmation,
                onArrivalConfirmation = { viewModel.confirmArrival(it) },
                onFirstQuestionYes = { viewModel.answerProtocolQuestionYes() },
                onFirstQuestionNo = { viewModel.answerProtocolQuestionNo() },
                onSecondQuestionYes = { viewModel.answerSecondQuestionYes() },
                onSecondQuestionNo = { viewModel.answerSecondQuestionNo() },
                onCancel = { viewModel.cancelEmergencyAlarm() }
            )
        }
    }
}


@Composable
private fun EscapeToSafetyScreen(
    statusMessage: String,
    nearestSafePlaces: List<SafePlace>,
    currentDestination: SafePlace?,
    isLoudAlarmActive: Boolean,
    onToggleLoudAlarm: () -> Unit,
    isRecordingActive: Boolean,
    recordingDuration: Int,
    onToggleRecording: () -> Unit,
    isFakeCallActive: Boolean,
    onToggleFakeCall: () -> Unit,
    isBreathingActive: Boolean,
    onToggleBreathing: () -> Unit,
    onNavigateToPlace: (SafePlace) -> Unit,
    showPoliceConfirmation: Boolean,
    onPoliceConfirmation: (Boolean) -> Unit,
    onRequestPolice: () -> Unit,
    alertHistory: List<AlertRecord>,
    showArrivalConfirmation: Boolean,
    onArrivalConfirmation: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var safeSectionExpanded by rememberSaveable { mutableStateOf(true) }
    val recordingMinutes = recordingDuration / 60
    val recordingSeconds = recordingDuration % 60
    val formattedRecording = String.format("%02d:%02d", recordingMinutes, recordingSeconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "HIGH ALERT – ESCAPE TO SAFETY",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AmberYellowDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))
        AssistChip(
            onClick = {},
            label = {
                Text(
                    statusMessage,
                    color = AmberYellowDark,
                    fontWeight = FontWeight.SemiBold
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = AmberYellow.copy(alpha = 0.18f)
            )
        )

        currentDestination?.let { destination ->
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Heading to ${destination.name}",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "Stay visible and move towards well-lit areas.",
                            color = Color(0xFF2E7D32),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { safeSectionExpanded = !safeSectionExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = AmberYellowDark)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "NEAREST SAFE PLACES",
                        fontWeight = FontWeight.Bold,
                        color = AmberYellowDark,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (safeSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = AmberYellowDark
                    )
                }

                AnimatedVisibility(visible = safeSectionExpanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        if (nearestSafePlaces.isEmpty()) {
                            Text(
                                text = "Fetching trusted locations...",
                                color = ModernTextSecondary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontSize = 12.sp
                            )
                        } else {
                            nearestSafePlaces.take(5).forEach { place ->
                                SafePlaceRow(
                                    place = place,
                                    onNavigate = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onNavigateToPlace(place)
                                    },
                                    buttonLabel = "Navigate Now"
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AdditionalProtectionSection(
            isLoudAlarmActive = isLoudAlarmActive,
            onToggleLoudAlarm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleLoudAlarm()
            },
            isRecordingActive = isRecordingActive,
            onToggleRecording = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleRecording()
            },
            isFakeCallActive = isFakeCallActive,
            onToggleFakeCall = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleFakeCall()
            },
            isBreathingActive = isBreathingActive,
            onToggleBreathing = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleBreathing()
            },
            onRequestPolice = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRequestPolice()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SafetyNetworkStatus(
            alertHistory = alertHistory,
            isRecordingActive = isRecordingActive,
            recordingTime = formattedRecording,
            compact = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        CallPoliceSection(
            onRequestPolice = onRequestPolice,
            primary = false
        )

        if (showPoliceConfirmation) {
            AlertDialog(
                onDismissRequest = { onPoliceConfirmation(false) },
                icon = {
                    Icon(Icons.Default.LocalPolice, contentDescription = null, tint = SafetyRed)
                },
                title = {
                    Text("Call 112 now?")
                },
                text = {
                    Text("We will connect you immediately to emergency services. Proceed?")
                },
                confirmButton = {
                    TextButton(onClick = { onPoliceConfirmation(true) }) {
                        Text("CALL NOW", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onPoliceConfirmation(false) }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showArrivalConfirmation) {
            AlertDialog(
                onDismissRequest = { onArrivalConfirmation(false) },
                icon = {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF43A047))
                },
                title = {
                    Text("Are you safe now?")
                },
                text = {
                    Text(
                        text = currentDestination?.let { "You are near ${it.name}. Confirm if you are safe to end emergency mode." }
                            ?: "You are near your selected safe place. Confirm if you are safe to end emergency mode."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onArrivalConfirmation(true) }) {
                        Text("YES, I'M SAFE", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onArrivalConfirmation(false) }) {
                        Text("Not yet")
                    }
                }
            )
        }
    }
}


@Composable
private fun NormalModeUI(
    isModelLoaded: Boolean,
    statusMessage: String,
    contacts: List<com.runanywhere.startup_hackathon20.EmergencyContact>,
    onEmergencyTrigger: () -> Unit,
    onLoadModel: () -> Unit,
    isStealthMode: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Title - 404 ERROR
        Text(
            text = "404 ERROR",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D2D2D),
            letterSpacing = 1.sp
        )
        
        Text(
            text = "Application Not Found",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF666666),
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Status Card - Clean white card with shadow
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF2D2D2D),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isModelLoaded) {
                        if (isStealthMode) "System monitoring" else "System initialized"
                    } else "System offline",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF2D2D2D),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Main 404 Button - Large circular button with gray outline
        Circular404Button(
            enabled = isModelLoaded,
            onClick = onEmergencyTrigger,
            isStealthMode = isStealthMode
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isStealthMode) "tap to re-send alerts" else "tap to retry connection",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.weight(1f))

        // Initialize System Button (if model not loaded)
        if (!isModelLoaded) {
            Button(
                onClick = onLoadModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2D2D2D)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "INITIALIZE SYSTEM",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Warning text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "first time initialization may take several minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun Circular404Button(
    enabled: Boolean,
    onClick: () -> Unit,
    isStealthMode: Boolean = false
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        // Circular button with gray outline
        Surface(
            modifier = Modifier
                .size(200.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = if (enabled) 4.dp else 0.dp,
            border = BorderStroke(
                width = 3.dp,
                color = if (enabled) Color(0xFFCCCCCC) else Color(0xFFE0E0E0)
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = enabled) { onClick() }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (!isStealthMode) {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastTapTime < 500) {
                                        tapCount++
                                        if (tapCount >= 3 && enabled) {
                                            onClick()
                                            tapCount = 0
                                        }
                                    } else {
                                        tapCount = 1
                                    }
                                    lastTapTime = currentTime
                                }
                            },
                            onLongPress = {
                                if (enabled) onClick()
                            }
                        )
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "404",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) Color(0xFFCCCCCC) else Color(
                            0xFFE0E0E0
                        ),
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "error",
                        fontSize = 16.sp,
                        color = if (enabled) Color(0xFF999999) else Color(
                            0xFFCCCCCC
                        ),
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * SIMPLE EMERGENCY UI - Easy for victim to understand and respond
 * Clear, straightforward interface when emergency is active
 */
@Composable
private fun SimpleEmergencyUI(
    currentQuestion: ProtocolQuestion?,
    currentQuestionTimeRemaining: Int?,
    secondQuestion: ProtocolQuestion?,
    secondQuestionTimeRemaining: Int?,
    statusMessage: String,
    emergencyPath: EmergencyPath,
    alertHistory: List<AlertRecord>,
    nearestSafePlaces: List<SafePlace>,
    isLoudAlarmActive: Boolean,
    onToggleLoudAlarm: () -> Unit,
    isRecordingActive: Boolean,
    recordingDuration: Int,
    onToggleRecording: () -> Unit,
    isFakeCallActive: Boolean,
    onToggleFakeCall: () -> Unit,
    isBreathingActive: Boolean,
    onToggleBreathing: () -> Unit,
    onNavigateToPlace: (SafePlace) -> Unit,
    onRequestPolice: () -> Unit,
    showPoliceConfirmation: Boolean,
    onPoliceConfirmation: (Boolean) -> Unit,
    currentDestination: SafePlace?,
    showArrivalConfirmation: Boolean,
    onArrivalConfirmation: (Boolean) -> Unit,
    onFirstQuestionYes: () -> Unit,
    onFirstQuestionNo: () -> Unit,
    onSecondQuestionYes: () -> Unit,
    onSecondQuestionNo: () -> Unit,
    onCancel: () -> Unit
) {
    if (emergencyPath == EmergencyPath.THREAT_NEARBY && currentQuestion == null && secondQuestion == null) {
        ThreatNearbyScreen(
            statusMessage = statusMessage,
            alertHistory = alertHistory,
            nearestSafePlaces = nearestSafePlaces,
            isLoudAlarmActive = isLoudAlarmActive,
            onToggleLoudAlarm = onToggleLoudAlarm,
            isRecordingActive = isRecordingActive,
            recordingDuration = recordingDuration,
            onToggleRecording = onToggleRecording,
            isFakeCallActive = isFakeCallActive,
            onToggleFakeCall = onToggleFakeCall,
            isBreathingActive = isBreathingActive,
            onToggleBreathing = onToggleBreathing,
            onNavigateToPlace = onNavigateToPlace,
            onRequestPolice = onRequestPolice,
            showPoliceConfirmation = showPoliceConfirmation,
            onPoliceConfirmation = onPoliceConfirmation
        )
        return
    }

    if (emergencyPath == EmergencyPath.ESCAPE_TO_SAFETY && currentQuestion == null && secondQuestion == null) {
        EscapeToSafetyScreen(
            statusMessage = statusMessage,
            nearestSafePlaces = nearestSafePlaces,
            currentDestination = currentDestination,
            isLoudAlarmActive = isLoudAlarmActive,
            onToggleLoudAlarm = onToggleLoudAlarm,
            isRecordingActive = isRecordingActive,
            recordingDuration = recordingDuration,
            onToggleRecording = onToggleRecording,
            isFakeCallActive = isFakeCallActive,
            onToggleFakeCall = onToggleFakeCall,
            isBreathingActive = isBreathingActive,
            onToggleBreathing = onToggleBreathing,
            onNavigateToPlace = onNavigateToPlace,
            showPoliceConfirmation = showPoliceConfirmation,
            onPoliceConfirmation = onPoliceConfirmation,
            onRequestPolice = onRequestPolice,
            alertHistory = alertHistory,
            showArrivalConfirmation = showArrivalConfirmation,
            onArrivalConfirmation = onArrivalConfirmation
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Clear status indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        when {
            currentQuestion != null -> {
                // Question card - simple and clear
                QuestionCard(
                    question = currentQuestion,
                    timeRemaining = currentQuestionTimeRemaining,
                    onAnswerYes = onFirstQuestionYes,
                    onAnswerNo = onFirstQuestionNo
                )
            }

            secondQuestion != null -> {
                // Proximity question card
                ProximityQuestionCard(
                    question = secondQuestion,
                    timeRemaining = secondQuestionTimeRemaining,
                    onAnswerYes = onSecondQuestionYes,
                    onAnswerNo = onSecondQuestionNo
                )
            }

            else -> {
                // Monitoring status card (shown after answer or during processing)
                MonitoringCard()
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Cancel button - smaller, less prominent
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF757575)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "False Alarm - Cancel Emergency",
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ThreatNearbyScreen(
    statusMessage: String,
    alertHistory: List<AlertRecord>,
    nearestSafePlaces: List<SafePlace>,
    isLoudAlarmActive: Boolean,
    onToggleLoudAlarm: () -> Unit,
    isRecordingActive: Boolean,
    recordingDuration: Int,
    onToggleRecording: () -> Unit,
    isFakeCallActive: Boolean,
    onToggleFakeCall: () -> Unit,
    isBreathingActive: Boolean,
    onToggleBreathing: () -> Unit,
    onNavigateToPlace: (SafePlace) -> Unit,
    onRequestPolice: () -> Unit,
    showPoliceConfirmation: Boolean,
    onPoliceConfirmation: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var safeSectionExpanded by rememberSaveable { mutableStateOf(false) }
    val recordingMinutes = recordingDuration / 60
    val recordingSeconds = recordingDuration % 60
    val formattedRecording = String.format("%02d:%02d", recordingMinutes, recordingSeconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "CRITICAL - THREAT NEARBY",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SafetyRed,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))
        AssistChip(
            onClick = {},
            label = {
                Text(
                    statusMessage,
                    color = SafetyRed,
                    fontWeight = FontWeight.SemiBold
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = SafetyRed.copy(alpha = 0.12f)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        ActionGrid(
            isLoudAlarmActive = isLoudAlarmActive,
            onToggleLoudAlarm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleLoudAlarm()
            },
            isRecordingActive = isRecordingActive,
            recordingTime = formattedRecording,
            onToggleRecording = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleRecording()
            },
            isFakeCallActive = isFakeCallActive,
            onToggleFakeCall = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleFakeCall()
            },
            isBreathingActive = isBreathingActive,
            onToggleBreathing = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleBreathing()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Safe places collapsible section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(containerColor = AmberYellow.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { safeSectionExpanded = !safeSectionExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = AmberYellowDark
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "IF YOU CAN MOVE – ESCAPE TO",
                        fontWeight = FontWeight.Bold,
                        color = AmberYellowDark,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (safeSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = AmberYellowDark
                    )
                }

                AnimatedVisibility(visible = safeSectionExpanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        if (nearestSafePlaces.isEmpty()) {
                            Text(
                                text = "Calculating nearest safe places...",
                                color = AmberYellowDark,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            nearestSafePlaces.take(3).forEach { place ->
                                SafePlaceRow(place = place, onNavigate = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onNavigateToPlace(place)
                                })
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SafetyNetworkStatus(
            alertHistory = alertHistory,
            isRecordingActive = isRecordingActive,
            recordingTime = formattedRecording
        )

        Spacer(modifier = Modifier.height(24.dp))

        CallPoliceSection(
            onRequestPolice = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRequestPolice()
            },
            primary = true
        )

        if (showPoliceConfirmation) {
            AlertDialog(
                onDismissRequest = { onPoliceConfirmation(false) },
                icon = {
                    Icon(Icons.Default.LocalPolice, contentDescription = null, tint = SafetyRed)
                },
                title = {
                    Text("Call 112 now?")
                },
                text = {
                    Text("We will connect you immediately to emergency services. Proceed?")
                },
                confirmButton = {
                    TextButton(onClick = { onPoliceConfirmation(true) }) {
                        Text("CALL NOW", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onPoliceConfirmation(false) }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ActionGrid(
    isLoudAlarmActive: Boolean,
    onToggleLoudAlarm: () -> Unit,
    isRecordingActive: Boolean,
    recordingTime: String,
    onToggleRecording: () -> Unit,
    isFakeCallActive: Boolean,
    onToggleFakeCall: () -> Unit,
    isBreathingActive: Boolean,
    onToggleBreathing: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrimaryActionButton(
                modifier = Modifier.weight(1f),
                title = if (isLoudAlarmActive) "Stop Alarm" else "Loud Alarm",
                subtitle = "Max volume siren + vibration",
                icon = Icons.Default.Campaign,
                containerColor = if (isLoudAlarmActive) SafetyRed else SafetyRed.copy(alpha = 0.9f),
                active = isLoudAlarmActive,
                onClick = onToggleLoudAlarm
            )

            PrimaryActionButton(
                modifier = Modifier.weight(1f),
                title = if (isRecordingActive) "Stop Recording" else "Record Evidence",
                subtitle = if (isRecordingActive) "Recording $recordingTime" else "Capture audio evidence",
                icon = Icons.Default.Mic,
                containerColor = if (isRecordingActive) Color(0xFFB71C1C) else Color(0xFFD32F2F),
                active = isRecordingActive,
                pulseAlpha = if (isRecordingActive) pulseAlpha else 0f,
                onClick = onToggleRecording
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrimaryActionButton(
                modifier = Modifier.weight(1f),
                title = if (isFakeCallActive) "End Fake Call" else "Fake Call",
                subtitle = if (isFakeCallActive) "Simulating active call" else "Show incoming call screen",
                icon = Icons.Default.Phone,
                containerColor = Color(0xFF1565C0),
                active = isFakeCallActive,
                onClick = onToggleFakeCall
            )

            PrimaryActionButton(
                modifier = Modifier.weight(1f),
                title = if (isBreathingActive) "Close Exercise" else "Breathing Exercise",
                subtitle = "Guided 4-4-4 calm routine",
                icon = Icons.Default.SelfImprovement,
                containerColor = Color(0xFF43A047),
                active = isBreathingActive,
                onClick = onToggleBreathing
            )
        }
    }
}

@Composable
private fun PrimaryActionButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    active: Boolean,
    onClick: () -> Unit,
    pulseAlpha: Float = 0f
) {
    Surface(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        tonalElevation = if (active) 10.dp else 4.dp,
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                if (pulseAlpha > 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SafePlaceRow(
    place: SafePlace,
    onNavigate: () -> Unit,
    buttonLabel: String = "Navigate"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (place.type.lowercase()) {
                    "police" -> Icons.Default.LocalPolice
                    "hospital" -> Icons.Default.LocalHospital
                    "fire" -> Icons.Default.LocalFireDepartment
                    "store" -> Icons.Default.Store
                    else -> Icons.Default.Place
                }
                Icon(icon, contentDescription = null, tint = AmberYellowDark)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(place.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = buildString {
                            place.distance?.let {
                                append("${it.toInt()} m")
                            }
                            place.walkingTimeMinutes?.let {
                                if (isNotEmpty()) append(" · ")
                                append("~${it} min walk")
                            }
                        },
                        color = Color(0xFF616161),
                        fontSize = 13.sp
                    )
                }
                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberYellowDark)
                ) {
                    Text(buttonLabel, color = Color.White)
                }
            }
            place.address.takeIf { it.isNotBlank() }?.let { address ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(address, color = Color(0xFF757575), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SafetyNetworkStatus(
    alertHistory: List<AlertRecord>,
    isRecordingActive: Boolean,
    recordingTime: String,
    compact: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "YOUR SAFETY NETWORK",
                fontWeight = FontWeight.Bold,
                color = ModernTextPrimary,
                fontSize = if (compact) 16.sp else 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = ModernTextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Location updates every 30 seconds",
                    color = ModernTextSecondary,
                    fontSize = if (compact) 12.sp else 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Radio, contentDescription = null, tint = ModernTextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRecordingActive) "Recording evidence – $recordingTime" else "Recording idle",
                    color = if (isRecordingActive) SafetyRed else ModernTextSecondary,
                    fontSize = if (compact) 12.sp else 13.sp,
                    fontWeight = if (isRecordingActive) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Alert Status",
                fontWeight = FontWeight.SemiBold,
                color = ModernTextPrimary,
                fontSize = if (compact) 13.sp else 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (alertHistory.isEmpty()) {
                Text(
                    text = "Alerts are being sent to your safety network...",
                    color = ModernTextSecondary,
                    fontSize = if (compact) 11.sp else 12.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    alertHistory.takeLast(3).reversed().forEach { record ->
                        AlertHistoryRow(record)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertHistoryRow(record: AlertRecord) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.recipientName,
                fontWeight = FontWeight.Medium,
                color = ModernTextPrimary,
                fontSize = 14.sp
            )
            Text(
                text = record.messageType.name,
                color = ModernTextSecondary,
                fontSize = 12.sp
            )
        }
        val statusColor = if (record.success) Color(0xFF43A047) else SafetyRed
        Text(
            text = if (record.success) "Delivered" else "Failed",
            color = statusColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun CallPoliceSection(
    onRequestPolice: () -> Unit,
    primary: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "DO YOU NEED POLICE?",
                fontWeight = FontWeight.Bold,
                color = ModernTextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (primary) {
                    Button(
                        onClick = onRequestPolice,
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyRed),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "YES - Call 112 NOW",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ModernTextSecondary)
                    ) {
                        Text(
                            "Not now",
                            color = ModernTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onRequestPolice,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SafetyRed)
                    ) {
                        Text("Call 112", color = SafetyRed, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Not now",
                            color = ModernTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: ProtocolQuestion,
    timeRemaining: Int?,
    onAnswerYes: () -> Unit,
    onAnswerNo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = question.question,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF2D2D2D),
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Timer - clear and visible
            timeRemaining?.let { time ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (time <= 10) Color(0xFFFFEBEE) else Color(0xFFF1F8E9)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$time",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (time <= 10) Color(0xFFD32F2F) else Color(0xFF689F38)
                        )
                        Text(
                            text = "seconds remaining",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Large, clear answer buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onAnswerYes,
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "YES",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Button(
                    onClick = onAnswerNo,
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "NO",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Volume button hint
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Use Volume Up/Down buttons to answer discreetly",
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ProximityQuestionCard(
    question: ProtocolQuestion,
    timeRemaining: Int?,
    onAnswerYes: () -> Unit,
    onAnswerNo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = question.question,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF2D2D2D),
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Timer - clear and visible
            timeRemaining?.let { time ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (time <= 10) Color(0xFFFFEBEE) else Color(0xFFF1F8E9)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$time",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (time <= 10) Color(0xFFD32F2F) else Color(0xFF689F38)
                        )
                        Text(
                            text = "seconds remaining",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Large, clear answer buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onAnswerYes,
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "YES",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Button(
                    onClick = onAnswerNo,
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "NO",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Volume button hint
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Use Volume Up/Down buttons to answer discreetly",
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitoringCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated icon
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .scale(alpha),
                tint = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Emergency Monitoring Active",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF2D2D2D)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your contacts have been notified.\nHelp is on the way.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color(0xFF666666),
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF1F8E9)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF689F38)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "System is monitoring your safety",
                            fontSize = 14.sp,
                            color = Color(0xFF558B2F),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Press back button to hide this screen if needed",
                        fontSize = 12.sp,
                        color = Color(0xFF7CB342),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
