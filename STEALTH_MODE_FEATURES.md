# 🕶️ Stealth Mode Features - Guardian AI Safety App

## Overview

The app now includes **stealth mode** to protect victims when an attacker is present. The emergency
UI is kept **simple and clear** for the victim to respond quickly, but can be hidden instantly by
pressing the back button.

---

## 🎭 How It Works

### Phase 1: Emergency Triggered (Instant Alert)

1. User triggers SOS via the 404 button (triple-tap or long-press)
2. **IMMEDIATELY** sends emergency SMS to ALL contacts with location
3. Alerts are sent BEFORE any questions are asked
4. Emergency session starts in background

### Phase 2: Simple Emergency UI (Easy to Use)

5. After alerts are sent, app shows a **clean, simple interface**
6. Question is displayed clearly with large text
7. **Easy YES/NO buttons** - Large green and red buttons, easy to tap even under stress
8. **Clear countdown timer** - Shows time remaining prominently
9. Victim can answer quickly and clearly

### Phase 3: Stealth Mode (Instant Hide)

10. If attacker approaches, victim presses **Back button**
11. App **instantly returns to normal 404 Error screen**
12. To any observer, it looks like a broken/crashed app
13. SOS button is disabled (shows "System error - retry later")
14. **Emergency continues monitoring in background**

---

## 🎯 Key Features

### 1. **Simple Emergency Interface**
```
╔═════════════════════════════════╗
║                                 ║
║  ✓ Emergency alerts sent        ║
║  All contacts notified          ║
║                                 ║
║  ┌───────────────────────────┐  ║
║  │     🔔 QUESTION           │  ║
║  │                           │  ║
║  │  Are you in immediate     │  ║
║  │  danger right now?        │  ║
║  │                           │  ║
║  │       30 seconds          │  ║
║  │    remaining              │  ║
║  │                           │  ║
║  │  ┌─────────┐ ┌─────────┐ │  ║
║  │  │   ✓     │ │    ✗    │ │  ║
║  │  │  YES    │ │   NO    │ │  ║
║  │  └─────────┘ └─────────┘ │  ║
║  └───────────────────────────┘  ║
║                                 ║
║  [False Alarm - Cancel]         ║
╚═════════════════════════════════╝
```

### ✅ ADVANTAGES:

- 📱 **Crystal clear** - Easy to read and understand
- 👆 **Large buttons** - Easy to tap even under stress
- ⏱️ **Visible timer** - Know how much time you have
- 🟢🔴 **Color coded** - Green = YES/Safe, Red = NO/Danger
- 🎯 **Simple choices** - Just two clear options

### 2. **Instant Stealth Mode**

When victim presses BACK button:

```
╔═════════════════════════════════╗
║                                 ║
║        404 ERROR                ║
║    Application Not Found        ║
║                                 ║
║  ┌───────────────────────────┐  ║
║  │ ℹ️ System initialized     │  ║
║  └───────────────────────────┘  ║
║                                 ║
║        ┌──────────┐             ║
║        │   404    │ (disabled)  ║
║        │  error   │             ║
║        └──────────┘             ║
║                                 ║
║   System error - retry later    ║
║                                 ║
╚═════════════════════════════════╝
```

### 🎭 WHAT ATTACKER SEES:

- 🐛 **Broken app** - Appears to have crashed
- 💤 **Boring** - Nothing interesting
- ❌ **Disabled** - Button doesn't work
- 📱 **Safe to show** - If attacker demands phone

### ✅ WHAT'S ACTUALLY HAPPENING:

- ✅ **Emergency STILL ACTIVE** - Running in background
- ✅ **Contacts already notified** - Help is on the way
- ✅ **Location tracking continues** - Real-time updates
- ✅ **AI monitoring ongoing** - Will escalate if needed
- ✅ **Victim protected** - UI doesn't reveal emergency

---

## 📊 Design Principles

| Aspect           | Design Choice            | Reason                                       |
|------------------|--------------------------|----------------------------------------------|
| **Emergency UI** | Simple, clean, clear     | Victim needs to respond quickly under stress |
| **Button Size**  | Large (100dp height)     | Easy to tap even if hands are shaking        |
| **Colors**       | Green (YES) / Red (NO)   | Universally understood color coding          |
| **Text Size**    | Large (24sp for buttons) | Easy to read in panic situation              |
| **Timer**        | Big, prominent display   | Victim knows exactly how much time they have |
| **Stealth Mode** | One button press (back)  | Quick escape if attacker approaches          |
| **Decoy UI**     | Boring 404 error         | Doesn't attract attention                    |

---

## 🎮 User Flow Example

### Scenario: Victim being followed home by stranger

**Step 1:** Victim discreetly triggers 404 button (triple-tap or long-press)
```
[Instantly happens in background]
✅ SMS sent to Mom: "🚨 EMERGENCY - I need help! Location: [map link]"
✅ SMS sent to Best Friend: "🚨 EMERGENCY - I need help! Location: [map link]"
✅ SMS sent to Partner: "🚨 EMERGENCY - I need help! Location: [map link]"
```

**Step 2:** Phone shows simple question

```
PHONE SCREEN:
┌─────────────────────────┐
│ ✓ Emergency alerts sent │
│                         │
│ Are you in immediate    │
│ danger right now?       │
│                         │
│    [  ✓ YES  ] [ ✗ NO ] │
└─────────────────────────┘

VICTIM: [Taps NO button - I'm in danger]
```

**Step 3:** Attacker gets closer - victim presses Back button
```
ATTACKER: "Who are you texting?"
VICTIM: [Presses back button]
PHONE SHOWS: "404 ERROR - Application Not Found"
ATTACKER: "Your phone is broken..."
Result: ✅ Doesn't suspect anything
        ✅ Contacts are already calling police with location
```

---

## 🔧 Technical Implementation

### Key Changes:

1. **Removed Complex Calculator UI**
    - Previous: Hidden calculator with secret codes (1=YES, 0=NO)
    - Now: **Clear, simple YES/NO buttons**
    - Why: Easier for victim to use under stress

2. **Removed DEBUG Section**
    - Previous: "DEBUG: 3 connections" card near SOS button
    - Now: **Clean, minimal interface**
    - Why: Less clutter, more professional look

3. **Simplified Emergency Screen**
    - Large, clear question text
    - Big YES/NO buttons (100dp height)
    - Prominent timer display
    - Blue/white color scheme (calming but clear)

### State Management:
```kotlin
// Tracks when to show decoy mode
LaunchedEffect(isAlarmActive, alertHistory) {
    if (isAlarmActive && alertHistory.isNotEmpty()) {
        showDecoyMode = true  // Can hide UI now
    }
}

// Handle back button during emergency
BackHandler(enabled = isAlarmActive && showDecoyMode) {
    viewModel.enterStealthMode()  // Return to 404 screen
}
```

---

## 📱 UI Comparison

### Emergency Active Screen:

**BEFORE (Calculator - Confusing):**

- ❌ Complex calculator interface
- ❌ Secret codes (press 1 for YES, 0 for NO)
- ❌ Hidden question in "Quick calculation"
- ❌ Victim needs to remember codes under stress

**NOW (Simple - Clear):**

- ✅ Clean, straightforward interface
- ✅ Large YES/NO buttons with icons
- ✅ Clear question text
- ✅ No hidden meanings - direct communication
- ✅ Easy to use even in panic

### Normal Screen (Same):

- Boring 404 Error screen
- Looks like broken app
- SOS button disabled in stealth mode
- No indication of active emergency

---

## ✅ Benefits of Simplified Design

### For the Victim:

1. **No confusion** - Clear buttons, obvious meaning
2. **Fast response** - Can answer in seconds
3. **Less stress** - Don't need to remember secret codes
4. **Large targets** - Easy to tap even if hands shaking
5. **Quick hide** - Back button instantly hides everything

### Safety Features:

1. **Alerts sent first** - Contacts notified before questions
2. **Background monitoring** - Continues even when hidden
3. **Instant stealth** - One button press to hide
4. **No suspicious look** - Hidden UI looks like error
5. **Continuous protection** - AI keeps monitoring

---

## 🎯 Why This Is Better

### OLD Approach (Calculator):

- 🤔 **Too complex**: Victim needs to figure out codes
- ⏰ **Slow**: Takes time to understand the interface
- 😰 **Stressful**: Hard to use in panic situation
- 🎭 **Over-engineered**: Trying to be too clever

### NEW Approach (Simple):

- ✅ **Crystal clear**: Anyone can understand instantly
- ⚡ **Fast**: Tap YES or NO, done
- 😌 **Calm**: Clear interface reduces panic
- 🎯 **Focused**: Does one thing well

---

## 💡 Key Insight

> **"In an emergency, simplicity saves lives."**

When someone is in danger:

- They're stressed, scared, possibly shaking
- They need to respond FAST
- They can't process complex interfaces
- They need OBVIOUS choices

The simplified UI respects this reality.

---

## ⚠️ Important Notes

### For Users:

- **Emergency UI is SIMPLE** - Just tap YES or NO
- **Back button HIDES** - Press back if attacker approaches
- **Trust the system** - Contacts already alerted
- **Stay calm** - Interface is designed to be easy

### For Developers:

- Emergency alerts sent BEFORE questions (immediate help)
- Simple UI reduces cognitive load on victim
- Stealth mode activated by back button press
- Emergency continues in background when hidden

---

## 📞 Quick Reference

### When Emergency Active:

- **See question?** → Tap **YES** (green) or **NO** (red)
- **Attacker coming?** → Press **BACK** button
- **False alarm?** → Tap "False Alarm - Cancel" at bottom

### When in Stealth Mode:

- **UI shows:** 404 Error (broken app)
- **Actually:** Emergency still active in background
- **Contacts:** Already have your location
- **You:** Look calm, appear normal

---

**Remember**: The best safety feature is one that's **simple to use** and **hard to detect**. 🛡️
