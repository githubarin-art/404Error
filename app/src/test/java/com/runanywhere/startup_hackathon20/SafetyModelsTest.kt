package com.runanywhere.startup_hackathon20

import android.location.Location
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Unit tests for Safety data models
 *
 * Tests data classes, enums, and sealed classes for correct behavior
 */
class SafetyModelsTest {

    private lateinit var testLocation: Location
    private lateinit var testContact: EmergencyContact

    @Before
    fun setup() {
        testLocation = Location("test").apply {
            latitude = 37.7749
            longitude = -122.4194
            accuracy = 10f
        }

        testContact = EmergencyContact(
            id = "test-1",
            name = "Test Contact",
            phoneNumber = "1234567890",
            relationship = "Family",
            priority = 1
        )
    }

    // EmergencyContact tests

    @Test
    fun `EmergencyContact has correct default values`() {
        assertTrue(testContact.canReceiveLocation)
    }

    @Test
    fun `EmergencyContact can be created without location permission`() {
        val contact = testContact.copy(canReceiveLocation = false)
        assertFalse(contact.canReceiveLocation)
    }

    @Test
    fun `EmergencyContact priority is within valid range`() {
        assertTrue(testContact.priority >= 1 && testContact.priority <= 5)
    }

    // ThreatLevel tests

    @Test
    fun `ThreatLevel enum has correct order`() {
        val levels = ThreatLevel.values()
        assertEquals(ThreatLevel.UNKNOWN, levels[0])
        assertEquals(ThreatLevel.LOW, levels[1])
        assertEquals(ThreatLevel.MEDIUM, levels[2])
        assertEquals(ThreatLevel.HIGH, levels[3])
        assertEquals(ThreatLevel.CRITICAL, levels[4])
    }

    @Test
    fun `ThreatLevel can be compared`() {
        assertTrue(ThreatLevel.LOW.ordinal < ThreatLevel.HIGH.ordinal)
        assertTrue(ThreatLevel.HIGH.ordinal < ThreatLevel.CRITICAL.ordinal)
    }

    // ProtocolQuestion tests

    @Test
    fun `ProtocolQuestion has correct structure`() {
        val question = ProtocolQuestion(
            id = "q1",
            question = "Are you safe?",
            timeoutSeconds = 30,
            threatLevelIfAnswered = ThreatLevel.LOW,
            threatLevelIfNotAnswered = ThreatLevel.HIGH
        )

        assertEquals("q1", question.id)
        assertEquals("Are you safe?", question.question)
        assertEquals(30, question.timeoutSeconds)
        assertEquals(ThreatLevel.LOW, question.threatLevelIfAnswered)
        assertEquals(ThreatLevel.HIGH, question.threatLevelIfNotAnswered)
    }

    @Test
    fun `ProtocolQuestion timeout default is 30 seconds`() {
        val question = ProtocolQuestion(
            id = "q1",
            question = "Test?",
            threatLevelIfAnswered = ThreatLevel.LOW,
            threatLevelIfNotAnswered = ThreatLevel.HIGH
        )

        assertEquals(30, question.timeoutSeconds)
    }

    // EmergencySession tests

    @Test
    fun `EmergencySession starts with default values`() {
        val session = EmergencySession(
            sessionId = "session-1",
            startTime = System.currentTimeMillis(),
            alarmTriggeredTime = System.currentTimeMillis(),
            currentThreatLevel = ThreatLevel.UNKNOWN,
            location = testLocation
        )

        assertTrue(session.isActive)
        assertEquals(ThreatLevel.UNKNOWN, session.currentThreatLevel)
        assertTrue(session.victimResponses.isEmpty())
        assertTrue(session.alertsSent.isEmpty())
    }

    @Test
    fun `EmergencySession can be updated with responses`() {
        val session = EmergencySession(
            sessionId = "session-1",
            startTime = System.currentTimeMillis(),
            alarmTriggeredTime = System.currentTimeMillis(),
            currentThreatLevel = ThreatLevel.UNKNOWN
        )

        val response = VictimResponse(
            questionId = "q1",
            answered = true,
            responseTime = System.currentTimeMillis(),
            timeTakenSeconds = 15
        )

        val updatedSession = session.copy(
            victimResponses = session.victimResponses + response
        )

        assertEquals(1, updatedSession.victimResponses.size)
        assertEquals(response, updatedSession.victimResponses.first())
    }

    @Test
    fun `EmergencySession can track multiple alerts`() {
        val session = EmergencySession(
            sessionId = "session-1",
            startTime = System.currentTimeMillis(),
            alarmTriggeredTime = System.currentTimeMillis(),
            currentThreatLevel = ThreatLevel.HIGH
        )

        val alert1 = AlertRecord(
            timestamp = System.currentTimeMillis(),
            recipientType = RecipientType.FAMILY,
            recipientName = "Contact 1",
            recipientPhone = "1234567890",
            messageType = MessageType.SMS,
            success = true
        )

        val alert2 = AlertRecord(
            timestamp = System.currentTimeMillis(),
            recipientType = RecipientType.FRIEND,
            recipientName = "Contact 2",
            recipientPhone = "0987654321",
            messageType = MessageType.CALL,
            success = true
        )

        val updatedSession = session.copy(
            alertsSent = session.alertsSent + alert1 + alert2
        )

        assertEquals(2, updatedSession.alertsSent.size)
        assertEquals(1, updatedSession.alertsSent.count { it.messageType == MessageType.SMS })
        assertEquals(1, updatedSession.alertsSent.count { it.messageType == MessageType.CALL })
    }

    // AlertRecord tests

    @Test
    fun `AlertRecord tracks successful alerts`() {
        val alert = AlertRecord(
            timestamp = System.currentTimeMillis(),
            recipientType = RecipientType.FAMILY,
            recipientName = "Test Contact",
            recipientPhone = "1234567890",
            messageType = MessageType.SMS,
            success = true
        )

        assertTrue(alert.success)
        assertNull(alert.errorMessage)
    }

    @Test
    fun `AlertRecord tracks failed alerts with error message`() {
        val alert = AlertRecord(
            timestamp = System.currentTimeMillis(),
            recipientType = RecipientType.FAMILY,
            recipientName = "Test Contact",
            recipientPhone = "1234567890",
            messageType = MessageType.SMS,
            success = false,
            errorMessage = "Network error"
        )

        assertFalse(alert.success)
        assertEquals("Network error", alert.errorMessage)
    }

    // RecipientType tests

    @Test
    fun `RecipientType has all required types`() {
        val types = RecipientType.values()
        assertTrue(types.contains(RecipientType.FAMILY))
        assertTrue(types.contains(RecipientType.FRIEND))
        assertTrue(types.contains(RecipientType.EMERGENCY_SERVICES))
        assertTrue(types.contains(RecipientType.POLICE))
        assertTrue(types.contains(RecipientType.MEDICAL))
    }

    // MessageType tests

    @Test
    fun `MessageType has all communication methods`() {
        val types = MessageType.values()
        assertTrue(types.contains(MessageType.SMS))
        assertTrue(types.contains(MessageType.CALL))
        assertTrue(types.contains(MessageType.MISSED_CALL))
        assertTrue(types.contains(MessageType.EMERGENCY_CALL))
    }

    // EmergencyPath tests

    @Test
    fun `EmergencyPath enum has correct values`() {
        assertEquals(EmergencyPath.NONE, EmergencyPath.valueOf("NONE"))
        assertEquals(EmergencyPath.THREAT_NEARBY, EmergencyPath.valueOf("THREAT_NEARBY"))
        assertEquals(EmergencyPath.ESCAPE_TO_SAFETY, EmergencyPath.valueOf("ESCAPE_TO_SAFETY"))
    }

    // SafePlace tests

    @Test
    fun `SafePlace has correct structure`() {
        val place = SafePlace(
            name = "Police Station",
            type = "police",
            latitude = 37.7749,
            longitude = -122.4194,
            is24_7 = true,
            address = "123 Main St"
        )

        assertEquals("Police Station", place.name)
        assertEquals("police", place.type)
        assertTrue(place.is24_7)
        assertEquals("123 Main St", place.address)
    }

    @Test
    fun `SafePlace can track distance`() {
        val place = SafePlace(
            name = "Hospital",
            type = "hospital",
            latitude = 37.7749,
            longitude = -122.4194,
            is24_7 = true
        )

        place.distance = 500f // 500 meters
        place.walkingTimeMinutes = 6 // ~5km/h walking speed

        assertEquals(500f, place.distance)
        assertEquals(6, place.walkingTimeMinutes)
    }

    // AIDecisionContext tests

    @Test
    fun `AIDecisionContext captures all context for AI`() {
        val context = AIDecisionContext(
            threatLevel = ThreatLevel.HIGH,
            victimResponded = false,
            timeSinceAlarm = 120, // 2 minutes
            location = testLocation,
            previousAlerts = emptyList(),
            availableContacts = listOf(testContact)
        )

        assertEquals(ThreatLevel.HIGH, context.threatLevel)
        assertFalse(context.victimResponded)
        assertEquals(120, context.timeSinceAlarm)
        assertNotNull(context.location)
        assertEquals(1, context.availableContacts.size)
    }

    // EmergencyAction tests

    @Test
    fun `EmergencyAction sealed class has all action types`() {
        // Test that we can create each type of action
        val smsAction = EmergencyAction.SendSMS(testContact, "Test message")
        val callAction = EmergencyAction.MakeCall(testContact)
        val missedCallAction = EmergencyAction.MakeMissedCall(testContact)
        val emergencyCallAction = EmergencyAction.CallEmergencyServices("Police", testLocation)
        val updateAction = EmergencyAction.UpdateThreatLevel(ThreatLevel.HIGH)

        assertTrue(smsAction is EmergencyAction)
        assertTrue(callAction is EmergencyAction)
        assertTrue(missedCallAction is EmergencyAction)
        assertTrue(emergencyCallAction is EmergencyAction)
        assertTrue(updateAction is EmergencyAction)
    }

    @Test
    fun `EmergencyAction SendSMS has correct structure`() {
        val action = EmergencyAction.SendSMS(testContact, "Emergency message")

        assertEquals(testContact, action.contact)
        assertEquals("Emergency message", action.message)
    }

    @Test
    fun `EmergencyAction CallEmergencyServices includes location`() {
        val action = EmergencyAction.CallEmergencyServices("Police", testLocation)

        assertEquals("Police", action.serviceType)
        assertEquals(testLocation, action.location)
    }

    // AIActionDecision tests

    @Test
    fun `AIActionDecision has reasoning and urgency`() {
        val action = EmergencyAction.SendSMS(testContact, "Help!")

        val decision = AIActionDecision(
            recommendedActions = listOf(action),
            reasoning = "High threat level detected",
            urgencyScore = 9
        )

        assertEquals(1, decision.recommendedActions.size)
        assertEquals("High threat level detected", decision.reasoning)
        assertEquals(9, decision.urgencyScore)
        assertTrue(decision.urgencyScore in 1..10)
    }

    @Test
    fun `AIActionDecision urgency score is bounded`() {
        val decision = AIActionDecision(
            recommendedActions = emptyList(),
            reasoning = "Test",
            urgencyScore = 5
        )

        assertTrue(decision.urgencyScore >= 1)
        assertTrue(decision.urgencyScore <= 10)
    }
}
