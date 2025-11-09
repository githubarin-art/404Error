# Guardian AI Safety App - Integration Architecture

## System Architecture Overview

This document details the integration points, state management, and data flow between all components
of the Guardian AI Safety App.

---

## 🏗️ Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI Layer (Compose)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ Emergency    │  │ Contacts     │  │ Settings     │         │
│  │ Screen       │  │ Screen       │  │ Screen       │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│         │                  │                  │                  │
└─────────┼──────────────────┼──────────────────┼─────────────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                    ViewModel Layer (State)                      │
│                    ┌──────────────────┐                         │
│                    │ SafetyViewModel  │                         │
│                    │  • StateFlow     │                         │
│                    │  • Events        │                         │
│                    │  • Commands      │                         │
│                    └────────┬─────────┘                         │
└─────────────────────────────┼───────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ↓                    ↓                     ↓
┌──────────────────┐  ┌──────────────┐  ┌──────────────────┐
│ SafetyAIEngine   │  │ SafetyModels │  │ Utils & Services │
│  • AI Decisions  │  │  • Data      │  │  • Permissions   │
│  • Questions     │  │  • States    │  │  • Location      │
│  • Threat Assess │  │  • Events    │  │  • Shake Detect  │
└──────────────────┘  └──────────────┘  └──────────────────┘
```

---

## 📊 Component Integration Matrix

| Component | Integrates With | Data Flow | Purpose |
|-----------|-----------------|-----------|---------|
| **SafetyViewModel** | All UI Screens | StateFlow → UI | Central state management |
| **SafetyAIEngine** | SafetyViewModel | Commands → Decisions | AI threat assessment |
| **SafetyModels** | All Components | Data classes | Data representation |
| **EmergencyScreen** | SafetyViewModel | Events ← StateFlow | Emergency UI |
| **PermissionManager** | SafetyViewModel | Check/Request | Permission handling |
| **ShakeDetector** | SafetyViewModel | Event trigger | Gesture detection |
| **LocationServices** | SafetyViewModel | Location updates | GPS tracking |

---

## 🔄 State Management Architecture

### 1. SafetyViewModel - Central State Hub

**Role**: Single source of truth for all app state

**State Flows (Observable):**

```kotlin
// Emergency session state
val currentSession: StateFlow<EmergencySession?>
val isAlarmActive: StateFlow<Boolean>
val currentQuestion: StateFlow<ProtocolQuestion?>
val questionTimeRemaining: StateFlow<Int?>

// Emergency path state
val emergencyPath: StateFlow<EmergencyPath>
val secondQuestion: StateFlow<ProtocolQuestion?>
val secondQuestionTimeRemaining: StateFlow<Int?>

// Location state
val currentLocation: StateFlow<Location?>
val nearestSafePlaces: StateFlow<List<SafePlace>>
val currentDestination: StateFlow<SafePlace?>

// Feature states
val isLoudAlarmActive: StateFlow<Boolean>
val isRecordingActive: StateFlow<Boolean>
val recordingDuration: StateFlow<Int>
val isFakeCallActive: StateFlow<Boolean>
val isBreathingActive: StateFlow<Boolean>

// Alert state
val alertHistory: StateFlow<List<AlertRecord>>
val emergencyContacts: StateFlow<List<EmergencyContact>>
val statusMessage: StateFlow<String>
```

**Event-Driven Actions:**

```kotlin
// User-triggered events
fun triggerEmergencyAlarm()
fun answerProtocolQuestionYes()
fun answerProtocolQuestionNo()
fun answerSecondQuestionYes()
fun answerSecondQuestionNo()
fun cancelEmergencyAlarm()

// Feature toggles
fun toggleLoudAlarm()
fun toggleRecording()
fun startFakeCall() / stopFakeCall()
fun startBreathingExercise() / stopBreathingExercise()

// Navigation events
fun navigateToPlace(place: SafePlace)
fun confirmArrival(isSafe: Boolean)

// Interaction tracking
fun registerUserInteraction()
```

### 2. SafetyAIEngine - Decision Logic

**Role**: AI-powered threat assessment and decision making

**Integration Points:**

```kotlin
// Called by SafetyViewModel
suspend fun generateProtocolQuestion(): ProtocolQuestion
suspend fun assessThreatLevel(): ThreatLevel
suspend fun decideEmergencyActions(context: AIDecisionContext): AIActionDecision
fun shouldEscalateThreatLevel(): ThreatLevel
```

**Data Flow:**

```
SafetyViewModel → SafetyAIEngine
  ↓
  Context (threat level, responses, time, location)
  ↓
AI Processing (on-device LLM)
  ↓
Decisions (questions, actions, escalation)
  ↓
SafetyViewModel executes decisions
```

### 3. SafetyModels - Data Layer

**Role**: Type-safe data representation

**Core Models:**

```kotlin
// Emergency session
data class EmergencySession(
    sessionId: String,
    startTime: Long,
    alarmTriggeredTime: Long,
    currentThreatLevel: ThreatLevel,
    location: Location?,
    victimResponses: List<VictimResponse>,
    alertsSent: List<AlertRecord>,
    isActive: Boolean
)

// Threat assessment
enum class ThreatLevel {
    UNKNOWN, LOW, MEDIUM, HIGH, CRITICAL
}

enum class EmergencyPath {
    NONE, THREAT_NEARBY, ESCAPE_TO_SAFETY
}

// Communication
data class EmergencyContact(
    id: String,
    name: String,
    phoneNumber: String,
    relationship: String,
    priority: Int
)

data class AlertRecord(
    timestamp: Long,
    recipientType: RecipientType,
    recipientName: String,
    recipientPhone: String?,
    messageType: MessageType,
    success: Boolean
)

// Location
data class SafePlace(
    name: String,
    type: String,
    latitude: Double,
    longitude: Double,
    is24_7: Boolean,
    address: String,
    distance: Float?,
    walkingTimeMinutes: Int?,
    hours: String?,
    notes: String?
)
```

---

## 🔌 Integration Points Detail

### Integration 1: UI → ViewModel

**Pattern**: Unidirectional Data Flow (UDF)

**UI Layer:**

```kotlin
@Composable
fun EmergencyScreen(viewModel: SafetyViewModel) {
    // Collect state
    val isAlarmActive by viewModel.isAlarmActive.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    
    // Trigger events
    Button(onClick = { viewModel.triggerEmergencyAlarm() }) {
        Text("Emergency")
    }
}
```

**Benefits:**

- ✅ Predictable state updates
- ✅ No direct UI manipulation
- ✅ Survives configuration changes
- ✅ Easy to test

### Integration 2: ViewModel → AIEngine

**Pattern**: Command/Query Pattern

**ViewModel calls AI for decisions:**

```kotlin
// In SafetyViewModel
private suspend fun presentProtocolQuestion() {
    _statusMessage.value = "Generating safety question..."
    
    // Command: Generate question
    val question = aiEngine.generateProtocolQuestion()
    
    // Update state
    _currentQuestion.value = question
    startQuestionTimer(question.timeoutSeconds)
}

private suspend fun makeAIDecision() {
    val session = _currentSession.value ?: return
    
    // Build context
    val context = AIDecisionContext(
        threatLevel = session.currentThreatLevel,
        victimResponded = session.victimResponses.lastOrNull()?.answered ?: false,
        timeSinceAlarm = (System.currentTimeMillis() - session.alarmTriggeredTime) / 1000,
        location = _currentLocation.value,
        previousAlerts = session.alertsSent,
        availableContacts = _emergencyContacts.value
    )
    
    // Query: Get AI decision
    val decision = aiEngine.decideEmergencyActions(context)
    
    // Execute actions
    executeEmergencyActions(decision.recommendedActions)
}
```

**Benefits:**

- ✅ AI logic isolated from state management
- ✅ Easy to swap AI implementations
- ✅ Testable without AI
- ✅ On-device processing (privacy)

### Integration 3: ViewModel → Models

**Pattern**: Immutable Data Classes

**State updates use copy():**

```kotlin
// Update session with new response
val response = VictimResponse(
    questionId = question.id,
    answered = true,
    responseTime = System.currentTimeMillis(),
    timeTakenSeconds = responseTime
)

val updatedSession = session.copy(
    victimResponses = session.victimResponses + response
)
_currentSession.value = updatedSession
```

**Benefits:**

- ✅ Thread-safe updates
- ✅ No accidental mutations
- ✅ Easy to track history
- ✅ Predictable state transitions

### Integration 4: ViewModel → Utils/Services

**Pattern**: Dependency Injection

**Permission checking:**

```kotlin
// In SafetyViewModel
fun toggleRecording() {
    if (_isRecordingActive.value) {
        stopRecording()
    } else {
        // Check permission before use
        if (!PermissionManager.isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)) {
            _statusMessage.value = "Microphone permission required"
            return
        }
        startRecording()
    }
}
```

**Location tracking:**

```kotlin
// In SafetyViewModel
private fun startLocationMonitoring() {
    // Check permissions
    val hasFine = PermissionManager.isPermissionGranted(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    
    if (!hasFine) {
        Log.w(TAG, "Location permission not granted")
        return
    }
    
    // Use FusedLocationProviderClient
    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        cancellationTokenSource.token
    ).addOnSuccessListener { location ->
        updateLocation(location)
    }
}
```

**Shake detection:**

```kotlin
// In SafetyViewModel (init)
shakeDetector = ShakeDetector(context) {
    // Callback when shake detected
    Log.i(TAG, "Shake gesture detected")
    triggerEmergencyAlarm()
}
```

---

## 🎯 State Machine: Emergency Flow

### States and Transitions

```
[IDLE]
  ↓ triggerEmergencyAlarm()
[ALARM_TRIGGERED]
  ↓ sendImmediateEmergencyAlerts()
[ALERTS_SENT]
  ↓ presentProtocolQuestion()
[FIRST_QUESTION_PRESENTED]
  ↓ answerProtocolQuestionYes()
  │   → [SESSION_ENDED]
  │
  ↓ answerProtocolQuestionNo()
[HIGH_THREAT_DETECTED]
  ↓ startContinuousLocationTracking()
  ↓ presentSecondQuestion()
[SECOND_QUESTION_PRESENTED]
  ↓ answerSecondQuestionYes()
  │   → [PATH_A_THREAT_NEARBY]
  │
  ↓ answerSecondQuestionNo()
      → [PATH_B_ESCAPE_TO_SAFETY]

[PATH_A_THREAT_NEARBY]
  • Show 4 large action buttons
  • Collapsible safe places (3)
  • Prominent police button
  
[PATH_B_ESCAPE_TO_SAFETY]
  • Show expanded safe places (5)
  • Journey tracking active
  • Collapsible additional protection

[ANY_STATE]
  ↓ cancelEmergencyAlarm()
  → [IDLE]
```

### State Validation

```kotlin
// Validate state transitions
private fun validateEmergencyState() {
    val session = _currentSession.value
    val isActive = _isAlarmActive.value
    
    // Invariants
    require((session != null) == isActive) {
        "Session and alarm state must be consistent"
    }
    
    if (isActive) {
        require(session!!.alertsSent.isNotEmpty() || 
                _currentQuestion.value != null) {
            "Active alarm must have alerts or active question"
        }
    }
}
```

---

## ⚡ Event-Driven Architecture

### Event Flow

```
User Action (UI)
    ↓
Event Triggered (ViewModel)
    ↓
State Validation
    ↓
Business Logic Execution
    ↓
State Update (StateFlow)
    ↓
UI Recomposition
    ↓
Side Effects (SMS, Location, etc.)
```

### Example: Complete Flow

```kotlin
// 1. User presses NO to "Are you safe?"
Button(onClick = { viewModel.answerProtocolQuestionNo() })

// 2. ViewModel processes event
fun answerProtocolQuestionNo() {
    viewModelScope.launch {
        // Validate state
        val question = _currentQuestion.value ?: return@launch
        val session = _currentSession.value ?: return@launch
        
        // Cancel timer
        questionTimerJob?.cancel()
        
        // Record response
        val response = VictimResponse(...)
        val updatedSession = session.copy(
            victimResponses = session.victimResponses + response
        )
        _currentSession.value = updatedSession
        
        // Update threat level
        updateThreatLevel(ThreatLevel.HIGH)
        
        // Execute emergency actions
        sendImmediateEmergencyAlerts()
        _emergencyContacts.value.sortedBy { it.priority }.take(2).forEach { 
            makeCall(it)
        }
        
        // Start tracking
        startContinuousLocationTracking()
        
        // Present next question
        presentSecondQuestion()
    }
}

// 3. UI automatically updates via StateFlow
val currentQuestion by viewModel.currentQuestion.collectAsState()
val threatLevel by viewModel.currentSession.collectAsState()
```

---

## 🧪 Testing Strategy

### Unit Tests (ViewModel)

```kotlin
class SafetyViewModelTest {
    @Test
    fun `when emergency triggered, should create session`() {
        // Arrange
        val viewModel = SafetyViewModel(mockContext)
        viewModel.addEmergencyContact(testContact)
        
        // Act
        viewModel.triggerEmergencyAlarm()
        
        // Assert
        assertNotNull(viewModel.currentSession.value)
        assertTrue(viewModel.isAlarmActive.value)
    }
    
    @Test
    fun `when NO answered, should set HIGH threat`() {
        // Arrange
        val viewModel = SafetyViewModel(mockContext)
        viewModel.triggerEmergencyAlarm()
        runBlocking { delay(100) } // Wait for question
        
        // Act
        viewModel.answerProtocolQuestionNo()
        
        // Assert
        assertEquals(
            ThreatLevel.HIGH,
            viewModel.currentSession.value?.currentThreatLevel
        )
    }
}
```

### Integration Tests (AI Engine)

```kotlin
class SafetyAIEngineTest {
    @Test
    fun `generateProtocolQuestion returns valid question`() = runBlocking {
        // Arrange
        val engine = SafetyAIEngine()
        
        // Act
        val question = engine.generateProtocolQuestion()
        
        // Assert
        assertNotNull(question.question)
        assertTrue(question.timeoutSeconds > 0)
        assertNotNull(question.threatLevelIfAnswered)
    }
}
```

### UI Tests (Compose)

```kotlin
class EmergencyScreenTest {
    @Test
    fun `clicking emergency button triggers alarm`() {
        // Arrange
        val viewModel = SafetyViewModel(testContext)
        composeTestRule.setContent {
            EmergencyScreen(viewModel)
        }
        
        // Act
        composeTestRule.onNodeWithText("Emergency").performClick()
        
        // Assert
        assertTrue(viewModel.isAlarmActive.value)
    }
}
```

---

## 🔒 Thread Safety

### Coroutine Scope Usage

```kotlin
// All async operations in viewModelScope
viewModelScope.launch {
    // Automatically cancelled when ViewModel cleared
    sendImmediateEmergencyAlerts()
}

// Jobs for cancellable operations
private var questionTimerJob: Job? = null
private var continuousLocationJob: Job? = null

// Cancel properly
override fun onCleared() {
    super.onCleared()
    questionTimerJob?.cancel()
    continuousLocationJob?.cancel()
}
```

### StateFlow Thread Safety

```kotlin
// StateFlow is thread-safe
private val _currentSession = MutableStateFlow<EmergencySession?>(null)
val currentSession: StateFlow<EmergencySession?> = _currentSession.asStateFlow()

// Updates are atomic
_currentSession.value = newSession

// Multiple coroutines can read/write safely
```

---

## 🐛 Race Condition Prevention

### Problem: Multiple timers updating same state

**Solution: Job cancellation**

```kotlin
private var questionTimerJob: Job? = null

fun startQuestionTimer(seconds: Int) {
    // Cancel existing timer first
    questionTimerJob?.cancel()
    
    // Start new timer
    questionTimerJob = viewModelScope.launch {
        var remaining = seconds
        _questionTimeRemaining.value = remaining
        
        while (remaining > 0) {
            delay(1000)
            remaining--
            _questionTimeRemaining.value = remaining
        }
        
        handleQuestionTimeout()
    }
}
```

### Problem: Simultaneous alert sending

**Solution: Suspend functions with sequential execution**

```kotlin
private suspend fun sendImmediateEmergencyAlerts() {
    // Sequential sending prevents race conditions
    _emergencyContacts.value.forEach { contact ->
        val success = sendSMS(contact, message)
        alertRecords.add(AlertRecord(...))
        delay(500) // Throttle
    }
}
```

### Problem: Location updates during navigation

**Solution: Single truth source with proper locking**

```kotlin
private val _currentLocation = MutableStateFlow<Location?>(null)

fun updateLocation(location: Location) {
    // Atomic update - thread-safe
    _currentLocation.value = location
    
    // Cascade updates
    updateNearestSafePlaces()
}
```

---

## 📦 Module Structure

```
app/
├── MainActivity.kt (Entry point)
├── SafetyViewModel.kt (State management)
├── SafetyAIEngine.kt (AI decisions)
├── SafetyModels.kt (Data models)
├── ui/
│   ├── screens/
│   │   ├── EmergencyScreen.kt (Emergency UI)
│   │   ├── ContactsScreen.kt (Contacts management)
│   │   ├── SettingsScreen.kt (App settings)
│   │   └── OnboardingScreen.kt (First-time setup)
│   └── theme/
│       ├── Color.kt (Color palette)
│       ├── Type.kt (Typography)
│       └── Theme.kt (Material 3 theme)
├── utils/
│   ├── PermissionManager.kt (Permission handling)
│   └── ShakeDetector.kt (Gesture detection)
└── services/
    └── EmergencyService.kt (Background service)
```

---

## ✅ Integration Checklist

### State Management:

- ✅ Single ViewModel as source of truth
- ✅ StateFlow for reactive UI updates
- ✅ Immutable data classes for state
- ✅ Proper coroutine scope management

### AI Integration:

- ✅ AIEngine isolated from ViewModel
- ✅ Context-based decision making
- ✅ Suspend functions for async operations
- ✅ On-device processing (privacy)

### Data Layer:

- ✅ Type-safe models
- ✅ Immutable data structures
- ✅ Clear semantic meaning
- ✅ Serializable for persistence

### Utils/Services:

- ✅ Permission checks before use
- ✅ Graceful error handling
- ✅ Dependency injection pattern
- ✅ Testable components

### Thread Safety:

- ✅ ViewModelScope for coroutines
- ✅ Job cancellation
- ✅ StateFlow atomic updates
- ✅ Sequential critical operations

### Testing:

- ✅ Unit tests for ViewModel
- ✅ Integration tests for AIEngine
- ✅ UI tests for screens
- ✅ Isolated component testing

---

## 🚀 Production Ready

The Guardian AI Safety App has a robust, modular architecture with:

- ✅ **Clean separation of concerns**
- ✅ **Event-driven state management**
- ✅ **Thread-safe operations**
- ✅ **Race condition prevention**
- ✅ **Comprehensive error handling**
- ✅ **Testable components**
- ✅ **Scalable design**

All integration points are properly documented and implemented! 🎯
