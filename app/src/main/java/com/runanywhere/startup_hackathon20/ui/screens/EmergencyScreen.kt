package com.runanywhere.startup_hackathon20.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
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
import com.runanywhere.startup_hackathon20.data.SafePlace
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.interaction.MutableInteractionSource
import android.util.Log

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
    val showStealthDecoy by viewModel.showStealthDecoy.collectAsState()
    val showInfoIcon by viewModel.showInfoIcon.collectAsState()
    val showDecoyAvailable by viewModel.showDecoyAvailable.collectAsState()

    // Track if user manually requested stealth mode
    var userRequestedStealthMode by remember { mutableStateOf(false) }
    
    // Auto-camouflage state
    var autoCamouflageActive by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // Triple tap detection for restoring UI
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    // Update last interaction time when user interacts
    LaunchedEffect(interactionSignal) {
        lastInteractionTime = interactionSignal
        // If user interacts, disable auto-camouflage
        if (autoCamouflageActive) {
            autoCamouflageActive = false
        }
    }

    // Auto-camouflage timer - activate after 30 seconds of inactivity
    LaunchedEffect(isAlarmActive, lastInteractionTime, showDecoyAvailable) {
        if (isAlarmActive && !showDecoyAvailable && !userRequestedStealthMode) {
            while (isAlarmActive) {
                delay(1000) // Check every second
                val timeSinceLastInteraction = System.currentTimeMillis() - lastInteractionTime
                
                // Activate camouflage after 30 seconds of inactivity
                if (timeSinceLastInteraction > 30000 && !autoCamouflageActive) {
                    autoCamouflageActive = true
                    Log.i("EmergencyScreen", "🎭 Auto-camouflage activated after 30s inactivity")
                }
            }
        }
    }


    // Reset stealth mode and auto-camouflage when alarm is cancelled
    LaunchedEffect(isAlarmActive) {
        if (!isAlarmActive) {
            userRequestedStealthMode = false
            autoCamouflageActive = false
        }
    }

    // Handle back button press - ONLY way to activate stealth mode manually
    BackHandler(enabled = isAlarmActive && !userRequestedStealthMode && !autoCamouflageActive) {
        // User pressed back - activate stealth mode
        userRequestedStealthMode = true
        viewModel.enterStealthMode()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isAlarmActive && !userRequestedStealthMode && !autoCamouflageActive) {
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
        when {
            // Show 404 error (camouflage or no emergency or user stealth)
            !isAlarmActive || userRequestedStealthMode || autoCamouflageActive -> {
                val reTriggerState = remember { mutableStateOf(false) }
                Fake404ErrorScreen(
                    isStealthMode = isAlarmActive && (userRequestedStealthMode || autoCamouflageActive),
                    autoCamouflage = autoCamouflageActive,
                    onSosButtonClick = {
                        // ALWAYS trigger emergency when 404 button is clicked
                        // If already in emergency, this will start a NEW emergency session
                        if (isAlarmActive) {
                            // Cancel current emergency first, then trigger new one
                            reTriggerState.value = true
                            viewModel.cancelEmergencyAlarm()
                        } else {
                            viewModel.triggerEmergencyAlarm()
                        }
                    },
                    onScreenTap = {
                        if (isAlarmActive && autoCamouflageActive) {
                            // Triple tap detection to restore UI from auto-camouflage
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastTapTime < 2000) {
                                tapCount++
                                if (tapCount >= 3) {
                                    // Triple tap detected - restore UI
                                    autoCamouflageActive = false
                                    tapCount = 0
                                    lastInteractionTime = currentTime
                                    viewModel.registerUserInteraction()
                                    Log.i(
                                        "EmergencyScreen",
                                        "🔓 Auto-camouflage disabled via triple tap"
                                    )
                                }
                            } else {
                                tapCount = 1
                            }
                            lastTapTime = currentTime
                        } else {
                            // Not in camouflage - just register interaction
                            viewModel.registerUserInteraction()
                        }
                    }
                )

                // Properly listen for "re-trigger" flow; wait for emergency to truly NOT be active before starting new alarm
                if (reTriggerState.value && !isAlarmActive) {
                    LaunchedEffect(reTriggerState.value, isAlarmActive) {
                        // Give a short delay to allow state to settle
                        delay(300)
                        viewModel.triggerEmergencyAlarm()
                        reTriggerState.value = false
                    }
                }
            }
            
            // Show emergency UI
            else -> {
                // Emergency is active and user has NOT hidden it - show emergency UI
                if (emergencyPath == EmergencyPath.ESCAPE_TO_SAFETY) {
                    if (showStealthDecoy) {
                        // Show decoy home screen with info icon for Path B stealth switching
                        Fake404ErrorScreenWithInfoIcon(
                            viewModel = viewModel,
                            isStealthMode = true,
                            autoCamouflage = false,
                            onSosButtonClick = { viewModel.triggerEmergencyAlarm() },
                            onScreenTap = { viewModel.registerUserInteraction() }
                        )
                    } else {
                        EscapeToSafetyScreen(
                            viewModel = viewModel,
                            statusMessage = statusMessage,
                            nearestSafePlaces = nearestSafePlaces,
                            currentDestination = currentDestination,
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
                            showPoliceConfirmation = showPoliceConfirmation,
                            onPoliceConfirmation = { viewModel.confirmCallPolice(it) },
                            onRequestPolice = { viewModel.requestCallPolice() },
                            alertHistory = alertHistory,
                            showArrivalConfirmation = showArrivalConfirmation,
                            onArrivalConfirmation = { viewModel.confirmArrival(it) }
                        )
                    }
                } else {
                    // For other paths (including THREAT_NEARBY), use existing logic
                    SimpleEmergencyUI(
                        viewModel = viewModel,
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
        
        // Full-screen overlays (shown on top of everything)
        if (isFakeCallActive) {
            FakeCallOverlay(
                onDismiss = { viewModel.stopFakeCall() }
            )
        }
        
        if (isBreathingActive) {
            BreathingExerciseOverlay(
                onDismiss = { viewModel.stopBreathingExercise() }
            )
        }
    }
}


@Composable
private fun EscapeToSafetyScreen(
    viewModel: SafetyViewModel,
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
    val showDecoyAvailable by viewModel.showDecoyAvailable.collectAsState()
    var safeSectionExpanded by rememberSaveable { mutableStateOf(true) } // EXPANDED by default for Path B
    var additionalProtectionExpanded by rememberSaveable { mutableStateOf(false) } // COLLAPSED by default
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

        if (showDecoyAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 16.dp, top = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = { viewModel.switchToDecoy() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.8f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "Hide to home screen (decoy)",
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        // Header
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

        // Journey progress card (if navigating)
        currentDestination?.let { destination ->
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Heading to ${destination.name}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20),
                                fontSize = 16.sp
                            )
                            destination.distance?.let { distance ->
                                Text(
                                    text = "${distance.toInt()}m away",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "📍 Location updates sent every 30 seconds",
                        color = Color(0xFF2E7D32),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 Stay visible, move towards well-lit populated areas",
                        color = Color(0xFF388E3C),
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // EXPANDED Safe Places Section (PRIMARY focus for Path B)
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
                    Icon(Icons.Default.Map, contentDescription = null, tint = AmberYellowDark, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "NEAREST SAFE PLACES",
                        fontWeight = FontWeight.Bold,
                        color = AmberYellowDark,
                        fontSize = 18.sp,
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
                                text = "Fetching trusted locations nearby...",
                                color = ModernTextSecondary,
                                fontStyle = FontStyle.Italic,
                                fontSize = 13.sp
                            )
                        } else {
                            // Show 4-5 places (expanded for Path B)
                            nearestSafePlaces.take(5).forEach { place ->
                                SafePlaceRow(
                                    place = place,
                                    onNavigate = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onNavigateToPlace(place)
                                    },
                                    buttonLabel = "Navigate Now",
                                    expanded = true // Show more details
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            // Info text
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ranked by priority and distance. Choose the safest populated route.",
                                color = ModernTextSecondary,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // COLLAPSIBLE Additional Protection Section (SECONDARY for Path B)
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
                        .clickable { additionalProtectionExpanded = !additionalProtectionExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = ModernTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "ADDITIONAL PROTECTION",
                        fontWeight = FontWeight.Bold,
                        color = ModernTextSecondary,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (additionalProtectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = ModernTextSecondary
                    )
                }

                AnimatedVisibility(visible = additionalProtectionExpanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        // Compact 2x2 grid of protection tools
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CompactActionButton(
                                modifier = Modifier.weight(1f),
                                title = if (isLoudAlarmActive) "Stop Alarm" else "Loud Alarm",
                                icon = Icons.Default.Campaign,
                                containerColor = if (isLoudAlarmActive) SafetyRed else SafetyRed.copy(alpha = 0.9f),
                                active = isLoudAlarmActive,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleLoudAlarm()
                                }
                            )

                            CompactActionButton(
                                modifier = Modifier.weight(1f),
                                title = if (isRecordingActive) "Recording..." else "Record",
                                icon = Icons.Default.Mic,
                                containerColor = if (isRecordingActive) Color(0xFFB71C1C) else Color(0xFFD32F2F),
                                active = isRecordingActive,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleRecording()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CompactActionButton(
                                modifier = Modifier.weight(1f),
                                title = if (isFakeCallActive) "End Call" else "Fake Call",
                                icon = Icons.Default.Phone,
                                containerColor = Color(0xFF1565C0),
                                active = isFakeCallActive,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleFakeCall()
                                }
                            )

                            CompactActionButton(
                                modifier = Modifier.weight(1f),
                                title = "Breathing",
                                icon = Icons.Default.SelfImprovement,
                                containerColor = Color(0xFF43A047),
                                active = isBreathingActive,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleBreathing()
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Call police button in this section too
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRequestPolice()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SafetyRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.LocalPolice, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Call Police (112)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Safety Network Status (less prominent than Path A)
        SafetyNetworkStatus(
            alertHistory = alertHistory,
            isRecordingActive = isRecordingActive,
            recordingTime = formattedRecording,
            compact = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Police call section (less prominent)
        CallPoliceSection(
            onRequestPolice = onRequestPolice,
            primary = false // NOT primary in Path B
        )

        // Dialogs
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
                        Text("YES, I'M SAFE", fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onArrivalConfirmation(false) }) {
                        Text("Not yet, continue")
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
    // Use fake 404 error screen as the UI
    Fake404ErrorScreen(
        isStealthMode = isStealthMode,
        autoCamouflage = false,
        onSosButtonClick = { if (isModelLoaded) onEmergencyTrigger() else onLoadModel() }
    )
}

/**
 * Fake 404 Error Screen - Camouflage Mode UI
 * Used when the app is hidden (stealth or auto-camouflage). "404 error: Page Not Found"
 * 
 * The 404 button/screen is ALWAYS clickable to trigger emergency
 */
@Composable
fun Fake404ErrorScreen(
    isStealthMode: Boolean,
    autoCamouflage: Boolean,
    onSosButtonClick: () -> Unit,
    onScreenTap: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isStealthMode || autoCamouflage) {
                        listOf(Color(0xFFF5F1E8), Color(0xFFF5F1E8))
                    } else {
                        listOf(Color(0xFFF5F5F5), Color(0xFFE8E8E8))
                    }
                )
            )
            .pointerInput(Unit) {
                // Click anywhere on screen (background) - NOT on the 404 button
                detectTapGestures(
                    onTap = {
                        onScreenTap()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 404 Error Button - ALWAYS CLICKABLE FOR SOS
            Surface(
                modifier = Modifier
                    .size(220.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSosButtonClick()
                    },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = if (isStealthMode || autoCamouflage) 0.dp else 6.dp,
                border = BorderStroke(
                    width = 2.dp,
                    color = if (isStealthMode || autoCamouflage) Color(0xFFE0E0E0) else Color(
                        0xFFCCCCCC
                    )
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "404",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStealthMode || autoCamouflage) Color(0xFFCCCCCC) else Color(
                                0xFFE0E0E0
                            ),
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "error",
                            fontSize = 18.sp,
                            color = if (isStealthMode || autoCamouflage) Color(0xFF999999) else Color(
                                0xFFCCCCCC
                            ),
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Page Not Found",
                fontSize = 24.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isStealthMode || autoCamouflage)
                    "The page you requested could not be found.\nError 404."
                else
                    "Looks like this page doesn't exist.",
                fontSize = 15.sp,
                color = Color(0xFF999999),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(36.dp))
            if (isStealthMode || autoCamouflage) {
                Text(
                    text = "Tap screen three times quickly to restore app",
                    fontSize = 13.sp,
                    color = Color(0xFFB0B0B0),
                    textAlign = TextAlign.Center,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(18.dp))
            } else {
                // When no emergency active, show subtle hint that 404 button is clickable
                Text(
                    text = "Tap the error code for emergency alert",
                    fontSize = 12.sp,
                    color = Color(0xFFCCCCCC),
                    textAlign = TextAlign.Center,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

/**
 * Fake 404 Error Screen with top-right Info icon for Path B decoy
 */
@Composable
fun Fake404ErrorScreenWithInfoIcon(
    viewModel: SafetyViewModel,
    isStealthMode: Boolean,
    autoCamouflage: Boolean,
    onSosButtonClick: () -> Unit,
    onScreenTap: () -> Unit
) {
    val showInfoIcon by viewModel.showInfoIcon.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF5F1E8), Color(0xFFF5F1E8))
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onScreenTap()
                    }
                )
            }
    ) {
        // Info icon button - top right, only if showInfoIcon
        if (showInfoIcon) {
            IconButton(
                onClick = { viewModel.onInfoIconClicked() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(
                        Color.White.copy(alpha = 0.8f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = "Return to safety navigation",
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Existing 404 content - centered
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 404 Error Button
            Surface(
                modifier = Modifier
                    .size(220.dp)
                    .clickable {
                        onSosButtonClick()
                    },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    width = 2.dp,
                    color = Color(0xFFE0E0E0)
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "404",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCCCCCC),
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "error",
                            fontSize = 18.sp,
                            color = Color(0xFF999999),
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Page Not Found",
                fontSize = 24.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "The page you requested could not be found.\nError 404.",
                fontSize = 15.sp,
                color = Color(0xFF999999),
                textAlign = TextAlign.Center
            )
        }
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
    viewModel: SafetyViewModel,
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
    val showStealthDecoy by viewModel.showStealthDecoy.collectAsState()

    if (showStealthDecoy) {
        Fake404ErrorScreenWithInfoIcon(
            viewModel = viewModel,
            isStealthMode = true,
            autoCamouflage = false,
            onSosButtonClick = { viewModel.triggerEmergencyAlarm() },
            onScreenTap = { viewModel.registerUserInteraction() }
        )
    } else {
        if (emergencyPath == EmergencyPath.THREAT_NEARBY && currentQuestion == null && secondQuestion == null) {
            ThreatNearbyScreen(
                viewModel = viewModel,
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
                viewModel = viewModel,
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
}

@Composable
private fun ThreatNearbyScreen(
    viewModel: SafetyViewModel,
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
    val showDecoyAvailable by viewModel.showDecoyAvailable.collectAsState()
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
        if (showDecoyAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 16.dp, top = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = { viewModel.switchToDecoy() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.8f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "Hide to home screen (decoy)",
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
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
                onClick = onToggleRecording,
                recordingTime = if (isRecordingActive) recordingTime else null,
                recordingUploading = isRecordingActive // show uploading if currently recording
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
    pulseAlpha: Float = 0f,
    recordingTime: String? = null,
    recordingUploading: Boolean = false
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

                // Show extra recording status details if recordingTime is provided
                if (recordingTime != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Recording timestamp",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = recordingTime,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                    if (recordingUploading) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Uploading",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Auto-uploading evidence",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
    buttonLabel: String = "Navigate",
    expanded: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (place.types.firstOrNull()?.lowercase()) {
                    "police" -> Icons.Default.LocalPolice
                    "hospital" -> Icons.Default.LocalHospital
                    "fire" -> Icons.Default.LocalFireDepartment
                    "store" -> Icons.Default.Store
                    else -> Icons.Default.Place
                }
                Icon(icon, contentDescription = null, tint = AmberYellowDark, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(place.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = buildString {
                            place.distance?.let {
                                append("${it.toInt()} m")
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
    // Format timestamp
    val timeText = remember(record.timestamp) {
        val now = System.currentTimeMillis()
        val diff = now - record.timestamp
        when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            else -> {
                val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                formatter.format(java.util.Date(record.timestamp))
            }
        }
    }
    
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = record.messageType.name,
                    color = ModernTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = " • ",
                    color = ModernTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = timeText,
                    color = ModernTextSecondary,
                    fontSize = 12.sp
                )
            }
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
    val haptic = LocalHapticFeedback.current

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
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAnswerYes()
                    },
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
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAnswerNo()
                    },
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
    val haptic = LocalHapticFeedback.current

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
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAnswerYes()
                    },
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
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAnswerNo()
                    },
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

/**
 * Additional Protection Section - Used in Escape to Safety path
 * Provides quick access to safety tools in a compact format
 */
@Composable
private fun AdditionalProtectionSection(
    isLoudAlarmActive: Boolean,
    onToggleLoudAlarm: () -> Unit,
    isRecordingActive: Boolean,
    onToggleRecording: () -> Unit,
    isFakeCallActive: Boolean,
    onToggleFakeCall: () -> Unit,
    isBreathingActive: Boolean,
    onToggleBreathing: () -> Unit,
    onRequestPolice: () -> Unit
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
                text = "ADDITIONAL PROTECTION",
                fontWeight = FontWeight.Bold,
                color = ModernTextPrimary,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Compact action buttons in 2x2 grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactActionButton(
                    modifier = Modifier.weight(1f),
                    title = if (isLoudAlarmActive) "Stop Alarm" else "Loud Alarm",
                    icon = Icons.Default.Campaign,
                    containerColor = if (isLoudAlarmActive) SafetyRed else SafetyRed.copy(alpha = 0.9f),
                    active = isLoudAlarmActive,
                    onClick = onToggleLoudAlarm
                )

                CompactActionButton(
                    modifier = Modifier.weight(1f),
                    title = if (isRecordingActive) "Recording..." else "Record",
                    icon = Icons.Default.Mic,
                    containerColor = if (isRecordingActive) Color(0xFFB71C1C) else Color(0xFFD32F2F),
                    active = isRecordingActive,
                    onClick = onToggleRecording
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactActionButton(
                    modifier = Modifier.weight(1f),
                    title = if (isFakeCallActive) "End Call" else "Fake Call",
                    icon = Icons.Default.Phone,
                    containerColor = Color(0xFF1565C0),
                    active = isFakeCallActive,
                    onClick = onToggleFakeCall
                )

                CompactActionButton(
                    modifier = Modifier.weight(1f),
                    title = "Breathing",
                    icon = Icons.Default.SelfImprovement,
                    containerColor = Color(0xFF43A047),
                    active = isBreathingActive,
                    onClick = onToggleBreathing
                )
            }
        }
    }
}

@Composable
private fun CompactActionButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    containerColor: Color,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        tonalElevation = if (active) 8.dp else 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * Fake Call Overlay - Realistic incoming call screen
 * Makes it appear like the user is receiving a call, useful for discreet exit from dangerous situations
 */
@Composable
fun FakeCallOverlay(
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    // Pulsing animation for the call screen
    val infiniteTransition = rememberInfiniteTransition(label = "callPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Caller info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile picture placeholder
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50),
                                    Color(0xFF2E7D32)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Dad",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "mobile",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Pulsing "incoming call" indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "incoming call...",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontStyle = FontStyle.Italic
                    )
                }
            }
            
            // Call action buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            shape = CircleShape,
                            color = Color(0xFF3C3C3E)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Remind me",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Remind Me",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            shape = CircleShape,
                            color = Color(0xFF3C3C3E)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Sms,
                                    contentDescription = "Message",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Message",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Main call buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Decline button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(72.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDismiss()
                                },
                            shape = CircleShape,
                            color = SafetyRed
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CallEnd,
                                    contentDescription = "Decline",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Decline",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    
                    // Accept button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(72.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Simulate answering - just close after brief delay
                                    onDismiss()
                                },
                            shape = CircleShape,
                            color = Color(0xFF4CAF50)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "Accept",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Accept",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/**
 * Breathing Exercise Overlay - Full-screen guided breathing
 * 4-4-4 pattern: Breathe in (4s), Hold (4s), Breathe out (4s)
 */
@Composable
fun BreathingExerciseOverlay(
    onDismiss: () -> Unit
) {
    var breathingPhase by remember { mutableStateOf(BreathingPhase.BREATHE_IN) }
    var countdown by remember { mutableStateOf(4) }
    
    // Animation for the breathing circle
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val circleSize by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circleSize"
    )
    
    // Countdown timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (countdown > 1) {
                countdown--
            } else {
                // Move to next phase
                breathingPhase = when (breathingPhase) {
                    BreathingPhase.BREATHE_IN -> BreathingPhase.HOLD
                    BreathingPhase.HOLD -> BreathingPhase.BREATHE_OUT
                    BreathingPhase.BREATHE_OUT -> BreathingPhase.BREATHE_IN
                }
                countdown = 4
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),
                        Color(0xFF0D47A1),
                        Color(0xFF01579B)
                    )
                )
            )
    ) {
        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Exit",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Breathing Exercise",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Follow the circle and breathe calmly",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Animated breathing circle
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .size(circleSize.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                // Main circle
                Box(
                    modifier = Modifier
                        .size((circleSize * 0.8f).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF64B5F6),
                                    Color(0xFF2196F3)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$countdown",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Phase indicator
            Text(
                text = when (breathingPhase) {
                    BreathingPhase.BREATHE_IN -> "Breathe In"
                    BreathingPhase.HOLD -> "Hold"
                    BreathingPhase.BREATHE_OUT -> "Breathe Out"
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = when (breathingPhase) {
                    BreathingPhase.BREATHE_IN -> "Slowly inhale through your nose"
                    BreathingPhase.HOLD -> "Hold your breath gently"
                    BreathingPhase.BREATHE_OUT -> "Slowly exhale through your mouth"
                },
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class BreathingPhase {
    BREATHE_IN,
    HOLD,
    BREATHE_OUT
}

/**
 * Permission Denied Dialog - Shows friendly error and alternatives
 * Follows Material 3 guidelines for error handling
 */
@Composable
fun PermissionDeniedDialog(
    permissionName: String,
    featureName: String,
    explanation: String,
    alternatives: List<String>,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = WarningOrange,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "$permissionName Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ModernTextPrimary,
                    lineHeight = 24.sp
                )
                
                if (alternatives.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Available alternatives:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ModernTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    alternatives.forEach { alternative ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TrustBlue
                            )
                            Text(
                                text = alternative,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ModernTextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrustBlue
                ),
                modifier = Modifier.heightIn(min = 48.dp) // Min touch target
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Open Settings",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 16.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp) // Min touch target
            ) {
                Text(
                    "Maybe Later",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 16.sp
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        tonalElevation = 8.dp
    )
}
