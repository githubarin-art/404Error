package com.runanywhere.startup_hackathon20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Decoy 404 Error Screen
 * 
 * Serves as camouflage during emergencies to hide the real app from attackers.
 * - Looks like a generic web error page
 * - All emergency operations continue silently in background
 * - Triple-tap anywhere within 2 seconds to restore real UI
 * - Buttons are fake and do nothing
 */
@Composable
fun DecoyErrorScreen(
    onTripleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var showTapHint by remember { mutableStateOf(false) }

    // Reset tap count after timeout
    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            delay(2000) // 2 second window for triple tap
            if (tapCount < 3) {
                tapCount = 0
            }
        }
    }

    // Show hint after 2 taps
    LaunchedEffect(showTapHint) {
        if (showTapHint) {
            delay(3000)
            showTapHint = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectTapGestures {
                    val now = System.currentTimeMillis()

                    // Check if within 2 second window
                    if (now - lastTapTime < 2000) {
                        tapCount++

                        if (tapCount >= 3) {
                            // Triple tap detected - restore emergency UI
                            onTripleTap()
                            tapCount = 0
                        } else if (tapCount == 2) {
                            // Show subtle hint after 2 taps
                            showTapHint = true
                        }
                    } else {
                        // Reset counter if too much time passed
                        tapCount = 1
                    }

                    lastTapTime = now
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Error code
            Text(
                text = "404",
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF333333),
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error title
            Text(
                text = "Page Not Found",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF555555),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error description
            Text(
                text = "The page you are looking for might have been removed, had its name changed, or is temporarily unavailable.",
                fontSize = 16.sp,
                color = Color(0xFF777777),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Fake buttons (do nothing)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Fake "Try Again" button
                OutlinedButton(
                    onClick = { /* Do nothing - fake button */ },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF666666)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Again", fontSize = 16.sp)
                }

                // Fake "Go Back" button
                Button(
                    onClick = { /* Do nothing - fake button */ },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF666666),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go Back", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Error code (technical looking)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Error Details",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF555555)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error Code: HTTP 404\nRequest ID: ${generateFakeRequestId()}\nTimestamp: ${getCurrentTimestamp()}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF888888),
                        lineHeight = 18.sp
                    )
                }
            }

            // Subtle hint after 2 taps (very discreet)
            if (showTapHint) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = ".",
                    fontSize = 10.sp,
                    color = Color(0xFFEEEEEE), // Almost invisible
                    modifier = Modifier.alpha(0.1f)
                )
            }
        }

        // Footer (looks like generic error page footer)
        Text(
            text = "If you continue to experience this issue, please contact support.",
            fontSize = 12.sp,
            color = Color(0xFF999999),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .padding(horizontal = 32.dp)
        )
    }
}

/**
 * Generate a fake request ID to make the error look authentic
 */
private fun generateFakeRequestId(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return buildString {
        repeat(8) {
            append(chars.random())
        }
        append("-")
        repeat(4) {
            append(chars.random())
        }
    }
}

/**
 * Get current timestamp in technical format
 */
private fun getCurrentTimestamp(): String {
    val now = java.util.Date()
    val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss UTC", java.util.Locale.US)
    format.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return format.format(now)
}
