package com.runanywhere.startup_hackathon20

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.listAvailableModels
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.runanywhere.startup_hackathon20.utils.ShakeDetector

/**
 * Main ViewModel for Safety App
 * Manages emergency sessions, AI decisions, and communication with services
 */
class SafetyViewModel(private val context: Context) : ViewModel() {
    // FusedLocationProviderClient for location tracking
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "SafetyViewModel"
        private const val PREFS_NAME = "SafetyAppPrefs"
        private const val KEY_EMERGENCY_CONTACTS = "emergencyContacts"
        private const val KEY_SHAKE_ENABLED = "shakeGestureEnabled"
        private const val KEY_LAST_LATITUDE = "lastLatitude"
        private const val KEY_LAST_LONGITUDE = "lastLongitude"
        private const val KEY_LAST_ACCURACY = "lastAccuracy"
        private const val KEY_LAST_LOCATION_TIME = "lastLocationTime"
    }

    private val aiEngine = SafetyAIEngine()

    // Shake detector
    private var shakeDetector: ShakeDetector? = null
    private val _isShakeEnabled = MutableStateFlow(false)
    val isShakeEnabled: StateFlow<Boolean> = _isShakeEnabled.asStateFlow()
    val sosShakeGestureEnabled: StateFlow<Boolean> = _isShakeEnabled.asStateFlow()

    // Emergency session state
    private val _currentSession = MutableStateFlow<EmergencySession?>(null)
    val currentSession: StateFlow<EmergencySession?> = _currentSession.asStateFlow()

    // Current protocol question
    private val _currentQuestion = MutableStateFlow<ProtocolQuestion?>(null)
    val currentQuestion: StateFlow<ProtocolQuestion?> = _currentQuestion.asStateFlow()

    // Question timer countdown
    private val _questionTimeRemaining = MutableStateFlow<Int?>(null)
    val questionTimeRemaining: StateFlow<Int?> = _questionTimeRemaining.asStateFlow()

    // Emergency contacts
    private val _emergencyContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val emergencyContacts: StateFlow<List<EmergencyContact>> = _emergencyContacts.asStateFlow()

    // Current location
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    // Status message
    private val _statusMessage = MutableStateFlow("Ready. Stay safe.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Model loading state
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    // Alert history
    private val _alertHistory = MutableStateFlow<List<AlertRecord>>(emptyList())
    val alertHistory: StateFlow<List<AlertRecord>> = _alertHistory.asStateFlow()

    // Alarm active state
    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

    private var questionTimerJob: Job? = null
    private var escalationMonitorJob: Job? = null
    private var autoRetriggerJob: Job? = null

    init {
        // Load emergency contacts from storage
        loadEmergencyContacts()

        // Check location availability on startup
        checkLocationAvailability()

        // Initialize shake detector
        initializeShakeDetector()

        // Load shake gesture preference
        loadShakeGesturePreference()
    }

    /**
     * Initialize shake detector
     */
    private fun initializeShakeDetector() {
        shakeDetector = ShakeDetector(context) {
            // Shake detected - trigger emergency alarm
            Log.i(TAG, "🔔 Shake gesture detected - triggering emergency alarm")
            triggerEmergencyAlarm()
        }

        if (!shakeDetector!!.isSupported()) {
            Log.w(TAG, "⚠️ Shake detection not supported on this device")
        }
    }

    /**
     * Enable or disable shake gesture for emergency activation
     */
    fun setShakeGestureEnabled(enabled: Boolean) {
        _isShakeEnabled.value = enabled

        if (enabled) {
            shakeDetector?.start()
            Log.i(TAG, "✅ Shake gesture ENABLED - shake phone to trigger emergency")
        } else {
            shakeDetector?.stop()
            Log.i(TAG, "🛑 Shake gesture DISABLED")
        }

        // Save preference
        saveShakeGesturePreference(enabled)
    }

    fun setSOSShakeGestureEnabled(enabled: Boolean) {
        setShakeGestureEnabled(enabled)
    }

    /**
     * Load shake gesture preference from storage
     */
    private fun loadShakeGesturePreference() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_SHAKE_ENABLED, false)

        if (enabled) {
            setShakeGestureEnabled(true)
        }

        Log.i(TAG, "Shake gesture preference loaded: ${if (enabled) "ENABLED" else "DISABLED"}")
    }

    /**
     * Save shake gesture preference to storage
     */
    private fun saveShakeGesturePreference(enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHAKE_ENABLED, enabled).apply()
        Log.i(TAG, "💾 Shake gesture preference saved: ${if (enabled) "ENABLED" else "DISABLED"}")
    }

    /**
     * MAIN ACTION: User triggers emergency alarm
     * IMPORTANT: Only uses contacts that the user added during onboarding.
     * No sample or dummy data is used for actual calls and messages.
     *
     * FLOW:
     * 1. Validate model is loaded and contacts exist
     * 2. Create emergency session
     * 3. Start location monitoring IMMEDIATELY
     * 4. IMMEDIATELY send SMS to ALL contacts with location (BEFORE questions)
     * 5. Present protocol questions to assess threat level
     * 6. AI makes decisions based on responses
     * 7. Continue monitoring and escalating as needed
     */
    fun triggerEmergencyAlarm() {
        viewModelScope.launch {
            try {
                if (!_isModelLoaded.value) {
                    _statusMessage.value = "Please load AI model first in settings"
                    return@launch
                }

                // Check if user has added emergency contacts
                if (_emergencyContacts.value.isEmpty()) {
                    _statusMessage.value = "⚠️ Please add emergency contacts first!"
                    Log.w(TAG, "Cannot trigger emergency - no contacts added")
                    return@launch
                }

                if (_isAlarmActive.value) {
                    // Alarm already active - re-send emergency alerts
                    Log.i(TAG, "Alarm already active - re-sending emergency alerts")
                    _statusMessage.value = "🚨 Re-sending emergency alerts"
                    sendImmediateEmergencyAlerts()
                    return@launch
                }

                _isAlarmActive.value = true
                _statusMessage.value = "🚨 Emergency alarm activated"

                Log.i(TAG, "========================================")
                Log.i(TAG, "EMERGENCY TRIGGERED - REAL CONTACTS ONLY")
                Log.i(
                    TAG,
                    "Emergency triggered with ${_emergencyContacts.value.size} user-added contacts:"
                )
                _emergencyContacts.value.forEach { contact ->
                    Log.i(
                        TAG,
                        "  → ${contact.name}: ${contact.phoneNumber} (${contact.relationship})"
                    )
                }
                Log.i(TAG, "========================================")

                // Create new emergency session
                val session = EmergencySession(
                    sessionId = UUID.randomUUID().toString(),
                    startTime = System.currentTimeMillis(),
                    alarmTriggeredTime = System.currentTimeMillis(),
                    currentThreatLevel = ThreatLevel.UNKNOWN,
                    location = _currentLocation.value
                )
                _currentSession.value = session

                Log.i(TAG, "Emergency session started: ${session.sessionId}")

                // IMMEDIATELY start location monitoring - don't wait!
                Log.i(TAG, "🚀 Starting location fetch IMMEDIATELY...")
                startLocationMonitoring()

                // Give location a very short time to initialize (only 1 second)
                // The sendImmediateEmergencyAlerts has its own retry logic for location
                delay(1000)

                // Check if we got location quickly
                val quickLocation = _currentLocation.value
                if (quickLocation != null) {
                    Log.i(TAG, "⚡ Location obtained quickly!")
                    Log.i(TAG, "   Location: ${quickLocation.latitude}, ${quickLocation.longitude}")
                } else {
                    Log.i(TAG, "⏱️ Location still loading, will retry during SMS send...")
                }

                // IMMEDIATELY send emergency SMS to all contacts
                // This function will wait for location if needed (up to 3 more seconds)
                sendImmediateEmergencyAlerts()

                // Generate and present protocol question
                presentProtocolQuestion()

                // Start escalation monitoring
                startEscalationMonitoring()

            } catch (e: Exception) {
                Log.e(TAG, "Error triggering alarm", e)
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * Send immediate emergency alerts to all contacts as soon as alarm is triggered
     * This happens BEFORE protocol questions to ensure contacts are notified immediately
     */
    private suspend fun sendImmediateEmergencyAlerts() {
        try {
            _statusMessage.value = "🚨 Sending emergency alerts to all contacts..."

            Log.i(TAG, "========================================")
            Log.i(TAG, "SENDING IMMEDIATE EMERGENCY ALERTS")
            Log.i(TAG, "========================================")

            val session = _currentSession.value ?: return
            val alertRecords = mutableListOf<AlertRecord>()

            // Get current location - wait a bit if needed
            var location = _currentLocation.value
            if (location == null) {
                Log.i(TAG, "⏱️ Location not available yet, waiting up to 3 seconds...")
                var retries = 0
                while (location == null && retries < 6) { // 6 retries * 500ms = 3 seconds max
                    delay(500)
                    location = _currentLocation.value
                    retries++
                    Log.i(
                        TAG,
                        "  Retry $retries/6: ${if (location != null) "Got location!" else "Still waiting..."}"
                    )
                }
            }

            // If still no location, try cached last known
            if (location == null) {
                location = getCachedLastLocation()
                if (location != null) {
                    Log.i(TAG, "✅ Using cached last known location for emergency SMS")
                }
            }

            // Log final location status
            if (location != null) {
                Log.i(TAG, "✅ Location available for emergency SMS!")
                Log.i(TAG, "   Location: ${location.latitude}, ${location.longitude}")
                Log.i(TAG, "   Accuracy: ${location.accuracy}m")
            } else {
                Log.w(TAG, "⚠️ No location available - sending SMS without location")
                Log.w(TAG, "Possible reasons:")
                Log.w(TAG, "  1. Location services disabled on device")
                Log.w(TAG, "  2. GPS signal not available (indoors)")
                Log.w(TAG, "  3. Location permission not granted")
            }

            // Compose emergency message with location (if available)
            val emergencyMessage = buildEmergencyMessage(location)

            Log.i(TAG, "Emergency Message Content:")
            Log.i(TAG, emergencyMessage)
            Log.i(TAG, "----------------------------------------")

            // Send SMS to ALL emergency contacts immediately
            _emergencyContacts.value.forEach { contact ->
                Log.i(TAG, "Sending emergency SMS to: ${contact.name} (${contact.phoneNumber})")

                val success = sendSMS(
                    contact,
                    emergencyMessage,
                    appendLocation = false
                ) // Location already in message

                alertRecords.add(
                    AlertRecord(
                        timestamp = System.currentTimeMillis(),
                        recipientType = mapRelationshipToType(contact.relationship),
                        recipientName = contact.name,
                        recipientPhone = contact.phoneNumber,
                        messageType = MessageType.SMS,
                        success = success
                    )
                )

                if (success) {
                    Log.i(TAG, "✅ Emergency SMS sent successfully to ${contact.name}")
                } else {
                    Log.e(TAG, "❌ Failed to send emergency SMS to ${contact.name}")
                }

                // Small delay between messages to avoid carrier throttling
                delay(500)
            }

            // Update session with alert records
            val updatedSession = session.copy(
                alertsSent = session.alertsSent + alertRecords
            )
            _currentSession.value = updatedSession
            _alertHistory.value = updatedSession.alertsSent

            val successCount = alertRecords.count { it.success }
            val totalCount = alertRecords.size

            Log.i(TAG, "========================================")
            Log.i(TAG, "EMERGENCY ALERTS SENT: $successCount/$totalCount successful")
            Log.i(TAG, "========================================")

            _statusMessage.value = "✅ Emergency alerts sent ($successCount/$totalCount)"

        } catch (e: Exception) {
            Log.e(TAG, "Error sending immediate emergency alerts", e)
            _statusMessage.value = "⚠️ Error sending alerts: ${e.message}"
        }
    }

    /**
     * Build the emergency message with location and threat details
     */
    private fun buildEmergencyMessage(location: Location? = null): String {
        // Use provided location or fall back to current location
        val finalLocation = location ?: _currentLocation.value

        val timestamp = java.text.SimpleDateFormat(
            "MMM dd, yyyy 'at' hh:mm a",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        val message = StringBuilder()
        message.append("🚨 EMERGENCY ALERT 🚨\n\n")
        message.append("I need immediate help! I've triggered my emergency alarm.\n\n")
        message.append("Time: $timestamp\n\n")

        if (finalLocation != null) {
            message.append("📍 MY LOCATION:\n")
            message.append("Latitude: ${finalLocation.latitude}\n")
            message.append("Longitude: ${finalLocation.longitude}\n")
            message.append("Accuracy: ${finalLocation.accuracy}m\n\n")
            message.append("🗺️ Open in Maps:\n")
            message.append("https://maps.google.com/?q=${finalLocation.latitude},${finalLocation.longitude}\n\n")
            message.append("Please come to my location or call emergency services if needed.\n")
        } else {
            message.append("⚠️ Location unavailable - Please try calling me!\n\n")
            message.append("If I don't respond, please contact emergency services.\n")
        }

        message.append("\n- Sent via Guardian AI Safety App")

        return message.toString()
    }

    /**
     * Present protocol question to assess threat level
     */
    private suspend fun presentProtocolQuestion() {
        try {
            _statusMessage.value = "Generating safety question..."

            // AI generates appropriate question
            val question = aiEngine.generateProtocolQuestion()
            _currentQuestion.value = question

            _statusMessage.value = "Please answer the question below"

            // Start countdown timer
            startQuestionTimer(question.timeoutSeconds)

        } catch (e: Exception) {
            Log.e(TAG, "Error presenting question", e)
            // Fallback: assume no response possible = high threat
            handleQuestionTimeout()
        }
    }

    /**
     * Start countdown timer for protocol question
     */
    private fun startQuestionTimer(seconds: Int) {
        questionTimerJob?.cancel()
        questionTimerJob = viewModelScope.launch {
            var remaining = seconds
            _questionTimeRemaining.value = remaining

            while (remaining > 0) {
                delay(1000)
                remaining--
                _questionTimeRemaining.value = remaining
            }

            // Time's up - no response
            handleQuestionTimeout()
        }
    }

    /**
     * User answered YES to protocol question
     */
    fun answerProtocolQuestionYes() {
        viewModelScope.launch {
            val question = _currentQuestion.value ?: return@launch
            val session = _currentSession.value ?: return@launch

            questionTimerJob?.cancel()
            val responseTime = (question.timeoutSeconds - (_questionTimeRemaining.value ?: 0))

            // Record response
            val response = VictimResponse(
                questionId = question.id,
                answered = true,
                responseTime = System.currentTimeMillis(),
                timeTakenSeconds = responseTime
            )

            // Update session
            val updatedSession = session.copy(
                victimResponses = session.victimResponses + response
            )
            _currentSession.value = updatedSession

            // Assess threat level
            val threatLevel = aiEngine.assessThreatLevel(question, true, responseTime)
            updateThreatLevel(threatLevel)

            _statusMessage.value = "Response recorded. Threat level: $threatLevel"

            // Clear question
            _currentQuestion.value = null
            _questionTimeRemaining.value = null

            // AI decides what to do
            makeAIDecision()
        }
    }

    /**
     * User answered NO to protocol question (or unable to respond)
     */
    fun answerProtocolQuestionNo() {
        viewModelScope.launch {
            val question = _currentQuestion.value ?: return@launch
            val session = _currentSession.value ?: return@launch

            questionTimerJob?.cancel()

            // Record response
            val response = VictimResponse(
                questionId = question.id,
                answered = false,
                responseTime = System.currentTimeMillis(),
                timeTakenSeconds = question.timeoutSeconds
            )

            val updatedSession = session.copy(
                victimResponses = session.victimResponses + response
            )
            _currentSession.value = updatedSession

            // No/timeout = high threat
            updateThreatLevel(ThreatLevel.HIGH)

            _statusMessage.value = "High threat detected. Alerting contacts..."

            _currentQuestion.value = null
            _questionTimeRemaining.value = null

            // AI decides emergency actions
            makeAIDecision()
        }
    }

    /**
     * Handle question timeout (no answer)
     */
    private fun handleQuestionTimeout() {
        viewModelScope.launch {
            Log.w(TAG, "Protocol question timeout - assuming high threat")

            val question = _currentQuestion.value
            if (question == null) {
                Log.e(TAG, "No question to timeout")
                return@launch
            }

            answerProtocolQuestionNo()
        }
    }

    /**
     * Update threat level and trigger appropriate actions
     */
    private fun updateThreatLevel(newLevel: ThreatLevel) {
        val session = _currentSession.value ?: return

        val updatedSession = session.copy(
            currentThreatLevel = newLevel
        )
        _currentSession.value = updatedSession

        Log.i(TAG, "Threat level updated: $newLevel")
    }

    /**
     * AI makes decision on what actions to take
     */
    private suspend fun makeAIDecision() {
        try {
            val session = _currentSession.value ?: return

            _statusMessage.value = "AI analyzing situation..."

            // Build context for AI
            val context = AIDecisionContext(
                threatLevel = session.currentThreatLevel,
                victimResponded = session.victimResponses.lastOrNull()?.answered ?: false,
                timeSinceAlarm = (System.currentTimeMillis() - session.alarmTriggeredTime) / 1000,
                location = _currentLocation.value,
                previousAlerts = session.alertsSent,
                availableContacts = _emergencyContacts.value
            )

            // AI decides actions
            val decision = aiEngine.decideEmergencyActions(context)

            Log.i(TAG, "AI Decision: ${decision.reasoning}")
            Log.i(TAG, "Urgency Score: ${decision.urgencyScore}/10")

            _statusMessage.value = "Executing ${decision.recommendedActions.size} actions..."

            // Execute actions
            executeEmergencyActions(decision.recommendedActions)

        } catch (e: Exception) {
            Log.e(TAG, "Error in AI decision", e)
            _statusMessage.value = "Error making decision: ${e.message}"
        }
    }

    /**
     * Execute emergency actions (SMS, calls, etc.)
     */
    private suspend fun executeEmergencyActions(actions: List<EmergencyAction>) {
        val session = _currentSession.value ?: return
        val alertRecords = mutableListOf<AlertRecord>()

        for (action in actions) {
            when (action) {
                is EmergencyAction.SendSMS -> {
                    val success = sendSMS(action.contact, action.message, appendLocation = true)
                    alertRecords.add(
                        AlertRecord(
                            timestamp = System.currentTimeMillis(),
                            recipientType = mapRelationshipToType(action.contact.relationship),
                            recipientName = action.contact.name,
                            recipientPhone = action.contact.phoneNumber,
                            messageType = MessageType.SMS,
                            success = success
                        )
                    )
                }

                is EmergencyAction.MakeCall -> {
                    val success = makeCall(action.contact)
                    alertRecords.add(
                        AlertRecord(
                            timestamp = System.currentTimeMillis(),
                            recipientType = mapRelationshipToType(action.contact.relationship),
                            recipientName = action.contact.name,
                            recipientPhone = action.contact.phoneNumber,
                            messageType = MessageType.CALL,
                            success = success
                        )
                    )
                }

                is EmergencyAction.MakeMissedCall -> {
                    val success = makeMissedCall(action.contact)
                    alertRecords.add(
                        AlertRecord(
                            timestamp = System.currentTimeMillis(),
                            recipientType = mapRelationshipToType(action.contact.relationship),
                            recipientName = action.contact.name,
                            recipientPhone = action.contact.phoneNumber,
                            messageType = MessageType.MISSED_CALL,
                            success = success
                        )
                    )
                }

                is EmergencyAction.CallEmergencyServices -> {
                    val success = callEmergencyServices(action.serviceType, action.location)
                    alertRecords.add(
                        AlertRecord(
                            timestamp = System.currentTimeMillis(),
                            recipientType = RecipientType.EMERGENCY_SERVICES,
                            recipientName = action.serviceType,
                            recipientPhone = null,
                            messageType = MessageType.EMERGENCY_CALL,
                            success = success
                        )
                    )
                }

                is EmergencyAction.UpdateThreatLevel -> {
                    updateThreatLevel(action.newLevel)
                }
            }
        }

        // Update session with alert records
        val updatedSession = session.copy(
            alertsSent = session.alertsSent + alertRecords
        )
        _currentSession.value = updatedSession
        _alertHistory.value = updatedSession.alertsSent

        val successCount = alertRecords.count { it.success }
        _statusMessage.value = "Sent $successCount/${alertRecords.size} alerts successfully"
    }

    /**
     * Monitor for threat escalation over time
     */
    private fun startEscalationMonitoring() {
        escalationMonitorJob?.cancel()
        escalationMonitorJob = viewModelScope.launch {
            while (_isAlarmActive.value) {
                delay(30000) // Check every 30 seconds

                val session = _currentSession.value ?: continue
                val timeSinceAlarm =
                    (System.currentTimeMillis() - session.alarmTriggeredTime) / 1000

                val newThreatLevel = aiEngine.shouldEscalateThreatLevel(
                    session.currentThreatLevel,
                    timeSinceAlarm
                )

                if (newThreatLevel != session.currentThreatLevel) {
                    Log.w(TAG, "Threat escalated: ${session.currentThreatLevel} -> $newThreatLevel")
                    updateThreatLevel(newThreatLevel)
                    makeAIDecision() // Re-evaluate actions with new threat level
                }
            }
        }

        // Start auto re-trigger for high threat
        startAutoRetriggerMonitoring()
    }

    /**
     * Automatically re-send emergency alerts every 5 minutes if threat is HIGH
     */
    private fun startAutoRetriggerMonitoring() {
        autoRetriggerJob?.cancel()
        autoRetriggerJob = viewModelScope.launch {
            while (_isAlarmActive.value) {
                delay(300000) // 5 minutes

                val session = _currentSession.value ?: continue

                if (session.currentThreatLevel == ThreatLevel.HIGH) {
                    Log.i(TAG, "🚨 HIGH THREAT: Auto re-sending emergency alerts")
                    _statusMessage.value = "🚨 Auto re-sending alerts (high threat)"
                    sendImmediateEmergencyAlerts()
                }
            }
        }
    }

    /**
     * Cancel emergency alarm (false alarm)
     */
    fun cancelEmergencyAlarm() {
        viewModelScope.launch {
            _isAlarmActive.value = false
            questionTimerJob?.cancel()
            escalationMonitorJob?.cancel()
            autoRetriggerJob?.cancel()

            val session = _currentSession.value
            if (session != null) {
                val updatedSession = session.copy(isActive = false)
                _currentSession.value = updatedSession

                // Notify contacts that alarm was cancelled
                sendCancellationNotifications()
            }

            _statusMessage.value = "Emergency alarm cancelled"
            _currentQuestion.value = null
            _questionTimeRemaining.value = null

            Log.i(TAG, "Emergency session ended")
        }
    }

    /**
     * Enter stealth mode - UI returns to normal while emergency monitoring continues
     * This is triggered when user presses back/home button during emergency
     * Emergency alerts have already been sent, now we just hide the UI from attacker
     */
    fun enterStealthMode() {
        // Don't cancel the emergency, just clear the UI elements
        _currentQuestion.value = null
        _questionTimeRemaining.value = null
        questionTimerJob?.cancel()
        
        Log.i(TAG, "🕶️ STEALTH MODE ACTIVATED")
        Log.i(TAG, "Emergency continues in background, but UI appears normal")
        Log.i(TAG, "Contacts have been alerted and will receive updates")
        
        // Emergency session and monitoring continues in background
        // The UI will show the normal 404 screen as a decoy
    }

    /**
     * Send notifications that alarm was cancelled (false alarm)
     */
    private suspend fun sendCancellationNotifications() {
        val session = _currentSession.value ?: return

        // Send SMS to contacts who were alerted
        session.alertsSent
            .filter { it.success && it.messageType != MessageType.EMERGENCY_CALL }
            .distinctBy { it.recipientPhone }
            .forEach { alert ->
                alert.recipientPhone?.let { phone ->
                    // Send cancellation SMS
                    sendSMS(
                        EmergencyContact(
                            id = "",
                            name = alert.recipientName,
                            phoneNumber = phone,
                            relationship = "",
                            priority = 1
                        ),
                        "False alarm. I'm safe now. Sorry for the concern."
                    )
                }
            }
    }

    // ============ Contact Management ============

    fun addEmergencyContact(contact: EmergencyContact) {
        val currentContacts = _emergencyContacts.value
        _emergencyContacts.value = currentContacts + contact
        saveEmergencyContacts()
        _statusMessage.value = "Contact added: ${contact.name}"
        Log.i(TAG, "✅ Emergency contact added: ${contact.name} - ${contact.phoneNumber}")
    }

    fun removeEmergencyContact(contactId: String) {
        _emergencyContacts.value = _emergencyContacts.value.filter { it.id != contactId }
        saveEmergencyContacts()
        _statusMessage.value = "Contact removed"
    }

    fun updateEmergencyContact(contact: EmergencyContact) {
        _emergencyContacts.value = _emergencyContacts.value.map {
            if (it.id == contact.id) contact else it
        }
        saveEmergencyContacts()
    }

    private fun loadEmergencyContacts() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val contactsJson = prefs.getString(KEY_EMERGENCY_CONTACTS, null)

        if (contactsJson != null) {
            try {
                val jsonArray = JSONArray(contactsJson)
                val contacts = mutableListOf<EmergencyContact>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    contacts.add(
                        EmergencyContact(
                            id = jsonObject.getString("id"),
                            name = jsonObject.getString("name"),
                            phoneNumber = jsonObject.getString("phoneNumber"),
                            relationship = jsonObject.getString("relationship"),
                            priority = jsonObject.getInt("priority")
                        )
                    )
                }

                _emergencyContacts.value = contacts
                Log.i(TAG, "✅ Loaded ${contacts.size} user-added emergency contacts from storage")
                contacts.forEach { contact ->
                    Log.i(TAG, "  → ${contact.name}: ${contact.phoneNumber}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading emergency contacts", e)
                _emergencyContacts.value = emptyList()
            }
        } else {
            _emergencyContacts.value = emptyList()
            Log.i(TAG, "No saved contacts found. User must add contacts through onboarding.")
        }
    }

    private fun saveEmergencyContacts() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val jsonArray = JSONArray()
        _emergencyContacts.value.forEach { contact ->
            val jsonObject = JSONObject()
            jsonObject.put("id", contact.id)
            jsonObject.put("name", contact.name)
            jsonObject.put("phoneNumber", contact.phoneNumber)
            jsonObject.put("relationship", contact.relationship)
            jsonObject.put("priority", contact.priority)
            jsonArray.put(jsonObject)
        }

        editor.putString(KEY_EMERGENCY_CONTACTS, jsonArray.toString())
        editor.apply()

        Log.i(
            TAG,
            "💾 Saved ${_emergencyContacts.value.size} emergency contacts to persistent storage"
        )
        _emergencyContacts.value.forEach { contact ->
            Log.i(TAG, "  → ${contact.name}: ${contact.phoneNumber}")
        }
    }

    // ============ Location Management ============

    private fun startLocationMonitoring() {
        // Check location permission
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        Log.i(TAG, "📍 Starting location monitoring...")
        Log.i(TAG, "Fine location: ${if (hasFine) "✅ Granted" else "❌ Denied"}")
        Log.i(TAG, "Coarse location: ${if (hasCoarse) "✅ Granted" else "❌ Denied"}")

        if (!hasFine && !hasCoarse) {
            Log.w(TAG, "⚠️ Location permission not granted.")
            Log.w(TAG, "Emergency alert will be sent without location.")
            Log.w(TAG, "Note: Android requires explicit user consent for location access.")
            _statusMessage.value = "Sending emergency alert (location not available)"

            // Still try to get location from Android system LocationManager as fallback
            tryGetLocationWithoutPermission()
            return
        }

        try {
            // Strategy 1: Try to get last known location (fastest, works even with coarse)
            if (hasCoarse || hasFine) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val age = (System.currentTimeMillis() - location.time) / 1000
                        Log.i(
                            TAG,
                            "✅ Got last known location: ${location.latitude}, ${location.longitude}"
                        )
                        Log.i(TAG, "   Accuracy: ${location.accuracy}m, Age: ${age}s old")

                        // Use last known location if it's recent (within 5 minutes)
                        if (age < 300) {
                            Log.i(TAG, "✅ Last known location is recent enough, using it")
                            updateLocation(location)
                        } else {
                            Log.w(
                                TAG,
                                "⚠️ Last known location is old (${age}s), will try fresh location"
                            )
                        }
                    } else {
                        Log.w(TAG, "⚠️ No last known location available")
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to get last known location: ${e.message}")
                }
            }

            // Strategy 2: Request current location with appropriate priority
            val cancellationTokenSource = CancellationTokenSource()
            val priority = if (hasFine) {
                Log.i(TAG, "Using HIGH_ACCURACY location (GPS + Network)")
                Priority.PRIORITY_HIGH_ACCURACY
            } else {
                Log.i(TAG, "Using BALANCED_POWER_ACCURACY (Network-based)")
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }

            fusedLocationClient.getCurrentLocation(
                priority,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.i(
                        TAG,
                        "✅ Got current location: ${location.latitude}, ${location.longitude}"
                    )
                    Log.i(TAG, "   Accuracy: ${location.accuracy}m, Provider: ${location.provider}")
                    updateLocation(location)
                } else {
                    Log.w(TAG, "⚠️ Current location returned null")
                    Log.w(TAG, "Possible reasons:")
                    Log.w(TAG, "  1. GPS/Location services disabled in device settings")
                    Log.w(TAG, "  2. Device is indoors with poor GPS/network signal")
                    Log.w(TAG, "  3. Location fetch timeout")
                    Log.w(TAG, "Emergency alert will be sent without location")
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "❌ Error fetching current location: ${e.message}", e)
                Log.e(TAG, "Emergency alert will be sent without location")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Security exception getting location: ${e.message}", e)
            Log.e(TAG, "This shouldn't happen as we checked permissions")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error getting location: ${e.message}", e)
        }
    }

    /**
     * Fallback method to try getting location without explicit permission
     * This uses passive location providers that may be available
     */
    private fun tryGetLocationWithoutPermission() {
        try {
            Log.i(TAG, "Attempting fallback location methods...")

            // Try using Android's LocationManager for passive location
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager

            if (locationManager != null) {
                // Check if any provider is enabled
                val gpsEnabled =
                    locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                val networkEnabled =
                    locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)

                Log.i(TAG, "Device Location Services:")
                Log.i(TAG, "  GPS Provider: ${if (gpsEnabled) "Enabled" else "Disabled"}")
                Log.i(TAG, "  Network Provider: ${if (networkEnabled) "Enabled" else "Disabled"}")

                if (!gpsEnabled && !networkEnabled) {
                    Log.w(TAG, "⚠️ All location providers are disabled on device")
                    Log.w(TAG, "User needs to enable Location in Device Settings")
                }
            }

            Log.i(TAG, "Emergency alert will proceed without location")
            Log.i(
                TAG,
                "To include location: Grant location permission during onboarding or in app settings"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error in fallback location method: ${e.message}")
        }
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = location

        val session = _currentSession.value
        if (session != null) {
            _currentSession.value = session.copy(location = location)
        }

        // Cache location in SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat(KEY_LAST_LATITUDE, location.latitude.toFloat())
            putFloat(KEY_LAST_LONGITUDE, location.longitude.toFloat())
            putFloat(KEY_LAST_ACCURACY, location.accuracy)
            putLong(KEY_LAST_LOCATION_TIME, location.time)
            apply()
        }
        Log.i(TAG, "💾 Cached last known location: ${location.latitude}, ${location.longitude}")
    }

    /**
     * Get cached last known location from SharedPreferences
     * Returns null if no cached location or if too old (>1 hour)
     */
    private fun getCachedLastLocation(): Location? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val latitude = prefs.getFloat(KEY_LAST_LATITUDE, 0f).toDouble()
        val longitude = prefs.getFloat(KEY_LAST_LONGITUDE, 0f).toDouble()
        val accuracy = prefs.getFloat(KEY_LAST_ACCURACY, 0f)
        val time = prefs.getLong(KEY_LAST_LOCATION_TIME, 0L)

        if (time == 0L) {
            Log.w(TAG, "No cached location available")
            return null
        }

        val age = (System.currentTimeMillis() - time) / 1000 / 60 // minutes
        if (age > 60) { // 1 hour max age for cached location
            Log.w(TAG, "Cached location too old (${age} minutes) - discarding")
            return null
        }

        val location = Location("cached")
        location.latitude = latitude
        location.longitude = longitude
        location.accuracy = accuracy
        location.time = time

        Log.i(TAG, "Using cached location (age: ${age} minutes)")
        return location
    }

    // ============ Model Management ============

    fun loadAIModel(modelId: String) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Initializing SDK..."
                
                // Wait for SDK to be fully initialized (give it time)
                var retryCount = 0
                var availableModels = listAvailableModels()
                
                while (availableModels.isEmpty() && retryCount < 10) {
                    delay(1000) // Wait 1 second
                    availableModels = listAvailableModels()
                    retryCount++
                    _statusMessage.value = "Waiting for SDK initialization... ($retryCount/10)"
                }
                
                if (availableModels.isEmpty()) {
                    _statusMessage.value = "SDK not initialized. Please restart the app."
                    Log.e(TAG, "No models available after waiting 10 seconds")
                    return@launch
                }
                
                _statusMessage.value = "Checking available models..."
                Log.i(TAG, "Available models: ${availableModels.joinToString { it.name }}")
                
                // Try to find the model by name or use the first available model
                var model = availableModels.find { it.name == modelId || it.id == modelId }
                
                if (model == null) {
                    Log.w(TAG, "Model '$modelId' not found, using first available model")
                    model = availableModels.firstOrNull()
                    
                    if (model == null) {
                        _statusMessage.value = "No models available"
                        return@launch
                    }
                }
                
                Log.i(TAG, "Using model: ${model.name}")
                
                // Check if model is downloaded
                if (!model.isDownloaded) {
                    _statusMessage.value = "Downloading ${model.name}... (this may take a few minutes)"
                    Log.i(TAG, "Downloading model: ${model.name}")

                    try {
                        RunAnywhere.downloadModel(model.id).collect { progress ->
                            _statusMessage.value =
                                "Downloading ${model.name}: ${(progress * 100).toInt()}%"
                        }
                        _statusMessage.value = "Model downloaded. Loading..."
                    } catch (e: Exception) {
                        _statusMessage.value = "Failed to download model. Check your internet connection."
                        Log.e(TAG, "Download error", e)
                        return@launch
                    }
                    
                    _statusMessage.value = "Model downloaded. Loading..."
                }
                
                // Load the model
                _statusMessage.value = "Loading AI model into memory..."
                Log.i(TAG, "Loading model: ${model.name}")
                
                val success = RunAnywhere.loadModel(model.id)

                if (success) {
                    _isModelLoaded.value = true
                    _statusMessage.value = "✅ AI model loaded. Ready for emergencies!"
                    Log.i(TAG, "Model loaded successfully: ${model.name}")
                } else {
                    _statusMessage.value = "Failed to load AI model. Try again."
                    Log.e(TAG, "Failed to load model: ${model.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model", e)
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    // ============ Communication Layer (Stub - Implement with Android APIs) ============

    private fun sendSMS(
        contact: EmergencyContact,
        message: String,
        appendLocation: Boolean = false
    ): Boolean {
        return try {
            // Add location to message if available and requested
            val fullMessage = if (appendLocation && _currentLocation.value != null) {
                val loc = _currentLocation.value!!
                "$message\n\nMy location: https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
            } else {
                message
            }

            Log.i(TAG, "SMS to ${contact.name}: $fullMessage")

            // Send actual SMS
            val smsManager =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(android.telephony.SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    android.telephony.SmsManager.getDefault()
                }

            // Split message if too long
            val parts = smsManager.divideMessage(fullMessage)
            if (parts.size == 1) {
                smsManager.sendTextMessage(contact.phoneNumber, null, fullMessage, null, null)
            } else {
                smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
            }

            Log.i(TAG, "✅ SMS sent successfully to ${contact.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send SMS to ${contact.name}: ${e.message}", e)
            false
        }
    }

    private fun makeCall(contact: EmergencyContact): Boolean {
        return try {
            Log.i(TAG, "Calling ${contact.name} at ${contact.phoneNumber}")

            val callIntent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                data = android.net.Uri.parse("tel:${contact.phoneNumber}")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(callIntent)
            Log.i(TAG, "✅ Call initiated to ${contact.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to call ${contact.name}: ${e.message}", e)
            false
        }
    }

    private fun makeMissedCall(contact: EmergencyContact): Boolean {
        return try {
            Log.i(TAG, "Missed call to ${contact.name}")

            // Make a call and immediately hang up (for stealth alerts)
            val callIntent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                data = android.net.Uri.parse("tel:${contact.phoneNumber}")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(callIntent)

            // Schedule hang up after 2 seconds
            viewModelScope.launch {
                delay(2000)
                // End call using telecom manager
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        // Check permission before ending call
                        if (context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS)
                            == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            val telecomManager =
                                context.getSystemService(android.telecom.TelecomManager::class.java)
                            telecomManager?.endCall()
                            Log.i(TAG, "✅ Missed call executed to ${contact.name}")
                        } else {
                            Log.w(TAG, "ANSWER_PHONE_CALLS permission not granted for auto-hangup")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not auto-hang up: ${e.message}")
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to make missed call to ${contact.name}: ${e.message}", e)
            false
        }
    }

    private fun callEmergencyServices(serviceType: String, location: Location?): Boolean {
        return try {
            Log.i(TAG, "⚠️ Calling emergency services: $serviceType")

            // Get emergency number based on location (defaulting to 911 for US)
            val emergencyNumber = "911" // TODO: Make this location-aware

            val callIntent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                data = android.net.Uri.parse("tel:$emergencyNumber")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(callIntent)
            Log.i(TAG, "✅ Emergency call initiated to $emergencyNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to call emergency services: ${e.message}", e)
            false
        }
    }

    private fun mapRelationshipToType(relationship: String): RecipientType {
        return when (relationship.lowercase()) {
            "family" -> RecipientType.FAMILY
            "friend" -> RecipientType.FRIEND
            else -> RecipientType.FRIEND
        }
    }

    override fun onCleared() {
        super.onCleared()
        questionTimerJob?.cancel()
        escalationMonitorJob?.cancel()
        autoRetriggerJob?.cancel()

        // Stop shake detector when ViewModel is cleared
        shakeDetector?.stop()
    }

    /**
     * Check if location services are available and configured properly
     * This helps diagnose location issues
     */
    private fun checkLocationAvailability() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "========================================")
                Log.i(TAG, "LOCATION DIAGNOSTIC CHECK")
                Log.i(TAG, "========================================")

                // Check permissions
                val hasFine = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                Log.i(TAG, "Permission Status:")
                Log.i(TAG, "  FINE_LOCATION: ${if (hasFine) "✅ GRANTED" else "❌ DENIED"}")
                Log.i(TAG, "  COARSE_LOCATION: ${if (hasCoarse) "✅ GRANTED" else "❌ DENIED"}")

                // Check location providers
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE)
                        as? android.location.LocationManager

                if (locationManager != null) {
                    val gpsEnabled = locationManager.isProviderEnabled(
                        android.location.LocationManager.GPS_PROVIDER
                    )
                    val networkEnabled = locationManager.isProviderEnabled(
                        android.location.LocationManager.NETWORK_PROVIDER
                    )

                    Log.i(TAG, "Device Location Services:")
                    Log.i(TAG, "  GPS Provider: ${if (gpsEnabled) "✅ ENABLED" else "❌ DISABLED"}")
                    Log.i(
                        TAG,
                        "  Network Provider: ${if (networkEnabled) "✅ ENABLED" else "❌ DISABLED"}"
                    )

                    if (!gpsEnabled && !networkEnabled) {
                        Log.w(TAG, "⚠️ WARNING: All location providers are DISABLED!")
                        Log.w(TAG, "User should enable Location in Device Settings")
                    }
                }

                // Try to get last known location if we have permission
                if (hasFine || hasCoarse) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val age =
                                (System.currentTimeMillis() - location.time) / 1000 / 60 // minutes
                            Log.i(TAG, "Last Known Location:")
                            Log.i(TAG, "  ✅ Available")
                            Log.i(TAG, "  Age: $age minutes old")
                            Log.i(TAG, "  Accuracy: ${location.accuracy}m")
                        } else {
                            Log.w(TAG, "Last Known Location: ❌ NOT AVAILABLE")
                            Log.w(TAG, "Device may have never obtained a location before")
                        }
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "❌ Failed to check last known location: ${e.message}")
                    }
                } else {
                    Log.w(TAG, "❌ Cannot check last known location - no permission")
                }

                Log.i(TAG, "========================================")

            } catch (e: Exception) {
                Log.e(TAG, "Error in location diagnostic: ${e.message}", e)
            }
        }
    }
}
