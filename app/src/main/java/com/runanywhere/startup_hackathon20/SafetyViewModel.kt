package com.runanywhere.startup_hackathon20

import android.content.Context
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

/**
 * Main ViewModel for Safety App
 * Manages emergency sessions, AI decisions, and communication with services
 */
class SafetyViewModel(private val context: Context) : ViewModel() {

    companion object {
        private const val TAG = "SafetyViewModel"
    }

    private val aiEngine = SafetyAIEngine()

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

    init {
        // Load emergency contacts from storage (implement persistence layer)
        loadEmergencyContacts()
    }

    /**
     * MAIN ACTION: User triggers emergency alarm
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
                    Log.w(TAG, "Alarm already active")
                    return@launch
                }

                _isAlarmActive.value = true
                _statusMessage.value = "🚨 Emergency alarm activated"

                Log.i(TAG, "Emergency triggered with ${_emergencyContacts.value.size} contacts:")
                _emergencyContacts.value.forEach { contact ->
                    Log.i(TAG, "  → ${contact.name}: ${contact.phoneNumber}")
                }

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

                // Start monitoring location updates
                startLocationMonitoring()

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
                    val success = sendSMS(action.contact, action.message)
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
    }

    /**
     * Cancel emergency alarm (false alarm)
     */
    fun cancelEmergencyAlarm() {
        viewModelScope.launch {
            _isAlarmActive.value = false
            questionTimerJob?.cancel()
            escalationMonitorJob?.cancel()

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
        // Add the user's contact to the list
        val currentContacts = _emergencyContacts.value

        // If this is the first contact being added, clear any sample contacts
        if (currentContacts.any { it.phoneNumber.startsWith("+123456789") }) {
            // Clear sample contacts - start fresh with only user's contacts
            _emergencyContacts.value = listOf(contact)
            Log.i(TAG, "Cleared sample contacts. Starting with user contact: ${contact.name}")
        } else {
            // Add to existing user contacts
            _emergencyContacts.value = currentContacts + contact
        }

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
        // Start with EMPTY list - no sample contacts
        // User MUST add contacts through onboarding
        _emergencyContacts.value = emptyList()
        Log.i(TAG, "Emergency contacts initialized. User must add contacts through onboarding.")
    }

    private fun saveEmergencyContacts() {
        // TODO: Implement SharedPreferences or Room database to persist contacts
        Log.i(TAG, "Saving ${_emergencyContacts.value.size} emergency contacts")
    }

    // ============ Location Management ============

    private fun startLocationMonitoring() {
        // TODO: Integrate with LocationManager/FusedLocationProvider
        // For now, this is a placeholder
        Log.i(TAG, "Location monitoring started")
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = location

        val session = _currentSession.value
        if (session != null) {
            _currentSession.value = session.copy(location = location)
        }
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

    private fun sendSMS(contact: EmergencyContact, message: String): Boolean {
        return try {
            // Add location to message if available
            val fullMessage = if (_currentLocation.value != null) {
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
    }
}
