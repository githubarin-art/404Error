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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isAlarmActive) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFB71C1C),
                            Color(0xFF880E4F)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            )
    ) {
        if (!isAlarmActive) {
            NormalModeUI(
                isModelLoaded = isModelLoaded,
                statusMessage = statusMessage,
                contacts = contacts,
                onEmergencyTrigger = { viewModel.triggerEmergencyAlarm() },
                onLoadModel = { viewModel.loadAIModel("Qwen 2.5 0.5B Instruct Q6_K") }
            )
        } else {
            EmergencyModeUI(
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
    onLoadModel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = statusMessage,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Main SOS Button
        AnimatedSOSButton(
            enabled = isModelLoaded,
            onClick = onEmergencyTrigger
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Triple tap or long press for instant SOS",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        if (!isModelLoaded) {
            Button(
                onClick = onLoadModel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load AI Model")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Note: First time may take a while to download",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contacts Summary
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Emergency Contacts: ${contacts.size}",
                    style = MaterialTheme.typography.titleSmall
                )
                contacts.take(3).forEach { contact ->
                    Text("• ${contact.name} (${contact.relationship})")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AnimatedSOSButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(220.dp)
            .scale(scale * if (enabled) pulseScale else 1f)
            .clip(CircleShape)
            .background(
                if (enabled) {
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF5252),
                            Color(0xFFD32F2F)
                        )
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Gray,
                            Color.DarkGray
                        )
                    )
                }
            )
            .clickable(enabled = enabled) { onClick() }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
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
                text = "🚨",
                fontSize = 72.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (enabled) "SOS" else "LOAD MODEL",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (enabled) {
                Text(
                    text = "EMERGENCY",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun EmergencyModeUI(
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
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "🚨 EMERGENCY ACTIVE",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = statusMessage,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        currentQuestion?.let { question ->
            QuestionCard(
                question = question,
                timeRemaining = timeRemaining,
                onAnswerYes = onAnswerYes,
                onAnswerNo = onAnswerNo
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            ),
            border = BorderStroke(2.dp, Color.White)
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cancel Emergency (False Alarm)", fontSize = 16.sp)
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
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = question.question,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(24.dp))

            timeRemaining?.let { time ->
                val progress = time.toFloat() / question.timeoutSeconds

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$time seconds remaining",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (time <= 10) Color.Red else Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (time <= 10) Color.Red else MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onAnswerYes,
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Text("YES", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onAnswerNo,
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Text("NO", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
