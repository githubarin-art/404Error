# Guardian AI - Personal Safety App

**An AI-Powered Personal Safety Application Using RunAnywhere SDK**

> **Discreet emergency assistance through intelligent threat assessment, automated contact alerting,
and Google Maps integration - completely on-device for maximum privacy.**

---

## **What This App Does**

Guardian AI is a personal safety application that uses **on-device AI** to:

1. **Assess Emergency Threats**: AI generates protocol questions to determine severity
2. **Smart Contact Alerting**: Automatically decides who to contact based on threat level
3. **Progressive Escalation**: Monitors situations and escalates if needed
4. **Complete Privacy**: All AI processing happens locally using RunAnywhere SDK
5. **Location Navigation**: Integrated Google Maps for safe place navigation

### How It Works

```
User triggers alarm (discreet button)
         ↓
AI generates safety question ("Can you safely move to a public area?")
         ↓
30-second timer for response
         ↓
┌────────────────────────────┐
│   Can user answer quickly? │
├────────────────┬───────────┤
│      YES       │    NO      │
↓                ↓
MEDIUM threat    HIGH threat
SMS to family    Calls + location + emergency services
         ↓
Navigate to nearest safe places via Google Maps
```

## **Key Features**

### **AI-Powered Decision Making**

- **Protocol Questions**: AI generates contextual safety questions
- **Threat Assessment**: Analyzes response time and ability to answer
- **Smart Escalation**: Automatically escalates based on time and responses
- **Action Selection**: AI decides who to contact and how (SMS, call, emergency services)

### **Privacy-First Architecture**

- **100% On-Device AI**: Uses RunAnywhere SDK with llama.cpp
- **No Cloud Processing**: All decisions made locally
- **No Data Collection**: Emergency contacts stored locally only
- **Location Privacy**: GPS only shared when emergency active

### **Emergency Response System**

- **5 Threat Levels**: UNKNOWN → LOW → MEDIUM → HIGH → CRITICAL
- **Automated Alerts**: SMS, calls, missed calls, emergency services
- **Location Sharing**: Automatic GPS coordinates in emergency messages
- **False Alarm Protection**: Easy cancellation with notifications

### **Advanced UI & Navigation**

- **Google Maps Integration**: Real-time safe place navigation
- **Emergency Screen**: Comprehensive emergency response interface
- **Stealth Mode**: Auto-camouflage after 30 seconds of inactivity
- **Discreet Activation**: Single button trigger with camouflage mode
- **Real-Time Countdown**: Visual timer for protocol questions
- **Status Monitoring**: Color-coded threat level indicators
- **Contact Management**: Priority-based emergency contacts

---

## **Project Structure**

```
app/src/main/java/com/runanywhere/startup_hackathon20/
│
├── MyApplication.kt              # SDK initialization, model registration
│
├── SafetyModels.kt               # Data models (contacts, threats, sessions)
├── SafetyAIEngine.kt             # AI decision engine (RunAnywhere SDK)
├── SafetyViewModel.kt            # Complete business logic
│
├── MainActivity.kt               # Safety UI implementation
│
├── data/
│   └── SafePlace.kt              # Safe location data model
│
├── ui/
│   ├── EscapeToSafetyScreen.kt   # Google Maps navigation screen
│   ├── SafePlaceCard.kt          # Safe place display component
│   └── screens/
│       └── EmergencyScreen.kt    # Main emergency response interface
│
├── [TO ADD] CommunicationManager.kt  # SMS, calls, emergency dialer
├── [TO ADD] LocationManager.kt       # GPS tracking, location services
├── [TO ADD] ContactsScreen.kt        # Manage emergency contacts
└── [TO ADD] SettingsScreen.kt        # Model management, permissions
```

### **What's Already Implemented**

1. **SafetyModels.kt** (103 lines)
    - `EmergencyContact`, `ThreatLevel`, `ProtocolQuestion`
    - `EmergencySession`, `AlertRecord`, `EmergencyAction`

2. **SafetyAIEngine.kt** (389 lines)
    - Protocol question generation
    - Threat level assessment
    - AI decision making with RunAnywhere SDK
    - Emergency message generation
    - Escalation logic

3. **SafetyViewModel.kt** (599 lines)
    - Complete emergency workflow
    - Protocol question presentation
    - Response handling (YES/NO/timeout)
    - Action execution orchestration
    - Escalation monitoring
    - False alarm cancellation

4. **EmergencyScreen.kt** (3041 lines)
    - Complete emergency response UI
    - Auto-camouflage after 30s inactivity
    - 404 error camouflage mode
    - Threat assessment screens
    - Safe place navigation integration

5. **EscapeToSafetyScreen.kt** (231 lines)
    - Google Maps integration
    - Safe place markers and navigation
    - Real-time location updates
    - Emergency status display

6. **SafePlaceCard.kt** (71 lines)
    - Safe place display component
    - Navigation integration
    - Type-based icons and styling

7. **data/SafePlace.kt** (9 lines)
    - Safe location data model
    - Place ID, name, types, coordinates
    - Distance and open hours info

### **What Needs Implementation**

See **[BUILD_SAFETY_APP.md](BUILD_SAFETY_APP.md)** for step-by-step guide.

Priority 1 (Week 1):

- Communication layer (SMS, calls)
- Location services integration
- Runtime permissions

Priority 2 (Week 2):

- Contacts management screen
- Settings screen
- Alert history

Priority 3 (Week 3+):

- Foreground service
- Advanced features (shake-to-activate, disguise mode, voice commands)

---

## **Quick Start**

### Prerequisites

- Android Studio (latest stable)
- JDK 17+
- Android device/emulator (API 24+, 2GB+ RAM)
- Google Maps API key (for navigation features)

### Step 1: Clone & Open

```bash
git clone https://github.com/githubarin-art/404Error.git
cd Hackss
# Open in Android Studio
```

### Step 2: Add Permissions

Add to `app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

### Step 3: Add Dependencies

Add to `app/build.gradle.kts`:

```kotlin
dependencies {
    // ... existing dependencies ...
    
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    
    // Location services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
}
```

### Step 4: Add Google Maps API Key

Add to `app/src/main/AndroidManifest.xml`:

```xml
<application>
    <meta-data
        android:name="com.google.android.geo.API_KEY"
        android:value="YOUR_API_KEY_HERE"/>
</application>
```

### Step 5: Build & Run

1. **Load AI model** (tap "Load AI Model" button)
2. **Trigger emergency** (tap red 404 button)
3. **Answer protocol question** within 30 seconds
4. **Navigate to safe places** using integrated Google Maps
5. **Check LogCat** for AI decisions and navigation updates

**Expected Logs:**

```
SafetyViewModel: Emergency session started
SafetyAIEngine: AI generated question: "Can you confirm you are safe right now?"
SafetyViewModel: Threat level updated: MEDIUM
SafetyViewModel: SMS to Mom: I've activated my safety app...
EscapeToSafetyScreen: Navigating to Police Station - 500m away
```

---

## **Documentation**

| Document                                                                     | Description                                 |
|------------------------------------------------------------------------------|---------------------------------------------|
| **[BUILD_SAFETY_APP.md](BUILD_SAFETY_APP.md)**                               | Quick start guide with ready-to-use UI code |
| **[SAFETY_APP_IMPLEMENTATION_GUIDE.md](SAFETY_APP_IMPLEMENTATION_GUIDE.md)** | Complete implementation guide (750+ lines)  |
| **[RUNANYWHERE_SDK_COMPLETE_GUIDE.md](RUNANYWHERE_SDK_COMPLETE_GUIDE.md)**   | Full RunAnywhere SDK documentation          |
| **[EMERGENCY_FLOW_FIX_SUMMARY.md](EMERGENCY_FLOW_FIX_SUMMARY.md)**           | Latest fixes and implementation details     |

---

## **Testing Scenarios**

### Scenario 1: Low Threat (Quick Response)
```
Trigger alarm → Question appears → Answer YES within 5 seconds
Expected: MEDIUM threat → SMS to primary family contact
```

### Scenario 2: Medium Threat (Delayed Response)

```
Trigger alarm → Question appears → Answer YES after 15 seconds
Expected: MEDIUM threat → Missed calls to top 3 contacts
```

### Scenario 3: High Threat (No Answer)

```
Trigger alarm → Question appears → Answer NO or timeout
Expected: HIGH threat → Calls + location SMS to top contacts
```

### Scenario 4: Critical Threat (Escalation)

```
Trigger alarm → No response → 5 minutes pass
Expected: CRITICAL threat → Emergency services + all contacts
```

### Scenario 5: False Alarm

```
Trigger alarm → Cancel button → Confirm
Expected: All contacted people receive "False alarm. I'm safe now."
```

### Scenario 6: Safe Place Navigation

```
Trigger alarm → Navigate to safe places → Select police station
Expected: Google Maps opens with navigation to selected safe place
```

### Scenario 7: Auto-Camouflage

```
Trigger alarm → Don't interact for 30 seconds
Expected: UI changes to 404 error screen (camouflage mode)
Triple tap screen → UI restored to emergency interface
```

---

## **Privacy & Security**

### On-Device AI Guarantees

All AI decisions processed locally (RunAnywhere SDK with llama.cpp)  
No cloud API calls for threat assessment or decision making  
Emergency contacts stored locally (SharedPreferences/Room)  
Location only shared during active emergency  
No training data collection - AI model is frozen

### Security Features

- **Auto-Camouflage**: UI disguises as 404 error after 30s inactivity
- **Triple Tap Restore**: Secret gesture to restore emergency UI
- **PIN/biometric protection**: Planned for alarm cancellation
- **Disguised app mode**: Looks like calculator when not in emergency
- **Encrypted emergency session history**: Planned

---

## **Technical Stack**

### Core Technologies

- **Kotlin** - Modern Android development
- **Jetpack Compose** - Declarative UI with Material 3
- **Coroutines + Flow** - Async operations and reactive state
- **ViewModel + StateFlow** - MVVM architecture
- **Google Maps Compose** - Interactive maps and navigation

### RunAnywhere SDK

- **Core SDK** (4.0 MB) - Model management, analytics, event system
- **LlamaCpp Module** (2.1 MB) - 7 ARM64 optimized inference variants
- **Models**: Qwen 2.5 0.5B (374 MB) for emergency response

### Android APIs Integrated

- **Google Maps SDK** - Interactive maps with markers and navigation
- **FusedLocationProvider** - GPS tracking and location services
- **SmsManager** - Emergency text messages (planned)
- **TelephonyManager** - Phone calls (planned)
- **NotificationManager** - Foreground service (planned)

---

## **AI Decision Logic**

The AI engine uses on-device LLM to make intelligent decisions:

### Example AI Prompt (Protocol Question)

```
You are a personal safety AI assistant...

Context: A person has triggered an emergency alarm.

Generate ONE simple yes/no question that can quickly assess if they 
are in immediate danger. The question should be something they can 
answer quickly if they're safe, but cannot answer if actively threatened.

Question:
```

### Example AI Response

```
"Can you safely move to a public area right now?"
```

### Threat Assessment Logic

```kotlin
when {
    !victimResponded -> ThreatLevel.HIGH
    responseTimeSeconds > 30 -> ThreatLevel.HIGH
    responseTimeSeconds < 5 -> ThreatLevel.MEDIUM
    else -> ThreatLevel.MEDIUM
}
```

### Action Selection (AI-Driven)

```
LOW threat:
  → SMS to primary family contact

MEDIUM threat:
  → Missed calls to top 3 contacts
  
HIGH threat:
  → Calls + location SMS to top 2 contacts
  
CRITICAL threat:
  → Emergency services + all priority contacts
```

---

## **Use Cases**

### Primary Use Cases

- **Personal Safety**: Walking alone at night with Google Maps navigation
- **Domestic Violence**: Discreet emergency alert with camouflage mode
- **Medical Emergency**: Quick family notification with location sharing
- **Child Safety**: Students with safety concerns and school navigation
- **Elderly Care**: Fall detection + alert with nearby medical facilities
- **Travel Safety**: Solo travelers with hotel/restaurant navigation

### Advanced Use Cases (Planned)

- **Check-in System**: Auto-alert if check-in missed
- **Geofencing**: Alert if entering/leaving safe zones
- **Scheduled Monitoring**: AI checks in during vulnerable times
- **Group Safety**: Multiple users with shared contacts

---

## **Roadmap**

### ✅ Phase 1: MVP (COMPLETED)

- [x] AI decision engine
- [x] Business logic and workflow
- [x] Safety UI implementation with emergency screens
- [x] Google Maps integration for safe place navigation
- [x] Auto-camouflage and stealth mode
- [x] All linter errors fixed and clean compilation

### Priority 1 (Next Week):

- [ ] Communication layer (SMS, calls)
- [ ] Location services integration
- [ ] Runtime permissions

### Priority 2 (Week 2):

- [ ] Contacts management screen
- [ ] Settings screen
- [ ] Alert history

### Priority 3 (Week 3+):

- [ ] Foreground service
- [ ] Advanced features (shake-to-activate, disguise mode, voice commands)

---

## **Contributing**

This is a safety-critical application. Contributions should be:

1. **Well-tested** - Emergency features must be reliable
2. **Privacy-focused** - Never compromise on-device processing
3. **Accessible** - Consider users in high-stress situations
4. **Documented** - Clear code and decision reasoning

---

## **License**

This project uses the RunAnywhere SDK which follows its own license terms.  
See [RunAnywhere SDK Repository](https://github.com/RunanywhereAI/runanywhere-sdks) for details.

---

## **Support**

- **Implementation Questions**: See [BUILD_SAFETY_APP.md](BUILD_SAFETY_APP.md)
- **SDK Issues**: Check [RUNANYWHERE_SDK_COMPLETE_GUIDE.md](RUNANYWHERE_SDK_COMPLETE_GUIDE.md)
- **Architecture Questions**:
  Review [SAFETY_APP_IMPLEMENTATION_GUIDE.md](SAFETY_APP_IMPLEMENTATION_GUIDE.md)
- **Latest Fixes**: See [EMERGENCY_FLOW_FIX_SUMMARY.md](EMERGENCY_FLOW_FIX_SUMMARY.md)

---

## **Disclaimer**

This app is designed to assist in emergency situations but should not be relied upon as the sole
means of protection. Always call emergency services (911, 112, etc.) directly if in immediate
danger.

---

## **Acknowledgments**

- **RunAnywhere SDK** - On-device AI inference
- **Google Maps Platform** - Interactive maps and navigation
- **llama.cpp** - Optimized LLM inference engine
- **HuggingFace** - Model hosting (Qwen 2.5)
- **Android Open Source Project** - Platform and APIs

---

## **Latest Updates (v2.0)**

### ✅ **Complete Emergency UI Implementation**

- Full emergency response screens with protocol questions
- Real-time countdown timers and threat level indicators
- Auto-camouflage after 30 seconds of inactivity
- 404 error disguise mode with triple-tap restore

### ✅ **Google Maps Integration**

- Interactive maps with safe place markers
- Real-time navigation to police stations, hospitals, etc.
- Location-based emergency response
- Color-coded markers by place type

### ✅ **Clean Code & Compilation**

- All linter errors fixed
- Updated to latest Material 3 APIs
- Proper imports and dependencies
- Clean Gradle build without errors

---

**Built with privacy, safety, and speed in mind using on-device AI and Google Maps.**

**Stay Safe. Stay Protected. Stay Private.**
