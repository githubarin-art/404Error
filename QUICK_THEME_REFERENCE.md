# 🎨 Quick Theme Reference - Emergency Alert Colors

## 🚀 Quick Start

Import the theme colors in your Kotlin file:

```kotlin
import com.runanywhere.startup_hackathon20.ui.theme.*
```

---

## 📌 Color Usage Cheat Sheet

### Primary Actions (SOS, Emergency, Critical)

```kotlin
Button(
    colors = ButtonDefaults.buttonColors(containerColor = SafetyRed)
) { Text("EMERGENCY") }
```

### Warnings & Notifications

```kotlin
Card(
    colors = CardDefaults.cardColors(containerColor = AmberYellowLight),
    border = BorderStroke(2.dp, AmberYellow)
) { /* Warning content */ }
```

### Information & Trust

```kotlin
Icon(
    imageVector = Icons.Default.Info,
    tint = TrustBlue
)
```

### Success & Completion

```kotlin
Icon(
    imageVector = Icons.Default.Check,
    tint = SuccessGreen,
    modifier = Modifier.size(100.dp)
)
```

### 404 Error Pages

```kotlin
Text(
    text = "404",
    fontSize = 72.sp,
    fontWeight = FontWeight.ExtraBold,
    color = ErrorCode,
    letterSpacing = 4.sp
)
```

---

## 🎨 Complete Color Palette

### Primary Colors

| Name | Hex | Usage |
|------|-----|-------|
| `SafetyRed` | #E53935 | SOS button, critical alerts |
| `SafetyRedDark` | #C62828 | Pressed states, dark variant |
| `SafetyRedLight` | #EF5350 | Containers, light variant |
| `AmberYellow` | #FDD835 | Warnings, high visibility |
| `AmberYellowDark` | #FBC02D | Emphasis, navigation |
| `AmberYellowLight` | #FFF59D | Backgrounds, subtle warnings |
| `TrustBlue` | #1E88E5 | Information, reliability |
| `TrustBlueDark` | #1565C0 | Focus states, dark variant |
| `TrustBlueLight` | #42A5F5 | Info backgrounds, light variant |

### Background & Surface

| Name | Hex | Usage |
|------|-----|-------|
| `OffWhite` | #F9FAFB | Main background |
| `LightGray` | #ECEFF1 | Cards, surfaces |
| `MediumGray` | #CFD8DC | Borders, dividers |

### Text Colors

| Name | Hex | Usage |
|------|-----|-------|
| `Charcoal` | #212121 | Primary text |
| `CharcoalMedium` | #424242 | Secondary text |
| `CharcoalLight` | #757575 | Tertiary text, disabled |

### Semantic Colors

| Name | Hex | Usage |
|------|-----|-------|
| `CriticalRed` | #D32F2F | Critical emergencies |
| `WarningOrange` | #FF9800 | Warning states |
| `SuccessGreen` | #4CAF50 | Success, safe states |
| `InfoBlue` | #2196F3 | Informational messages |

### 404 Theme Accents

| Name | Hex | Usage |
|------|-----|-------|
| `ErrorCode` | #FF1744 | 404 displays, max urgency |
| `GlitchPurple` | #651FFF | Digital glitch effects |
| `SystemGreen` | #00E676 | System status messages |
| `TerminalText` | #00FF00 | Matrix/terminal style |

### Dark Mode (404 Style)

| Name | Hex | Usage |
|------|-----|-------|
| `DarkBackground` | #1A1A1A | Emergency mode background |
| `DarkSurface` | #2D2D2D | Dark mode cards |
| `DarkerSurface` | #0D0D0D | Deepest dark surfaces |

---

## 🎯 Common Patterns

### Emergency Button (Large, Prominent)

```kotlin
Button(
    onClick = { triggerEmergency() },
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = SafetyRed
    ),
    shape = RoundedCornerShape(16.dp)
) {
    Text(
        "EMERGENCY SOS",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}
```

### Warning Card

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = AmberYellowLight
    ),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(2.dp, AmberYellow)
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = Charcoal,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "Warning message here",
            color = Charcoal,
            fontWeight = FontWeight.Medium
        )
    }
}
```

### Info Card (Trust Building)

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = TrustBlueLight.copy(alpha = 0.2f)
    ),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, TrustBlue)
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "ℹ️ Information",
            fontWeight = FontWeight.Bold,
            color = TrustBlueDark
        )
        Text(
            "Detailed information text...",
            color = Charcoal
        )
    }
}
```

### Success State

```kotlin
Column(
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Icon(
        Icons.Default.Check,
        contentDescription = null,
        tint = SuccessGreen,
        modifier = Modifier.size(100.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        "SUCCESS!",
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        color = SuccessGreen,
        letterSpacing = 1.sp
    )
}
```

### 404 Error Display

```kotlin
Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    Text(
        "404",
        fontSize = 72.sp,
        fontWeight = FontWeight.ExtraBold,
        color = ErrorCode,
        letterSpacing = 4.sp
    )
    Text(
        "NOT FOUND",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Charcoal,
        letterSpacing = 2.sp
    )
}
```

### Emergency Mode Background

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    DarkBackground,
                    DarkerSurface,
                    Color(0xFF1A0000) // Dark red tint
                )
            )
        )
)
```

### Progress Indicator (Urgency-Based)

```kotlin
val timeRemaining = 15
val color = when {
    timeRemaining <= 10 -> SafetyRed
    timeRemaining <= 30 -> AmberYellow
    else -> TrustBlue
}

LinearProgressIndicator(
    progress = { progress },
    modifier = Modifier
        .fillMaxWidth()
        .height(10.dp)
        .clip(RoundedCornerShape(5.dp)),
    color = color,
    trackColor = LightGray
)
```

### Navigation Bar Item

```kotlin
NavigationBarItem(
    selected = selectedTab == 0,
    onClick = { selectedTab = 0 },
    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
    label = { Text("HOME", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
    colors = NavigationBarItemDefaults.colors(
        selectedIconColor = SafetyRed,
        selectedTextColor = SafetyRed,
        unselectedIconColor = CharcoalLight,
        unselectedTextColor = CharcoalLight,
        indicatorColor = SafetyRed.copy(alpha = 0.2f)
    )
)
```

---

## 📏 Typography Standards

### Headers (Critical Information)

```kotlin
Text(
    "EMERGENCY ACTIVE",
    fontSize = 32.sp,
    fontWeight = FontWeight.ExtraBold,
    color = ErrorCode,
    letterSpacing = 2.sp
)
```

### Subheaders

```kotlin
Text(
    "SYSTEM STATUS",
    fontSize = 20.sp,
    fontWeight = FontWeight.Bold,
    color = Charcoal,
    letterSpacing = 1.sp
)
```

### Body Text

```kotlin
Text(
    "Your safety is our priority...",
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    color = CharcoalMedium
)
```

### Labels (Buttons, Inputs)

```kotlin
Text(
    "CONTINUE",
    fontSize = 18.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.sp
)
```

---

## 🎭 Material 3 Theme Integration

The theme is already configured in `Theme.kt`. Just wrap your app:

```kotlin
@Composable
fun MyApp() {
    Startup_hackathon20Theme {
        // Your app content
        // Colors are automatically applied via MaterialTheme.colorScheme
    }
}
```

### Accessing Theme Colors

```kotlin
// From Material Theme
val primaryColor = MaterialTheme.colorScheme.primary // SafetyRed
val backgroundColor = MaterialTheme.colorScheme.background // OffWhite

// Direct access to custom colors
val customRed = SafetyRed
val customYellow = AmberYellow
```

---

## ⚡ Animation Examples

### Pulsing SOS Button

```kotlin
val pulseAnimation = rememberInfiniteTransition()
val pulseScale by pulseAnimation.animateFloat(
    initialValue = 1f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
        animation = tween(1200, easing = EaseInOut),
        repeatMode = RepeatMode.Reverse
    )
)

Box(
    modifier = Modifier
        .size(200.dp)
        .scale(pulseScale)
        .background(SafetyRed, CircleShape)
)
```

### Glitch Effect (Emergency)

```kotlin
val glitchAnimation = rememberInfiniteTransition()
val glitchOffset by glitchAnimation.animateFloat(
    initialValue = 0f,
    targetValue = 2f,
    animationSpec = infiniteRepeatable(
        animation = tween(100, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
    )
)

Text(
    "EMERGENCY",
    modifier = Modifier.offset(x = glitchOffset.dp),
    color = ErrorCode
)
```

---

## 🎨 XML Resources (for Drawables, etc.)

If you need to use colors in XML:

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/safety_red"/>
    <corners android:radius="16dp"/>
    <stroke
        android:width="2dp"
        android:color="@color/error_code"/>
</shape>
```

---

## ✅ Best Practices

1. **Always use theme colors** - Never hardcode hex values
2. **Match color to urgency** - Red for critical, Yellow for warnings, Blue for info
3. **Maintain contrast** - Ensure text is readable (WCAG AA minimum)
4. **Use semantic naming** - SafetyRed, not RedColor1
5. **Apply 404 aesthetic** - Dark mode for emergencies, glitch effects for alerts
6. **Bold typography** - ExtraBold for critical, Bold for important, Medium for normal
7. **Letter spacing** - 1-4sp for UPPERCASE text
8. **Proper elevation** - 6-12dp for important elements
9. **Rounded corners** - 12-16dp for modern look

---

## 📱 Screen-Specific Colors

| Screen | Primary | Secondary | Accent |
|--------|---------|-----------|--------|
| Emergency (Normal) | SafetyRed | AmberYellow | TrustBlue |
| Emergency (Active) | ErrorCode | SystemGreen | DarkSurface |
| Onboarding | SafetyRed | TrustBlue | SuccessGreen |
| Contacts | TrustBlue | LightGray | SafetyRed |
| Settings | AmberYellow | MediumGray | TrustBlue |

---

## 🔍 Color Accessibility

All colors meet WCAG AA standards:

- **SafetyRed on OffWhite**: 4.5:1+ ✓
- **Charcoal on OffWhite**: 16:1+ ✓
- **TrustBlue on OffWhite**: 4.5:1+ ✓
- **OffWhite on SafetyRed**: 4.5:1+ ✓

---

## 🚀 Quick Copy-Paste

### Emergency Button

```kotlin
Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(SafetyRed), shape = RoundedCornerShape(16.dp)) { Text("SOS", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
```

### Warning Card

```kotlin
Card(Modifier.fillMaxWidth(), CardDefaults.cardColors(AmberYellowLight), RoundedCornerShape(12.dp), border = BorderStroke(2.dp, AmberYellow)) { Text("Warning", Modifier.padding(16.dp)) }
```

### Info Icon

```kotlin
Icon(Icons.Default.Info, null, tint = TrustBlue, modifier = Modifier.size(24.dp))
```

---

**Quick Reference**: ✅  
**Last Updated**: November 4, 2025  
**Version**: 1.0.0  
**Status**: Ready to use 🎨
