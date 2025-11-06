package com.runanywhere.startup_hackathon20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.SafetyViewModel
import com.runanywhere.startup_hackathon20.ui.theme.*
import com.runanywhere.sdk.public.extensions.listAvailableModels
import com.runanywhere.sdk.models.ModelInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isModelLoaded by viewModel.isModelLoaded.collectAsState()
    
    var showModelDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSOSDialog by remember { mutableStateOf(false) }
    var showThreatProtocolDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CharcoalMedium,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Configure Guardian AI",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Model Section
            item {
                SectionHeader("AI Model")
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "AI Model Status",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Charcoal
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (isModelLoaded) "Model loaded and ready" else "No model loaded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CharcoalMedium
                                )
                            }
                            
                            Icon(
                                if (isModelLoaded) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isModelLoaded) SuccessGreen else AmberYellowDark,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        if (!isModelLoaded) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showModelDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TrustBlue
                                )
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Load AI Model")
                            }
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showModelDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Change Model")
                            }
                        }
                        
                        if (statusMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = TrustBlue.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    statusMessage,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CharcoalMedium
                                )
                            }
                        }
                    }
                }
            }
            
            // Emergency Settings
            item {
                SectionHeader("Emergency")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Emergency Contacts",
                    subtitle = "Manage who gets alerted",
                    onClick = { /* User will switch to Contacts tab manually */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Warning,
                    title = "SOS Activation",
                    subtitle = "Configure alarm trigger methods",
                    onClick = { showSOSDialog = true }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Shield,
                    title = "Threat Protocol",
                    subtitle = "Adjust sensitivity and escalation",
                    onClick = { showThreatProtocolDialog = true }
                )
            }
            
            // Location & Privacy
            item {
                SectionHeader("Location & Privacy")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.LocationOn,
                    title = "Location Sharing",
                    subtitle = "Configure location tracking",
                    onClick = { showLocationDialog = true }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy Settings",
                    subtitle = "Data collection and security",
                    onClick = { showPrivacyDialog = true }
                )
            }
            
            // Notifications
            item {
                SectionHeader("Notifications")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Alert Channels",
                    subtitle = "SMS, calls, and in-app alerts",
                    onClick = { showNotificationDialog = true }
                )
            }
            
            // About
            item {
                SectionHeader("About")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About Guardian AI",
                    subtitle = "Version 1.0.0",
                    onClick = { showAboutDialog = true }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "Privacy Policy",
                    subtitle = "How we protect your data",
                    onClick = { showPrivacyDialog = true }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Help,
                    title = "Help & Support",
                    subtitle = "Get assistance",
                    onClick = { showHelpDialog = true }
                )
            }
            
            // Danger Zone
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SafetyRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "⚠️ Danger Zone",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SafetyRed
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { /* TODO: Implement clear data with confirmation */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SafetyRed
                            )
                        ) {
                            Text("Clear All Data")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { /* TODO: Implement reset with confirmation */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SafetyRed
                            )
                        ) {
                            Text("Reset to Defaults")
                        }
                    }
                }
            }
        }
    }
    
    // All Dialogs
    if (showModelDialog) {
        ModelSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showModelDialog = false }
        )
    }
    
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
    
    if (showSOSDialog) {
        SOSActivationDialog(onDismiss = { showSOSDialog = false })
    }
    
    if (showThreatProtocolDialog) {
        ThreatProtocolDialog(onDismiss = { showThreatProtocolDialog = false })
    }
    
    if (showLocationDialog) {
        LocationSharingDialog(onDismiss = { showLocationDialog = false })
    }
    
    if (showPrivacyDialog) {
        PrivacySettingsDialog(onDismiss = { showPrivacyDialog = false })
    }
    
    if (showNotificationDialog) {
        NotificationSettingsDialog(onDismiss = { showNotificationDialog = false })
    }
    
    if (showHelpDialog) {
        HelpSupportDialog(onDismiss = { showHelpDialog = false })
    }
}

// ... existing code (SectionHeader, SettingsItem) ...

@Composable
fun SOSActivationDialog(onDismiss: () -> Unit) {
    var hiddenButtonEnabled by remember { mutableStateOf(true) }
    var longPressEnabled by remember { mutableStateOf(true) }
    var tripleTabEnabled by remember { mutableStateOf(false) }
    var shakeGestureEnabled by remember { mutableStateOf(false) }
    var volumeButtonsEnabled by remember { mutableStateOf(false) }
    var silentAlarmMode by remember { mutableStateOf(false) }
    var hapticFeedback by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = SafetyRed,
                    modifier = Modifier.size(28.dp)
                )
                Text("SOS Activation")
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Text(
                        "Trigger Methods",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hidden Button", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap hidden emergency button", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = hiddenButtonEnabled, onCheckedChange = { hiddenButtonEnabled = it })
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Long Press", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Hold button for 3 seconds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = longPressEnabled, onCheckedChange = { longPressEnabled = it })
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Triple Tap", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Quickly tap 3 times", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = tripleTabEnabled, onCheckedChange = { tripleTabEnabled = it })
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shake Gesture", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Shake phone vigorously", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = shakeGestureEnabled, onCheckedChange = { shakeGestureEnabled = it })
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Volume Buttons", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Press volume up 5 times", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = volumeButtonsEnabled, onCheckedChange = { volumeButtonsEnabled = it })
                    }
                }
                
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "Additional Options",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Silent Alarm Mode", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("No sound or vibration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = silentAlarmMode, onCheckedChange = { silentAlarmMode = it })
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Haptic Feedback", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Vibrate on activation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = hapticFeedback, onCheckedChange = { hapticFeedback = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ThreatProtocolDialog(onDismiss: () -> Unit) {
    var sensitivity by remember { mutableStateOf(1) } // 0=Conservative, 1=Balanced, 2=Aggressive
    var autoEscalation by remember { mutableStateOf(true) }
    var escalationDelay by remember { mutableStateOf(60) }
    var biometricFallback by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = TrustBlue,
                    modifier = Modifier.size(28.dp)
                )
                Text("Threat Protocol")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Threat Sensitivity
                Column {
                    Text(
                        "Threat Sensitivity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        when (sensitivity) {
                            0 -> "Conservative - Less sensitive, fewer false alarms"
                            1 -> "Balanced - Recommended setting"
                            else -> "Aggressive - Highly sensitive, triggers faster"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = sensitivity == 0,
                            onClick = { sensitivity = 0 },
                            label = { Text("Conservative") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = sensitivity == 1,
                            onClick = { sensitivity = 1 },
                            label = { Text("Balanced") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = sensitivity == 2,
                            onClick = { sensitivity = 2 },
                            label = { Text("Aggressive") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Divider()
                
                // Auto Escalation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Escalation", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Automatically increase threat level over time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = autoEscalation, onCheckedChange = { autoEscalation = it })
                }
                
                // Escalation Delay
                if (autoEscalation) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Escalation Delay", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("$escalationDelay sec", color = TrustBlue, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = escalationDelay.toFloat(),
                            onValueChange = { escalationDelay = it.toInt() },
                            valueRange = 30f..300f,
                            steps = 17
                        )
                        Text("Time before threat level increases", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Divider()
                
                // Biometric Fallback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Biometric Fallback", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Use fingerprint/face to cancel alarm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = biometricFallback, onCheckedChange = { biometricFallback = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LocationSharingDialog(onDismiss: () -> Unit) {
    var locationMode by remember { mutableStateOf(0) } // 0=Live, 1=Last Known, 2=On Demand
    var updateInterval by remember { mutableStateOf(30) }
    var lowBatteryFallback by remember { mutableStateOf(true) }
    var geofenceEnabled by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TrustBlue,
                    modifier = Modifier.size(28.dp)
                )
                Text("Location Sharing")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Location Mode
                Text(
                    "Location Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { locationMode = 0 },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = locationMode == 0, onClick = { locationMode = 0 })
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Live Sharing", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Continuous location updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { locationMode = 1 },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = locationMode == 1, onClick = { locationMode = 1 })
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Last Known Location", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Share most recent location", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { locationMode = 2 },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = locationMode == 2, onClick = { locationMode = 2 })
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("On Demand Only", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Only when emergency triggered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (locationMode == 0) {
                    Divider()
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Update Interval", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("$updateInterval sec", color = TrustBlue, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = updateInterval.toFloat(),
                            onValueChange = { updateInterval = it.toInt() },
                            valueRange = 5f..120f,
                            steps = 22
                        )
                        Text("Lower = More battery drain", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Low Battery Fallback", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Reduce accuracy when battery low", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = lowBatteryFallback, onCheckedChange = { lowBatteryFallback = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Geofence Alerts", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Alert when leaving safe zones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = geofenceEnabled, onCheckedChange = { geofenceEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PrivacySettingsDialog(onDismiss: () -> Unit) {
    var dataCollection by remember { mutableStateOf(false) }
    var locationSharing by remember { mutableStateOf(true) }
    var sensorAccess by remember { mutableStateOf(true) }
    var analytics by remember { mutableStateOf(false) }
    var encryptLocal by remember { mutableStateOf(true) }
    var anonymizeData by remember { mutableStateOf(true) }
    var autoDelete by remember { mutableStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = CharcoalMedium,
                    modifier = Modifier.size(28.dp)
                )
                Text("Privacy & Data")
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Surface(
                        color = TrustBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(20.dp))
                            Text(
                                "Your privacy is our priority. All data is encrypted and never shared with third parties.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                item {
                    Text(
                        "Data Permissions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Location Sharing", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Required for emergency alerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = locationSharing, onCheckedChange = { locationSharing = it })
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sensor Access", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("For shake and motion detection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = sensorAccess, onCheckedChange = { sensorAccess = it })
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anonymous Analytics", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Help improve the app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = analytics, onCheckedChange = { analytics = it })
                    }
                }

                item {
                    Divider()
                    Text(
                        "Data Security",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Encrypt Local Data", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Encrypt all stored information", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = encryptLocal, onCheckedChange = { encryptLocal = it })
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anonymize Data", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Remove identifying information", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = anonymizeData, onCheckedChange = { anonymizeData = it })
                    }
                }

                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Auto-Delete After", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("$autoDelete days", color = TrustBlue, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = autoDelete.toFloat(),
                            onValueChange = { autoDelete = it.toInt() },
                            valueRange = 7f..365f,
                            steps = 11
                        )
                        Text("Emergency records will be deleted after this period", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NotificationSettingsDialog(onDismiss: () -> Unit) {
    var smsEnabled by remember { mutableStateOf(true) }
    var callEnabled by remember { mutableStateOf(false) }
    var inAppEnabled by remember { mutableStateOf(true) }
    var emailEnabled by remember { mutableStateOf(false) }
    var escalationEnabled by remember { mutableStateOf(true) }
    var escalationDelay by remember { mutableStateOf(2) }
    var repeatUntilAck by remember { mutableStateOf(true) }
    var messageTone by remember { mutableStateOf(1) } // 0=Formal, 1=Urgent, 2=Discreet

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = AmberYellowDark,
                    modifier = Modifier.size(28.dp)
                )
                Text("Alert Channels")
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Text(
                        "Notification Channels",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SMS Message", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Send text messages to contacts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = smsEnabled, onCheckedChange = { smsEnabled = it })
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Phone Call", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Call contacts directly", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = callEnabled, onCheckedChange = { callEnabled = it })
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("In-App Notification", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Show notification in app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = inAppEnabled, onCheckedChange = { inAppEnabled = it })
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Email", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Send email alerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = emailEnabled, onCheckedChange = { emailEnabled = it })
                    }
                }

                item {
                    Divider()
                    Text(
                        "Message Tone",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = messageTone == 0,
                            onClick = { messageTone = 0 },
                            label = { Text("Formal") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = messageTone == 1,
                            onClick = { messageTone = 1 },
                            label = { Text("Urgent") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = messageTone == 2,
                            onClick = { messageTone = 2 },
                            label = { Text("Discreet") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Divider()
                    Text(
                        "Escalation Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Escalation", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Resend alerts if no response", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = escalationEnabled, onCheckedChange = { escalationEnabled = it })
                    }
                }

                if (escalationEnabled) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Escalation Delay", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                Text("$escalationDelay min", color = TrustBlue, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = escalationDelay.toFloat(),
                                onValueChange = { escalationDelay = it.toInt() },
                                valueRange = 1f..10f,
                                steps = 8
                            )
                            Text("Time before resending alerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Repeat Until Acknowledged", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Keep sending until contact responds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = repeatUntilAck, onCheckedChange = { repeatUntilAck = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HelpSupportDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Help,
                contentDescription = null,
                tint = TrustBlue,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("Help & Support", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Quick Guide
                Card(
                    colors = CardDefaults.cardColors(containerColor = TrustBlue.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Quick Start Guide",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("1. Add emergency contacts in Contacts tab", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("2. Load AI model in Settings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("3. Test the emergency button (don't send)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("4. Enable location permissions for alerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Divider()

                // FAQ
                Text(
                    "Common Questions",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Q: How does the emergency alarm work?", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "A: When triggered, it immediately sends SMS with your location to all emergency contacts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Q: Can I test without alerting contacts?", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "A: Currently, triggering the alarm will send real alerts. Future versions will have test mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Q: What if I have no internet?", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "A: SMS works without internet. The app will send last known location if GPS is unavailable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider()

                // Contact Support
                Surface(
                    color = SuccessGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Need More Help?",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Email: support@guardianai.app\nPhone: 1-800-GUARDIAN",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it!")
            }
        }
    )
}

// ... existing code (SectionHeader, SettingsItem, ModelSelectionDialog, AboutDialog) ...


@Composable
fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = CharcoalMedium,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = TrustBlue,
                modifier = Modifier.size(32.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Charcoal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = CharcoalMedium
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CharcoalLight,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ModelSelectionDialog(
    viewModel: SafetyViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var availableModels by remember { mutableStateOf<List<ModelInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        availableModels = listAvailableModels()
        isLoading = false
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select AI Model")
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (availableModels.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No models available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CharcoalMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Please ensure SDK is initialized",
                        style = MaterialTheme.typography.bodySmall,
                        color = CharcoalLight
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableModels) { model ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        viewModel.loadAIModel(model.id)
                                        onDismiss()
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (model.isDownloaded) 
                                    SuccessGreen.copy(alpha = 0.1f) 
                                else 
                                    Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    if (model.isDownloaded) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = if (model.isDownloaded) SuccessGreen else TrustBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        model.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Charcoal
                                    )
                                    Text(
                                        if (model.isDownloaded) "Downloaded" else "Not downloaded",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CharcoalMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = TrustBlue,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Guardian AI",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Version 1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    "Guardian AI is a personal safety application that uses AI to help protect you in emergency situations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Built with ❤️ for your safety",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    color = AmberYellow.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "⚠️ This app provides assistance but does not replace professional emergency services. Always call 911 in life-threatening situations.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
