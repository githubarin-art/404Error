# ✅ SMS & Calls NOW ENABLED!

## 🎉 Real Messaging is Active

Your Guardian AI app will now **send actual SMS messages and make real phone calls** to the
emergency contacts you add during onboarding!

---

## 📱 How It Works

### **During Onboarding:**

1. You add emergency contacts (Name, Phone, Relationship)
2. Contacts are saved to `SafetyViewModel`
3. They're ready to receive emergency alerts

### **When Emergency Triggers:**

```
User clicks SOS
    ↓
AI asks protocol question
    ↓
User answers YES/NO (or timeout)
    ↓
AI assesses threat level
    ↓
AI decides who to contact and how
    ↓
✅ SENDS REAL SMS & MAKES REAL CALLS
```

---

## 📨 What Gets Sent

### **SMS Messages:**

Format:

```
🚨 EMERGENCY ALERT from [Your Name]

I need help! [Threat-specific message]

My location: https://maps.google.com/?q=37.4220,-122.0841

This is an automated alert from Guardian AI.
```

**Features:**

- ✅ Includes Google Maps link to your exact location
- ✅ Automatically splits long messages
- ✅ Sends to all contacts based on threat level
- ✅ Includes timestamp in logs

### **Phone Calls:**

- ✅ Direct calls to emergency contacts
- ✅ Calls emergency services (911) for high threats
- ✅ Phone dialer opens automatically

### **Missed Calls (Stealth):**

- ✅ Calls contact and hangs up after 2 seconds
- ✅ Creates missed call notification
- ✅ Useful for stealth alerts

---

## 🎯 Threat Level → Actions

### **LOW Threat** (Quick YES response)

```
Actions:
- ✅ SMS to 1 primary contact
- ✅ Location shared in SMS
```

### **MEDIUM Threat** (Delayed response)

```
Actions:
- ✅ SMS to ALL family contacts
- ✅ Missed calls to family
- ✅ Location shared in SMS
```

### **HIGH Threat** (NO response or timeout)

```
Actions:
- ✅ SMS to ALL contacts
- ✅ Direct phone calls to family
- ✅ Calls to emergency services (911)
- ✅ Location shared in SMS
- ✅ Audio recording starts
```

### **CRITICAL Threat** (Escalated)

```
Actions:
- ✅ Everything from HIGH
- ✅ Multiple calls to all contacts
- ✅ Continuous location updates
- ✅ Persistent alerts
```

---

## 🔐 Permissions Required

The app requests these permissions during onboarding:

**Already in Manifest:**

- ✅ `SEND_SMS` - Send text messages
- ✅ `CALL_PHONE` - Make phone calls
- ✅ `ANSWER_PHONE_CALLS` - End calls (for missed calls)
- ✅ `ACCESS_FINE_LOCATION` - Get GPS coordinates
- ✅ `RECORD_AUDIO` - Record evidence

**User Must Grant:**
During onboarding, user sees permission dialogs and can:

- ✅ Grant all permissions
- ⚠️ Skip (but SMS/calls won't work without permissions)

---

## 💡 Example Scenario

### **Scenario: User in Danger**

**Step 1:** User triple-taps SOS button

```
✅ Emergency alarm activated
```

**Step 2:** AI asks: "Can you speak freely right now?"

```
User taps: NO
```

**Step 3:** AI assesses HIGH threat

```
AI Decision: HIGH threat detected
Threat Score: 8/10
Actions: 5 recommended
```

**Step 4:** System executes actions

```
✅ SMS to Mom: "🚨 EMERGENCY ALERT..."
✅ SMS to Dad: "🚨 EMERGENCY ALERT..."  
✅ SMS to Friend: "🚨 EMERGENCY ALERT..."
✅ Calling Mom at +1234567890
✅ Calling emergency services: 911
✅ Location tracking started
✅ Audio recording started
```

**Step 5:** Contacts receive

```
Mom's Phone:
- SMS with location link received
- Phone starts ringing
- Can see exact location on Google Maps

Emergency Services:
- Call received
- Can hear audio from device
```

---

## 📋 SMS Message Examples

### **Quick Response (LOW Threat):**

```
🚨 EMERGENCY ALERT

I need assistance. I'm safe but need help.

My location: https://maps.google.com/?q=37.4220,-122.0841

This is an automated alert from Guardian AI.
```

### **Delayed Response (MEDIUM Threat):**

```
🚨 EMERGENCY ALERT

I'm in a concerning situation and may need help.

My location: https://maps.google.com/?q=37.4220,-122.0841

Please check on me. Automated alert.
```

### **No Response (HIGH Threat):**

```
🚨 EMERGENCY ALERT - URGENT

I CANNOT RESPOND. I AM IN DANGER.

My location: https://maps.google.com/?q=37.4220,-122.0841

PLEASE SEND HELP IMMEDIATELY.

Automated emergency alert.
```

---

## 🧪 Testing Safely

### ⚠️ **IMPORTANT: Testing Guidelines**

**DO NOT test with real emergencies!**

### **Safe Testing Options:**

#### **Option 1: Test Mode (Recommended)**

Use test phone numbers that you control:

```kotlin
// In onboarding, add YOUR OWN numbers:
Name: "Test Mom"
Phone: YOUR_PHONE_NUMBER
Relationship: "Family"
```

Then trigger emergency and you'll receive the SMS yourself.

#### **Option 2: Log-Only Mode**

Temporarily disable actual sending by commenting out:

```kotlin
// In SafetyViewModel.kt, line ~650
// smsManager.sendTextMessage(...) // Comment this out
Log.i(TAG, "Would send: $fullMessage") // See what would be sent
```

#### **Option 3: Use Emulator Numbers**

Android emulator can send SMS to other emulator instances.

### **What to Check:**

- [ ] SMS contains correct message
- [ ] SMS includes location link
- [ ] Location link opens Google Maps
- [ ] Phone calls connect
- [ ] Correct contacts are alerted based on threat
- [ ] Multiple contacts receive alerts
- [ ] Emergency services called for HIGH threats

---

## 🔍 LogCat Monitoring

Watch these logs to see what's happening:

```bash
# Filter in Android Studio LogCat:
SafetyViewModel

# You'll see:
SafetyViewModel: SMS to Mom: 🚨 EMERGENCY ALERT...
SafetyViewModel: ✅ SMS sent successfully to Mom
SafetyViewModel: Calling Dad at +1234567891
SafetyViewModel: ✅ Call initiated to Dad
SafetyViewModel: ⚠️ Calling emergency services: Police
SafetyViewModel: ✅ Emergency call initiated to 911
```

Success = ✅  
Failure = ❌

---

## ⚙️ How Contacts Flow Through System

### **1. Onboarding:**

```kotlin
User adds contact in OnboardingScreen
    ↓
Contact stored in local list
    ↓
On "Finish Setup":
emergencyContacts.forEach { contact ->
    viewModel.addEmergencyContact(contact)
}
```

### **2. Storage:**

```kotlin
SafetyViewModel._emergencyContacts 
    ↓
StateFlow<List<EmergencyContact>>
    ↓
Available throughout app
```

### **3. Emergency Use:**

```kotlin
AI decides actions
    ↓
EmergencyAction.SendSMS(contact, message)
    ↓
sendSMS(contact, message)
    ↓
✅ Android SMS Manager sends actual SMS
```

---

## 🛡️ Privacy & Security

### **Data Never Leaves Device (Except Emergency SMS)**

- ✅ AI runs 100% on-device
- ✅ No cloud servers
- ✅ No data collection
- ✅ SMS only sent when YOU trigger SOS
- ✅ Contacts stored locally
- ✅ Full control over when to send

### **User Control**

- ✅ "Cancel Emergency" button always available
- ✅ Can modify contacts anytime
- ✅ Can disable permissions anytime
- ✅ False alarm notification sent automatically

---

## 📞 Emergency Services Note

**Current Implementation:**

- Calls `911` (US emergency number)
- Only called for HIGH/CRITICAL threats
- Requires user to speak when operator answers

**Future Enhancement:**

- Location-aware emergency numbers (112 in EU, 999 in UK, etc.)
- Pre-recorded emergency message
- Silent emergency alert systems

---

## ✅ Verification Checklist

Before deploying to real users:

- [ ] Test with your own phone numbers
- [ ] Verify SMS messages are received
- [ ] Check location links work
- [ ] Test phone calls connect
- [ ] Verify permissions are granted
- [ ] Test "Cancel Emergency" sends cancellation SMS
- [ ] Check different threat levels trigger correct actions
- [ ] Verify no false positives
- [ ] Test with airplane mode (offline mode)
- [ ] Document emergency contact requirements

---

## 🚨 Production Considerations

### **Before Going Live:**

1. **Legal Compliance**
    - Check local laws about automated emergency calls
    - Emergency services may have regulations
    - User consent required

2. **User Training**
    - Educate users on when to trigger SOS
    - Explain what happens when triggered
    - Demonstrate cancel/false alarm feature

3. **Contact Verification**
    - Verify contact phone numbers are correct
    - Ensure contacts know they're emergency contacts
    - Test with contacts before real emergencies

4. **Liability**
    - Add disclaimers
    - Not a replacement for 911
    - Technical failures may occur

---

## 🎯 Summary

### ✅ **What's Enabled:**

- Real SMS sending with location
- Real phone calls to contacts
- Real emergency services calls (911)
- Missed calls for stealth alerts
- Multi-contact alerts
- Location sharing via Google Maps links

### 🔧 **How to Use:**

1. Add contacts during onboarding
2. Grant SMS and Call permissions
3. Contacts receive real alerts when SOS triggered
4. Location automatically included in SMS

### 🧪 **Testing:**

- Use your own phone numbers first
- Check LogCat for confirmation
- Verify SMS content and links
- Test cancel feature

---

**Your Guardian AI app is now fully functional and ready to send real emergency alerts!** 🛡️✅

**Remember:** Always test with caution and never trigger false emergency alerts to services like
911.
