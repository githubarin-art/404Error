package com.runanywhere.startup_hackathon20

import android.location.Location

// Emergency Contact Data
data class EmergencyContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String, // "Family", "Friend", "Guardian", etc.
    val priority: Int, // 1 (highest) to 5 (lowest)
    val canReceiveLocation: Boolean = true
)

// Threat Level Assessment
enum class ThreatLevel {
    UNKNOWN,        // Initial state
    LOW,            // Victim can handle, notify family
    MEDIUM,         // Victim needs help, alert close contacts
    HIGH,           // Victim cannot respond, alert emergency services
    CRITICAL        // Immediate danger, call police + emergency contacts
}

// Protocol Question for Threat Assessment
data class ProtocolQuestion(
    val id: String,
    val question: String,
    val timeoutSeconds: Int = 30, // Time to answer before escalating
    val threatLevelIfAnswered: ThreatLevel,
    val threatLevelIfNotAnswered: ThreatLevel
)

// Emergency Session
data class EmergencySession(
    val sessionId: String,
    val startTime: Long,
    val alarmTriggeredTime: Long,
    val currentThreatLevel: ThreatLevel = ThreatLevel.UNKNOWN,
    val location: Location? = null,
    val victimResponses: List<VictimResponse> = emptyList(),
    val alertsSent: List<AlertRecord> = emptyList(),
    val isActive: Boolean = true
)

// Victim Response to Protocol Question
data class VictimResponse(
    val questionId: String,
    val answered: Boolean,
    val responseTime: Long, // Timestamp
    val timeTakenSeconds: Int
)

// Alert Record
data class AlertRecord(
    val timestamp: Long,
    val recipientType: RecipientType,
    val recipientName: String,
    val recipientPhone: String?,
    val messageType: MessageType,
    val success: Boolean,
    val errorMessage: String? = null
)

enum class RecipientType {
    FAMILY,
    FRIEND,
    EMERGENCY_SERVICES,
    POLICE,
    MEDICAL
}

enum class MessageType {
    SMS,
    CALL,
    MISSED_CALL,
    EMERGENCY_CALL
}

// AI Decision Context
data class AIDecisionContext(
    val threatLevel: ThreatLevel,
    val victimResponded: Boolean,
    val timeSinceAlarm: Long,
    val location: Location?,
    val previousAlerts: List<AlertRecord>,
    val availableContacts: List<EmergencyContact>
)

// AI Action Decision
data class AIActionDecision(
    val recommendedActions: List<EmergencyAction>,
    val reasoning: String, // AI explanation for logging
    val urgencyScore: Int // 1-10
)

sealed class EmergencyAction {
    data class SendSMS(val contact: EmergencyContact, val message: String) : EmergencyAction()
    data class MakeCall(val contact: EmergencyContact) : EmergencyAction()
    data class MakeMissedCall(val contact: EmergencyContact) : EmergencyAction()
    data class CallEmergencyServices(val serviceType: String, val location: Location?) : EmergencyAction()
    data class UpdateThreatLevel(val newLevel: ThreatLevel) : EmergencyAction()
}
