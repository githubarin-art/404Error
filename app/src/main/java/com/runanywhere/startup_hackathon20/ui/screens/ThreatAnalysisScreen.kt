package com.runanywhere.startup_hackathon20.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.*
import com.runanywhere.startup_hackathon20.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ThreatAnalysisScreen(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val location by viewModel.currentLocation.collectAsState()

    val threatEngine = remember { ThreatAnalysisEngine(context) }
    var threatResult by remember { mutableStateOf<ThreatAnalysisResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var lastUpdateTime by remember { mutableStateOf(System.currentTimeMillis()) }

    val scope = rememberCoroutineScope()

    // Auto-refresh every minute
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // 1 minute
            isAnalyzing = true
            val result = threatEngine.analyzeThreatLevel(location, forceRefresh = true)
            threatResult = result
            lastUpdateTime = System.currentTimeMillis()
            isAnalyzing = false
        }
    }

    // Initial load
    LaunchedEffect(location) {
        if (threatResult == null) {
            isAnalyzing = true
            val result = threatEngine.analyzeThreatLevel(location, forceRefresh = false)
            threatResult = result
            lastUpdateTime = System.currentTimeMillis()
            isAnalyzing = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = when (threatResult?.threatLevel) {
                RegionalThreatLevel.LOW -> SuccessGreen
                RegionalThreatLevel.MEDIUM -> AmberYellowDark
                RegionalThreatLevel.HIGH -> SafetyRed
                null -> CharcoalMedium
            },
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "Threat Analysis",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Real-time safety assessment",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Threat Level Card
            item {
                ThreatLevelCard(
                    threatResult = threatResult,
                    isAnalyzing = isAnalyzing,
                    lastUpdateTime = lastUpdateTime,
                    onRefresh = {
                        scope.launch {
                            isAnalyzing = true
                            val result =
                                threatEngine.analyzeThreatLevel(location, forceRefresh = true)
                            threatResult = result
                            lastUpdateTime = System.currentTimeMillis()
                            isAnalyzing = false
                        }
                    }
                )
            }

            // Threat Factors
            if (threatResult != null && threatResult!!.factors.isNotEmpty()) {
                item {
                    Text(
                        "Risk Factors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Charcoal,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(threatResult!!.factors.sortedByDescending { it.contribution }) { factor ->
                    ThreatFactorCard(factor)
                }
            }

            // Primary Reasons
            if (threatResult != null && threatResult!!.primaryReasons.isNotEmpty()) {
                item {
                    Text(
                        "Key Concerns",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Charcoal,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            threatResult!!.primaryReasons.forEach { reason ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = AmberYellowDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        reason,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Charcoal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Data Availability
            if (threatResult != null) {
                item {
                    DataAvailabilityCard(threatResult!!)
                }
            }

            // Disclaimer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TrustBlue.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = TrustBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                "Reliable Data Analysis",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TrustBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "This analysis uses authentic data from your device's sensors, actual time patterns, and statistical crime data. Scores remain consistent for 5 minutes to provide reliable threat assessment you can trust.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CharcoalMedium
                            )
                        }
                    }
                }
            }

            // Legal Disclaimer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AmberYellow.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = AmberYellowDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "This analysis is based on statistical patterns and does not guarantee actual danger or outcomes. Always trust your instincts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CharcoalMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThreatLevelCard(
    threatResult: ThreatAnalysisResult?,
    isAnalyzing: Boolean,
    lastUpdateTime: Long,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 6.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Analyzing...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalMedium
                )
            } else if (threatResult != null) {
                // Threat Level Badge
                val threatColor = when (threatResult.threatLevel) {
                    RegionalThreatLevel.LOW -> SuccessGreen
                    RegionalThreatLevel.MEDIUM -> AmberYellowDark
                    RegionalThreatLevel.HIGH -> SafetyRed
                }

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(threatColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${(threatResult.threatScore * 100).toInt()}",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = threatColor,
                            fontSize = 48.sp
                        )
                        Text(
                            "SCORE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = threatColor,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "${threatResult.threatLevel.displayName.uppercase()} THREAT",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = threatColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    when (threatResult.threatLevel) {
                        RegionalThreatLevel.LOW -> "Your current area appears relatively safe"
                        RegionalThreatLevel.MEDIUM -> "Exercise normal caution in this area"
                        RegionalThreatLevel.HIGH -> "High-risk conditions detected - stay alert"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confidence Bar
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Confidence",
                            style = MaterialTheme.typography.labelMedium,
                            color = CharcoalMedium
                        )
                        Text(
                            "${(threatResult.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Charcoal
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { threatResult.confidence },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = threatColor,
                        trackColor = threatColor.copy(alpha = 0.2f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Last Update
                val timeSinceUpdate = (System.currentTimeMillis() - lastUpdateTime) / 1000
                Text(
                    "Updated ${timeSinceUpdate}s ago",
                    style = MaterialTheme.typography.labelSmall,
                    color = CharcoalLight
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Refresh Button
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh Analysis")
                }
            } else {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = CharcoalLight
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Data Available",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalMedium
                )
            }
        }
    }
}

@Composable
fun ThreatFactorCard(factor: ThreatFactor) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        factor.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Charcoal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        factor.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = CharcoalMedium
                    )
                }

                // Score Badge
                val scoreColor = when {
                    factor.value < 0.33f -> SuccessGreen
                    factor.value < 0.67f -> AmberYellowDark
                    else -> SafetyRed
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(scoreColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${(factor.value * 100).toInt()}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { factor.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    factor.value < 0.33f -> SuccessGreen
                    factor.value < 0.67f -> AmberYellowDark
                    else -> SafetyRed
                },
                trackColor = CharcoalLight.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun DataAvailabilityCard(result: ThreatAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TrustBlue.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Data Sources",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Charcoal
                )
                Text(
                    "${result.dataSourcesAvailable}/${result.dataSourcesTotal}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TrustBlue
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { result.dataSourcesAvailable.toFloat() / result.dataSourcesTotal.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = TrustBlue,
                trackColor = TrustBlue.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Analysis based on ${result.dataSourcesAvailable} available data sources",
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalMedium
            )
        }
    }
}
