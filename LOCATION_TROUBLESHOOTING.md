# 📍 Location Troubleshooting Guide

## Problem: Location Not Showing in Emergency SMS

If location is showing as "Location unavailable" in emergency messages, follow these steps:

---

## ✅ Step 1: Check Logcat

Open Logcat in Android Studio and filter by `SafetyViewModel` tag. Look for:

### **When App Starts:**

```
========================================
LOCATION DIAGNOSTIC CHECK
========================================
Permission Status:
  FINE_LOCATION: ✅ GRANTED  (or ❌ DENIED)
  COARSE_LOCATION: ✅ GRANTED  (or ❌ DENIED)
Device Location Services:
  GPS Provider: ✅ ENABLED  (or ❌ DISABLED)
  Network Provider: ✅ ENABLED  (or ❌ DISABLED)
Last Known Location:
  ✅ Available  (or ❌ NOT AVAILABLE)
========================================
```

### **When Emergency Triggered:**

```
⏱️ Fetching location (will wait up to 5 seconds)...
📍 Starting location monitoring...
✅ Got last known location: 28.5383, 77.3904
   OR
✅ Got current location: 28.5383, 77.3904
   OR
⚠️ No location available after 5 seconds
```

---

## 🔧 Step 2: Fix Common Issues

### **Issue 1: Permission Not Granted** ❌

**Symptoms:**

```
Permission Status:
  FINE_LOCATION: ❌ DENIED
  COARSE_LOCATION: ❌ DENIED
```

**Solution:**

1. Go to device **Settings** > **Apps** > **404Error**
2. Tap **Permissions**
3. Tap **Location**
4. Select **"Allow only while using the app"** or **"Allow all the time"**
5. Restart the app

OR grant during onboarding when prompted

---

### **Issue 2: Location Services Disabled on Device** ❌

**Symptoms:**

```
Device Location Services:
  GPS Provider: ❌ DISABLED
  Network Provider: ❌ DISABLED
```

**Solution:**

1. Go to device **Settings** > **Location**
2. Turn **ON** location services
3. Ensure **GPS** and **Network** location are enabled
4. Restart the app

---

### **Issue 3: No Last Known Location** ⚠️

**Symptoms:**

```
Last Known Location: ❌ NOT AVAILABLE
Device may have never obtained a location before
```

**Solution:**

1. Open **Google Maps** or any GPS app
2. Let it get your location once
3. Close Maps
4. Restart 404Error app
5. Trigger emergency - location should work now

This "primes" the device's location cache.

---

### **Issue 4: Poor GPS Signal** 📡

**Symptoms:**

```
⚠️ Current location returned null
Possible reasons:
  1. GPS/Location services disabled in device settings
  2. Device is indoors with poor GPS/network signal
  3. Location fetch timeout
```

**Solution:**

1. Go outdoors or near a window
2. Wait 30 seconds for GPS to lock
3. Trigger emergency again

OR

1. Enable **Wi-Fi** (helps with network location)
2. Trigger emergency again

---

### **Issue 5: Testing on Emulator** 🖥️

**Symptoms:**
Location never works in emulator

**Solution:**

1. In Android Studio, open **Tools** > **Device Manager**
2. Click on your emulator's **3-dot menu**
3. Select **Extended Controls** (or press Cmd+Shift+E)
4. Go to **Location** tab
5. Enter GPS coordinates manually:
    - Latitude: `28.5383`
    - Longitude: `77.3904`
6. Click **Send**
7. Trigger emergency in app

---

## 🧪 Step 3: Test Location Manually

Add this test code to `EmergencyScreen.kt` temporarily:

```kotlin
Button(
    onClick = {
        // Test location fetch
        viewModel.updateLocation(
            Location("test").apply {
                latitude = 28.5383
                longitude = 77.3904
                accuracy = 10f
                time = System.currentTimeMillis()
            }
        )
    }
) {
    Text("Test Location")
}
```

This manually sets a test location to verify the SMS generation works.

---

## 📊 Expected Behavior

### **Scenario A: Permission Granted + GPS Enabled**

```
Result: ✅ Location included in SMS
Timeline:
  0s: User triggers SOS
  0-5s: App fetches location
  5s: SMS sent with coordinates
  
SMS Content:
  📍 MY LOCATION:
  Latitude: 28.5383
  Longitude: 77.3904
  🗺️ Open in Maps: https://maps.google.com/?q=28.5383,77.3904
```

### **Scenario B: Permission Granted + GPS Disabled**

```
Result: ⚠️ Location unavailable
Timeline:
  0s: User triggers SOS
  0-5s: App tries to fetch location
  5s: SMS sent without coordinates
  
SMS Content:
  ⚠️ Location unavailable - Please try calling me!
```

### **Scenario C: Permission Not Granted**

```
Result: ⚠️ Location unavailable
Timeline:
  0s: User triggers SOS
  0s: Permission check fails immediately
  5s: SMS sent without coordinates
  
SMS Content:
  ⚠️ Location unavailable - Please try calling me!
```

---

## 🔍 Debug Checklist

Run through this checklist:

- [ ] Location permission granted in Settings > Apps > 404Error
- [ ] Location services ON in device Settings > Location
- [ ] GPS provider enabled (check Logcat)
- [ ] Network provider enabled (check Logcat)
- [ ] Last known location available (check Logcat)
- [ ] Not testing indoors (if using real device)
- [ ] Wi-Fi enabled (helps with network location)
- [ ] Google Play Services up to date
- [ ] Device has gotten location before (opened Maps once)
- [ ] Not in airplane mode
- [ ] Waited full 5 seconds after triggering

---

## 🚑 Quick Fix Commands

### **Reset App Permissions (ADB):**

```bash
adb shell pm reset-permissions com.runanywhere.startup_hackathon20
```

### **Grant Location Permission (ADB):**

```bash
adb shell pm grant com.runanywhere.startup_hackathon20 android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.runanywhere.startup_hackathon20 android.permission.ACCESS_COARSE_LOCATION
```

### **Check App Permissions (ADB):**

```bash
adb shell dumpsys package com.runanywhere.startup_hackathon20 | grep permission
```

### **View Logcat (ADB):**

```bash
adb logcat -s SafetyViewModel:I
```

---

## 📝 Key Points

1. **Permission is REQUIRED** - Android won't give location without explicit user consent
2. **5-second wait** - App waits up to 5 seconds for location
3. **Last known location** - Used if available and recent (< 5 minutes old)
4. **Current location** - Fetched with HIGH_ACCURACY if permission granted
5. **Graceful fallback** - SMS still sent even without location
6. **Device must have location** - If device never got location, there's nothing to fetch

---

## ✅ Verification Steps

1. Open the app
2. Check Logcat for "LOCATION DIAGNOSTIC CHECK"
3. Verify all checkmarks (✅) are green
4. Trigger emergency
5. Check Logcat for "✅ Location obtained successfully!"
6. Verify SMS contains GPS coordinates

If you see ❌ anywhere, that's the issue to fix!

---

## 🆘 Still Not Working?

If you've tried everything above and location still doesn't work:

1. **Check device settings:**
    - Settings > Privacy > Location services > ON
    - Settings > Apps > 404Error > Permissions > Location > Allow

2. **Try another location app:**
    - Open Google Maps
    - If Maps can't get location, it's a device issue

3. **Check Play Services:**
    - Settings > Apps > Google Play Services
    - Make sure it's enabled and up to date

4. **Restart everything:**
    - Restart the app
    - Restart the device
    - Clear app data (Settings > Apps > 404Error > Storage > Clear Data)

5. **Check Logcat:**
    - Look for `SecurityException`
    - Look for `Failed to get location`
    - Share the logs for help

---

**Remember: The app will ALWAYS send emergency SMS, even without location. Location is helpful but
not required for the alert to work!** 🚨
