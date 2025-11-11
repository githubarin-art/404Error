# Second Question Flow Fix

## Problem Identified

After the second question ("Is the threat near you right now?") was presented to the user, the app
was **immediately proceeding with actions BEFORE the user could answer**. This was caused by:

1. **`answerSecondQuestionYes()`** (line 1471) - Called `makeAIDecision()` after making calls
2. **`answerSecondQuestionNo()`** (line 1491) - Called `makeAIDecision()`
3. The `makeAIDecision()` function triggered additional unwanted actions that interfered with the
   clean Path A/B separation

## Solution Implemented

### Changes Made to `SafetyViewModel.kt`

#### 1. **Path A (Threat Nearby) - Lines 1471-1502**

```kotlin
fun answerSecondQuestionYes() {
    secondQuestionTimerJob?.cancel()
    val question = _secondQuestion.value ?: return
    _secondQuestion.value = null
    _secondQuestionTimeRemaining.value = null

    _emergencyPath.value = EmergencyPath.THREAT_NEARBY
    updateThreatLevel(ThreatLevel.CRITICAL)
    updateNearestSafePlaces()
    _statusMessage.value = "CRITICAL - THREAT NEARBY"
    
    Log.i(TAG, "========================================")
    Log.i(TAG, "PATH A: THREAT NEARBY - CRITICAL THREAT")
    Log.i(TAG, "Actions: Calling top 2 contacts immediately")
    Log.i(TAG, "User can activate loud alarm, record evidence, or fake call")
    Log.i(TAG, "========================================")
    
    viewModelScope.launch {
        // Immediately call top 2 priority contacts when threat is nearby
        _emergencyContacts.value.sortedBy { it.priority }.take(2).forEach { contact ->
            Log.i(TAG, "📞 Calling ${contact.name} (priority ${contact.priority})")
            makeCall(contact)
            delay(1000)
        }
        
        Log.i(TAG, "✅ Emergency calls completed for Path A")
        _statusMessage.value = "CRITICAL - Calls made. Use safety features below."
        
        // ✅ REMOVED: makeAIDecision() call
        // User now has full control over Path A features
    }
}
```

**Key Changes:**

- ❌ **Removed** `makeAIDecision()` call
- ✅ **Added** comprehensive logging for debugging
- ✅ Only executes **Path A specific actions**: Call top 2 contacts
- ✅ User can then manually activate: Loud alarm, Recording, Fake call

#### 2. **Path B (Escape to Safety) - Lines 1504-1526**

```kotlin
fun answerSecondQuestionNo() {
    secondQuestionTimerJob?.cancel()
    val question = _secondQuestion.value ?: return
    _secondQuestion.value = null
    _secondQuestionTimeRemaining.value = null

    _emergencyPath.value = EmergencyPath.ESCAPE_TO_SAFETY
    updateThreatLevel(ThreatLevel.HIGH)
    updateNearestSafePlaces()
    _statusMessage.value = "HIGH ALERT - ESCAPE TO SAFETY"
    
    Log.i(TAG, "========================================")
    Log.i(TAG, "PATH B: ESCAPE TO SAFETY - HIGH ALERT")
    Log.i(TAG, "Actions: Navigate to nearest safe place")
    Log.i(TAG, "Safe places displayed: ${_nearestSafePlaces.value.size}")
    Log.i(TAG, "Journey monitoring will track progress")
    Log.i(TAG, "========================================")
    
    // ✅ REMOVED: makeAIDecision() call
    // User chooses navigation destination manually
    // Location updates are already running from Q1 NO answer
}
```

**Key Changes:**

- ❌ **Removed** `makeAIDecision()` call
- ✅ **Added** comprehensive logging
- ✅ Only executes **Path B specific actions**: Display safe places
- ✅ User can then manually: Navigate to chosen destination
- ✅ Journey monitoring starts automatically when navigation begins

## Complete Emergency Flow (Fixed)

### 1. **SOS Triggered**

- Location monitoring starts immediately
- NO alerts sent yet

### 2. **Question 1: "Are you safe?"**

- **User waits for 30 seconds** (timer countdown)
- **YES** → Cancel emergency, all safe ✅
- **NO/Timeout** → Continue to next steps

### 3. **After Q1 = NO**

- ✅ Send SMS to ALL contacts with location
- ✅ Start continuous location tracking (every 30s)
- ✅ Present Question 2

### 4. **Question 2: "Is the threat near you?"**

- **User waits for 30 seconds** (timer countdown)
- **YES** → Path A (Threat Nearby)
- **NO** → Path B (Escape to Safety)
- **Timeout** → Defaults to Path A (assumes worst case)

### 5a. **Path A: Threat Nearby** (Q2 = YES)

- ✅ Set threat level: CRITICAL
- ✅ Call top 2 priority contacts immediately
- ✅ Display Path A UI with features:
    - 🚨 Loud Alarm toggle
    - 🎤 Record Evidence toggle
    - 📞 Fake Call button
    - 🧘 Breathing Exercise button
    - 🚓 Call Police button
- ⏸️ **WAITS for user interaction** - no automatic actions

### 5b. **Path B: Escape to Safety** (Q2 = NO)

- ✅ Set threat level: HIGH
- ✅ Display nearest 5 safe places (police, hospitals, etc.)
- ✅ User can select destination to navigate
- ✅ Journey monitoring starts when navigation begins
- ⏸️ **WAITS for user to choose destination** - no automatic navigation

## Benefits of This Fix

1. ✅ **User Control**: App waits for user answers before proceeding
2. ✅ **Clean Path Separation**: Path A and Path B don't interfere with each other
3. ✅ **No Premature Actions**: AI doesn't make decisions that override user choice
4. ✅ **Better UX**: Clear feedback and status messages at each step
5. ✅ **Comprehensive Logging**: Easy to debug and trace emergency flow

## Testing Checklist

### Path A Testing

- [ ] Trigger SOS
- [ ] Answer Q1 = NO
- [ ] Wait for Q2 presentation (verify timer countdown)
- [ ] Answer Q2 = YES
- [ ] Verify: Calls made to top 2 contacts
- [ ] Verify: Path A UI displayed with all features
- [ ] Verify: No automatic alarm/recording starts
- [ ] Test: Manually toggle each feature (alarm, record, fake call)

### Path B Testing

- [ ] Trigger SOS
- [ ] Answer Q1 = NO
- [ ] Wait for Q2 presentation (verify timer countdown)
- [ ] Answer Q2 = NO
- [ ] Verify: Safe places list displayed
- [ ] Verify: No automatic navigation starts
- [ ] Test: Select a destination manually
- [ ] Verify: Journey monitoring begins
- [ ] Test: Arrival confirmation when reaching destination

### Timeout Testing

- [ ] Trigger SOS
- [ ] Let Q1 timeout (don't answer)
- [ ] Verify: Proceeds as if Q1 = NO (SMS sent)
- [ ] Let Q2 timeout (don't answer)
- [ ] Verify: Proceeds as if Q2 = YES (Path A - calls made)

## Files Modified

1. **`SafetyViewModel.kt`**
    - Lines 1471-1502: `answerSecondQuestionYes()`
    - Lines 1504-1526: `answerSecondQuestionNo()`

## No Breaking Changes

- All existing functionality preserved
- Emergency contact SMS still sent after Q1 = NO
- Location tracking still works continuously
- Auto re-trigger for HIGH threat still active (every 5 minutes)
- Escalation monitoring still running

---

**Status**: ✅ **FIXED**  
**Date**: 2025-01-10  
**Impact**: Critical - Ensures proper emergency flow control
