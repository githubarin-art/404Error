package com.runanywhere.startup_hackathon20.ui.screens

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*
import com.runanywhere.startup_hackathon20.EmergencyContact
import com.runanywhere.startup_hackathon20.SafetyViewModel
import com.runanywhere.startup_hackathon20.ui.theme.*

enum class OnboardingStep {
    WELCOME,
    MODEL_INSTALLATION,
    PHONE_NUMBER,
    EMERGENCY_CONTACTS,
    LOCATION_PERMISSION,
    SMS_CALL_PERMISSION,
    COMPLETION
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    viewModel: SafetyViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var userPhoneNumber by remember { mutableStateOf("") }
    var emergencyContacts by remember { mutableStateOf(listOf<EmergencyContact>()) }

    val isModelLoaded by viewModel.isModelLoaded.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OffWhite,
                        LightGray,
                        MediumGray.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn() togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(300)
                        ) + fadeOut()
            },
            label = "onboarding_transition"
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    onContinue = {
                        currentStep = OnboardingStep.MODEL_INSTALLATION
                        viewModel.loadAIModel("Qwen 2.5 0.5B Instruct Q6_K")
                    }
                )

                OnboardingStep.MODEL_INSTALLATION -> ModelInstallationStep(
                    statusMessage = statusMessage,
                    isComplete = isModelLoaded,
                    onContinue = { currentStep = OnboardingStep.PHONE_NUMBER }
                )

                OnboardingStep.PHONE_NUMBER -> PhoneNumberStep(
                    phoneNumber = userPhoneNumber,
                    onPhoneNumberChange = { userPhoneNumber = it },
                    onContinue = { currentStep = OnboardingStep.EMERGENCY_CONTACTS }
                )

                OnboardingStep.EMERGENCY_CONTACTS -> EmergencyContactsStep(
                    contacts = emergencyContacts,
                    onContactsChange = { emergencyContacts = it },
                    onContinue = { currentStep = OnboardingStep.LOCATION_PERMISSION }
                )

                OnboardingStep.LOCATION_PERMISSION -> LocationPermissionStep(
                    onContinue = { currentStep = OnboardingStep.SMS_CALL_PERMISSION },
                    onSkip = { currentStep = OnboardingStep.SMS_CALL_PERMISSION }
                )

                OnboardingStep.SMS_CALL_PERMISSION -> SmsCallPermissionStep(
                    onContinue = { currentStep = OnboardingStep.COMPLETION },
                    onSkip = { currentStep = OnboardingStep.COMPLETION }
                )

                OnboardingStep.COMPLETION -> CompletionStep(
                    onFinish = {
                        // Save contacts to ViewModel
                        emergencyContacts.forEach { contact ->
                            viewModel.addEmergencyContact(contact)
                        }
                        onComplete()
                    }
                )
            }
        }

        // Progress Indicator at top with emergency theme
        if (currentStep != OnboardingStep.WELCOME && currentStep != OnboardingStep.COMPLETION) {
            LinearProgressIndicator(
                progress = { getProgressForStep(currentStep) },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = SafetyRed,
                trackColor = LightGray
            )
        }
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Icon/Logo with emergency theme
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(30.dp),
            color = SafetyRed,
            shadowElevation = 12.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "🛡️",
                    fontSize = 64.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "GUARDIAN AI",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SafetyRed,
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Emergency Response System",
            fontSize = 18.sp,
            color = Charcoal,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = AmberYellowLight
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, AmberYellow)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "To enable advanced safety features, we need to install the required AI model:",
                    fontSize = 16.sp,
                    color = Charcoal,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = TrustBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Qwen 2.5 0.5B 6K Model",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Charcoal
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Size: ~374 MB • On-device AI • 100% Private",
                    fontSize = 14.sp,
                    color = CharcoalMedium,
                    modifier = Modifier.padding(start = 36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SafetyRed
            )
        ) {
            Text(
                "CONTINUE",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "By continuing, you agree to our Terms & Privacy Policy",
            fontSize = 12.sp,
            color = CharcoalLight,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ModelInstallationStep(
    statusMessage: String,
    isComplete: Boolean,
    onContinue: () -> Unit
) {
    LaunchedEffect(isComplete) {
        if (isComplete) {
            kotlinx.coroutines.delay(1000) // Show success for 1 second
            onContinue()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!isComplete) {
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                strokeWidth = 6.dp,
                color = SafetyRed,
                trackColor = LightGray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "INSTALLING AI MODEL...",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Charcoal,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = statusMessage,
                fontSize = 16.sp,
                color = CharcoalMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TrustBlueLight.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TrustBlue)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💡 Did you know?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TrustBlueDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your AI model runs entirely on your device. No data is sent to the cloud, ensuring complete privacy.",
                        fontSize = 14.sp,
                        color = Charcoal
                    )
                }
            }
        } else {
            // Success animation
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "INSTALLATION COMPLETE!",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SuccessGreen,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AI model is ready for use",
                fontSize = 16.sp,
                color = CharcoalMedium
            )
        }
    }
}

@Composable
private fun PhoneNumberStep(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = null,
            tint = TrustBlue,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "YOUR PHONE NUMBER",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Charcoal,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Enter your mobile number for alerts and verification",
            fontSize = 16.sp,
            color = CharcoalMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone Number") },
            placeholder = { Text("+1 (555) 123-4567") },
            leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = null, tint = TrustBlue)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrustBlue,
                unfocusedBorderColor = MediumGray,
                focusedLabelColor = TrustBlue
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = phoneNumber.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TrustBlue,
                disabledContainerColor = MediumGray
            )
        ) {
            Text(
                "CONTINUE",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun EmergencyContactsStep(
    contacts: List<EmergencyContact>,
    onContactsChange: (List<EmergencyContact>) -> Unit,
    onContinue: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = SafetyRed,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "EMERGENCY CONTACTS",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Charcoal,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Add trusted contacts who will be notified in emergencies",
            fontSize = 16.sp,
            color = CharcoalMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Contacts list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            contacts.forEach { contact ->
                ContactCard(
                    contact = contact,
                    onRemove = {
                        onContactsChange(contacts.filter { it.id != contact.id })
                    }
                )
            }

            // Add contact button
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, TrustBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TrustBlue
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "ADD EMERGENCY CONTACT",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = contacts.isNotEmpty(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SafetyRed,
                disabledContainerColor = MediumGray
            )
        ) {
            Text(
                "CONTINUE",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }

    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { contact ->
                onContactsChange(contacts + contact)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ContactCard(contact: EmergencyContact, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LightGray
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MediumGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = TrustBlue
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = contact.name.first().uppercase(),
                        color = OffWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Charcoal
                )
                Text(
                    text = "${contact.phoneNumber} • ${contact.relationship}",
                    fontSize = 14.sp,
                    color = CharcoalMedium
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = SafetyRed
                )
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (EmergencyContact) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Family") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Emergency Contact") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship") },
                    placeholder = { Text("Family, Friend, etc.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onAdd(
                            EmergencyContact(
                                id = java.util.UUID.randomUUID().toString(),
                                name = name,
                                phoneNumber = phone,
                                relationship = relationship,
                                priority = 1
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LocationPermissionStep(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = SafetyRed,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "LOCATION ACCESS REQUIRED",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = SafetyRed,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Location permission is REQUIRED to send your GPS coordinates in emergency alerts. This is critical for emergency responders to find you quickly.",
            fontSize = 16.sp,
            color = CharcoalMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Show warning if permission not granted
        if (!locationPermissions.allPermissionsGranted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SafetyRed.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "⚠️ REQUIRED PERMISSION",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SafetyRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You must grant location permission to use this safety app. Without location, emergency contacts cannot find you.",
                        fontSize = 14.sp,
                        color = CharcoalMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = TrustBlueLight.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, TrustBlue)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                PermissionFeatureItem("📍", "Share location in emergency SMS")
                PermissionFeatureItem("🗺️", "Help responders find you quickly")
                PermissionFeatureItem("🔒", "Used only when SOS is activated")
                PermissionFeatureItem("✅", "Required for app to function")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Show grant button if permission not granted
        if (!locationPermissions.allPermissionsGranted) {
            Button(
                onClick = {
                    locationPermissions.launchMultiplePermissionRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafetyRed
                )
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("GRANT LOCATION PERMISSION", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Tap the button above and select \"Allow only while using the app\"",
                fontSize = 12.sp,
                color = CharcoalLight,
                textAlign = TextAlign.Center
            )
        } else {
            // Permission granted - show success and continue
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SuccessGreen
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("CONTINUE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Auto-continue when permission granted
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            kotlinx.coroutines.delay(500)
            onContinue()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun SmsCallPermissionStep(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val smsCallPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = SafetyRed,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "SMS & CALL ACCESS",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = SafetyRed,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Grant SMS and Call access to send automatic alerts if you trigger SOS or face a threat situation.",
            fontSize = 16.sp,
            color = CharcoalMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = TrustBlueLight.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, TrustBlue)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                PermissionFeatureItem("💬", "Send emergency SMS to your contacts")
                PermissionFeatureItem("📞", "Make automated emergency calls")
                PermissionFeatureItem("🎙️", "Record audio evidence during emergencies")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (smsCallPermissions.allPermissionsGranted) {
                    onContinue()
                } else {
                    smsCallPermissions.launchMultiplePermissionRequest()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TrustBlue
            )
        ) {
            Text(
                text = if (smsCallPermissions.allPermissionsGranted) "CONTINUE" else "ALLOW ACCESS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SKIP FOR NOW")
        }
    }

    LaunchedEffect(smsCallPermissions.allPermissionsGranted) {
        if (smsCallPermissions.allPermissionsGranted) {
            kotlinx.coroutines.delay(500)
            onContinue()
        }
    }
}

@Composable
private fun CompletionStep(onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success animation
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "ALL SET!",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SuccessGreen,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Great! Your app will now send automated real-time alerts to selected contacts in case of emergency.",
            fontSize = 16.sp,
            color = CharcoalMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = SafetyRed.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SafetyRed)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💡 QUICK TIP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SafetyRed
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You can update contacts or permissions anytime in Settings",
                    fontSize = 14.sp,
                    color = CharcoalMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SuccessGreen
            )
        ) {
            Text("FINISH SETUP", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PermissionFeatureItem(icon: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            fontSize = 15.sp
        )
    }
}

private fun getProgressForStep(step: OnboardingStep): Float {
    return when (step) {
        OnboardingStep.WELCOME -> 0f
        OnboardingStep.MODEL_INSTALLATION -> 0.17f
        OnboardingStep.PHONE_NUMBER -> 0.33f
        OnboardingStep.EMERGENCY_CONTACTS -> 0.50f
        OnboardingStep.LOCATION_PERMISSION -> 0.67f
        OnboardingStep.SMS_CALL_PERMISSION -> 0.83f
        OnboardingStep.COMPLETION -> 1f
    }
}
