package com.runanywhere.startup_hackathon20.state

import android.location.Location
import com.runanywhere.startup_hackathon20.EmergencyContact
import com.runanywhere.startup_hackathon20.EmergencySession
import com.runanywhere.startup_hackathon20.ProtocolQuestion
import com.runanywhere.startup_hackathon20.ThreatLevel
import com.runanywhere.startup_hackathon20.AlertRecord
import com.runanywhere.startup_hackathon20.EmergencyPath
import com.runanywhere.startup_hackathon20.SafePlace

/**
 * State Machine for Emergency Flow
 * 
 * This implements a proper state machine pattern to prevent race conditions
 * and ensure predictable emergency handling.
 * 
 * State transitions:
 * Idle -> Triggered -> Questioning -> PathSelection -> Active -> Resolved
 */
sealed class EmergencyState {
    /**
     * No emergency active
     */
    object Idle : EmergencyState()

    /**
     * Emergency just triggered, initializing systems
     */
    data class Triggered(
        val session: EmergencySession,
        val triggerTime: Long = System.currentTimeMillis()
    ) : EmergencyState()

    /**
     * Presenting first protocol question to user
     */
    data class Questioning(
        val session: EmergencySession,
        val question: ProtocolQuestion,
        val timeRemaining: Int
    ) : EmergencyState()

    /**
     * User answered NO - presenting second question for path selection
     */
    data class PathSelection(
        val session: EmergencySession,
        val question: ProtocolQuestion,
        val timeRemaining: Int,
        val alertsSent: List<AlertRecord>
    ) : EmergencyState()

    /**
     * Emergency actively being monitored
     */
    data class Active(
        val session: EmergencySession,
        val path: EmergencyPath,
        val location: Location?,
        val contacts: List<EmergencyContact>,
        val alertHistory: List<AlertRecord>,
        val safePlaces: List<SafePlace> = emptyList(),
        val currentDestination: SafePlace? = null
    ) : EmergencyState()

    /**
     * Emergency resolved (safe or cancelled)
     */
    data class Resolved(
        val session: EmergencySession,
        val resolvedTime: Long = System.currentTimeMillis(),
        val reason: ResolutionReason
    ) : EmergencyState()

    enum class ResolutionReason {
        USER_SAFE,
        FALSE_ALARM,
        ARRIVED_AT_SAFETY,
        MANUAL_CANCEL
    }
}

/**
 * Events that can trigger state transitions
 * 
 * Using sealed classes ensures all events are handled and makes
 * the system easier to test and debug.
 */
sealed class EmergencyEvent {
    // Trigger events
    object TriggerEmergency : EmergencyEvent()
    object CancelEmergency : EmergencyEvent()

    // Question events
    data class PresentQuestion(val question: ProtocolQuestion) : EmergencyEvent()
    object AnswerYes : EmergencyEvent()
    object AnswerNo : EmergencyEvent()
    object QuestionTimeout : EmergencyEvent()

    // Path selection events
    data class SelectPath(val path: EmergencyPath) : EmergencyEvent()
    object ThreatNearby : EmergencyEvent()
    object EscapeToSafety : EmergencyEvent()

    // Location events
    data class LocationUpdated(val location: Location) : EmergencyEvent()
    data class NavigateToPlace(val place: SafePlace) : EmergencyEvent()
    object ArrivedAtDestination : EmergencyEvent()

    // Alert events
    data class AlertsSent(val alerts: List<AlertRecord>) : EmergencyEvent()
    data class ThreatLevelUpdated(val newLevel: ThreatLevel) : EmergencyEvent()

    // Resolution events
    object UserConfirmedSafe : EmergencyEvent()
    object FalseAlarm : EmergencyEvent()
}

/**
 * Side effects that should be executed in response to state changes
 * 
 * This separates state management from actual side effects (I/O operations)
 * making the code more testable.
 */
sealed class EmergencyEffect {
    // Location effects
    object StartLocationMonitoring : EmergencyEffect()
    object StopLocationMonitoring : EmergencyEffect()
    object StartContinuousLocationTracking : EmergencyEffect()

    // Communication effects
    data class SendEmergencyAlerts(
        val contacts: List<EmergencyContact>,
        val message: String,
        val location: Location?
    ) : EmergencyEffect()

    data class MakeEmergencyCalls(
        val contacts: List<EmergencyContact>
    ) : EmergencyEffect()

    data class SendLocationUpdate(
        val contacts: List<EmergencyContact>,
        val location: Location,
        val message: String
    ) : EmergencyEffect()

    // Timer effects
    data class StartQuestionTimer(val seconds: Int) : EmergencyEffect()
    object StopQuestionTimer : EmergencyEffect()

    // Monitoring effects
    object StartEscalationMonitoring : EmergencyEffect()
    object StopEscalationMonitoring : EmergencyEffect()

    data class StartJourneyMonitoring(val destination: SafePlace) : EmergencyEffect()
    object StopJourneyMonitoring : EmergencyEffect()

    // Alert effects
    object StartLoudAlarm : EmergencyEffect()
    object StopLoudAlarm : EmergencyEffect()

    object StartRecording : EmergencyEffect()
    object StopRecording : EmergencyEffect()

    // Notification effects
    data class ShowNotification(val title: String, val message: String) : EmergencyEffect()
    object DismissNotifications : EmergencyEffect()

    // Navigation effects
    data class OpenNavigation(val destination: SafePlace) : EmergencyEffect()

    // Contact notification effects
    data class NotifyCancellation(val contacts: List<EmergencyContact>) : EmergencyEffect()
}

/**
 * Result of a state transition
 * Contains the new state and any side effects to execute
 */
data class StateTransitionResult(
    val newState: EmergencyState,
    val effects: List<EmergencyEffect> = emptyList()
)
