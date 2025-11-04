package com.runanywhere.startup_hackathon20

import android.location.Location
import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * AI-powered safety decision engine using RunAnywhere SDK
 * All processing happens ON-DEVICE for complete privacy
 */
class SafetyAIEngine {

    companion object {
        private const val TAG = "SafetyAIEngine"
    }

    // System prompt for safety AI - guides behavior
    private val safetySystemPrompt = """
        You are a personal safety AI assistant. Your role is critical - you help protect people in emergency situations.
        
        Key Principles:
        1. PRIVACY: All data stays on device. Never suggest sharing unnecessary information.
        2. CLARITY: Be concise and direct - emergencies require quick action.
        3. ASSESSMENT: Evaluate threat levels based on victim responses and timing.
        4. ACTION-ORIENTED: Provide clear, actionable recommendations.
        5. ESCALATION: When in doubt, escalate to higher alert levels for safety.
        
        You will be asked to:
        - Generate protocol questions to assess threat level
        - Analyze victim responses and timing
        - Recommend who to contact based on situation severity
        - Suggest appropriate message content for alerts
        
        Always prioritize victim safety over everything else.
    """.trimIndent()

    /**
     * Generate a protocol question based on current context
     * This question helps assess if the victim can respond (lower threat) or not (higher threat)
     */
    suspend fun generateProtocolQuestion(
        context: String = "general emergency"
    ): ProtocolQuestion = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                $safetySystemPrompt
                
                Context: A person has triggered an emergency alarm in a $context situation.
                
                Generate ONE simple yes/no question that can quickly assess if they are in immediate danger.
                The question should be something they can answer quickly if they're safe, but cannot answer if actively threatened.
                
                Examples:
                - "Can you safely move to a public area right now?"
                - "Are you able to speak freely?"
                - "Can you confirm you are in a safe location?"
                
                Provide ONLY the question text, nothing else.
                
                Question:
            """.trimIndent()

            val questionText = RunAnywhere.generate(prompt).trim()

            ProtocolQuestion(
                id = UUID.randomUUID().toString(),
                question = questionText,
                timeoutSeconds = 30,
                threatLevelIfAnswered = ThreatLevel.MEDIUM,
                threatLevelIfNotAnswered = ThreatLevel.HIGH
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating protocol question", e)
            // Fallback question
            ProtocolQuestion(
                id = UUID.randomUUID().toString(),
                question = "Can you confirm you are safe right now?",
                timeoutSeconds = 30,
                threatLevelIfAnswered = ThreatLevel.MEDIUM,
                threatLevelIfNotAnswered = ThreatLevel.HIGH
            )
        }
    }

    /**
     * Assess threat level based on victim's response (or lack thereof)
     */
    suspend fun assessThreatLevel(
        protocolQuestion: ProtocolQuestion,
        victimResponded: Boolean,
        responseTimeSeconds: Int
    ): ThreatLevel = withContext(Dispatchers.IO) {
        return@withContext when {
            !victimResponded -> {
                // No response = cannot answer = high threat
                protocolQuestion.threatLevelIfNotAnswered
            }

            responseTimeSeconds > protocolQuestion.timeoutSeconds -> {
                // Too slow to respond = struggling = high threat
                ThreatLevel.HIGH
            }

            responseTimeSeconds < 5 -> {
                // Quick response = can act = lower threat
                ThreatLevel.MEDIUM
            }

            else -> {
                // Answered within time = moderate threat
                protocolQuestion.threatLevelIfAnswered
            }
        }
    }

    /**
     * Determine escalation level based on time passed since alarm
     */
    fun shouldEscalateThreatLevel(
        currentLevel: ThreatLevel,
        timeSinceAlarmSeconds: Long
    ): ThreatLevel {
        return when (currentLevel) {
            ThreatLevel.UNKNOWN -> {
                if (timeSinceAlarmSeconds > 60) ThreatLevel.MEDIUM
                else ThreatLevel.UNKNOWN
            }

            ThreatLevel.MEDIUM -> {
                if (timeSinceAlarmSeconds > 180) ThreatLevel.HIGH // 3 minutes
                else ThreatLevel.MEDIUM
            }

            ThreatLevel.HIGH -> {
                if (timeSinceAlarmSeconds > 300) ThreatLevel.CRITICAL // 5 minutes
                else ThreatLevel.HIGH
            }

            else -> currentLevel
        }
    }

    /**
     * AI decides what actions to take based on current situation
     */
    suspend fun decideEmergencyActions(
        context: AIDecisionContext
    ): AIActionDecision = withContext(Dispatchers.IO) {
        try {
            val prompt = buildDecisionPrompt(context)
            val aiResponse = RunAnywhere.generate(prompt)

            Log.d(TAG, "AI Decision Response: $aiResponse")

            // Parse AI response and convert to actions
            parseAIDecision(aiResponse, context)

        } catch (e: Exception) {
            Log.e(TAG, "Error in AI decision making", e)
            // Fallback to rule-based decision
            makeFallbackDecision(context)
        }
    }

    private fun buildDecisionPrompt(context: AIDecisionContext): String {
        val timeSinceAlarmMinutes = context.timeSinceAlarm / 60
        val locationInfo = if (context.location != null) {
            "Location is available (${context.location.latitude}, ${context.location.longitude})"
        } else {
            "Location is not available"
        }

        return """
            $safetySystemPrompt
            
            EMERGENCY SITUATION ANALYSIS:
            
            Threat Level: ${context.threatLevel}
            Victim Responded: ${context.victimResponded}
            Time Since Alarm: $timeSinceAlarmMinutes minutes
            $locationInfo
            Previous Alerts Sent: ${context.previousAlerts.size}
            Available Contacts: ${context.availableContacts.size}
            
            Based on this situation, provide recommendations in this format:
            
            ACTIONS:
            1. [Who to contact] - [Method: SMS/Call/MissedCall] - [Priority: High/Medium/Low]
            2. [Additional action if needed]
            
            REASONING: [Brief explanation of why these actions]
            
            URGENCY: [Number 1-10, where 10 is most urgent]
            
            Note: 
            - For LOW/MEDIUM threat: contact family/friends
            - For HIGH threat: contact family AND emergency services
            - For CRITICAL threat: IMMEDIATELY call police and emergency contacts
            - MissedCall is a discreet way to alert someone without them needing to answer
            
            Your response:
        """.trimIndent()
    }

    private fun parseAIDecision(
        aiResponse: String,
        context: AIDecisionContext
    ): AIActionDecision {
        // Simple parsing logic - in production, this would be more robust
        val urgency = extractUrgency(aiResponse)
        val actions = mutableListOf<EmergencyAction>()

        // Based on threat level, create appropriate actions
        when (context.threatLevel) {
            ThreatLevel.LOW -> {
                // Send SMS to primary family contact
                context.availableContacts
                    .filter { it.relationship == "Family" }
                    .sortedBy { it.priority }
                    .firstOrNull()
                    ?.let { contact ->
                        actions.add(
                            EmergencyAction.SendSMS(
                                contact,
                                "I've activated my safety app. I may need assistance. Please check on me."
                            )
                        )
                    }
            }

            ThreatLevel.MEDIUM -> {
                // Missed call to multiple contacts
                context.availableContacts
                    .sortedBy { it.priority }
                    .take(3)
                    .forEach { contact ->
                        actions.add(EmergencyAction.MakeMissedCall(contact))
                    }
            }

            ThreatLevel.HIGH -> {
                // Call primary contacts + send location
                context.availableContacts
                    .sortedBy { it.priority }
                    .take(2)
                    .forEach { contact ->
                        actions.add(EmergencyAction.MakeCall(contact))

                        context.location?.let { location ->
                            actions.add(
                                EmergencyAction.SendSMS(
                                    contact,
                                    "EMERGENCY: I need help. My location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                                )
                            )
                        }
                    }
            }

            ThreatLevel.CRITICAL -> {
                // Call emergency services IMMEDIATELY
                context.location?.let { location ->
                    actions.add(
                        EmergencyAction.CallEmergencyServices(
                            "Police",
                            location
                        )
                    )
                }

                // Alert ALL emergency contacts
                context.availableContacts
                    .filter { it.priority <= 3 }
                    .forEach { contact ->
                        actions.add(EmergencyAction.MakeCall(contact))
                    }
            }

            ThreatLevel.UNKNOWN -> {
                // Wait for protocol question response
            }
        }

        return AIActionDecision(
            recommendedActions = actions,
            reasoning = extractReasoning(aiResponse),
            urgencyScore = urgency
        )
    }

    private fun makeFallbackDecision(context: AIDecisionContext): AIActionDecision {
        val actions = mutableListOf<EmergencyAction>()

        // Simple rule-based fallback
        if (context.threatLevel == ThreatLevel.CRITICAL || context.threatLevel == ThreatLevel.HIGH) {
            // Emergency services
            context.location?.let {
                actions.add(EmergencyAction.CallEmergencyServices("Emergency", it))
            }

            // Top priority contacts
            context.availableContacts
                .sortedBy { it.priority }
                .take(2)
                .forEach { contact ->
                    actions.add(EmergencyAction.MakeCall(contact))
                }
        } else {
            // Just alert family
            context.availableContacts
                .filter { it.relationship == "Family" }
                .firstOrNull()
                ?.let { contact ->
                    actions.add(
                        EmergencyAction.SendSMS(
                            contact,
                            "Safety alert triggered. Please check on me."
                        )
                    )
                }
        }

        return AIActionDecision(
            recommendedActions = actions,
            reasoning = "Fallback rule-based decision due to AI error",
            urgencyScore = if (context.threatLevel == ThreatLevel.CRITICAL) 10 else 7
        )
    }

    private fun extractUrgency(aiResponse: String): Int {
        val urgencyMatch = Regex("URGENCY:?\\s*(\\d+)").find(aiResponse)
        return urgencyMatch?.groupValues?.get(1)?.toIntOrNull() ?: 7
    }

    private fun extractReasoning(aiResponse: String): String {
        val reasoningMatch =
            Regex("REASONING:?\\s*(.+?)(?=URGENCY:|$)", RegexOption.DOT_MATCHES_ALL)
                .find(aiResponse)
        return reasoningMatch?.groupValues?.get(1)?.trim()
            ?: "AI assessment based on threat level and available information"
    }

    /**
     * Generate emergency message content
     */
    suspend fun generateEmergencyMessage(
        recipientName: String,
        threatLevel: ThreatLevel,
        location: Location?
    ): String = withContext(Dispatchers.IO) {
        try {
            val locationText = location?.let {
                "Location: https://maps.google.com/?q=${it.latitude},${it.longitude}"
            } ?: "Location unavailable"

            val prompt = """
                Generate a brief emergency message (max 160 characters for SMS) for this situation:
                
                Recipient: $recipientName
                Threat Level: $threatLevel
                $locationText
                
                The message should:
                - Be urgent but not cause panic
                - Include that this is from their safety app
                - Request immediate action if threat is high
                
                Message:
            """.trimIndent()

            RunAnywhere.generate(prompt).trim().take(160)

        } catch (e: Exception) {
            // Fallback message
            when (threatLevel) {
                ThreatLevel.LOW -> "Safety alert: I may need assistance. Please check on me."
                ThreatLevel.MEDIUM -> "URGENT: I need help. Safety app triggered. Please call me."
                ThreatLevel.HIGH, ThreatLevel.CRITICAL ->
                    "EMERGENCY: I'm in danger. Call police if you can't reach me. ${location?.let { "Location: ${it.latitude},${it.longitude}" } ?: ""}"

                ThreatLevel.UNKNOWN -> "Safety app activated. Please check on me."
            }
        }
    }
}
