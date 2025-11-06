# 📱 404Error Safety App - Complete Flow Guide

## Overview

This document explains the complete flow of the **404Error** safety app from first launch to
emergency alert.

---

## 🎯 App Flow

### **1. First Launch - Onboarding** 🚀

When user opens the app for the first time:

**File:** `MainActivity.kt`

- Checks `SharedPreferences` for `onboarding_complete` flag
- If `false` → Shows `OnboardingScreen`
- If `true` → Shows main app

**Onboarding Steps:**

#### **Step 1: Welcome Screen**

- Shows app branding: "404Error"
- Explains AI model requirement
- User clicks "Continue"

#### **Step 2: AI Model Installation**

- Downloads AI model (Qwen 2.5 0.5B ~374 MB)
- Shows progress bar
- Auto-proceeds when complete
- **Required for emergency AI decision-making**

#### **Step 3: Phone Number Entry**

- User enters their mobile number
- Used for verification and identification
- Required to proceed

#### **Step 4: Emergency Contacts** ⭐ **CRITICAL**

- User MUST add at least 1 emergency contact
- For each contact, user enters:
    - **Name** (e.g., "Mom", "John")
    - **Phone Number** (e.g., "+1234567890")
    - **Relationship** (e.g., "Family", "Friend")
- Can add multiple contacts
- **These are the ONLY numbers that will receive emergency alerts**
- **NO sample/dummy data is used**
- Contacts saved to `SharedPreferences` as JSON

#### **Step 5: Location Permission** 📍

- Requests `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`
- **Critical for sending location in emergency SMS**
- User can skip (but location won't be in alerts)
- Permission can be granted later in Settings

#### **Step 6: SMS & Call Permission**

- Requests:
    - `SEND_SMS` - To send emergency messages
    - `CALL_PHONE` - To make emergency calls
    - `RECORD_AUDIO` - For audio evidence
- Required for emergency features
- User can skip but app won't function fully

#### **Step 7: Completion**

- All contacts saved to ViewModel
- Marks `onboarding_complete = true`
- User proceeds to main app

---

### **2. Main App - Home Screen** 🏠

**File:** `EmergencyScreen.kt` (Main screen)

**User sees:**

- Large SOS/Emergency button
- Status message
- Emergency contacts list
- AI protocol questions (when active)

**Navigation Bar:**

- **Home** - Emergency screen
- **Contacts** - Manage contacts (placeholder)
- **Settings** - App settings (placeholder)

---

### **3. Emergency Triggered** 🚨

**What happens when user presses SOS button:**

**File:** `SafetyViewModel.kt` → `triggerEmergencyAlarm()`

#### **Step 1: Validation** ✅

```
1. Check if AI model is loaded
   ❌ If not → Show "Please load AI model first"
   
2. Check if emergency contacts exist
   ❌ If empty → Show "Please add emergency contacts first!"
   
3. Check if alarm already active
   ❌ If yes → Ignore (prevent duplicate triggers)
```

#### **Step 2: Create Emergency Session** 📝

```kotlin
EmergencySession(
    sessionId: "unique-uuid",
    startTime: current_time,
    alarmTriggeredTime: current_time,
    currentThreatLevel: UNKNOWN,
    location: null (will be updated)
)
```

#### **Step 3: Get Location** 📍

**File:** `SafetyViewModel.kt` → `startLocationMonitoring()`

**Location Strategy:**

**Scenario A: Permission Granted (Onboarding)**

```
1. Check Fine/Coarse location permission
   ✅ Granted → Proceed with location fetch
   
2. Try Last Known Location (Fastest)
   - If available and recent (< 5 minutes) → Use it
   - If old → Try fresh location
   
3. Request Current Location
   - Fine permission → HIGH_ACCURACY (GPS + Network)
   - Coarse permission → BALANCED (Network-based)
   - Timeout: 2 seconds
   
4. Update location in session
```

**Scenario B: Permission NOT Granted**

```
⚠️ Location permission not granted
   
1. Log warning
2. Check if GPS/Network is enabled on device
3. Emergency alert STILL PROCEEDS without location
4. Message will show: "Location unavailable - Please try calling me!"

Note: Android requires explicit user consent for location.
Cannot access location without permission for security reasons.
```

**Wait 2 seconds** for location fetch to complete

#### **Step 4: Send Immediate Emergency Alerts** 📨

**File:** `SafetyViewModel.kt` → `sendImmediateEmergencyAlerts()`

**For EACH emergency contact:**

```
1. Build emergency message with:
   - Emergency alert header
   - Timestamp
   - Location (if available):
     * GPS coordinates
     * Google Maps link
   - Instructions for contacts
   - App signature

2. Send SMS to contact

3. Log result (success/failure)

4. Wait 500ms (avoid carrier throttling)

5. Repeat for next contact
```

**Emergency Message Format:**

**With Location:**

```
🚨 EMERGENCY ALERT 🚨

I need immediate help! I've triggered my emergency alarm.

Time: Nov 03, 2025 at 09:30 PM

📍 MY LOCATION:
Latitude: 28.5383
Longitude: 77.3904

🗺️ Open in Maps:
https://maps.google.com/?q=28.5383,77.3904

Please come to my location or call emergency services if needed.

- Sent via 404Error Safety App
```

**Without Location (No Permission):**

```
🚨 EMERGENCY ALERT 🚨

I need immediate help! I've triggered my emergency alarm.

Time: Nov 03, 2025 at 09:30 PM

⚠️ Location unavailable - Please try calling me!

If I don't respond, please contact emergency services.

- Sent via 404Error Safety App
```

#### **Step 5: Protocol Questions** ❓

**File:** `SafetyAIEngine.kt` → `generateProtocolQuestion()`

AI generates questions like:

- "Are you in immediate danger?"
- "Can you speak freely?"
- "Do you need police assistance?"

**User has 30 seconds to respond:**

- **YES** → Lower threat level, AI reassesses
- **NO** → High threat level, escalate actions
- **No Response** → Timeout → High threat, escalate

#### **Step 6: AI Decision Making** 🤖

**File:** `SafetyAIEngine.kt` → `decideEmergencyActions()`

Based on:

- Threat level (LOW, MEDIUM, HIGH, CRITICAL)
- User response time
- Location availability
- Previous alerts sent

**AI decides:**

- Send follow-up SMS
- Make phone calls
- Send missed calls (stealth alert)
- Call emergency services (911/112)

#### **Step 7: Escalation Monitoring** ⏱️

Runs every 30 seconds:

```
1. Check time since alarm triggered
2. If threat level should escalate → Update
3. Make new AI decisions
4. Send additional alerts if needed
```

#### **Step 8: Cancel Alarm** ✖️

When user clicks "Cancel" or "False Alarm":

```
1. Stop all monitoring
2. Mark session as inactive
3. Send cancellation SMS to all alerted contacts:
   "False alarm. I'm safe now. Sorry for the concern."
```

---

## 📂 Key Files

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Entry point, handles onboarding flow |
| `OnboardingScreen.kt` | 7-step onboarding wizard |
| `EmergencyScreen.kt` | Main emergency button screen |
| `SafetyViewModel.kt` | Core logic, handles emergency flow |
| `SafetyAIEngine.kt` | AI decision making |
| `SafetyModels.kt` | Data classes |

---

## 🔒 Security Features

1. ✅ **No Sample Data** - Only user-provided contacts
2. ✅ **Persistent Storage** - Contacts saved locally
3. ✅ **Permission Checks** - Validates before accessing features
4. ✅ **Comprehensive Logging** - All actions logged for debugging
5. ✅ **Graceful Degradation** - Works even without location
6. ✅ **Privacy** - AI runs on-device, no cloud

---

## 📱 Permission Handling

### **Location Permission**

**When NOT Granted:**

```
❌ Location unavailable in emergency message
✅ Emergency alert STILL sent to all contacts
✅ Contacts can call victim directly
⚠️ Message includes warning about location unavailability
```

**When Granted:**

```
✅ Real GPS coordinates included
✅ Google Maps clickable link
✅ Contacts can navigate directly to victim
✅ Timestamp shows when location was fetched
```

**Important:** Android does NOT allow "secret" location access. User MUST explicitly grant
permission in:

1. Onboarding flow
2. OR Device Settings > Apps > 404Error > Permissions

### **SMS/Call Permission**

**When NOT Granted:**

```
❌ Cannot send emergency SMS
❌ Cannot make emergency calls
⚠️ App functionality severely limited
```

**When Granted:**

```
✅ Send emergency SMS to all contacts
✅ Make voice calls if AI decides
✅ Make missed calls for stealth alerts
✅ Call emergency services if critical
```

---

## 🎨 404Error Theme

**App Name:** 404Error  
**Theme:** Dark tech/error aesthetic  
**Colors:**

- Primary: Error Red (#FF0000)
- Background: Very Dark (#1A1A1A)
- Accents: Neon Green, Cyan, Purple

**Design Philosophy:**

- Emergency-focused
- High contrast for visibility
- Dark UI for OLED battery savings
- Tech/cyberpunk aesthetic

---

## 🧪 Testing Checklist

**Onboarding:**

- [ ] AI model downloads successfully
- [ ] Can add emergency contacts
- [ ] Contacts saved and persist after app restart
- [ ] Location permission requested
- [ ] SMS/Call permissions requested

**Emergency Flow:**

- [ ] SOS button triggers alarm
- [ ] Location fetched (if permission granted)
- [ ] SMS sent to ALL contacts
- [ ] Message includes location (if available)
- [ ] Message includes Google Maps link
- [ ] Protocol questions appear
- [ ] AI makes decisions based on responses
- [ ] Can cancel alarm
- [ ] Cancellation SMS sent

**Edge Cases:**

- [ ] Works without location permission
- [ ] Works with only coarse location permission
- [ ] Works indoors (poor GPS)
- [ ] Works with airplane mode off/on
- [ ] Works with mobile data only
- [ ] Handles SMS send failures gracefully

---

## 🚀 User Journey Summary

```
1. Download App
   ↓
2. Complete Onboarding
   - Install AI model
   - Add emergency contacts
   - Grant permissions
   ↓
3. Use App Normally
   ↓
4. EMERGENCY! Press SOS
   ↓
5. App Fetches Location (2 seconds)
   ↓
6. SMS Sent to ALL Contacts
   - With location (if available)
   - With Google Maps link
   - With timestamp
   ↓
7. Answer Protocol Questions
   - AI assesses threat level
   ↓
8. AI Takes Additional Actions
   - Send follow-up messages
   - Make calls if needed
   - Call emergency services if critical
   ↓
9. Monitor Until Safe
   - Escalates if no response
   - Continuous monitoring
   ↓
10. Cancel When Safe
    - Sends "False alarm" to contacts
```

---

## ✅ Key Takeaways

1. **Onboarding is mandatory** - User must add contacts
2. **Location is optional** - App works without it
3. **Emergency alerts are immediate** - Sent before questions
4. **All contacts are real** - No sample/dummy data
5. **AI enhances safety** - Makes smart decisions
6. **Privacy-focused** - Everything on-device
7. **Graceful degradation** - Works with limited permissions

**The app prioritizes getting help to the victim ASAP, even if some features aren't available!** 🚨
