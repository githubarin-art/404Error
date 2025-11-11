# Path B Implementation Summary

## ✅ What Was Implemented

### Problem Statement

User reported that after answering Q2 = NO (escape to safety), the app should:

1. Make phone calls to contacts FIRST
2. Keep recording/features persistent in background during calls
3. Auto-switch between escape UI and home screen every 10 seconds for stealth

### Solution Implemented

#### 1. **Updated `answerSecondQuestionNo()` Function**

```kotlin
fun answerSecondQuestionNo() {
    // Step 1: Call ALL contacts (not just top 2)
    _emergencyContacts.value.sortedBy { it.priority }.forEach { contact ->
        makeCall(contact)
        delay(2000) // 2 second delay between calls
    }
    
    // Step 2: Show escape UI
    _statusMessage.value = "HIGH ALERT - Navigate to safety"
    
    // Step 3: Start auto-switching UI (stealth mode)
    startStealthModeSwitching()
}
```

#### 2. **New Stealth Mode Auto-Switching**

```kotlin
private fun startStealthModeSwitching() {
    stealthModeSwitchingJob = viewModelScope.launch {
        delay(10000) // Initial 10s delay
        
        var showDecoy = true
        while (isAlarmActive && emergencyPath == ESCAPE_TO_SAFETY) {
            _showStealthDecoy.value = showDecoy
            delay(10000) // Switch every 10 seconds
            showDecoy = !showDecoy
        }
    }
}
```

#### 3. **New State Variables**

```kotlin
// Job for stealth mode switching
private var stealthModeSwitchingJob: Job? = null

// Controls UI switching between escape UI and decoy
private val _showStealthDecoy = MutableStateFlow(false)
val showStealthDecoy: StateFlow<Boolean> = _showStealthDecoy.asStateFlow()
```

## How It Works

### Timeline

```
T+0s:    User answers Q2 = NO
         → Calls ALL contacts automatically
         → Shows escape UI with safe places
         
T+10s:   AUTO-SWITCH to home screen (404 decoy)
         → Attacker sees nothing suspicious
         → Recording/alarm still active in background
         
T+20s:   AUTO-SWITCH to escape UI
         → Victim checks navigation progress
         → Can interact with features
         
T+30s:   AUTO-SWITCH to home screen
         → Repeat cycle...
         
T+XYZ:   User arrives at destination
         → Confirms arrival
         → Stealth mode stops
         → Emergency ends
```

### Key Features

1. **Phone Calls During Path B**
    - ✅ Calls ALL contacts (not just top 2 like Path A)
    - ✅ 2-second delay between calls
    - ✅ Happens automatically before showing UI

2. **Stealth Mode Switching**
    - ✅ Every 10 seconds alternates between:
        - Escape UI (navigation, features)
        - Home Screen (404 decoy page)
    - ✅ Confuses attackers
    - ✅ Guides victim to safety

3. **Background Persistence**
    - ✅ Audio recording continues
    - ✅ Loud alarm continues
    - ✅ Fake call works
    - ✅ Location updates every 30s
    - ✅ Journey monitoring active

## UI Integration

The UI layer should observe `showStealthDecoy`:

```kotlin
val showStealthDecoy by viewModel.showStealthDecoy.collectAsState()
val emergencyPath by viewModel.emergencyPath.collectAsState()

when {
    emergencyPath == EmergencyPath.ESCAPE_TO_SAFETY && showStealthDecoy -> {
        // Show home screen (404 decoy)
        DecoyErrorScreen()
    }
    emergencyPath == EmergencyPath.ESCAPE_TO_SAFETY -> {
        // Show escape UI with navigation
        PathBEscapeScreen(
            safePlaces = nearestSafePlaces,
            isRecording = isRecordingActive,
            isAlarmActive = isLoudAlarmActive,
            onNavigate = { place -> viewModel.navigateToPlace(place) },
            onToggleRecording = { viewModel.toggleRecording() },
            onToggleAlarm = { viewModel.toggleLoudAlarm() }
        )
    }
}
```

## Comparison: Path A vs Path B

| Aspect | Path A (Threat Nearby) | Path B (Escape to Safety) |
|--------|------------------------|---------------------------|
| Trigger | Q2 = YES | Q2 = NO |
| Calls | Top 2 contacts | ALL contacts |
| UI Behavior | Fixed emergency UI | Auto-switching stealth UI |
| Switching | No | Yes (every 10s) |
| Primary Goal | Alert & protect in place | Navigate to safety |
| Stealth Mode | No | Yes |

## Files Modified

1. **`app/src/main/java/com/runanywhere/startup_hackathon20/SafetyViewModel.kt`**
    - Lines 1504-1543: `answerSecondQuestionNo()` updated
    - Lines 2009-2045: New stealth mode functions
    - Line 842: Cancel stealth job in cleanup
    - Line 863: Reset stealth state in cleanup
    - Lines 1971-1972: Stop stealth on arrival

2. **New Documentation Files**
    - `PATH_B_STEALTH_MODE_IMPLEMENTATION.md` - Detailed guide
    - `IMPLEMENTATION_SUMMARY_PATH_B.md` - This file

## Testing Instructions

1. **Basic Flow Test**
   ```
   1. Trigger SOS
   2. Answer Q1 = NO
   3. Wait for SMS to be sent
   4. Answer Q2 = NO
   5. Verify: Phone calls ALL contacts
   6. Verify: Escape UI appears with safe places
   7. Wait 10 seconds
   8. Verify: UI switches to home screen (404)
   9. Wait 10 seconds
   10. Verify: UI switches back to escape UI
   11. Verify: Switching continues every 10s
   ```

2. **Background Persistence Test**
   ```
   1. Activate Path B
   2. Start audio recording
   3. Wait for UI to switch to home screen
   4. Verify: Recording timer still counting
   5. UI switches back to escape UI
   6. Verify: Recording still active
   7. Stop recording
   8. Verify: File saved successfully
   ```

3. **Navigation Test**
   ```
   1. Activate Path B
   2. Select a destination (e.g., police station)
   3. Google Maps opens for navigation
   4. Return to app
   5. Verify: UI switches to home screen after 10s
   6. Verify: Location updates sent to contacts
   7. Simulate arrival (within 50m)
   8. Verify: "Have you arrived safely?" dialog
   9. Answer YES
   10. Verify: Stealth mode stops, emergency ends
   ```

## Key Benefits

✅ **Stealth Protection**: Attacker sees normal home screen, not emergency UI  
✅ **Automatic Guidance**: Victim gets periodic access to navigation UI  
✅ **Background Safety**: All safety features continue regardless of UI state  
✅ **Contact Alerts**: ALL contacts called immediately, not just top 2  
✅ **Journey Monitoring**: Tracks progress, alerts if stopped or deviated

## What Happens Next?

The UI team needs to integrate the `showStealthDecoy` state to switch between:

- `DecoyErrorScreen()` when `showStealthDecoy == true`
- `PathBEscapeScreen()` when `showStealthDecoy == false`

All background features (recording, alarm, location tracking) work independently of UI state.

---

**Status**: ✅ **COMPLETE**  
**Ready for**: UI Integration & Testing  
**Date**: 2025-01-10
