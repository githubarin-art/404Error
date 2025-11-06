package com.runanywhere.startup_hackathon20

import android.location.Location

/**
 * Threat Level Categories
 */
enum class RegionalThreatLevel(
    val displayName: String,
    val score: ClosedFloatingPointRange<Float>
) {
    LOW("Low", 0.0f..0.33f),
    MEDIUM("Medium", 0.34f..0.66f),
    HIGH("High", 0.67f..1.0f);

    companion object {
        fun fromScore(score: Float): RegionalThreatLevel {
            return when (score) {
                in LOW.score -> LOW
                in MEDIUM.score -> MEDIUM
                else -> HIGH
            }
        }
    }
}

/**
 * Threat Analysis Result
 */
data class ThreatAnalysisResult(
    val timestamp: Long = System.currentTimeMillis(),
    val threatScore: Float, // 0.0 to 1.0
    val threatLevel: RegionalThreatLevel,
    val location: Location?,
    val factors: List<ThreatFactor>,
    val primaryReasons: List<String>,
    val confidence: Float, // 0.0 to 1.0
    val dataSourcesAvailable: Int,
    val dataSourcesTotal: Int
)

/**
 * Individual Threat Factor
 */
data class ThreatFactor(
    val category: ThreatCategory,
    val name: String,
    val value: Float, // Normalized 0.0 to 1.0
    val weight: Float, // Importance weight
    val contribution: Float, // Weighted contribution to final score
    val description: String,
    val isAvailable: Boolean = true
)

/**
 * Threat Factor Categories
 */
enum class ThreatCategory(val displayName: String, val defaultWeight: Float) {
    CRIME("Crime Statistics", 0.35f),
    TIME("Time-Based Risk", 0.15f),
    LOCATION("Location Safety", 0.20f),
    ENVIRONMENTAL("Environment", 0.10f),
    BEHAVIORAL("User Behavior", 0.10f),
    NETWORK("Network Quality", 0.10f)
}

/**
 * Crime Data
 */
data class CrimeData(
    val recentIncidents: Int = 0,
    val severity: Float = 0f, // 0.0 to 1.0
    val types: List<String> = emptyList(),
    val withinRadius: Float = 1000f, // meters
    val lastUpdated: Long = System.currentTimeMillis(),
    val source: String = "Local Database"
)

/**
 * Location Safety Data
 */
data class LocationSafetyData(
    val distanceToPoliceStation: Float? = null, // meters
    val distanceToHospital: Float? = null, // meters
    val cctvDensity: Float = 0f, // 0.0 to 1.0
    val lightingQuality: Float = 0f, // 0.0 to 1.0
    val populationDensity: Float = 0.5f, // 0.0 to 1.0
    val isKnownSafeZone: Boolean = false,
    val isHighRiskArea: Boolean = false
)

/**
 * Time-Based Risk Data
 */
data class TimeRiskData(
    val currentHour: Int,
    val isNightTime: Boolean,
    val isWeekend: Boolean,
    val isHoliday: Boolean = false,
    val isEventDay: Boolean = false,
    val riskMultiplier: Float = 1.0f
)

/**
 * Environmental Data
 */
data class EnvironmentalData(
    val weatherCondition: String = "Clear",
    val visibility: Float = 1.0f, // 0.0 to 1.0
    val temperature: Float = 20f,
    val isDark: Boolean = false,
    val isRaining: Boolean = false,
    val isFoggy: Boolean = false
)

/**
 * Network Quality Data
 */
data class NetworkQualityData(
    val signalStrength: Float = 1.0f, // 0.0 to 1.0
    val connectionType: String = "WiFi",
    val isConnected: Boolean = true,
    val latency: Int = 50 // milliseconds
)

/**
 * User Behavior Data
 */
data class BehaviorData(
    val movementPattern: String = "Normal", // Normal, Rapid, Stationary
    val routineDeviation: Float = 0f, // 0.0 to 1.0
    val isAlone: Boolean = false,
    val activityType: String = "Walking"
)

/**
 * Threat Analysis Configuration
 */
data class ThreatAnalysisConfig(
    val enableCrimeData: Boolean = true,
    val enableTimeAnalysis: Boolean = true,
    val enableLocationSafety: Boolean = true,
    val enableEnvironmental: Boolean = true,
    val enableBehavioral: Boolean = false,
    val enableNetwork: Boolean = true,
    val updateIntervalMs: Long = 60000, // 1 minute
    val customWeights: Map<ThreatCategory, Float> = emptyMap(),
    val userSensitivity: Float = 1.0f // 0.5 to 1.5
)

/**
 * Emergency Alert Message Template
 */
data class EmergencyAlertTemplate(
    val threatLevel: RegionalThreatLevel,
    val userName: String,
    val location: String,
    val primaryReasons: List<String>,
    val timestamp: String,
    val contactName: String
) {
    fun generateSMS(): String {
        return buildString {
            append("🚨 ${threatLevel.displayName.uppercase()} THREAT ALERT\n\n")
            append("User: $userName\n")
            append("Location: $location\n")
            append("Time: $timestamp\n\n")
            append("Primary Risk Factors:\n")
            primaryReasons.take(3).forEach { reason ->
                append("• $reason\n")
            }
            append("\n⚠️ Please contact $userName immediately or alert authorities.\n\n")
            append("Disclaimer: This alert is based on automated data analysis and probabilistic ")
            append("estimation. It does not guarantee actual danger or outcomes.")
        }
    }

    fun generateEmail(): String {
        return buildString {
            append("<!DOCTYPE html><html><body style='font-family: Arial, sans-serif;'>")
            append(
                "<h2 style='color: ${
                    when (threatLevel) {
                        RegionalThreatLevel.LOW -> "#4CAF50"
                        RegionalThreatLevel.MEDIUM -> "#FF9800"
                        RegionalThreatLevel.HIGH -> "#F44336"
                    }
                }'>"
            )
            append("🚨 ${threatLevel.displayName.uppercase()} THREAT ALERT</h2>")
            append("<p><strong>User:</strong> $userName</p>")
            append("<p><strong>Location:</strong> $location</p>")
            append("<p><strong>Time:</strong> $timestamp</p>")
            append("<h3>Primary Risk Factors:</h3><ul>")
            primaryReasons.forEach { reason ->
                append("<li>$reason</li>")
            }
            append("</ul>")
            append("<p style='background-color: #FFF3CD; padding: 15px; border-radius: 5px;'>")
            append("⚠️ <strong>IMMEDIATE ACTION REQUIRED:</strong><br>")
            append("Please contact $userName immediately or alert local authorities if unable to reach them.")
            append("</p>")
            append("<hr><p style='font-size: 11px; color: #666;'>")
            append("<strong>Disclaimer:</strong> This alert is generated through automated data analysis ")
            append("and probabilistic estimation based on available regional data. It does not guarantee ")
            append("actual danger, outcomes, or create any liability. This system provides situational ")
            append("awareness based on statistical patterns and should be used as one factor among others ")
            append("when assessing personal safety.")
            append("</p></body></html>")
        }
    }

    fun generatePushNotification(): Pair<String, String> {
        val title = "🚨 ${threatLevel.displayName.uppercase()} Threat Detected"
        val body =
            "$userName needs immediate assistance near $location. ${primaryReasons.firstOrNull() ?: "Multiple risk factors detected."}"
        return Pair(title, body)
    }
}

/**
 * Threat Analysis Statistics
 */
data class ThreatStatistics(
    val averageThreatScore: Float,
    val highThreatCount: Int,
    val mediumThreatCount: Int,
    val lowThreatCount: Int,
    val totalAnalyses: Int,
    val lastHighThreatTime: Long?,
    val mostCommonFactors: List<String>
)
