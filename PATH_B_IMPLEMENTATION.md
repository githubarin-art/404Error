# Guardian AI Safety App - Path B Implementation

## Path B: Escape to Safety (HIGH ALERT)

### Overview

When a user answers "NO" to "Is the threat near you right now?", they enter **Path B:
ESCAPE_TO_SAFETY** mode with threat level set to **HIGH**.

---

## ✅ Implemented Features

### 1. UI Header

- **Status**: "HIGH ALERT – ESCAPE TO SAFETY" displayed in amber/yellow
- **Status chip**: Shows current emergency status with amber color coding
- **Purpose**: Indicates user can move to safety

### 2. Journey Progress Card (When Navigating)

Displayed prominently when user has selected a destination:

#### Features:

- **Destination name**: "Heading to [Place Name]"
- **Distance**: Shows current distance in meters (e.g., "400m away")
- **Location updates**: "📍 Location updates sent every 30 seconds"
- **Safety tip**: "💡 Stay visible, move towards well-lit populated areas"
- **Visual**: Green-themed card (#E8F5E9 background)
- **Icon**: Route icon with 32dp size

### 3. EXPANDED Nearest Safe Places Section (PRIMARY FOCUS)

This is the **main focus** of Path B - helping user escape to safety.

#### Features:

- **State**: Expanded by default (collapsed for Path A)
- **Title**: "NEAREST SAFE PLACES" (18sp, bold, amber)
- **Content**: Shows 4-5 categorized places

#### Place Categories (Ranked by Priority):

1. **Police Stations** (🚓 Highest priority)
    - Pune City Police Station (24/7, emergency response)
    - Shivajinagar Police Station (24/7, main station)

2. **Hospitals** (🏥 Second priority)
    - Sassoon General Hospital (24/7 emergency)
    - Ruby Hall Clinic (24/7, well-staffed)

3. **Fire Stations** (🚒)
    - Fire Brigade Station (24/7, emergency services)

4. **24/7 Stores** (🏪 Populated, well-lit)
    - 24/7 Reliance Mart (security present)
    - DMart Supermarket (busy shopping area)

5. **Malls** (🏢 Secure, CCTV)
    - Amanora Mall (security guards, CCTV)

6. **Hotels** (🏨 Staffed, secure)
    - Hyatt Regency Hotel (24/7 reception, secure)

7. **Religious Places** (⛪ Crowded, safe during hours)
    - Dagadusheth Temple (crowded during hours)

8. **Transport Hubs** (🚉 Populated, police presence)
    - Pune Railway Station (RPF security, crowded)
    - Swargate Bus Stand (police post, major hub)

#### Information Displayed Per Place:

- **Icon**: Type-specific (police, hospital, store, etc.) - 32dp
- **Name**: Bold, 16sp
- **Distance**: In meters (calculated from current location)
- **Walking time**: Estimated at 5 km/h (~83m/min)
- **Address**: Full address
- **Hours**: Operating hours (e.g., "24/7 Open", "8 AM - 11 PM")
- **Notes**: Safety info (e.g., "Well-lit, security present")
- **Navigate Now button**: Opens Google Maps with walking navigation

#### Ranking Algorithm:

```kotlin
1. Filter by time (24/7 places prioritized at night)
2. Sort by priority: police > hospital/fire > others
3. Sort by distance (closest first within each category)
4. Take top 5 places
```

#### Footer Info:

"Ranked by priority and distance. Choose the safest populated route."

### 4. Journey Tracking (Active Monitoring)

When user starts navigation to a safe place:

#### Location Updates (Every 30 seconds):

- **Message format**: "Moving towards [Place] - [Distance]m away"
- **Sent to**: ALL emergency contacts
- **Includes**: Current GPS coordinates link

#### Stationary Alert (>2 minutes):

- **Trigger**: User hasn't moved >10m in 2 minutes
- **Message**: "Stopped moving towards [Place] for over 2 minutes"
- **Action**: Sends URGENT ALERT to contacts

#### Route Deviation Alert (>50m):

- **Trigger**: User moved >50m away from route
- **Message**: "Deviated from route to [Place]"
- **Action**: Sends URGENT ALERT to contacts

#### Arrival Detection (Within 50m):

- **Trigger**: User is within 50m of destination
- **Dialog**: "Are you safe now?"
- **Options**:
    - "YES, I'M SAFE" (green button) → Ends emergency mode
    - "Not yet, continue" → Continues monitoring

### 5. COLLAPSIBLE Additional Protection Section (SECONDARY)

Less prominent than in Path A - user is escaping, not staying in place.

#### Features:

- **State**: Collapsed by default
- **Title**: "ADDITIONAL PROTECTION" (gray, less prominent)
- **Icon**: Security shield icon

#### Tools Available (When Expanded):

Compact 2x2 grid + police button:

1. **Loud Alarm** (red)
    - Max volume siren + vibration
    - Toggleable

2. **Record Evidence** (dark red)
    - Audio recording
    - Shows "Recording..." when active

3. **Fake Call** (blue)
    - Incoming call overlay
    - Discreet exit strategy

4. **Breathing Exercise** (green)
    - 4-4-4 calm routine
    - Full-screen overlay

5. **Call Police (112)** (red button, full width)
    - Confirmation dialog
    - Emergency services

### 6. Safety Network Status Card

**Less prominent** than in Path A (compact mode).

#### Features:

- **Title**: "YOUR SAFETY NETWORK" (16sp vs 18sp)
- **Location tracking**: "Location updates every 30 seconds"
- **Recording status**: Shows if evidence recording is active
- **Alert history**: Last 3 alerts with timestamps
- **Compact**: `compact = true` parameter

### 7. Police Call Section

**Less visually prominent** than in Path A.

#### Features:

- **Style**: Outlined button (not filled red primary)
- **Button**: "Call 112" (outlined, red border)
- **Secondary button**: "Not now" (text button)
- **Confirmation dialog**: Same as Path A

---

## 🎨 Design Differences from Path A

### Visual Hierarchy

**Path A (Threat Nearby)**:

1. 4 Large Action Buttons (PRIMARY - 140dp height)
2. Collapsible Escape Section (SECONDARY)
3. Safety Network Status
4. Police Call (prominent)

**Path B (Escape to Safety)**:

1. Journey Progress Card (if navigating)
2. **EXPANDED Safe Places Section (PRIMARY - up to 5 places)**
3. Collapsible Additional Protection (SECONDARY)
4. Safety Network Status (compact)
5. Police Call (less prominent)

### Color Scheme

- **Primary**: Amber/Yellow (#FFC107, #F57F17) - focus on escape
- **Journey**: Green (#E8F5E9, #2E7D32) - progress/safety
- **Additional tools**: Same colors but less prominent
- **Police**: Red but outlined, not filled

### Prominence Adjustments

- Safe places section: **Expanded by default, large icons, detailed info**
- Protection tools: **Collapsed by default, compact layout**
- Police button: **Outlined instead of filled red**
- Safety status: **Compact mode (smaller text)**

---

## 🚀 Technical Implementation

### Files Modified

1. **EmergencyScreen.kt**:
    - Updated `EscapeToSafetyScreen` composable
    - Enhanced journey progress card with distance
    - Expanded safe places section (5 places)
    - Added collapsible additional protection section
    - Updated place card with hours and notes
    - Modified arrival dialog messaging

2. **SafetyModels.kt**:
    - Added `hours: String?` to SafePlace
    - Added `notes: String?` to SafePlace

3. **SafetyViewModel.kt**:
    - Updated `getSafePlaces()` with 12 categorized places
    - Added hours and notes to all places
    - Journey monitoring already implemented:
        - `startJourneyMonitoring()` - Monitors route
        - `sendLocationUpdateToContacts()` - Every 30s
        - `sendAlertToContacts()` - For deviations
        - `confirmArrival()` - Ends emergency if safe

### State Management

Journey tracking uses coroutines:

```kotlin
journeyMonitoringJob = viewModelScope.launch {
    while (currentDestination != null) {
        delay(30000) // 30 seconds
        
        // Calculate distance to destination
        // Check for stationary >2 min
        // Check for deviation >50m
        // Send appropriate updates/alerts
        // Check for arrival <50m
    }
}
```

### Data Flow

```
User Selects Safe Place
    ↓
Opens Google Maps Navigation
    ↓
Sets currentDestination
    ↓
Starts Journey Monitoring
    ↓
Every 30 seconds:
  - Get current location
  - Calculate distance
  - Send update to contacts
  - Check for alerts
    ↓
Within 50m:
  - Show "Are you safe?" dialog
  - If YES: End emergency
  - If NO: Continue monitoring
```

---

## 📱 User Experience Flow

### Scenario: User Escaping to Police Station

1. **User answers NO** to "Is threat near you?"
2. **Screen shows**: "HIGH ALERT – ESCAPE TO SAFETY"
3. **Safe places section**: Expanded showing 5 places
4. **User sees**: "Pune City Police Station - 800m - ~10 min walk"
5. **User taps**: "Navigate Now"
6. **Google Maps opens**: Walking directions
7. **Journey card shows**: "Heading to Pune City Police Station - 800m away"
8. **Every 30s contacts receive**: "Moving towards Pune City Police Station - 650m away"
9. **If user stops >2 min**: Contacts get "URGENT: Stopped moving for 2 minutes"
10. **If user deviates >50m**: Contacts get "URGENT: Deviated from route"
11. **At 40m away**: Dialog appears "Are you safe now?"
12. **User confirms**: Emergency mode ends, contacts notified

---

## 🎯 Success Criteria Met

✅ **UI displays "HIGH ALERT – ESCAPE TO SAFETY"**  
✅ **Expanded safe places section (4-5 places)**  
✅ **Places categorized by type (police, hospital, store, transport, religious)**  
✅ **Places ranked by priority and distance**  
✅ **Each place shows: name, icon, distance, walking time, hours, notes**  
✅ **Navigate Now button launches Google Maps**  
✅ **Journey tracking with location updates every 30 seconds**  
✅ **Alert if stationary >2 minutes**  
✅ **Alert if deviated >50m from route**  
✅ **Arrival trigger within 50m**  
✅ **"Are you safe now?" confirmation dialog**  
✅ **Collapsible additional protection section**  
✅ **All tools functional (alarm, record, fake call, breathing, police)**  
✅ **Safety Network Status (compact mode)**  
✅ **Police call section (less prominent)**

---

## 🆚 Path A vs Path B Comparison

| Feature | Path A (Threat Nearby) | Path B (Escape to Safety) |
|---------|----------------------|--------------------------|
| **Primary Focus** | 4 Large Action Buttons | Expanded Safe Places |
| **Safe Places** | Collapsible, 3 places | Expanded, 5 places |
| **Button Size** | 140dp (Large) | 80dp (Compact) |
| **Additional Tools** | PRIMARY (expanded) | SECONDARY (collapsed) |
| **Police Button** | Filled Red (prominent) | Outlined Red (less prominent) |
| **Journey Tracking** | Not emphasized | Prominent progress card |
| **Color Theme** | Red (critical) | Amber/Yellow (alert) |
| **Use Case** | Cannot move, need help | Can escape, need directions |

---

## ✨ Complete and Ready

Path B (Escape to Safety) is fully implemented with:

- ✅ Expanded safe places (5 categorized locations)
- ✅ Journey tracking with automatic updates
- ✅ Route monitoring (stationary & deviation alerts)
- ✅ Arrival detection and confirmation
- ✅ Collapsible additional protection tools
- ✅ Less prominent police/safety status
- ✅ Clear visual hierarchy for escape focus

The implementation provides comprehensive support for users who can escape to safety! 🎯
