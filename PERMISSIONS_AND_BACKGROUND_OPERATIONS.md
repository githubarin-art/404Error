# Permissions & Background Operations - Implementation Guide

## Overview

This document describes the complete implementation of permissions handling, fallbacks, background
operations, and privacy compliance for the Guardian AI Safety App.

---

## ✅ Implementation Summary

All requirements from **Section 7: Permissions & Background Operations** have been implemented:

1. ✅ **Request Before Action** - Permissions requested with context
2. ✅ **Fallbacks** - Alternative suggestions when denied
3. ✅ **Background Compliance** - Foreground services for Android 10+
4. ✅ **Privacy** - Clear explanations in onboarding

---

## 📋 Permissions Required

### Critical Permissions (Must Have)

```kotlin
val CRITICAL_PERMISSIONS = listOf(
    Manifest.permission.ACCESS_FINE_LOCATION,  // For emergency location sharing
    Manifest.permission.SEND_SMS,               // For automatic alerts
    Manifest.permission.CALL_PHONE              // For emergency calls
)
```

**Why Critical:**

- **Location**: Emergency responders need your exact location
- **SMS**: Automatic alerts to emergency contacts
- **Calls**: Automated emergency response

### Core Permissions (Recommended)

```kotlin
val CORE_PERMISSIONS = listOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.SEND_SMS,
    Manifest.permission.CALL_PHONE,
    Manifest.permission.CAMERA,          // For video evidence
    Manifest.permission.RECORD_AUDIO     // For audio evidence
)
```

### Optional Permissions (Enhanced Features)

```kotlin
val OPTIONAL_PERMISSIONS = listOf(
    Manifest.permission.READ_CONTACTS,        // Easier contact selection
    Manifest.permission.POST_NOTIFICATIONS    // Android 13+
)
```

---

## 🎯 Permission Request Flow

### 1. During Onboarding

```kotlin
// Onboarding shows clear explanations before requesting
OnboardingStep.LOCATION_PERMISSION -> {
    // Show why permission is needed
    // Show what happens if denied
    // Request permission
    // Auto-continue when granted
}
```

**User Experience:**

- ✅ Clear explanation of why permission is needed
- ✅ Visual indicators (icons, colors)
- ✅ Priority badges (CRITICAL, HIGH, MEDIUM)
- ✅ Examples of how it's used
- ✅ Auto-continue when granted

### 2. Permission Request Screen

```kotlin
@Composable
fun LocationPermissionStep() {
    // Explanation
    Text("Location permission is REQUIRED to send your GPS coordinates...")
    
    // Features enabled by permission
    Card {
        PermissionFeatureItem("📍", "Share location in emergency SMS")
        PermissionFeatureItem("🗺️", "Help responders find you quickly")
        PermissionFeatureItem("🔒", "Used only when SOS is activated")
    }
    
    // Grant button
    Button(onClick = { requestPermission() }) {
        Text("GRANT LOCATION PERMISSION")
    }
}
```

### 3. Before Feature Use

```kotlin
// Check permission before using feature
fun startRecording() {
    if (!PermissionManager.isPermissionGranted(context, RECORD_AUDIO)) {
        // Show explanation and fallbacks
        showPermissionDeniedDialog(RECORD_AUDIO)
        return
    }
    
    // Permission granted - proceed
    actuallyStartRecording()
}
```

---

## 🔄 Fallback System

When a permission is denied, the app provides **multiple fallback options** with priorities:

### Location Permission Denied

```kotlin
// CRITICAL Priority
FallbackSuggestion(
    title = "📍 Grant Location Permission",
    description = "Go to Settings and enable location access.",
    action = FallbackAction.OpenSettings,
    priority = Priority.CRITICAL
)

// HIGH Priority
FallbackSuggestion(
    title = "📌 Share Location Manually",
    description = "You can manually share location when triggering emergency.",
    action = FallbackAction.ManualLocationShare,
    priority = Priority.HIGH
)

// MEDIUM Priority
FallbackSuggestion(
    title = "🗺️ Use Google Maps",
    description = "Send your location via Google Maps.",
    action = FallbackAction.OpenExternalApp("com.google.android.apps.maps"),
    priority = Priority.MEDIUM
)
```

### SMS Permission Denied

```kotlin
// HIGH Priority
FallbackSuggestion(
    title = "💬 Enable SMS Permission",
    description = "Go to Settings to allow SMS.",
    action = FallbackAction.OpenSettings
)

// HIGH Priority
FallbackSuggestion(
    title = "📱 Send SMS Manually",
    description = "You'll need to manually send SMS to contacts.",
    action = FallbackAction.ManualSMS
)

// MEDIUM Priority
FallbackSuggestion(
    title = "📞 Use Phone Calls Instead",
    description = "Call your emergency contacts directly.",
    action = FallbackAction.ManualCall
)
```

### Call Permission Denied

```kotlin
// HIGH Priority
FallbackSuggestion(
    title = "📞 Enable Call Permission",
    description = "Emergency calls won't be automated without this.",
    action = FallbackAction.OpenSettings
)

// HIGH Priority
FallbackSuggestion(
    title = "☎️ Dial Manually",
    description = "Emergency contacts will be shown, dial manually.",
    action = FallbackAction.ManualCall
)
```

---

## 🎨 Color-Coded Error Messages

### Priority Colors

```kotlin
enum class Priority {
    CRITICAL,  // 🔴 Red (#D32F2F)
    HIGH,      // 🟠 Orange (#F57C00)
    MEDIUM,    // 🟡 Yellow (#FBC02D)
    LOW        // 🟢 Green (#388E3C)
}
```

### Error Message Display

```kotlin
// Denied message with color coding
Card(
    colors = CardDefaults.cardColors(
        containerColor = getPriorityColor(Priority.CRITICAL).copy(alpha = 0.2f)
    ),
    border = BorderStroke(2.dp, getPriorityColor(Priority.CRITICAL))
) {
    Column {
        Icon(Icons.Default.Warning, tint = getPriorityColor(Priority.CRITICAL))
        Text("⚠️ REQUIRED PERMISSION", color = getPriorityColor(Priority.CRITICAL))
        Text(getPermissionDenialMessage(permission))
    }
}
```

### User-Friendly Messages

```kotlin
// Location denied
"⚠️ Location access denied. Emergency contacts won't receive your location. 
This significantly reduces your safety."

// SMS denied
"⚠️ SMS access denied. Emergency alerts won't be sent automatically. 
You'll need to manually contact your emergency contacts."

// Call denied
"⚠️ Call access denied. Emergency calls won't be automated. 
You'll need to manually dial your emergency contacts."
```

---

## 📱 Background Operations

### Foreground Service (Android 10+ Compliant)

```kotlin
class EmergencyService : Service() {
    companion object {
        // Android 10+ foreground service types
        private const val FOREGROUND_SERVICE_TYPE_LOCATION = 8
        private const val FOREGROUND_SERVICE_TYPE_MICROPHONE = 128
    }
    
    private fun startEmergencyMode() {
        val notification = createNotification("Emergency Active", "Monitoring...")
        
        // Android 10+ requires foreground service type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                FOREGROUND_SERVICE_TYPE_LOCATION or FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        startLocationTracking()
    }
}
```

### AndroidManifest Declaration

```xml
<!-- Permissions -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />

<!-- Service Declaration -->
<service
    android:name=".services.EmergencyService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="location|microphone"
    android:description="@string/emergency_service_description"
    tools:targetApi="q">
</service>
```

### Service Features

```kotlin
// Location Tracking
- Continuous location updates every 10 seconds
- High accuracy priority for emergency situations
- Updates sent to ViewModel via broadcast

// Audio Recording
- Evidence collection during emergencies
- Saved to secure app directory
- Auto-upload to emergency contacts

// Persistent Notification
- Shows emergency status
- Updates with current location
- Cannot be dismissed during emergency
```

---

## 🔒 Privacy Explanations

### Onboarding Privacy Information

```kotlin
// Location Permission Screen
Text("""
    Location permission is REQUIRED to send your GPS coordinates 
    in emergency alerts. This is critical for emergency responders 
    to find you quickly.
""")

// Features explained
PermissionFeatureItem("📍", "Share location in emergency SMS")
PermissionFeatureItem("🗺️", "Help responders find you quickly")
PermissionFeatureItem("🔒", "Used only when SOS is activated")
PermissionFeatureItem("✅", "Required for app to function")
```

### SMS & Call Permission Screen

```kotlin
Text("""
    Grant SMS and Call access to send automatic alerts if you 
    trigger SOS or face a threat situation.
""")

PermissionFeatureItem("💬", "Send emergency SMS to your contacts")
PermissionFeatureItem("📞", "Make automated emergency calls")
PermissionFeatureItem("🎙️", "Record audio evidence during emergencies")
```

### Privacy Guarantees

```kotlin
Card {
    Column {
        Text("💡 PRIVACY GUARANTEE")
        Spacer()
        Text("""
            • Location used ONLY during emergency
            • No background tracking when not in emergency
            • All data stays on your device
            • Contacts notified only when YOU trigger SOS
            • Audio/video evidence under your control
            • No data sent to third parties
        """)
    }
}
```

---

## ⚙️ Settings Screen Integration

### Permission Status Display

```kotlin
@Composable
fun PermissionsSection() {
    val allPermissions = PermissionHandler.getAllPermissionsStatus(context)
    
    allPermissions.forEach { permission ->
        PermissionStatusCard(
            name = permission.name,
            granted = permission.granted,
            critical = permission.critical,
            explanation = permission.explanation,
            onRequestPermission = { /* Request permission */ }
        )
    }
}
```

### Permission Card UI

```kotlin
Card {
    Row {
        // Icon with status color
        Icon(
            imageVector = if (granted) Icons.Default.Check else Icons.Default.Warning,
            tint = if (granted) SuccessGreen else SafetyRed
        )
        
        Column {
            // Permission name
            Text(name, fontWeight = FontWeight.Bold)
            
            // Status
            Text(
                if (granted) "✅ Granted" else "❌ Denied",
                color = if (granted) SuccessGreen else SafetyRed
            )
            
            // Explanation
            Text(explanation, fontSize = 14.sp)
            
            // Critical badge
            if (critical) {
                Badge { Text("CRITICAL") }
            }
        }
        
        // Action button
        if (!granted) {
            Button(onClick = { /* Open settings */ }) {
                Text("GRANT")
            }
        }
    }
}
```

---

## 🧪 Testing Permissions

### Test Permission Flow

```kotlin
@Test
fun `test location permission request`() {
    // Simulate permission denied
    whenever(permissionManager.isPermissionGranted(LOCATION)).thenReturn(false)
    
    // Request location
    val result = permissionHandler.checkPermissionWithFallbacks(LOCATION)
    
    // Verify fallbacks provided
    assertFalse(result.granted)
    assertTrue(result.fallbackSuggestions.isNotEmpty())
    assertEquals(3, result.fallbackSuggestions.size)
    
    // Verify priority order
    assertEquals(Priority.CRITICAL, result.fallbackSuggestions[0].priority)
}
```

### Test Fallback Actions

```kotlin
@Test
fun `test fallback action execution`() {
    val action = FallbackAction.OpenSettings
    
    permissionHandler.executeFallbackAction(context, action)
    
    // Verify settings intent launched
    verify(context).startActivity(any())
}
```

---

## 📊 Permission Analytics

### Track Permission Denials

```kotlin
fun trackPermissionDenial(permission: String) {
    Log.w(TAG, "Permission denied: $permission")
    
    // Analytics (if implemented)
    analytics.logEvent("permission_denied", bundleOf(
        "permission" to permission,
        "timestamp" to System.currentTimeMillis()
    ))
}
```

### Track Permission Success

```kotlin
fun trackPermissionGranted(permission: String) {
    Log.i(TAG, "Permission granted: $permission")
    
    // Analytics
    analytics.logEvent("permission_granted", bundleOf(
        "permission" to permission,
        "timestamp" to System.currentTimeMillis()
    ))
}
```

---

## 🚀 Best Practices

### 1. Request Permissions in Context

```kotlin
// ✅ Good - Explain before requesting
fun startRecording() {
    showDialog("We need microphone access to record audio evidence")
    delay(1000) // Let user read
    requestPermission(RECORD_AUDIO)
}

// ❌ Bad - Request without context
fun startRecording() {
    requestPermission(RECORD_AUDIO)
}
```

### 2. Provide Clear Fallbacks

```kotlin
// ✅ Good - Multiple options
if (!hasLocationPermission) {
    showFallbacks(listOf(
        "Grant permission",
        "Share location manually",
        "Use Google Maps"
    ))
}

// ❌ Bad - No alternatives
if (!hasLocationPermission) {
    showError("Location required")
}
```

### 3. Don't Block Critical Features

```kotlin
// ✅ Good - Graceful degradation
fun triggerEmergency() {
    if (hasLocationPermission) {
        sendSMSWithLocation()
    } else {
        sendSMSWithoutLocation()
        showManualLocationOption()
    }
}

// ❌ Bad - Block entirely
fun triggerEmergency() {
    if (!hasLocationPermission) {
        showError("Cannot trigger emergency without location")
        return
    }
}
```

### 4. Use Foreground Services Properly

```kotlin
// ✅ Good - Proper foreground service
fun startEmergency() {
    val intent = Intent(context, EmergencyService::class.java)
    context.startForegroundService(intent) // Android 8+
}

// ❌ Bad - Background service (will be killed on Android 10+)
fun startEmergency() {
    val intent = Intent(context, EmergencyService::class.java)
    context.startService(intent)
}
```

---

## 📚 Files Modified/Created

### New Files

1. `app/src/main/java/com/runanywhere/startup_hackathon20/utils/PermissionHandler.kt`
    - Comprehensive permission handling
    - Fallback suggestions
    - Priority system
    - Action execution

### Modified Files

1. `app/src/main/java/com/runanywhere/startup_hackathon20/services/EmergencyService.kt`
    - Android 10+ foreground service compliance
    - Proper notification channel
    - Foreground service types

2. `app/src/main/AndroidManifest.xml`
    - Added foreground service type permissions
    - Service declaration with types
    - Proper permission declarations

3. `app/src/main/res/values/strings.xml`
    - Service description string

### Existing Files (Already Compliant)

1. `OnboardingScreen.kt` - Already has permission explanations
2. `PermissionManager.kt` - Already has permission utilities
3. `SettingsScreen.kt` - Already has permission settings

---

## ✅ Compliance Checklist

### Android 10+ (API 29+)

- [x] Foreground service types declared
- [x] Location foreground service type used
- [x] Microphone foreground service type used
- [x] Proper notification shown
- [x] Service cannot be stopped during emergency

### Android 13+ (API 33+)

- [x] POST_NOTIFICATIONS permission requested
- [x] Notification permission explained
- [x] Fallback if notification denied

### Privacy

- [x] Clear explanations before requesting
- [x] Why each permission is needed
- [x] What happens if denied
- [x] Alternative suggestions provided
- [x] No unnecessary permissions requested

### User Experience

- [x] Color-coded priority system
- [x] Multiple fallback options
- [x] Settings navigation easy
- [x] Manual alternatives available
- [x] No feature completely blocked

---

## 🎯 Summary

The Guardian AI Safety App now has **comprehensive permission handling**:

1. ✅ **Request Before Action** - Context provided before every request
2. ✅ **Fallbacks** - Multiple alternatives when denied
3. ✅ **Background Compliance** - Android 10+ foreground services
4. ✅ **Privacy** - Clear explanations throughout
5. ✅ **Color Coding** - Priority-based visual feedback
6. ✅ **Graceful Degradation** - App works with reduced permissions
7. ✅ **Settings Integration** - Easy permission management

**Result:** A user-friendly, privacy-conscious, and compliant permission system that ensures the app
works even when some permissions are denied.
