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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.ProtocolQuestion
import com.runanywhere.startup_hackathon20.SafetyViewModel
import com.runanywhere.startup_hackathon20.ui.theme.*
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.*

@Composable
fun EmergencyScreen(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val isAlarmActive by viewModel.isAlarmActive.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val timeRemaining by viewModel.questionTimeRemaining.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isModelLoaded by viewModel.isModelLoaded.collectAsState()
    val contacts by viewModel.emergencyContacts.collectAsState()
    val alertHistory by viewModel.alertHistory.collectAsState()

    // Track if user manually requested stealth mode
    var userRequestedStealthMode by remember { mutableStateOf(false) }

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
                timeRemaining = timeRemaining,
                statusMessage = statusMessage,
                onAnswerYes = { viewModel.answerProtocolQuestionYes() },
                onAnswerNo = { viewModel.answerProtocolQuestionNo() },
                onCancel = { viewModel.cancelEmergencyAlarm() }
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
                    text = if (isModelLoaded) "System initialized" else "System offline",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF2D2D2D),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Main 404 Button - Large circular button with gray outline
        Circular404Button(
            enabled = isModelLoaded && !isStealthMode,
            onClick = onEmergencyTrigger,
            isStealthMode = isStealthMode
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isStealthMode) "system error - retry later" else "tap to retry connection",
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
            shadowElevation = if (enabled && !isStealthMode) 4.dp else 0.dp,
            border = BorderStroke(
                width = 3.dp,
                color = if (enabled && !isStealthMode) Color(0xFFCCCCCC) else Color(0xFFE0E0E0)
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = enabled && !isStealthMode) { onClick() }
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
                                if (enabled && !isStealthMode) onClick()
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
                        color = if (enabled && !isStealthMode) Color(0xFFCCCCCC) else Color(
                            0xFFE0E0E0
                        ),
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "error",
                        fontSize = 16.sp,
                        color = if (enabled && !isStealthMode) Color(0xFF999999) else Color(
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
    timeRemaining: Int?,
    statusMessage: String,
    onAnswerYes: () -> Unit,
    onAnswerNo: () -> Unit,
    onCancel: () -> Unit
) {
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

        // Show question if available, otherwise show monitoring status
        if (currentQuestion != null) {
            // Question card - simple and clear
            QuestionCard(
                question = currentQuestion,
                timeRemaining = timeRemaining,
                onAnswerYes = onAnswerYes,
                onAnswerNo = onAnswerNo
            )
        } else {
            // Monitoring status card (shown after answer or during processing)
            MonitoringCard()
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
