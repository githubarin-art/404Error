package com.runanywhere.startup_hackathon20.state

import android.location.Location
import com.runanywhere.startup_hackathon20.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for EmergencyStateMachine
 * 
 * Tests all state transitions and edge cases to ensure:
 * - Valid transitions work correctly
 * - Invalid transitions are rejected
 * - Effects are emitted properly
 * - No race conditions
 */
class EmergencyStateMachineTest {

    private lateinit var stateMachine: EmergencyStateMachine
    private lateinit var testContacts: List<EmergencyContact>
    private lateinit var testLocation: Location

    @Before
    fun setup() {
        stateMachine = EmergencyStateMachine()
        
        testContacts = listOf(
            EmergencyContact(
                id = "1",
                name = "Test Contact 1",
                phoneNumber = "1234567890",
                relationship = "Family",
                priority = 1
            ),
            EmergencyContact(
                id = "2",
                name = "Test Contact 2",
                phoneNumber = "0987654321",
                relationship = "Friend",
                priority = 2
            )
        )

        testLocation = Location("test").apply {
            latitude = 37.7749
            longitude = -122.4194
        }
    }

    @Test
    fun `initial state is Idle`() {
        assertTrue(stateMachine.currentState.value is EmergencyState.Idle)
        assertFalse(stateMachine.isEmergencyActive())
    }

    @Test
    fun `trigger emergency transitions from Idle to Triggered`() {
        val result = stateMachine.processEvent(
            EmergencyEvent.TriggerEmergency,
            contacts = testContacts,
            location = testLocation
        )

        assertTrue(result.newState is EmergencyState.Triggered)
        assertTrue(stateMachine.isEmergencyActive())
        
        // Should emit location monitoring effect
        assertTrue(result.effects.any { it is EmergencyEffect.StartLocationMonitoring })
    }

    @Test
    fun `present question transitions from Triggered to Questioning`() {
        // Setup: Trigger emergency first
        stateMachine.processEvent(EmergencyEvent.TriggerEmergency, testContacts, testLocation)

        val testQuestion = ProtocolQuestion(
            id = "test_q1",
            question = "Are you safe?",
            timeoutSeconds = 30,
            threatLevelIfAnswered = ThreatLevel.LOW,
            threatLevelIfNotAnswered = ThreatLevel.HIGH
        )

        val result = stateMachine.processEvent(
            EmergencyEvent.PresentQuestion(testQuestion),
            testContacts,
            testLocation
        )

        assertTrue(result.newState is EmergencyState.Questioning)
        
        val questioningState = result.newState as EmergencyState.Questioning
        assertEquals(testQuestion.id, questioningState.question.id)
        assertEquals(30, questioningState.timeRemaining)
        
        // Should start question timer
        assertTrue(result.effects.any { it is EmergencyEffect.StartQuestionTimer })
    }

    @Test
    fun `answer YES resolves emergency`() {
        // Setup: Get to questioning state
        stateMachine.processEvent(EmergencyEvent.TriggerEmergency, testContacts, testLocation)
        val testQuestion = ProtocolQuestion(
            id = "test_q1",
            question = "Are you safe?",
            timeoutSeconds = 30,
            threatLevelIfAnswered = ThreatLevel.LOW,
            threatLevelIfNotAnswered = ThreatLevel.HIGH
        )
        stateMachine.processEvent(
            EmergencyEvent.PresentQuestion(testQuestion),
            testContacts,
            testLocation
        )

        // Answer YES
        val result = stateMachine.processEvent(
            EmergencyEvent.AnswerYes,
            testContacts,
            testLocation
        )

        assertTrue(result.newState is EmergencyState.Resolved)
        val resolvedState = result.newState as EmergencyState.Resolved
        assertEquals(EmergencyState.ResolutionReason.USER_SAFE, resolvedState.reason)
        
        // Should stop all monitoring
        assertTrue(result.effects.any { it is EmergencyEffect.StopQuestionTimer })
        assertTrue(result.effects.any { it is EmergencyEffect.StopLocationMonitoring })
    }

    @Test
    fun `answer NO transitions to PathSelection`() {
        // Setup: Get to questioning state
        stateMachine.processEvent(EmergencyEvent.TriggerEmergency, testContacts, testLocation)
        val testQuestion = ProtocolQuestion(
            id = "test_q1",
            question = "Are you safe?",
            timeoutSeconds = 30,
            threatLevelIfAnswered = ThreatLevel.LOW,
            threatLevelIfNotAnswered = ThreatLevel.HIGH
        )
        stateMachine.processEvent(
            EmergencyEvent.PresentQuestion(testQuestion),
            testContacts,
            testLocation
        )

        // Answer NO
        val result = stateMachine.processEvent(
            EmergencyEvent.AnswerNo,
            testContacts,
            testLocation
        )

        assertTrue(result.newState is EmergencyState.PathSelection)
        
        // Should send emergency alerts
        assertTrue(result.effects.any { it is EmergencyEffect.SendEmergencyAlerts })
        // Should make emergency calls
        assertTrue(result.effects.any { it is EmergencyEffect.MakeEmergencyCalls })
        // Should start continuous location tracking
        assertTrue(result.effects.any { it is EmergencyEffect.StartContinuousLocationTracking })
    }

    @Test
    fun `question timeout behaves like answer NO`() {
        // Setup: Get to questioning state
        stateMachine.processEvent(EmergencyEvent.TriggerEmergency, testContacts, testLocation)
        val testQuestion = ProtocolQuestion(
            id = "test_q1",
            question = "Are you safe?",
            timeoutSeconds = 30,
            threatLevelIfAnswered = ThreatLevel.LOW,
            threatLevelIfNotAnswered = ThreatLevel.HIGH
        )
        stateMachine.processEvent(
            EmergencyEvent.PresentQuestion(testQuestion),
            testContacts,
            testLocation
        )

        // Timeout
        val result = stateMachine.processEvent(
            EmergencyEvent.QuestionTimeout,
            testContacts,
            testLocation
        )

        assertTrue(result.newState is EmergencyState.PathSelection)
        assertTrue(result.effects.any { it is EmergencyEffect.SendEmergencyAlerts })
    }

    @Test
    fun `threat nearby transitions to Active with THREAT_NEARBY path`() {
        // Setup: Get to path selection
        setupPathSelection()

        // Select threat nearby
        val result = stateMachine.processEvent(
            EmergencyEvent.ThreatNearby,
            testContacts,
            testLocation
        )

        assertTrue(result.newState is EmergencyState.Active)
        val activeState = result.newState as EmergencyState.Active
        assertEquals(EmergencyPath.THREAT_NEARBY, activeState.path)
        assertEquals(ThreatLevel.CRITICAL, activeState.session.currentThreatLevel)
    }

    @Test
    fun `escape to safety transitions to Active with ESCAPE_TO_SAFETY path`() {
        // Setup: Get to path selection
        setupPathSelection()

        // Select escape to safety
        val result = stateMachine.processEvent(
            EmergencyEvent.EscapeToSafety,
            testContacts,
            testLocation
        )

        assertTrue(result.newState is EmergencyState.Active)
        val activeState = result.newState as EmergencyState.Active
        assertEquals(EmergencyPath.ESCAPE_TO_SAFETY, activeState.path)
        assertEquals(ThreatLevel.HIGH, activeState.session.currentThreatLevel)
    }

    @Test
    fun `location update in Active state updates location`() {
        // Setup: Get to active state
        setupActiveState()

        val newLocation = Location("test").apply {
            latitude = 40.7128
            longitude = -74.0060
        }

        val result = stateMachine.processEvent(
            EmergencyEvent.LocationUpdated(newLocation),
            testContacts,
            testLocation
        )

        assertTrue(result.newState is EmergencyState.Active)
        val activeState = result.newState as EmergencyState.Active
        assertEquals(newLocation.latitude, activeState.location?.latitude)
        assertEquals(newLocation.longitude, activeState.location?.longitude)
    }

    @Test
    fun `navigate to place starts journey monitoring`() {
        // Setup: Get to active state
        setupActiveState()

        val testPlace = SafePlace(
            name = "Police Station",
            type = "police",
            latitude = 40.7128,
            longitude = -74.0060,
            is24_7 = true
        )

        val result = stateMachine.processEvent(
            EmergencyEvent.NavigateToPlace(testPlace),
            testContacts,
            testLocation
        )

        assertTrue(result.newState is EmergencyState.Active)
        val activeState = result.newState as EmergencyState.Active
        assertEquals(testPlace, activeState.currentDestination)
        
        // Should start journey monitoring
        assertTrue(result.effects.any { it is EmergencyEffect.StartJourneyMonitoring })
        assertTrue(result.effects.any { it is EmergencyEffect.OpenNavigation })
    }

    @Test
    fun `arrived at destination resolves emergency`() {
        // Setup: Get to active state with destination
        setupActiveState()
        val testPlace = SafePlace(
            name = "Police Station",
            type = "police",
            latitude = 40.7128,
            longitude = -74.0060,
            is24_7 = true
        )
        stateMachine.processEvent(
            EmergencyEvent.NavigateToPlace(testPlace),
            testContacts,
            testLocation
        )

        val result = stateMachine.processEvent(
            EmergencyEvent.ArrivedAtDestination,
            testContacts,
            testLocation
        )

        assertTrue(result.newState is EmergencyState.Resolved)
        val resolvedState = result.newState as EmergencyState.Resolved
        assertEquals(EmergencyState.ResolutionReason.ARRIVED_AT_SAFETY, resolvedState.reason)
        
        // Should stop all monitoring
        assertTrue(result.effects.any { it is EmergencyEffect.StopJourneyMonitoring })
        assertTrue(result.effects.any { it is EmergencyEffect.StopLocationMonitoring })
    }

    @Test
    fun `cancel emergency stops all operations`() {
        // Setup: Get to active state
        setupActiveState()

        val result = stateMachine.processEvent(
            EmergencyEvent.CancelEmergency,
            testContacts,
            testLocation
        )

        assertTrue(result.newState is EmergencyState.Resolved)
        val resolvedState = result.newState as EmergencyState.Resolved
        assertEquals(EmergencyState.ResolutionReason.MANUAL_CANCEL, resolvedState.reason)
        
        // Should stop all monitoring and alerts
        assertTrue(result.effects.any { it is EmergencyEffect.StopQuestionTimer })
        assertTrue(result.effects.any { it is EmergencyEffect.StopLocationMonitoring })
        assertTrue(result.effects.any { it is EmergencyEffect.StopEscalationMonitoring })
        assertTrue(result.effects.any { it is EmergencyEffect.StopLoudAlarm })
        assertTrue(result.effects.any { it is EmergencyEffect.StopRecording })
    }

    @Test
    fun `getCurrentSession returns correct session`() {
        assertNull(stateMachine.getCurrentSession())

        stateMachine.processEvent(EmergencyEvent.TriggerEmergency, testContacts, testLocation)
        
        val session = stateMachine.getCurrentSession()
        assertNotNull(session)
        assertTrue(session!!.isActive)
    }

    @Test
    fun `reset returns to Idle state`() {
        // Setup: Get to active state
        setupActiveState()
        assertTrue(stateMachine.isEmergencyActive())

        stateMachine.reset()

        assertTrue(stateMachine.currentState.value is EmergencyState.Idle)
        assertFalse(stateMachine.isEmergencyActive())
        assertNull(stateMachine.getCurrentSession())
    }

    @Test
    fun `invalid event in state is ignored`() {
        // Try to answer question when in Idle state
        val result = stateMachine.processEvent(
            EmergencyEvent.AnswerYes,
            testContacts,
            testLocation
        )

        // Should remain in Idle state
        assertTrue(result.newState is EmergencyState.Idle)
        assertTrue(result.effects.isEmpty())
    }

    @Test
    fun `effects are cleared after processing`() {
        val result = stateMachine.processEvent(
            EmergencyEvent.TriggerEmergency,
            testContacts,
            testLocation
        )

        // Effects should be emitted
        assertTrue(result.effects.isNotEmpty())
        
        // Clear effects
        stateMachine.clearEffects()
        
        // Effects should be empty now
        assertTrue(stateMachine.effects.value.isEmpty())
    }

    // Helper methods

    private fun setupPathSelection() {
        stateMachine.processEvent(EmergencyEvent.TriggerEmergency, testContacts, testLocation)
        val testQuestion = ProtocolQuestion(
            id = "test_q1",
            question = "Are you safe?",
            timeoutSeconds = 30,
            threatLevelIfAnswered = ThreatLevel.LOW,
            threatLevelIfNotAnswered = ThreatLevel.HIGH
        )
        stateMachine.processEvent(
            EmergencyEvent.PresentQuestion(testQuestion),
            testContacts,
            testLocation
        )
        stateMachine.processEvent(EmergencyEvent.AnswerNo, testContacts, testLocation)
    }

    private fun setupActiveState() {
        setupPathSelection()
        stateMachine.processEvent(EmergencyEvent.EscapeToSafety, testContacts, testLocation)
    }
}
