# Personal Safety App - Implementation Guide

## 🎯 Project Overview

**App Name**: Guardian AI (Personal Safety App)

**Purpose**: AI-powered personal safety application that provides discreet emergency assistance
through intelligent threat assessment and automated contact alerting.

**Key Innovation**: Uses **on-device AI** (RunAnywhere SDK) for complete privacy - no data leaves
the device.

---

## ✅ What Has Been Implemented

### 1. Core Data Models (`SafetyModels.kt`) ✓

- **EmergencyContact**: Store trusted contacts with priority levels
- **ThreatLevel**: 5-level threat assessment (UNKNOWN → LOW → MEDIUM → HIGH → CRITICAL)
- **ProtocolQuestion**: AI-generated questions to assess victim's ability to respond
- **EmergencySession**: Track active emergency with responses and alerts
- **AlertRecord**: History of all communications sent
- **EmergencyAction**: Sealed class for different action types (SMS, Call, Emergency Services)

### 2. AI Engine (`SafetyAIEngine.kt`) ✓

Uses RunAnywhere SDK for:

- **Protocol Question Generation**: AI creates contextual safety questions
- **Threat Assessment**: Analyzes response time and ability to answer
- **Decision Making**: AI recommends actions based on threat level
- **Message Generation**: Creates appropriate emergency messages
- **Escalation Logic**: Automatically escalates threat over time

**Privacy Note**: All AI processing happens ON-DEVICE using llama.cpp

### 3. Main Business Logic (`SafetyViewModel.kt`) ✓

Complete emergency workflow:

- **Alarm Triggering**: Starts emergency session
- **Protocol Questions**: Present and time safety questions
- **Response Handling**: Process YES/NO answers with timing
- **AI Decision Making**: Determine who to contact and how
- **Action Execution**: Send SMS, make calls, alert emergency services
- **Escalation Monitoring**: Auto-escalate if no resolution
- **False Alarm Cancellation**: Send all-clear messages

---

## 🚧 What Needs to be Implemented

### Phase 1: Core Integration (MUST DO)

#### 1. Update AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Existing permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />
    
    <!-- NEW: Communication permissions -->
    <uses-permission android:name="android.permission.SEND_SMS" />
    <uses-permission android:name="android.permission.CALL_PHONE" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    
    <!-- NEW: Location permissions -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
    
    <!-- NEW: Foreground service for emergency monitoring -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
    
    <!-- NEW: Contacts permission (optional) -->
    <uses-permission android:name="android.permission.READ_CONTACTS" />

    <application
        android:name=".MyApplication"
        android:largeHeap="true"
        ...>
        
        <activity android:name=".MainActivity" ... />
        
        <!-- NEW: Emergency Service -->
        <service
            android:name=".EmergencyService"
            android:foregroundServiceType="location"
            android:exported="false" />
            
    </application>
</manifest>
```

#### 2. Implement Communication Layer

Create `CommunicationManager.kt`:

```kotlin
class CommunicationManager(private val context: Context) {
    
    // Send SMS
    fun sendSMS(phoneNumber: String, message: String): Boolean {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.e("CommsManager", "SMS permission not granted")
                return false
            }
            
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.i("CommsManager", "SMS sent to $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e("CommsManager", "SMS failed", e)
            false
        }
    }
    
    // Make phone call
    fun makeCall(phoneNumber: String): Boolean {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("CommsManager", "Call failed", e)
            false
        }
    }
    
    // Make missed call (call and hang up quickly)
    fun makeMissedCall(phoneNumber: String): Boolean {
        // Implementation: Place call, wait 1 second, hang up
        // Requires call state monitoring
        return makeCall(phoneNumber)
    }
    
    // Call emergency services
    fun callEmergencyServices(emergencyNumber: String = "911"): Boolean {
        return makeCall(emergencyNumber)
    }
}
```

#### 3. Implement Location Services

Create `LocationManager.kt`:

```kotlin
class SafetyLocationManager(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private var locationCallback: ((Location) -> Unit)? = null
    
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(callback: (Location) -> Unit) {
        if (!hasLocationPermission()) {
            Log.e("LocationManager", "Location permission not granted")
            return
        }
        
        this.locationCallback = callback
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // 10 seconds
        ).setMinUpdateIntervalMillis(5000L)
         .build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    callback(location)
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
    
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            callback(location)
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
```

#### 4. Build Safety UI

Create `SafetyActivity.kt` (Main UI):

```kotlin
@Composable
fun SafetyScreen(viewModel: SafetyViewModel = viewModel()) {
    val isAlarmActive by viewModel.isAlarmActive.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val timeRemaining by viewModel.questionTimeRemaining.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guardian AI") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isAlarmActive) 
                        Color.Red else MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Banner
            StatusBanner(statusMessage, currentSession?.currentThreatLevel)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (!isAlarmActive) {
                // Main Alarm Button (Discreet)
                EmergencyAlarmButton(
                    onClick = { viewModel.triggerEmergencyAlarm() }
                )
            } else {
                // Emergency Active View
                EmergencyActiveView(
                    question = currentQuestion,
                    timeRemaining = timeRemaining,
                    onYes = { viewModel.answerProtocolQuestionYes() },
                    onNo = { viewModel.answerProtocolQuestionNo() },
                    onCancel = { viewModel.cancelEmergencyAlarm() }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Quick Access Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { /* Navigate to Contacts */ }) {
                    Text("Contacts")
                }
                Button(onClick = { /* Navigate to Settings */ }) {
                    Text("Settings")
                }
                Button(onClick = { /* View Alert History */ }) {
                    Text("History")
                }
            }
        }
    }
}

@Composable
fun EmergencyAlarmButton(onClick: () -> Unit) {
    // Design this to be discreet - maybe looks like calculator app?
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(200.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Emergency",
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Emergency",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun EmergencyActiveView(
    question: ProtocolQuestion?,
    timeRemaining: Int?,
    onYes: () -> Unit,
    onNo: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Emergency indicator
        Text(
            text = "🚨 EMERGENCY ACTIVE",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Red,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        question?.let {
            // Protocol Question
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Safety Check",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = it.question,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Timer
                    timeRemaining?.let { time ->
                        Text(
                            text = "Time remaining: $time seconds",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (time < 10) Color.Red else Color.Gray
                        )
                        
                        LinearProgressIndicator(
                            progress = { time.toFloat() / it.timeoutSeconds },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // YES / NO Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = onYes,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("YES", fontSize = 20.sp)
                        }
                        
                        Button(
                            onClick = onNo,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            )
                        ) {
                            Text("NO", fontSize = 20.sp)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Cancel button (for false alarms)
        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Gray
            )
        ) {
            Text("False Alarm - Cancel")
        }
    }
}

@Composable
fun StatusBanner(message: String, threatLevel: ThreatLevel?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (threatLevel) {
                ThreatLevel.CRITICAL -> Color(0xFFD32F2F)
                ThreatLevel.HIGH -> Color(0xFFF57C00)
                ThreatLevel.MEDIUM -> Color(0xFFFBC02D)
                ThreatLevel.LOW -> Color(0xFF388E3C)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
```

#### 5. Contacts Management Screen

Create UI for adding/managing emergency contacts with priority levels.

#### 6. Settings Screen

- Model download/loading
- Test emergency alerts
- Configure threat escalation timing
- Privacy settings

---

### Phase 2: Advanced Features (NICE TO HAVE)

#### 1. Foreground Service

Keep emergency monitoring active even when app is in background.

#### 2. Disguised App Mode

Make app look like calculator/other app to hide from attacker.

#### 3. Shake-to-Activate

Alternative discreet trigger (shake phone pattern).

#### 4. Voice Command

"Hey Guardian, emergency" trigger.

#### 5. Geofencing

Auto-trigger if entering/leaving certain areas.

#### 6. Silent Mode

Vibration-only alerts, no sound when phone is on silent.

#### 7. Recording Evidence

Auto-record audio/photo when alarm triggered (with privacy consent).

#### 8. Check-in System

Regular safety check-ins; auto-alert if missed.

---

## 🔧 Integration Steps

### Step 1: Update Dependencies

Add to `app/build.gradle.kts`:

```kotlin
dependencies {
    // Existing SDK dependencies...
    
    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
}
```

### Step 2: Request Runtime Permissions

Create permission request flow in your UI:

```kotlin
@Composable
fun RequestPermissions(onPermissionsGranted: () -> Unit) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    if (permissionsState.allPermissionsGranted) {
        onPermissionsGranted()
    } else {
        // Show permission rationale UI
    }
}
```

### Step 3: Update SafetyViewModel

Replace stub communication methods with real implementations:

```kotlin
// In SafetyViewModel.kt
private val communicationManager = CommunicationManager(context)
private val locationManager = SafetyLocationManager(context)

private fun sendSMS(contact: EmergencyContact, message: String): Boolean {
    return communicationManager.sendSMS(contact.phoneNumber, message)
}

private fun makeCall(contact: EmergencyContact): Boolean {
    return communicationManager.makeCall(contact.phoneNumber)
}

private fun startLocationMonitoring() {
    locationManager.startLocationUpdates { location ->
        updateLocation(location)
    }
}
```

### Step 4: Update MyApplication

Ensure AI model is loaded at startup:

```kotlin
private suspend fun registerModels() {
    // Use smaller model for faster response times in emergencies
    addModelFromURL(
        url = "https://huggingface.co/prithivMLmods/SmolLM2-360M-GGUF/resolve/main/SmolLM2-360M.Q8_0.gguf",
        name = "SmolLM2 360M Q8_0",
        type = "LLM"
    )
    
    // Auto-download and load model
    try {
        val modelId = "SmolLM2 360M Q8_0"
        RunAnywhere.downloadModel(modelId).collect { progress ->
            Log.d("MyApp", "Model download: ${(progress * 100).toInt()}%")
        }
        RunAnywhere.loadModel(modelId)
        Log.i("MyApp", "Safety model ready")
    } catch (e: Exception) {
        Log.e("MyApp", "Model setup failed", e)
    }
}
```

---

## 📱 User Flow

### Normal Usage:

1. **Setup** → Add emergency contacts with priorities
2. **Configure** → Download AI model, grant permissions
3. **Ready** → App runs in background, ready for emergency

### Emergency Scenario:

```
User triggers alarm (button/shake/voice)
         ↓
App activates emergency session
         ↓
Location tracking starts
         ↓
AI generates protocol question
         ↓
30-second timer starts
         ↓
┌────────────────────────────┐
│   User answers question?   │
├────────────────┬───────────┤
│      YES       │    NO      │
↓                ↓
Quick response   Slow/No response
Threat: MEDIUM   Threat: HIGH
         ↓                ↓
AI decides who to contact
         ↓
Execute actions:
- Send SMS to family
- Make missed calls
- Send location
- Call emergency services (if HIGH/CRITICAL)
         ↓
Monitor for escalation
(auto-escalate if unresolved)
         ↓
User cancels or emergency resolved
```

---

## 🔐 Privacy & Security

### Privacy Guarantees:

1. ✅ **All AI processing on-device** (RunAnywhere SDK)
2. ✅ **No data sent to cloud** (except emergency SMS/calls)
3. ✅ **Contacts stored locally only**
4. ✅ **Location only shared when emergency active**
5. ✅ **Emergency session history encrypted**

### Security Considerations:

- Require PIN/biometric to cancel alarm (prevent attacker cancellation)
- Option to hide app from launcher
- Disguised app mode
- Wipe data after multiple failed PIN attempts

---

## 🧪 Testing Plan

### Unit Tests:

- AI decision logic with different threat levels
- Escalation timing
- Contact prioritization

### Integration Tests:

- Full emergency workflow
- Permission handling
- Communication layer

### Manual Testing:

1. **Low Threat**: Quick YES response → SMS to family
2. **Medium Threat**: Slow YES response → Missed calls to multiple contacts
3. **High Threat**: NO response → Calls + location SMS
4. **Critical Threat**: No response + 5 minutes → Emergency services
5. **False Alarm**: Cancel and verify all-clear messages sent

---

## 📊 Success Metrics

- Emergency response time < 5 seconds
- AI model load time < 10 seconds
- Location accuracy < 50 meters
- SMS delivery success > 95%
- Battery drain < 2% per hour (background monitoring)

---

## 🚀 Next Steps

1. **Implement communication layer** (SMS, calls)
2. **Implement location services**
3. **Build SafetyActivity UI**
4. **Add contacts management**
5. **Request and handle permissions**
6. **Test emergency workflow end-to-end**
7. **Add foreground service for background monitoring**
8. **Implement advanced features** (disguise mode, shake-to-activate)

---

## 📚 Additional Resources

- [RunAnywhere SDK Guide](./RUNANYWHERE_SDK_COMPLETE_GUIDE.md)
- [Android SMS Manager](https://developer.android.com/reference/android/telephony/SmsManager)
- [Fused Location Provider](https://developers.google.com/location-context/fused-location-provider)
- [Foreground Services](https://developer.android.com/guide/components/foreground-services)

---

## 💡 UX Considerations

### Discreet Activation:

- Main button could be disguised (calculator app icon?)
- Volume button pattern (press volume down 3x quickly)
- Shake pattern detection
- Voice command with wake word

### Accessibility:

- Large touch targets for emergency buttons
- High contrast mode
- Voice feedback option
- Works with TalkBack

### International Support:

- Localized emergency numbers (911, 112, 999, etc.)
- Multi-language UI
- AI responses in user's language

---

**Built with privacy, safety, and speed in mind using on-device AI.**
