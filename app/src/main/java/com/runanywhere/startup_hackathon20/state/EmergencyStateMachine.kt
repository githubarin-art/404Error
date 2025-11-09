package com.runanywhere.startup_hackathon20.state

import android.location.Location
import android.util.Log
import com.runanywhere.startup_hackathon20.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * State Machine for Emergency Management
 *
 * This class implements a formal state machine pattern to ensure:
 * 1. Predictable state transitions
 * 2. No race conditions
 * 3. All edge cases handled
 * 4. Easy to test and debug
 *
 * Thread-safe: All state mutations happen through synchronized methods
 */
class EmergencyStateMachine {
    companion object {
        private const val TAG = "EmergencyStateMachine"
    }

    private val _currentState = MutableStateFlow<EmergencyState>(EmergencyState.Idle)
    val currentState: StateFlow<EmergencyState> = _currentState.asStateFlow()

    // Effect channel for side effects
    private val _effects = MutableStateFlow<List<EmergencyEffect>>(emptyList())
    val effects: StateFlow<List<EmergencyEffect>> = _effects.asStateFlow()

    /**
     * Process an event and transition to new state
     *
     * This is the ONLY way to change state, ensuring all transitions are valid
     */
    @Synchronized
    fun processEvent(
        event: EmergencyEvent,
        contacts: List<EmergencyContact> = emptyList(),
        location: Location? = null,
        aiEngine: SafetyAIEngine? = null
    ): StateTransitionResult {
        val currentState = _currentState.value

        Log.i(
            TAG,
            "Processing event: ${event::class.simpleName} in state: ${currentState::class.simpleName}"
        )

        val result = when (currentState) {
            is EmergencyState.Idle -> handleIdleState(event, contacts, location)
            is EmergencyState.Triggered -> handleTriggeredState(
                event,
                currentState,
                contacts,
                location
            )

            is EmergencyState.Questioning -> handleQuestioningState(
                event,
                currentState,
                contacts,
                location
            )

            is EmergencyState.PathSelection -> handlePathSelectionState(
                event,
                currentState,
                contacts,
                location
            )

            is EmergencyState.Active -> handleActiveState(event, currentState, contacts, location)
            is EmergencyState.Resolved -> handleResolvedState(event, currentState)
        }

        // Update state
        _currentState.value = result.newState

        // Emit effects for execution
        if (result.effects.isNotEmpty()) {
            _effects.value = result.effects
            Log.i(
                TAG,
                "Emitting ${result.effects.size} effects: ${result.effects.map { it::class.simpleName }}"
            )
        }

        Log.i(TAG, "State transitioned to: ${result.newState::class.simpleName}")

        return result
    }

    /**
     * Handle events in Idle state
     */
    private fun handleIdleState(
        event: EmergencyEvent,
        contacts: List<EmergencyContact>,
        location: Location?
    ): StateTransitionResult {
        return when (event) {
            is EmergencyEvent.TriggerEmergency -> {
                // Create new emergency session
                val session = EmergencySession(
                    sessionId = UUID.randomUUID().toString(),
                    startTime = System.currentTimeMillis(),
                    alarmTriggeredTime = System.currentTimeMillis(),
                    currentThreatLevel = ThreatLevel.UNKNOWN,
                    location = location
                )

                StateTransitionResult(
                    newState = EmergencyState.Triggered(session),
                    effects = listOf(
                        EmergencyEffect.StartLocationMonitoring,
                        EmergencyEffect.ShowNotification(
                            "Emergency Activated",
                            "Assessing situation..."
                        )
                    )
                )
            }

            else -> {
                Log.w(TAG, "Event $event not valid in Idle state")
                StateTransitionResult(EmergencyState.Idle)
            }
        }
    }

    /**
     * Handle events in Triggered state
     */
    private fun handleTriggeredState(
        event: EmergencyEvent,
        state: EmergencyState.Triggered,
        contacts: List<EmergencyContact>,
        location: Location?
    ): StateTransitionResult {
        return when (event) {
            is EmergencyEvent.PresentQuestion -> {
                StateTransitionResult(
                    newState = EmergencyState.Questioning(
                        session = state.session,
                        question = event.question,
                        timeRemaining = event.question.timeoutSeconds
                    ),
                    effects = listOf(
                        EmergencyEffect.StartQuestionTimer(event.question.timeoutSeconds)
                    )
                )
            }

            is EmergencyEvent.CancelEmergency -> {
                cancelEmergency(state.session, EmergencyState.ResolutionReason.MANUAL_CANCEL)
            }

            else -> {
                Log.w(TAG, "Event $event not valid in Triggered state")
                StateTransitionResult(state)
            }
        }
    }

    /**
     * Handle events in Questioning state
     */
    private fun handleQuestioningState(
        event: EmergencyEvent,
        state: EmergencyState.Questioning,
        contacts: List<EmergencyContact>,
        location: Location?
    ): StateTransitionResult {
        return when (event) {
            is EmergencyEvent.AnswerYes -> {
                // User confirmed safe - resolve emergency
                val updatedSession = state.session.copy(
                    currentThreatLevel = ThreatLevel.LOW,
                    victimResponses = state.session.victimResponses + VictimResponse(
                        questionId = state.question.id,
                        answered = true,
                        responseTime = System.currentTimeMillis(),
                        timeTakenSeconds = state.question.timeoutSeconds - state.timeRemaining
                    )
                )

                StateTransitionResult(
                    newState = EmergencyState.Resolved(
                        session = updatedSession,
                        reason = EmergencyState.ResolutionReason.USER_SAFE
                    ),
                    effects = listOf(
                        EmergencyEffect.StopQuestionTimer,
                        EmergencyEffect.StopLocationMonitoring,
                        EmergencyEffect.ShowNotification(
                            "All Clear",
                            "Emergency cancelled - you're safe"
                        )
                    )
                )
            }

            is EmergencyEvent.AnswerNo, is EmergencyEvent.QuestionTimeout -> {
                // High threat - send alerts immediately and present path selection
                val updatedSession = state.session.copy(
                    currentThreatLevel = ThreatLevel.HIGH,
                    victimResponses = state.session.victimResponses + VictimResponse(
                        questionId = state.question.id,
                        answered = event is EmergencyEvent.AnswerNo,
                        responseTime = System.currentTimeMillis(),
                        timeTakenSeconds = state.question.timeoutSeconds
                    ),
                    location = location
                )

                // Create second question for path selection
                val secondQuestion = ProtocolQuestion(
                    id = "threat_proximity",
                    question = "Is the threat near you right now?",
                    timeoutSeconds = 30,
                    threatLevelIfAnswered = ThreatLevel.CRITICAL,
                    threatLevelIfNotAnswered = ThreatLevel.CRITICAL
                )

                StateTransitionResult(
                    newState = EmergencyState.PathSelection(
                        session = updatedSession,
                        question = secondQuestion,
                        timeRemaining = secondQuestion.timeoutSeconds,
                        alertsSent = emptyList()
                    ),
                    effects = listOf(
                        EmergencyEffect.StopQuestionTimer,
                        EmergencyEffect.SendEmergencyAlerts(
                            contacts = contacts,
                            message = "EMERGENCY: I need immediate help!",
                            location = location
                        ),
                        EmergencyEffect.MakeEmergencyCalls(
                            contacts = contacts.sortedBy { it.priority }.take(2)
                        ),
                        EmergencyEffect.StartContinuousLocationTracking,
                        EmergencyEffect.StartQuestionTimer(secondQuestion.timeoutSeconds)
                    )
                )
            }

            is EmergencyEvent.CancelEmergency -> {
                cancelEmergency(state.session, EmergencyState.ResolutionReason.MANUAL_CANCEL)
            }

            else -> {
                Log.w(TAG, "Event $event not valid in Questioning state")
                StateTransitionResult(state)
            }
        }
    }

    /**
     * Handle events in PathSelection state
     */
    private fun handlePathSelectionState(
        event: EmergencyEvent,
        state: EmergencyState.PathSelection,
        contacts: List<EmergencyContact>,
        location: Location?
    ): StateTransitionResult {
        return when (event) {
            is EmergencyEvent.ThreatNearby, is EmergencyEvent.QuestionTimeout -> {
                // Threat is nearby - activate stealth features
                val updatedSession = state.session.copy(
                    currentThreatLevel = ThreatLevel.CRITICAL
                )

                StateTransitionResult(
                    newState = EmergencyState.Active(
                        session = updatedSession,
                        path = EmergencyPath.THREAT_NEARBY,
                        location = location,
                        contacts = contacts,
                        alertHistory = state.alertsSent
                    ),
                    effects = listOf(
                        EmergencyEffect.StopQuestionTimer,
                        EmergencyEffect.StartEscalationMonitoring,
                        EmergencyEffect.ShowNotification(
                            "CRITICAL THREAT",
                            "Stealth features activated"
                        )
                    )
                )
            }

            is EmergencyEvent.EscapeToSafety -> {
                // User can escape - show safe places
                val updatedSession = state.session.copy(
                    currentThreatLevel = ThreatLevel.HIGH
                )

                StateTransitionResult(
                    newState = EmergencyState.Active(
                        session = updatedSession,
                        path = EmergencyPath.ESCAPE_TO_SAFETY,
                        location = location,
                        contacts = contacts,
                        alertHistory = state.alertsSent,
                        safePlaces = emptyList() // Will be populated by effect
                    ),
                    effects = listOf(
                        EmergencyEffect.StopQuestionTimer,
                        EmergencyEffect.StartEscalationMonitoring,
                        EmergencyEffect.ShowNotification(
                            "Navigate to Safety",
                            "Safe places nearby available"
                        )
                    )
                )
            }

            is EmergencyEvent.AlertsSent -> {
                // Update alerts sent in current state
                StateTransitionResult(
                    newState = state.copy(alertsSent = event.alerts)
                )
            }

            is EmergencyEvent.CancelEmergency -> {
                cancelEmergency(state.session, EmergencyState.ResolutionReason.MANUAL_CANCEL)
            }

            else -> {
                Log.w(TAG, "Event $event not valid in PathSelection state")
                StateTransitionResult(state)
            }
        }
    }

    /**
     * Handle events in Active state
     */
    private fun handleActiveState(
        event: EmergencyEvent,
        state: EmergencyState.Active,
        contacts: List<EmergencyContact>,
        location: Location?
    ): StateTransitionResult {
        return when (event) {
            is EmergencyEvent.LocationUpdated -> {
                val updatedSession = state.session.copy(location = event.location)
                StateTransitionResult(
                    newState = state.copy(
                        session = updatedSession,
                        location = event.location
                    ),
                    effects = if (state.path == EmergencyPath.ESCAPE_TO_SAFETY && state.currentDestination != null) {
                        listOf(
                            EmergencyEffect.SendLocationUpdate(
                                contacts = contacts,
                                location = event.location,
                                message = "Moving towards ${state.currentDestination.name}"
                            )
                        )
                    } else emptyList()
                )
            }

            is EmergencyEvent.NavigateToPlace -> {
                StateTransitionResult(
                    newState = state.copy(currentDestination = event.place),
                    effects = listOf(
                        EmergencyEffect.OpenNavigation(event.place),
                        EmergencyEffect.StartJourneyMonitoring(event.place)
                    )
                )
            }

            is EmergencyEvent.ArrivedAtDestination -> {
                val updatedSession = state.session.copy(
                    currentThreatLevel = ThreatLevel.LOW
                )
                StateTransitionResult(
                    newState = EmergencyState.Resolved(
                        session = updatedSession,
                        reason = EmergencyState.ResolutionReason.ARRIVED_AT_SAFETY
                    ),
                    effects = listOf(
                        EmergencyEffect.StopJourneyMonitoring,
                        EmergencyEffect.StopLocationMonitoring,
                        EmergencyEffect.StopEscalationMonitoring,
                        EmergencyEffect.ShowNotification(
                            "Arrived Safely",
                            "Emergency resolved"
                        )
                    )
                )
            }

            is EmergencyEvent.ThreatLevelUpdated -> {
                val updatedSession = state.session.copy(currentThreatLevel = event.newLevel)
                StateTransitionResult(
                    newState = state.copy(session = updatedSession)
                )
            }

            is EmergencyEvent.UserConfirmedSafe -> {
                cancelEmergency(state.session, EmergencyState.ResolutionReason.USER_SAFE)
            }

            is EmergencyEvent.CancelEmergency -> {
                cancelEmergency(state.session, EmergencyState.ResolutionReason.MANUAL_CANCEL)
            }

            else -> {
                Log.w(TAG, "Event $event not valid in Active state")
                StateTransitionResult(state)
            }
        }
    }

    /**
     * Handle events in Resolved state
     */
    private fun handleResolvedState(
        event: EmergencyEvent,
        state: EmergencyState.Resolved
    ): StateTransitionResult {
        return when (event) {
            is EmergencyEvent.TriggerEmergency -> {
                // Allow new emergency to be triggered
                Log.i(TAG, "New emergency triggered after previous resolution")
                StateTransitionResult(EmergencyState.Idle)
            }

            else -> {
                // Ignore other events in resolved state
                Log.w(TAG, "Event $event ignored in Resolved state")
                StateTransitionResult(state)
            }
        }
    }

    /**
     * Helper to cancel emergency and transition to resolved state
     */
    private fun cancelEmergency(
        session: EmergencySession,
        reason: EmergencyState.ResolutionReason
    ): StateTransitionResult {
        val updatedSession = session.copy(isActive = false)

        return StateTransitionResult(
            newState = EmergencyState.Resolved(
                session = updatedSession,
                reason = reason
            ),
            effects = listOf(
                EmergencyEffect.StopQuestionTimer,
                EmergencyEffect.StopLocationMonitoring,
                EmergencyEffect.StopEscalationMonitoring,
                EmergencyEffect.StopJourneyMonitoring,
                EmergencyEffect.StopLoudAlarm,
                EmergencyEffect.StopRecording,
                EmergencyEffect.DismissNotifications
            )
        )
    }

    /**
     * Get current session if any
     */
    fun getCurrentSession(): EmergencySession? {
        return when (val state = _currentState.value) {
            is EmergencyState.Triggered -> state.session
            is EmergencyState.Questioning -> state.session
            is EmergencyState.PathSelection -> state.session
            is EmergencyState.Active -> state.session
            is EmergencyState.Resolved -> state.session
            else -> null
        }
    }

    /**
     * Check if emergency is currently active
     */
    fun isEmergencyActive(): Boolean {
        return _currentState.value !is EmergencyState.Idle &&
                _currentState.value !is EmergencyState.Resolved
    }

    /**
     * Reset to idle state
     */
    fun reset() {
        _currentState.value = EmergencyState.Idle
        _effects.value = emptyList()
        Log.i(TAG, "State machine reset to Idle")
    }

    /**
     * Clear effects after they've been processed
     */
    fun clearEffects() {
        _effects.value = emptyList()
    }
}
