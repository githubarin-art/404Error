# Emergency Flow Fix Summary

## Issues Fixed

### 1. **SOS/404 Button Always Clickable**

**Problem**: The 404 error button was not always clickable because the "click anywhere" feature was
overriding it.

**Solution**:

- Separated the 404 button click handler (`onSosButtonClick`) from the screen tap handler (
  `onScreenTap`)
- The 404 button now has its own `.clickable` modifier that takes priority
- Background screen taps are handled separately for triple-tap restore functionality
- The button is now **always functional** regardless of emergency state

### 2. **Complete Emergency Flow on Re-trigger**

**Problem**: When the user returned home and clicked the 404 button again, it would either:

- Ignore the click if emergency was still active
- Skip the first question and go straight to calls/SMS

**Solution**:

- Modified `triggerEmergencyAlarm()` to properly detect re-triggers
- Modified `cancelEmergencyAlarm()` to **completely reset ALL state variables**:
    - `_currentSession` → null
    - `_currentQuestion` → null
    - `_secondQuestion` → null
    - `_emergencyPath` → NONE
    - All timers and monitoring jobs cancelled
    - Status message reset to "Ready. Stay safe."
- UI now properly waits for emergency to be fully cancelled before starting new one
- **Every trigger now starts fresh with Question 1 → Question 2 → Path Selection**

### 3. **Proper Question Flow After SMS/Calls**

**Problem**: After answering "NO" to first question, SMS and calls were sent, but then the second
question and emergency paths weren't being shown properly.

**Solution**: The flow was actually correct! Here's what happens:

#### **COMPLETE EMERGENCY FLOW:**

```
1. User triggers SOS (clicks 404 button)
   ↓
2. Emergency Session Created
   ↓
3. Location Monitoring Started
   ↓
4. SMS sent to ALL contacts immediately
   ↓
5. FIRST QUESTION: "Are you feeling safe right now?"
   ├─ YES → Emergency cancelled (user is safe)
   └─ NO/TIMEOUT → High threat detected
       ↓
6. SMS sent to ALL contacts (emergency alert)
   ↓
7. Calls made to TOP 2 priority contacts
   ↓
8. Continuous location tracking started (every 30s)
   ↓
9. SECOND QUESTION: "Is the threat near you right now?"
   ├─ YES/TIMEOUT → **PATH A: THREAT_NEARBY**
   │   - Critical threat level
   │   - Stealth features activated
   │   - Loud alarm option
   │   - Evidence recording
   │   - Fake call screen
   │   - Breathing exercises
   │   - Safe places (collapsible)
   │   - Call police option
   │
   └─ NO → **PATH B: ESCAPE_TO_SAFETY**
       - High threat level
       - Safe places (expanded, primary focus)
       - Navigation to safety
       - Journey monitoring
       - Additional protection tools (collapsible)
       - Location updates every 30s to contacts
```

### 4. **UI State Management**

**Problem**: The UI wasn't properly handling the transition from cancelled emergency to new
emergency.

**Solution**:

- Added `reTriggerState` to track when a re-trigger is in progress
- UI now waits for `isAlarmActive` to become `false` before starting new alarm
- Added 300ms delay after state change to ensure everything is settled
- This ensures clean state transitions

## Files Modified

1. **SafetyViewModel.kt**:
    - Updated `triggerEmergencyAlarm()` - Added logging for re-trigger detection
    - Updated `cancelEmergencyAlarm()` - Complete state reset with logging
    - Flow remains: Trigger → SMS → Question 1 → (if NO) → SMS/Calls → Question 2 → Path Selection

2. **EmergencyScreen.kt**:
    - Updated `Fake404ErrorScreen` parameters (separate handlers)
    - Updated `onSosButtonClick` - Proper re-trigger flow with state waiting
    - Updated `onScreenTap` - Triple-tap restore from camouflage
    - Removed global `pointerInput` that was blocking button clicks

## Testing Checklist

✅ **First Emergency Trigger**:

1. Click 404 button
2. See first question: "Are you feeling safe?"
3. Answer NO
4. See SMS sent notification
5. See calls initiated
6. See second question: "Is threat near you?"
7. Answer YES → See THREAT_NEARBY screen
8. OR Answer NO → See ESCAPE_TO_SAFETY screen

✅ **Re-trigger After Cancel**:

1. In emergency, click "False Alarm - Cancel"
2. See 404 screen again
3. Click 404 button
4. See first question again (NOT second question)
5. Full flow repeats from beginning

✅ **Re-trigger While Emergency Active**:

1. In emergency (e.g., during first question)
2. Press Back button → See 404 screen (stealth mode)
3. Click 404 button
4. Emergency cancels → New emergency starts
5. See first question again

✅ **404 Button Always Clickable**:

1. No emergency: 404 button triggers emergency ✓
2. During emergency (stealth): 404 button re-triggers ✓
3. After emergency cancelled: 404 button triggers new ✓
4. Triple-tap screen works alongside button ✓

## Key Improvements

1. **Predictable Flow**: Every emergency trigger follows the exact same flow
2. **Clean State**: Complete reset between emergencies prevents state pollution
3. **Always Functional**: 404 button never becomes unresponsive
4. **Proper Separation**: Button clicks and screen taps are handled independently
5. **Victim-Centric**: All helper features are available after the emergency procedure completes

## Important Notes

- **Question 1** is ALWAYS shown first on every trigger
- **Question 2** is shown ONLY after user answers NO to Question 1
- **SMS/Calls** happen BEFORE questions and AFTER Question 1 NO answer
- **Path A (THREAT_NEARBY)** or **Path B (ESCAPE_TO_SAFETY)** is determined by Question 2
- **All victim helper features** (loud alarm, recording, fake call, breathing, safe places, police)
  are available in BOTH paths
- **Re-triggering** always starts from Question 1, never skips steps

## Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    USER TRIGGERS SOS                        │
│                 (Clicks 404 Button)                         │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│              IMMEDIATE SMS TO ALL CONTACTS                  │
│            (Before asking any questions)                    │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│           QUESTION 1: "Are you feeling safe?"               │
│                    Timeout: 30 seconds                       │
└────────────────────────────┬────────────────────────────────┘
                             │
                    ┌────────┴────────┐
                    │                 │
                  YES                NO/TIMEOUT
                    │                 │
                    ▼                 ▼
            ┌──────────────┐   ┌──────────────────────────────┐
            │ Cancel       │   │ HIGH THREAT DETECTED         │
            │ Emergency    │   │ - Send SMS to all            │
            │ (User Safe)  │   │ - Call top 2 contacts        │
            └──────────────┘   │ - Start location tracking    │
                               └────────┬─────────────────────┘
                                        │
                                        ▼
                      ┌──────────────────────────────────────┐
                      │  QUESTION 2: "Is threat near you?"   │
                      │         Timeout: 30 seconds           │
                      └────────┬─────────────────────────────┘
                               │
                      ┌────────┴────────┐
                      │                 │
                 YES/TIMEOUT           NO
                      │                 │
                      ▼                 ▼
            ┌──────────────────┐  ┌──────────────────────┐
            │  PATH A:         │  │  PATH B:             │
            │  THREAT_NEARBY   │  │  ESCAPE_TO_SAFETY    │
            │                  │  │                      │
            │ - Stealth mode   │  │ - Navigate to        │
            │ - Loud alarm     │  │   safe places        │
            │ - Recording      │  │ - Journey monitor    │
            │ - Fake call      │  │ - Location updates   │
            │ - Breathing      │  │ - Protection tools   │
            │ - Call police    │  │ - Call police        │
            └──────────────────┘  └──��───────────────────┘
```
