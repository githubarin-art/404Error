# Quick Reference - Integration Points

## 🚀 Quick Start

### Using the State Machine

```kotlin
// 1. Create state machine
val stateMachine = EmergencyStateMachine()

// 2. Observe state changes
stateMachine.currentState.collect { state ->
    when (state) {
        is EmergencyState.Idle -> // Show normal UI
        is EmergencyState.Questioning -> // Show question
        is EmergencyState.Active -> // Show emergency UI
        is EmergencyState.Resolved -> // Show resolution
    }
}

// 3. Process events
stateMachine.processEvent(
    EmergencyEvent.TriggerEmergency,
    contacts = contacts,
    location = location
)
```

### Handling Effects

```kotlin
// Execute effects from state transitions
stateMachine.effects.collect { effects ->
    effects.forEach { effect ->
        when (effect) {
            is EmergencyEffect.SendEmergencyAlerts -> sendAlerts(effect)
            is EmergencyEffect.StartLocationMonitoring -> startLocation()
            is EmergencyEffect.ShowNotification -> showNotif(effect)
            // ... handle other effects
        }
    }
}
```

---

## 📦 State Machine States

```kotlin
sealed class EmergencyState {
    object Idle                     // No emergency
    data class Triggered(...)       // Just started
    data class Questioning(...)     // Asking first question
    data class PathSelection(...)   // Asking second question
    data class Active(...)          // Emergency in progress
    data class Resolved(...)        // Emergency ended
}
```

---

## 🎯 Common Events

```kotlin
// Emergency control
EmergencyEvent.TriggerEmergency
EmergencyEvent.CancelEmergency

// Question responses
EmergencyEvent.AnswerYes
EmergencyEvent.AnswerNo
EmergencyEvent.QuestionTimeout

// Path selection
EmergencyEvent.ThreatNearby
EmergencyEvent.EscapeToSafety

// Location
EmergencyEvent.LocationUpdated(location)
EmergencyEvent.NavigateToPlace(place)
EmergencyEvent.ArrivedAtDestination

// Resolution
EmergencyEvent.UserConfirmedSafe
```

---

## ⚡ Common Effects

```kotlin
// Location
EmergencyEffect.StartLocationMonitoring
EmergencyEffect.StopLocationMonitoring

// Communication
EmergencyEffect.SendEmergencyAlerts(contacts, message, location)
EmergencyEffect.MakeEmergencyCalls(contacts)

// Monitoring
EmergencyEffect.StartEscalationMonitoring
EmergencyEffect.StartJourneyMonitoring(destination)

// Alarms
EmergencyEffect.StartLoudAlarm
EmergencyEffect.StopLoudAlarm

// UI
EmergencyEffect.ShowNotification(title, message)
```

---

## 🧪 Testing Examples

### Test State Transition

```kotlin
@Test
fun `test emergency trigger`() {
    val result = stateMachine.processEvent(
        EmergencyEvent.TriggerEmergency,
        contacts = testContacts,
        location = testLocation
    )
    
    assertTrue(result.newState is EmergencyState.Triggered)
    assertTrue(result.effects.any { it is EmergencyEffect.StartLocationMonitoring })
}
```

### Test Effect Emission

```kotlin
@Test
fun `test alerts sent on NO answer`() {
    // Setup
    setupQuestioningState()
    
    // Answer NO
    val result = stateMachine.processEvent(
        EmergencyEvent.AnswerNo,
        contacts = testContacts
    )
    
    // Verify effects
    assertTrue(result.effects.any { it is EmergencyEffect.SendEmergencyAlerts })
    assertTrue(result.effects.any { it is EmergencyEffect.MakeEmergencyCalls })
}
```

---

## 📊 State Flow Cheat Sheet

```
Idle → TriggerEmergency → Triggered
Triggered → PresentQuestion → Questioning
Questioning → AnswerYes → Resolved (USER_SAFE)
Questioning → AnswerNo → PathSelection
PathSelection → ThreatNearby → Active (CRITICAL)
PathSelection → EscapeToSafety → Active (HIGH)
Active → ArrivedAtDestination → Resolved (ARRIVED_AT_SAFETY)
Active → UserConfirmedSafe → Resolved (USER_SAFE)
Any → CancelEmergency → Resolved (MANUAL_CANCEL)
```

---

## 🔧 Common Patterns

### Check if Emergency Active

```kotlin
if (stateMachine.isEmergencyActive()) {
    // Handle active emergency
}
```

### Get Current Session

```kotlin
val session = stateMachine.getCurrentSession()
if (session != null) {
    val threatLevel = session.currentThreatLevel
    val location = session.location
}
```

### Reset State Machine

```kotlin
stateMachine.reset() // Returns to Idle
```

---

## 📝 Data Models Quick Reference

### EmergencyContact

```kotlin
EmergencyContact(
    id = "unique-id",
    name = "John Doe",
    phoneNumber = "1234567890",
    relationship = "Family", // Family, Friend, Guardian
    priority = 1 // 1 (highest) to 5 (lowest)
)
```

### EmergencySession

```kotlin
EmergencySession(
    sessionId = UUID.randomUUID().toString(),
    startTime = System.currentTimeMillis(),
    alarmTriggeredTime = System.currentTimeMillis(),
    currentThreatLevel = ThreatLevel.HIGH,
    location = location
)
```

### ThreatLevel

```kotlin
enum class ThreatLevel {
    UNKNOWN,    // Initial state
    LOW,        // User can handle
    MEDIUM,     // User needs help
    HIGH,       // Cannot respond
    CRITICAL    // Immediate danger
}
```

---

## 🛠️ Permissions

### Check Permission

```kotlin
if (PermissionManager.isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
    // Permission granted
}
```

### Get Missing Permissions

```kotlin
val missing = PermissionManager.getMissingPermissions(
    context,
    PermissionManager.CORE_PERMISSIONS
)
// Request missing permissions
```

### Get User-Friendly Explanation

```kotlin
val explanation = PermissionManager.getPermissionExplanation(
    Manifest.permission.SEND_SMS
)
// Show to user
```

---

## 🧪 Running Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests EmergencyStateMachineTest

# With coverage
./gradlew testDebugUnitTest jacocoTestReport

# Integration tests (requires device)
./gradlew connectedAndroidTest
```

---

## 📚 Key Files

### Implementation

- `state/EmergencyState.kt` - State, Event, Effect definitions
- `state/EmergencyStateMachine.kt` - State transition logic
- `SafetyViewModel.kt` - UI coordination
- `SafetyModels.kt` - Data models

### Tests

- `state/EmergencyStateMachineTest.kt` - State machine tests
- `SafetyModelsTest.kt` - Data model tests
- `utils/PermissionManagerTest.kt` - Permission tests

### Documentation

- `INTEGRATION_POINTS_IMPLEMENTATION.md` - Full architecture guide
- `TESTING_GUIDE.md` - Testing guide
- `INTEGRATION_POINTS_COMPLETED.md` - Summary of changes

---

## 💡 Best Practices

### 1. Always Use Events

```kotlin
// ✅ Good - Use events
stateMachine.processEvent(EmergencyEvent.AnswerYes, ...)

// ❌ Bad - Direct state mutation
stateMachine._currentState.value = EmergencyState.Resolved(...)
```

### 2. Handle All Effects

```kotlin
// ✅ Good - Handle all effects
when (effect) {
    is EmergencyEffect.SendAlerts -> sendAlerts()
    is EmergencyEffect.StartLocation -> startLocation()
    // ... all other effects
}

// ❌ Bad - Ignore some effects
if (effect is EmergencyEffect.SendAlerts) {
    sendAlerts()
}
```

### 3. Test State Transitions

```kotlin
// ✅ Good - Test transitions
@Test
fun `trigger transitions to Triggered`() {
    val result = stateMachine.processEvent(EmergencyEvent.TriggerEmergency)
    assertTrue(result.newState is EmergencyState.Triggered)
}

// ❌ Bad - No tests
```

### 4. Clear Effects After Processing

```kotlin
// ✅ Good - Clear after processing
effects.forEach { executeEffect(it) }
stateMachine.clearEffects()

// ❌ Bad - Never clear
effects.forEach { executeEffect(it) }
```

---

## 🚨 Troubleshooting

### State Not Changing?

```kotlin
// Check if event is valid for current state
val currentState = stateMachine.currentState.value
Log.i(TAG, "Current state: ${currentState::class.simpleName}")
Log.i(TAG, "Processing event: ${event::class.simpleName}")
```

### Effects Not Executing?

```kotlin
// Make sure you're observing effects
stateMachine.effects.collect { effects ->
    if (effects.isEmpty()) {
        Log.w(TAG, "No effects to execute")
    } else {
        Log.i(TAG, "Executing ${effects.size} effects")
        effects.forEach { executeEffect(it) }
    }
}
```

### Tests Failing?

```kotlin
// Make sure to reset between tests
@Before
fun setup() {
    stateMachine = EmergencyStateMachine()
}

@After
fun teardown() {
    stateMachine.reset()
}
```

---

## 📞 Support

For questions or issues:

1. Check `INTEGRATION_POINTS_IMPLEMENTATION.md` for detailed architecture
2. Check `TESTING_GUIDE.md` for testing help
3. Check `INTEGRATION_POINTS_COMPLETED.md` for summary
4. Review test files for examples
5. Check logs for state transitions

---

## ✅ Checklist for New Features

When adding a new feature:

- [ ] Add new event to `EmergencyEvent` if needed
- [ ] Add new effect to `EmergencyEffect` if needed
- [ ] Update state machine transition logic
- [ ] Handle new effects in ViewModel
- [ ] Add tests for new functionality
- [ ] Update documentation if architecture changes
- [ ] Verify no race conditions introduced
- [ ] Test error handling

---

**Remember:** The state machine is the **single source of truth** for emergency state. Always use
events to trigger changes and handle all effects properly.
