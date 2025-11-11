# Path B: Escape to Safety with Stealth Mode

## Overview

When the user answers **NO** to the second question ("Is the threat near you?"), it means **the
threat is NOT nearby** and they need to **escape to safety**. This activates **Path B** with a
sophisticated stealth mode that alternates between escape UI and home screen to confuse attackers
while guiding the victim.

## Complete Path B Flow

### 1. **User Answers Q2 = NO**

```
User: "The threat is not near me, I need to escape"
```

### 2. **Immediate Actions (Automatic)**

#### Step 1: Call ALL Emergency Contacts

- ✅ System calls **ALL contacts** (not just top 2) in priority order
- ✅ 2-second delay between each call to avoid overwhelming victim
- ✅ Victim sees: "HIGH ALERT - CALLING CONTACTS"

```kotlin
_emergencyContacts.value.sortedBy { it.priority }.forEach { contact ->
    Log.i(TAG, "📞 Calling ${contact.name} (priority ${contact.priority})")
    makeCall(contact)
    delay(2000) // 2 second delay between calls
}
```

#### Step 2: Show Escape UI

- ✅ Display nearest safe places (police, hospitals, stores, etc.)
- ✅ Show Path B features:
    - 🗺️ Navigate to safe place buttons
    - 🚨 Loud Alarm toggle
    - 🎤 Record Evidence toggle
    - 📞 Fake Call button
    - 🧘 Breathing Exercise button
    - 🚓 Call Police button

#### Step 3: Start Stealth Mode Auto-Switching

- ✅ After 10 seconds → Switch to HOME SCREEN (404 decoy)
- ✅ After another 10 seconds → Switch back to ESCAPE UI
- ✅ **Repeat this cycle** until emergency ends

## Stealth Mode Auto-Switching Logic

### Purpose

**Confuse attackers** while **guiding victim** to safety:

- When attacker looks: Sees normal home screen (404 page)
- When victim needs guidance: Sees escape UI with navigation

### Timeline

```
T+0s:    Path B activated → Show Escape UI
         [User can see safe places and features]
         
T+10s:   Switch to HOME SCREEN (404 decoy)
         [Attacker sees nothing suspicious]
         [Recording, fake call, etc. still work in background]
         
T+20s:   Switch to ESCAPE UI (navigation)
         [Victim can check progress, choose destination]
         
T+30s:   Switch to HOME SCREEN (404 decoy)
         [Attacker sees nothing suspicious]
         
T+40s:   Switch to ESCAPE UI (navigation)
         [Victim can navigate to safety]
         
... continues until emergency ends
```

### Implementation

```kotlin
private fun startStealthModeSwitching() {
    stealthModeSwitchingJob?.cancel()
    stealthModeSwitchingJob = viewModelScope.launch {
        Log.i(TAG, "🕶️ Starting stealth mode UI switching")
        
        // Initial delay before first switch (let user see escape UI first)
        delay(10000) // 10 seconds
        
        var showDecoy = true
        while (_isAlarmActive.value && _emergencyPath.value == EmergencyPath.ESCAPE_TO_SAFETY) {
            _showStealthDecoy.value = showDecoy
            
            if (showDecoy) {
                Log.i(TAG, "🏠 Switching to HOME SCREEN (decoy) for 10 seconds")
                _statusMessage.value = "Stealth mode: Hiding UI"
            } else {
                Log.i(TAG, "🗺️ Switching to ESCAPE UI (navigation) for 10 seconds")
                _statusMessage.value = "Navigate to safety"
            }
            
            delay(10000) // 10 seconds
            showDecoy = !showDecoy // Toggle
        }
        
        Log.i(TAG, "🛑 Stealth mode UI switching stopped")
        _showStealthDecoy.value = false
    }
}
```

## State Management

### New State Variables

```kotlin
// Stealth mode switching job
private var stealthModeSwitchingJob: Job? = null

// Controls whether to show decoy (home screen) or escape UI
private val _showStealthDecoy = MutableStateFlow(false)
val showStealthDecoy: StateFlow<Boolean> = _showStealthDecoy.asStateFlow()
```

### UI Layer Integration

The UI layer should observe `showStealthDecoy`:

```kotlin
val showStealthDecoy by viewModel.showStealthDecoy.collectAsState()

when {
    showStealthDecoy -> {
        // Show DecoyErrorScreen (404 page)
        DecoyErrorScreen()
    }
    else -> {
        // Show Path B Escape UI
        PathBEscapeScreen(
            safePlaces = nearestSafePlaces,
            onNavigate = { place -> viewModel.navigateToPlace(place) },
            // ... other features
        )
    }
}
```

## Features Persistence

### Background Features (Always Available)

These features remain **active and accessible** even when home screen (decoy) is shown:

1. **🎤 Audio Recording**
    - Continues recording in background
    - Duration timer keeps counting
    - Evidence saved to device

2. **🚨 Loud Alarm**
    - Continues playing in background
    - Vibration continues
    - Attacker hears alarm even if UI is hidden

3. **📞 Fake Call**
    - Realistic incoming call screen
    - Works even during stealth mode
    - Can dismiss attacker's suspicion

4. **📍 Location Tracking**
    - Updates every 30 seconds
    - Sends updates to contacts
    - Cached for offline access

5. **🗺️ Journey Monitoring**
    - Tracks progress to destination
    - Alerts if stopped moving (>2 min)
    - Alerts if deviated from route

## Path B vs Path A Comparison

| Feature | Path A (Threat Nearby) | Path B (Escape to Safety) |
|---------|------------------------|---------------------------|
| **Trigger** | Q2 = YES (threat nearby) | Q2 = NO (threat not nearby) |
| **Calls** | Top 2 contacts only | ALL contacts |
| **UI** | Fixed emergency UI | Auto-switching stealth UI |
| **Primary Action** | Activate alarm/recording | Navigate to safe place |
| **Threat Level** | CRITICAL | HIGH |
| **Stealth Mode** | No | Yes (10s switching) |
| **Duration** | Until user cancels | Until arrival or cancel |

## User Experience Examples

### Example 1: Safe Escape

```
1. User answers Q2 = NO (threat not nearby)
2. System calls all 3 contacts automatically
3. User sees escape UI with 5 nearest safe places
4. User selects "Pune Police Station" (500m away)
5. Google Maps opens for navigation
6. After 10s → UI switches to home screen (decoy)
7. User continues walking while attacker sees nothing
8. After 10s → UI switches back to escape UI
9. User checks progress: "300m remaining"
10. Cycle continues until user arrives
11. "Have you arrived safely?" → YES
12. Emergency ends, all contacts notified
```

### Example 2: Recording Evidence During Escape

```
1. User answers Q2 = NO
2. System calls all contacts
3. User sees escape UI
4. User activates RECORD EVIDENCE
5. Recording starts in background
6. User selects navigation destination
7. UI switches to home screen (stealth)
8. Recording continues in background
9. Attacker sees normal home screen
10. UI switches back to escape UI
11. User sees "Recording: 4:32" (still running)
12. User stops recording when safe
```

### Example 3: Attacker Confrontation During Escape

```
1. User is navigating to safety (Path B active)
2. UI shows escape UI with navigation
3. Attacker approaches: "What are you doing?"
4. UI auto-switches to home screen (10s interval)
5. User shows phone: "Just browsing, see? Normal page"
6. Attacker sees 404 error page (looks normal)
7. Attacker leaves
8. After 10s → UI switches back to escape UI
9. User continues navigation
10. Background: Recording still active, location updates sent
```

## Journey Monitoring Features

When user selects a destination in Path B, journey monitoring automatically starts:

### 1. **Progress Tracking**

- Updates every 30 seconds
- Sends distance updates to contacts
- "Moving towards Pune Police Station - 450m away"

### 2. **Deviation Detection**

- Alerts if user moves >50m away from route
- "URGENT ALERT: Deviated from route to Pune Police Station"

### 3. **Stopped Movement Detection**

- Alerts if user stops moving for >2 minutes
- "URGENT ALERT: Stopped moving towards Pune Police Station for over 2 minutes"

### 4. **Arrival Confirmation**

- When within 50m of destination
- "Have you arrived safely?"
- YES → End emergency
- NO → Continue monitoring

## Emergency Termination

### User Cancels

- User presses "Cancel Emergency"
- Stealth mode stops immediately
- Contacts notified of cancellation

### Arrival at Safe Place

- User confirms arrival
- Journey monitoring stops
- Stealth mode stops
- Emergency ends gracefully

### Manual Cancel

```kotlin
fun confirmArrival(isSafe: Boolean) {
    _showArrivalConfirmation.value = false
    if (isSafe) {
        stealthModeSwitchingJob?.cancel()  // Stop stealth mode
        _showStealthDecoy.value = false    // Reset decoy state
        cancelEmergencyAlarm()              // End emergency
    } else {
        _statusMessage.value = "Continue to safety"
    }
}
```

## Files Modified

### 1. **SafetyViewModel.kt**

#### New Variables (Lines 2009-2011)

```kotlin
private var stealthModeSwitchingJob: Job? = null
private val _showStealthDecoy = MutableStateFlow(false)
val showStealthDecoy: StateFlow<Boolean> = _showStealthDecoy.asStateFlow()
```

#### Updated Functions

- **`answerSecondQuestionNo()`** (Lines 1504-1543)
    - Now calls ALL contacts (not just displays UI)
    - Starts stealth mode switching

- **`startStealthModeSwitching()`** (Lines 2013-2045)
    - New function for auto-switching UI
    - 10-second intervals
    - Alternates between decoy and escape UI

- **`cancelEmergencyAlarm()`** (Lines 825-880)
    - Cancels stealth mode job
    - Resets decoy state

- **`confirmArrival()`** (Lines 1969-1978)
    - Stops stealth mode on safe arrival

## Testing Checklist

### Basic Path B Flow

- [ ] Trigger SOS
- [ ] Answer Q1 = NO
- [ ] Answer Q2 = NO
- [ ] Verify: ALL contacts called (not just top 2)
- [ ] Verify: Escape UI shown with safe places
- [ ] Verify: After 10s → Switches to home screen
- [ ] Verify: After another 10s → Switches back to escape UI
- [ ] Verify: Switching continues every 10 seconds

### Background Feature Persistence

- [ ] Start recording during escape
- [ ] Wait for UI to switch to home screen
- [ ] Verify: Recording continues (check duration counter)
- [ ] Switch back to escape UI
- [ ] Verify: Recording still active
- [ ] Stop recording
- [ ] Verify: Evidence saved

### Navigation & Journey Monitoring

- [ ] Select a safe place destination
- [ ] Verify: Google Maps opens
- [ ] Verify: UI switches to home screen after 10s
- [ ] Verify: Location updates sent to contacts
- [ ] Simulate stopping movement
- [ ] Verify: Alert sent after 2 minutes
- [ ] Reach destination
- [ ] Verify: "Have you arrived safely?" shown
- [ ] Answer YES
- [ ] Verify: Emergency ends, stealth mode stops

### Stealth Mode in Different Scenarios

- [ ] Activate Path B
- [ ] Start loud alarm
- [ ] Wait for UI to switch to home screen
- [ ] Verify: Alarm still audible
- [ ] Verify: Home screen looks normal (404 page)
- [ ] UI switches back
- [ ] Verify: Can toggle alarm off/on

## Known Limitations & Future Enhancements

### Current Limitations

1. 10-second interval is fixed (could be configurable)
2. No manual trigger for UI switch (user can't force switch)
3. No indicator showing "next switch in X seconds"

### Planned Enhancements

1. Shake phone to force switch to escape UI
2. Volume button quick switch
3. Configurable interval (5s, 10s, 15s)
4. Notification showing next switch countdown
5. Option to disable auto-switching (manual only)

---

**Status**: ✅ **IMPLEMENTED**  
**Date**: 2025-01-10  
**Impact**: Critical - Provides stealth escape guidance for Path B
