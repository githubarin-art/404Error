package com.runanywhere.startup_hackathon20

import java.io.File
import java.security.MessageDigest

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
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.runanywhere.startup_hackathon20.utils.ShakeDetector
// New imports for emergency path & features
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import java.util.Calendar
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri
import android.content.Intent
import com.runanywhere.startup_hackathon20.utils.PermissionManager
import com.runanywhere.startup_hackathon20.data.SafePlace // Add import for new data class


/**
 * Main ViewModel for Safety App
 * Manages emergency sessions, AI decisions, and communication with services
 */
class SafetyViewModel(private val context: Context) : ViewModel() {
    // FusedLocationProviderClient for location tracking
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // New vars for emergency path 2
    private var continuousLocationJob: Job? = null
    private var secondQuestionTimerJob: Job? = null
    private var recordingJob: Job? = null
    private var journeyMonitoringJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var mediaRecorder: MediaRecorder? = null

    companion object {
        private const val TAG = "SafetyViewModel"
        private const val PREFS_NAME = "SafetyAppPrefs"
        private const val KEY_EMERGENCY_CONTACTS = "emergencyContacts"
        private const val KEY_SHAKE_ENABLED = "shakeGestureEnabled"
        private const val KEY_LAST_LATITUDE = "lastLatitude"
        private const val KEY_LAST_LONGITUDE = "lastLongitude"
        private const val KEY_LAST_ACCURACY = "lastAccuracy"
        private const val KEY_LAST_LOCATION_TIME = "lastLocationTime"

        const val CURRENT_MODEL_VERSION = "2.1"
        const val EXPECTED_MODEL_MD5 =
            "d41d8cd98f00b204e9800998ecf8427e" // Placeholder MD5 for version 2.1 (empty file example; replace with real)
        const val MODEL_VERSION_FILE = "model_version.txt"
        const val PREF_KEY_LAST_REMIND = "last_remind_timestamp"
        const val PREF_KEY_SKIPPED_VERSION = "skipped_model_version"
        private const val REMIND_DAYS = 7L
        // End companion object
    }

// --- Safe Places Prefetch Background Helpers ---

private fun hasLocationPermission(): Boolean {
    val hasFine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return hasFine || hasCoarse
}

private suspend fun getInitialLocationForPrefetch(): Location? {
    return try {
        suspendCancellableCoroutine { cont ->
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                cont.resume(location, null)
            }.addOnFailureListener { e ->
                cont.resume(null, null)
            }
        }
    } catch (e: Exception) {
        try {
            suspendCancellableCoroutine { cont ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        cont.resume(location, null)
                    }
                    .addOnFailureListener { e2 ->
                        cont.resume(null, null)
                    }
            }
        } catch (e2: Exception) {
            null
        }
    }
}

private fun startSafePlacesPrefetch(initialLocation: Location) {
    safePlacesPreFetchJob?.cancel()
    safePlacesPreFetchJob = viewModelScope.launch {
        updateNearestSafePlaces() // Initial prefetch
        Log.i(TAG, "Background safe places prefetch started")
        
        while (!_isAlarmActive.value) { // Only prefetch when not in emergency
            delay(300000) // Every 5 minutes
            val currentLoc = _currentLocation.value
            if (currentLoc != null) {
                updateNearestSafePlaces()
                Log.i(TAG, "Background safe places updated")
            }
        }
    }
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

    // Add _userLocation StateFlow after _currentLocation
    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    // Status message
    private val _statusMessage = MutableStateFlow("Ready. Stay safe.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Model loading state
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    // --- Onboarding Model Automatic Installation ---
    private val _isModelInstalling = MutableStateFlow(false)
    val isModelInstalling: StateFlow<Boolean> = _isModelInstalling.asStateFlow()

    private val _modelInstallProgress = MutableStateFlow(0f)
    val modelInstallProgress: StateFlow<Float> = _modelInstallProgress.asStateFlow()

    // --- Update/Version Management StateFlows ---
    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _updateProgress = MutableStateFlow(0f)
    val updateProgress: StateFlow<Float> = _updateProgress.asStateFlow()

    // Alert history
    private val _alertHistory = MutableStateFlow<List<AlertRecord>>(emptyList())
    val alertHistory: StateFlow<List<AlertRecord>> = _alertHistory.asStateFlow()

    // Alarm active state
    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

    // Emergency path 2 variables
    private val _emergencyPath = MutableStateFlow(EmergencyPath.NONE)
    val emergencyPath: StateFlow<EmergencyPath> = _emergencyPath.asStateFlow()

    private val _secondQuestion = MutableStateFlow<ProtocolQuestion?>(null)
    val secondQuestion: StateFlow<ProtocolQuestion?> = _secondQuestion.asStateFlow()

    private val _secondQuestionTimeRemaining = MutableStateFlow<Int?>(null)
    val secondQuestionTimeRemaining: StateFlow<Int?> = _secondQuestionTimeRemaining.asStateFlow()

    private val _nearestSafePlaces = MutableStateFlow<List<SafePlace>>(emptyList()) // Updated for new fields
    val nearestSafePlaces: StateFlow<List<SafePlace>> = _nearestSafePlaces.asStateFlow()

    private val _currentDestination = MutableStateFlow<SafePlace?>(null)
    val currentDestination: StateFlow<SafePlace?> = _currentDestination.asStateFlow()

    private val _isLoudAlarmActive = MutableStateFlow(false)
    val isLoudAlarmActive: StateFlow<Boolean> = _isLoudAlarmActive.asStateFlow()

    private val _isRecordingActive = MutableStateFlow(false)
    val isRecordingActive: StateFlow<Boolean> = _isRecordingActive.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    private val _isFakeCallActive = MutableStateFlow(false)
    val isFakeCallActive: StateFlow<Boolean> = _isFakeCallActive.asStateFlow()

    private val _isBreathingActive = MutableStateFlow(false)
    val isBreathingActive: StateFlow<Boolean> = _isBreathingActive.asStateFlow()

    private val _showPoliceConfirmation = MutableStateFlow(false)
    val showPoliceConfirmation: StateFlow<Boolean> = _showPoliceConfirmation.asStateFlow()

    private val _showArrivalConfirmation = MutableStateFlow(false)
    val showArrivalConfirmation: StateFlow<Boolean> = _showArrivalConfirmation.asStateFlow()

    private val _interactionTimestamp = MutableStateFlow(System.currentTimeMillis())
    val interactionTimestamp: StateFlow<Long> = _interactionTimestamp.asStateFlow()

    private var questionTimerJob: Job? = null
    private var escalationMonitorJob: Job? = null
    private var autoRetriggerJob: Job? = null

    // Background prefetch job for safe places (non-emergency sessions)
    private var safePlacesPreFetchJob: Job? = null

    // Escape tracking job for location during escape to safety
    private var escapeLocationTrackingJob: Job? = null

    // Cooldown before user can re-arm SOS after an emergency ends
    private val _nextSOSAllowedAt = MutableStateFlow(0L)
    val nextSOSAllowedAt: StateFlow<Long> = _nextSOSAllowedAt.asStateFlow()

    init {
        // Load emergency contacts from storage
        loadEmergencyContacts()

        // Check location availability on startup
        checkLocationAvailability()

        // Initialize shake detector
        initializeShakeDetector()

        // Load shake gesture preference
        loadShakeGesturePreference()

        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        // --- Phase 1: Onboarding Model Installation ---
        viewModelScope.launch {
            checkAndInstallModelIfNeeded()
            // After model check, perform version check
            checkForModelUpdates()
        }

        // --- Background safe places prefetch initialization ---
        viewModelScope.launch {
            if (hasLocationPermission()) {
                val location = getInitialLocationForPrefetch()
                if (location != null) {
                    startSafePlacesPrefetch(location)
                }
            }
        }
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
     * FLOW (Updated as per product requirement):
     * 1. Validate model is loaded and contacts exist
     * 2. Enforce 10s cooldown if just ended a session
     * 3. Create emergency session
     * 4. Start location monitoring IMMEDIATELY
     * 5. Present first protocol question (NO auto alert before Q1)
     * 6. If Q1 = NO/timeout: send SMS to contacts (no calls yet), start continuous location
     * 7. Present second question
     * 8. If Q2 = YES (threat nearby): call top contacts
     * 9. Continue monitoring and escalating as needed
     */
    fun triggerEmergencyAlarm() {
        viewModelScope.launch {
            try {
                // Enforce 10s cooldown after previous emergency ended
                val now = System.currentTimeMillis()
                if (now < _nextSOSAllowedAt.value) {
                    val waitMs = _nextSOSAllowedAt.value - now
                    val waitSec = (waitMs / 1000).coerceAtLeast(1)
                    _statusMessage.value = "Re-arming SOS… wait ${waitSec}s"
                    Log.w(TAG, "SOS re-arm cooldown active: ${waitSec}s remaining")
                    return@launch
                }

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

                // If alarm is already active, this is a re-trigger - we should NOT ignore it
                // Instead, we continue with a NEW emergency session
                if (_isAlarmActive.value) {
                    Log.i(TAG, "⚠️ Emergency already active - this appears to be a re-trigger")
                    Log.i(
                        TAG,
                        "Note: Proper flow should cancel existing emergency first before re-triggering"
                    )
                    _statusMessage.value = "Emergency already active"
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
                delay(1000)

                // Present first protocol question (NO auto-SMS before Q1)
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

            // Victim reported feeling safe – gracefully wind down session
            _statusMessage.value = "Safety confirmed. Returning to standby..."
            delay(1500)
            cancelEmergencyAlarm()
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

            _statusMessage.value = "High threat detected. Sending SMS to contacts..."

            _currentQuestion.value = null
            _questionTimeRemaining.value = null

            // IMMEDIATE ACTIONS as per new product flow:
            // 1. Send emergency SMS to all contacts (NO CALLS YET)
            Log.i(TAG, "========================================")
            Log.i(TAG, "USER ANSWERED NO - SENDING SMS (calls deferred to Q2)")
            Log.i(TAG, "========================================")
            
            sendImmediateEmergencyAlerts()

            // 2. Start continuous location tracking every 30 seconds
            startContinuousLocationTracking()

            Log.i(TAG, "✅ Emergency SMS sent, location tracking started")
            Log.i(TAG, "Presenting second question: Is threat near you?")

            // 3. Present second question instead of AI decision
            presentSecondQuestion()
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
            Log.i(TAG, "========================================")
            Log.i(TAG, "CANCELING EMERGENCY SESSION")
            Log.i(TAG, "========================================")

            // Cancel all running jobs first
            questionTimerJob?.cancel()
            escalationMonitorJob?.cancel()
            autoRetriggerJob?.cancel()
            continuousLocationJob?.cancel()
            journeyMonitoringJob?.cancel()
            secondQuestionTimerJob?.cancel()
            recordingJob?.cancel()
            stealthModeSwitchingJob?.cancel()

            val session = _currentSession.value
            if (session != null) {
                val updatedSession = session.copy(isActive = false)
                _currentSession.value = updatedSession

                // Notify contacts that alarm was cancelled
                sendCancellationNotifications()
            }

            // Reset ALL state to prepare for next emergency
            _isAlarmActive.value = false
            _currentSession.value = null
            _currentQuestion.value = null
            _questionTimeRemaining.value = null
            _secondQuestion.value = null
            _secondQuestionTimeRemaining.value = null
            _emergencyPath.value = EmergencyPath.NONE
            _nearestSafePlaces.value = emptyList()
            _currentDestination.value = null
            _alertHistory.value = emptyList()
            _showStealthDecoy.value = false
            _showInfoIcon.value = false
            _showDecoyAvailable.value = false

            stopLoudAlarm()
            stopRecording()
            _isFakeCallActive.value = false
            _isBreathingActive.value = false
            _showPoliceConfirmation.value = false
            _showArrivalConfirmation.value = false

            // 10-second cooldown to prevent rapid SOS re-arms after emergency end
            val cooldownMs = 10_000L
            _nextSOSAllowedAt.value = System.currentTimeMillis() + cooldownMs

            _statusMessage.value = "Ready. Stay safe."

            Log.i(TAG, "✅ Emergency session completely reset - ready for new emergency")
            Log.i(TAG, "========================================")
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
                @SuppressLint("MissingPermission")
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

            @SuppressLint("MissingPermission")
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

    // In updateLocation, add user location update and arrival check
    fun updateLocation(location: Location) {
        _currentLocation.value = location
        _userLocation.value = location  // Update user location StateFlow

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

        // Update nearest safe places: Always during emergency, or trigger prefetch if not
        viewModelScope.launch {
            if (_isAlarmActive.value || _nearestSafePlaces.value.isEmpty()) {
                updateNearestSafePlaces()
            } else {
                // If pre-fetched and not emergency, just log
                Log.i(TAG, "Safe places pre-fetched, ready for instant display")
            }
        }

        // Start/continue background prefetch if location improved and not in emergency
        if (!_isAlarmActive.value) {
            startSafePlacesPrefetch(location)
        }

        // Check arrival if in escape mode
        if (_isAlarmActive.value && _currentDestination.value != null) {
            val dest = _currentDestination.value!!
            val destLoc = locationFromPlace(dest)
            val distance = calculateDistance(location, destLoc)
            if (distance <= 50f) {
                endEmergencyIfArrived()
            }
        }
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

    suspend fun checkAndInstallModelIfNeeded() {
        _isModelInstalling.value = true
        _statusMessage.value = "Checking AI model..."

        val availableModels = listAvailableModels()
        if (availableModels.isEmpty()) {
            Log.i(TAG, "No model available, starting download")
            _statusMessage.value = "Installing Emergency AI Model (374 MB)"
            downloadAndInstallModel()
        } else {
            Log.i(TAG, "Model available, loading...")
            loadModelAutomatically(availableModels.first().id)
        }

        _isModelInstalling.value = false
    }

    private suspend fun downloadAndInstallModel() {
        try {
            val modelId = "qwen-0.5b"
            RunAnywhere.downloadModel(modelId).collect { progress ->
                _modelInstallProgress.value = progress
                val percent = (progress * 100).toInt()
                _statusMessage.value = "Downloading AI Model: $percent%"
                Log.i(TAG, "Model download progress: $percent%")
            }

            saveModelVersion("2.1")

            val availableModels = listAvailableModels()
            if (availableModels.isNotEmpty()) {
                loadModelAutomatically(availableModels.first().id)
                _statusMessage.value = "AI Model installed. Ready for emergencies."
                Log.i(TAG, "✅ AI Model installed successfully")
            } else {
                Log.e(TAG, "Model downloaded but not listed in available models")
                _statusMessage.value = "Installation failed. Please restart the app."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model", e)
            _statusMessage.value = "Failed to download model. Check connection."
        }
    }

    private fun loadModelAutomatically(modelId: String) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Loading AI model into memory..."
                val success = RunAnywhere.loadModel(modelId)
                if (success) {
                    _isModelLoaded.value = true
                    _statusMessage.value = "✅ AI Model loaded. Ready for emergencies!"
                    Log.i(TAG, "Model loaded automatically: $modelId")
                } else {
                    _statusMessage.value = "Failed to load model. Please restart."
                    Log.e(TAG, "Failed to load model automatically")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model", e)
                _statusMessage.value = "Error loading model: ${e.message}"
            }
        }
    }

    // Save model version as a simple file (optional for upgrades/future)
    private fun saveModelVersion(version: String) {
        try {
            val file = File(context.filesDir, MODEL_VERSION_FILE)
            file.parentFile?.mkdirs()
            file.writeText(version)
            Log.i(TAG, "Model version $version saved")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save model version: ${e.message}")
        }
    }

    // Manual load function remains for settings/manual use.
    // This is now fallback; automatic load and update are preferred.
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

    // ----------- Model Update/Version Check Logic ------------

    private fun checkForModelUpdates() {
        viewModelScope.launch {
            val localVersion = getLocalModelVersion()
            val skippedVersion = getSkippedVersionFromPrefs()
            val lastRemind = getLastRemindTimestampFromPrefs()

            val now = System.currentTimeMillis()
            val daysSinceRemind =
                if (lastRemind > 0) (now - lastRemind) / (1000 * 60 * 60 * 24) else 0

            // Show dialog if: new version available, not skipped, and (first time or >7 days since remind)
            if (localVersion != CURRENT_MODEL_VERSION &&
                skippedVersion != CURRENT_MODEL_VERSION &&
                (lastRemind == 0L || daysSinceRemind >= REMIND_DAYS)
            ) {
                _updateAvailable.value = true
                _showUpdateDialog.value = true
                _statusMessage.value = "Safety AI Model Update Available"
                Log.i(TAG, "Model update dialog shown for version $CURRENT_MODEL_VERSION")
            } else {
                Log.i(
                    TAG,
                    "No model update needed (local: $localVersion, skipped: $skippedVersion)"
                )
            }
        }
    }

    private fun getLocalModelVersion(): String {
        return try {
            val file = File(context.filesDir, MODEL_VERSION_FILE)
            if (file.exists()) {
                file.readText().trim()
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading model version file: ${e.message}")
            ""
        }
    }

    private fun getSkippedVersionFromPrefs(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_KEY_SKIPPED_VERSION, "") ?: ""
    }

    private fun saveSkippedVersion(version: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY_SKIPPED_VERSION, version).apply()
        Log.i(TAG, "Skipped version $version saved")
    }

    private fun getLastRemindTimestampFromPrefs(): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(PREF_KEY_LAST_REMIND, 0L)
    }

    private fun saveLastRemindTimestamp() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(PREF_KEY_LAST_REMIND, System.currentTimeMillis()).apply()
        Log.i(TAG, "Last remind timestamp saved")
    }

    // Public functions for UI to call
    fun handleUpdateNow() {
        viewModelScope.launch {
            _showUpdateDialog.value = false
            _updateProgress.value = 0f
            _statusMessage.value = "Updating Safety AI Model..."

            try {
                val modelId = "qwen-0.5b" // Assuming same ID for update
                RunAnywhere.downloadModel(modelId).collect { progress ->
                    _updateProgress.value = progress
                    val percent = (progress * 100).toInt()
                    _statusMessage.value = "Updating Model: $percent%"
                    Log.i(TAG, "Model update progress: $percent%")
                }

                // Verify MD5 (assuming we can get the model file path; SDK may abstract, placeholder)
                val modelFile = File(context.filesDir, "models/qwen-0.5b.gguf") // Assumed path
                if (modelFile.exists()) {
                    val actualMD5 = calculateMD5(modelFile)
                    if (actualMD5 == EXPECTED_MODEL_MD5) {
                        saveModelVersion(CURRENT_MODEL_VERSION)
                        // Reload model
                        val availableModels = listAvailableModels()
                        if (availableModels.isNotEmpty()) {
                            RunAnywhere.loadModel(availableModels.first().id)
                            _isModelLoaded.value = true
                            _statusMessage.value = "AI Model updated. Emergency features enhanced."
                            Log.i(TAG, "✅ Model updated and verified successfully")
                        }
                    } else {
                        _statusMessage.value = "Update verification failed. Reverting."
                        Log.e(TAG, "MD5 mismatch: expected $EXPECTED_MODEL_MD5, got $actualMD5")
                        // Revert: could redownload old, but for now, keep old
                    }
                } else {
                    Log.e(TAG, "Model file not found after download")
                    _statusMessage.value = "Update failed: File not found."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating model", e)
                _statusMessage.value = "Update failed: ${e.message}. Please try again."
            }
        }
    }

    fun handleRemindLater() {
        saveLastRemindTimestamp()
        _showUpdateDialog.value = false
        _statusMessage.value = "We'll remind you later."
        Log.i(TAG, "Update dialog dismissed - remind later")
    }

    fun handleSkipVersion() {
        saveSkippedVersion(CURRENT_MODEL_VERSION)
        _showUpdateDialog.value = false
        _statusMessage.value = "Update skipped for this version."
        Log.i(TAG, "Model update skipped for version $CURRENT_MODEL_VERSION")
    }

    private fun calculateMD5(file: File): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "MD5 calculation error: ${e.message}")
            ""
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

    // --- Emergency Path 2 Functions ---

    private fun presentSecondQuestion() {
        val question = ProtocolQuestion(
            id = "threat_proximity",
            question = "Is the threat near you right now?",
            timeoutSeconds = 30,
            threatLevelIfAnswered = ThreatLevel.CRITICAL, // YES = threat nearby = CRITICAL  
            threatLevelIfNotAnswered = ThreatLevel.CRITICAL // Timeout defaults to YES (threat nearby)
        )
        _secondQuestion.value = question
        _statusMessage.value = "Answer the proximity question"

        Log.i(TAG, "========================================")
        Log.i(TAG, "SECOND QUESTION PRESENTED")
        Log.i(TAG, "========================================")
        Log.i(TAG, "Question: ${question.question}")
        Log.i(TAG, "Timeout: ${question.timeoutSeconds} seconds")
        Log.i(TAG, "YES answer = THREAT NEARBY (CRITICAL)")
        Log.i(TAG, "NO answer = ESCAPE TO SAFETY (HIGH)")
        Log.i(TAG, "Timeout default = YES (CRITICAL - assumes threat nearby)")
        Log.i(TAG, "========================================")

        startSecondQuestionTimer(question.timeoutSeconds)
    }

    private fun startSecondQuestionTimer(seconds: Int) {
        secondQuestionTimerJob?.cancel()
        secondQuestionTimerJob = viewModelScope.launch {
            var remaining = seconds
            _secondQuestionTimeRemaining.value = remaining
            while (remaining > 0) {
                delay(1000)
                remaining--
                _secondQuestionTimeRemaining.value = remaining
            }
            handleSecondQuestionTimeout()
        }
    }

    private fun handleSecondQuestionTimeout() {
        viewModelScope.launch {
            Log.w(TAG, "Second protocol question timeout - assuming threat nearby")
            answerSecondQuestionYes()
        }
    }

    fun answerSecondQuestionYes() {
        secondQuestionTimerJob?.cancel()
        val question = _secondQuestion.value ?: return
        _secondQuestion.value = null
        _secondQuestionTimeRemaining.value = null

        _emergencyPath.value = EmergencyPath.THREAT_NEARBY
        updateThreatLevel(ThreatLevel.CRITICAL)
        viewModelScope.launch {
            updateNearestSafePlaces()
        }
        _statusMessage.value = "CRITICAL - THREAT NEARBY"

        Log.i(TAG, "========================================")
        Log.i(TAG, "PATH A: THREAT NEARBY - CRITICAL THREAT")
        Log.i(TAG, "========================================")
        Log.i(TAG, "Actions: Calling top 2 contacts immediately")
        Log.i(TAG, "User can activate loud alarm, record evidence, or fake call")
        Log.i(TAG, "========================================")

        viewModelScope.launch {
            // Immediately call top 2 priority contacts when threat is nearby
            _emergencyContacts.value.sortedBy { it.priority }.take(2).forEach { contact ->
                Log.i(TAG, "📞 Calling ${contact.name} (priority ${contact.priority})")
                makeCall(contact)
                delay(1000)
            }

            // Enable decoy/manual switching controls
            _showDecoyAvailable.value = true
            _showInfoIcon.value = true
            Log.i(TAG, "✅ Decoy controls enabled for Path A")

            Log.i(TAG, "✅ Emergency calls completed for Path A")
            _statusMessage.value = "CRITICAL - Calls made. Use safety features below."

            // Do NOT call makeAIDecision() - let user control the path features
        }
    }

    // Update answerSecondQuestionNo to integrate
    fun answerSecondQuestionNo() {
        secondQuestionTimerJob?.cancel()
        val question = _secondQuestion.value ?: return
        _secondQuestion.value = null
        _secondQuestionTimeRemaining.value = null

        _emergencyPath.value = EmergencyPath.ESCAPE_TO_SAFETY
        updateThreatLevel(ThreatLevel.HIGH)
        viewModelScope.launch {
            updateNearestSafePlaces()
        }
        _statusMessage.value = "HIGH ALERT - Navigate to safety"

        Log.i(TAG, "========================================")
        Log.i(TAG, "PATH B: ESCAPE TO SAFETY - HIGH ALERT")
        Log.i(TAG, "========================================")
        Log.i(TAG, "Step 1: Calling ALL contacts immediately")
        Log.i(TAG, "Step 2: Show escape UI with safe places DURING calls")
        Log.i(TAG, "Step 3: Manual decoy switching enabled after calls")
        Log.i(TAG, "========================================")

        viewModelScope.launch {
            // STEP 1: Call ALL emergency contacts (not just top 2)
            _emergencyContacts.value.sortedBy { it.priority }.forEach { contact ->
                Log.i(TAG, "📞 Calling ${contact.name} (priority ${contact.priority})")
                makeCall(contact)
                delay(2000) // 2 second delay between calls
            }

            _statusMessage.value = "HIGH ALERT - Navigate to safety"

            // STEP 2: Stop prefetch, start escape tracking
            safePlacesPreFetchJob?.cancel()
            startEscapeLocationTracking()

            // STEP 3: Manual decoy available after calls
            _showDecoyAvailable.value = true
            _showInfoIcon.value = true
            Log.i(TAG, "✅ Decoy controls enabled for Path B")

            Log.i(TAG, "✅ ALL Emergency calls completed for Path B")
            
            // Location updates are already running from Q1 NO answer
        }
    }

    private fun startContinuousLocationTracking() {
        continuousLocationJob?.cancel()
        continuousLocationJob = viewModelScope.launch {
            while (_isAlarmActive.value) {
                startLocationMonitoring()
                delay(30000)
            }
        }
        Log.i(TAG, "✅ Continuous location tracking started (every 30s)")
    }


    private fun typePriority(type: String): Int {
        return when (type) {
            "police" -> 0
            "hospital", "fire" -> 1
            else -> 2
        }
    }

    // Update fetchAndCalculateSafePlaces to return List<SafePlace> with new fields
    private suspend fun fetchAndCalculateSafePlaces(currentLocation: Location): List<SafePlace> = withContext(Dispatchers.IO) {
        try {
            val apiKey = "AIzaSyDVkedQ-2iUAak1TJVGZd6Qi4jdjUO_3Wo"
            val types = "police|hospital|fire_station|pharmacy|convenience_store|transit_station"
            val locationStr = "${currentLocation.latitude},${currentLocation.longitude}"
            val urlStr = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=$locationStr&radius=5000&type=$types&key=$apiKey"
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "API request failed: $responseCode")
                return@withContext emptyList()
            }
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            connection.disconnect()
            val jsonObject = JSONObject(response)
            if (jsonObject.getString("status") != "OK") {
                Log.e(TAG, "Places API error: ${jsonObject.getString("status")}")
                return@withContext emptyList()
            }
            val results = jsonObject.getJSONArray("results")
            val places = mutableListOf<SafePlace>()
            for (i in 0 until results.length()) {
                val placeObj = results.getJSONObject(i)
                val placeId = placeObj.optString("place_id", "")
                val name = placeObj.getString("name")
                val typesArray = placeObj.getJSONArray("types")
                val typesList = mutableListOf<String>()
                for (j in 0 until typesArray.length()) {
                    typesList.add(typesArray.getString(j))
                }
                val geometry = placeObj.getJSONObject("geometry").getJSONObject("location")
                val lat = geometry.getDouble("lat")
                val lng = geometry.getDouble("lng")
                val phoneNumber = placeObj.optString("formatted_phone_number", null)
                var isOpen24h = false
                val openingHours = placeObj.optJSONObject("opening_hours")
                if (openingHours != null) {
                    isOpen24h = openingHours.optBoolean("open_now", false)
                } else {
                    // Default true for critical services
                    if (typesList.contains("police") || typesList.contains("hospital") || typesList.contains("fire_station")) {
                        isOpen24h = true
                    }
                }
                val placeLoc = Location("place").apply {
                    latitude = lat
                    longitude = lng
                }
                val distance = calculateDistance(currentLocation, placeLoc)
                places.add(SafePlace(
                    placeId = placeId,
                    name = name,
                    types = typesList,
                    latitude = lat,
                    longitude = lng,
                    distance = distance,
                    isOpen24h = isOpen24h,
                    phoneNumber = phoneNumber
                ))
            }
            Log.i(TAG, "Fetched ${places.size} nearby safe places")
            places
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching safe places", e)
            emptyList()
        }
    }

    // Update updateNearestSafePlaces to use new SafePlace structure (no changes to prioritization logic, just use new fields)
    private suspend fun updateNearestSafePlaces() {
        val currentLoc = _currentLocation.value ?: run {
            Log.w(TAG, "Cannot update safe places - location unavailable")
            _nearestSafePlaces.value = emptyList()
            _statusMessage.value = "Location needed for safe places"
            return
        }

        // Set loading status if empty
        if (_nearestSafePlaces.value.isEmpty()) {
            _statusMessage.value = "Calculating nearest safe places..."
        }

        val allPlaces = fetchAndCalculateSafePlaces(currentLoc)
            .sortedBy { it.distance }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNight = hour < 6 || hour >= 22

        val filteredPlaces = allPlaces.filter { if (isNight) it.isOpen24h else true }

        val prioritized = filteredPlaces.sortedWith(
            compareBy<SafePlace> { 
                when {
                    it.types.contains("police") -> 0
                    it.types.contains("hospital") -> 1
                    it.isOpen24h -> 2
                    else -> 3
                }
            }.thenBy { it.distance }
        )

        val takeCount = if (_emergencyPath.value == EmergencyPath.ESCAPE_TO_SAFETY) 5 else 3
        _nearestSafePlaces.value = prioritized.take(takeCount)
        
        if (_nearestSafePlaces.value.isNotEmpty()) {
            _statusMessage.value = "Safe places ready"
            Log.i(TAG, "Safe places updated: ${_nearestSafePlaces.value.size} locations cached")
        }
    }

    fun toggleLoudAlarm() {
        if (_isLoudAlarmActive.value) {
            stopLoudAlarm()
        } else {
            startLoudAlarm()
        }
    }

    private fun startLoudAlarm() {
        _isLoudAlarmActive.value = true

        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, alarmUri)
                setVolume(1.0f, 1.0f)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to play alarm sound", e)
            _statusMessage.value = "Alarm sound unavailable"
        }

        // Continuous vibration
        vibrator?.let { vib ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 500), 0))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(longArrayOf(0, 1000, 500), 0)
            }
        }

        Log.i(TAG, "🚨 Loud alarm activated")
    }

    private fun stopLoudAlarm() {
        _isLoudAlarmActive.value = false
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Error stopping alarm player", e)
            } finally {
                player.release()
                mediaPlayer = null
            }
        }
        vibrator?.cancel()
        Log.i(TAG, "🛑 Loud alarm deactivated")
    }

    fun toggleRecording() {
        if (_isRecordingActive.value) {
            stopRecording()
        } else {
            // Check permission before starting recording
            if (!PermissionManager.isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)) {
                _statusMessage.value = "Microphone permission required for recording evidence"
                Log.w(TAG, "⚠️ RECORD_AUDIO permission denied - cannot start recording")
                Log.w(TAG, "Alternative: User can use fake call or loud alarm features")
                return
            }
            startRecording()
        }
    }

    private fun startRecording() {
        _isRecordingActive.value = true

        val outputFile = "${context.externalCacheDir?.absolutePath}/evidence_${System.currentTimeMillis()}.3gp"

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile)
            prepare()
            start()
        }

        // Start duration timer
        _recordingDuration.value = 0
        recordingJob = viewModelScope.launch {
            var duration = 0
            while (_isRecordingActive.value) {
                delay(1000)
                duration++
                _recordingDuration.value = duration
            }
        }

        Log.i(TAG, "🎤 Recording evidence started")
    }

    private fun stopRecording() {
        _isRecordingActive.value = false
        recordingJob?.cancel()
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        _recordingDuration.value = 0

        // TODO: Auto-upload to cloud or contacts
        Log.i(TAG, "🛑 Recording stopped - evidence saved")
    }

    fun startFakeCall() {
        _isFakeCallActive.value = true
        Log.i(TAG, "📞 Fake incoming call activated (from Dad)")
        // UI should display realistic call screen with ringtone
    }

    fun stopFakeCall() {
        _isFakeCallActive.value = false
        Log.i(TAG, "🛑 Fake call deactivated")
    }

    fun startBreathingExercise() {
        _isBreathingActive.value = true
        Log.i(TAG, "🧘 Breathing exercise launched")
        // UI should show animation and prompts
    }

    fun stopBreathingExercise() {
        _isBreathingActive.value = false
        Log.i(TAG, "🛑 Breathing exercise stopped")
    }

    fun requestCallPolice() {
        _showPoliceConfirmation.value = true
    }

    fun confirmCallPolice(confirm: Boolean) {
        _showPoliceConfirmation.value = false
        if (confirm) {
            callPolice()
        }
    }

    private fun callPolice() {
        if (!PermissionManager.isPermissionGranted(context, Manifest.permission.CALL_PHONE)) {
            _statusMessage.value = "Call permission required to contact police"
            return
        }

        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:112")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(callIntent)
            Log.i(TAG, "📞 Calling police (112)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call police", e)
            _statusMessage.value = "Unable to make call"
        }
    }

    fun navigateToPlace(place: SafePlace) {
        val uri = Uri.parse("google.navigation:q=${place.latitude},${place.longitude}&mode=w")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        try {
            context.startActivity(intent)
            _currentDestination.value = place
            startJourneyMonitoring()
            Log.i(TAG, "🗺️ Navigation to ${place.name} started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start navigation", e)
            _statusMessage.value = "Google Maps required for navigation"
        }
    }

    private fun startJourneyMonitoring() {
        journeyMonitoringJob?.cancel()
        journeyMonitoringJob = viewModelScope.launch {
            var lastDistance: Float? = null
            var lastTime = System.currentTimeMillis()

            while (_currentDestination.value != null) {
                delay(30000)

                val currentLoc = _currentLocation.value ?: continue
                val dest = _currentDestination.value ?: break
                val destLoc = locationFromPlace(dest)
                val currentDistance = currentLoc.distanceTo(destLoc)

                if (lastDistance == null) {
                    lastDistance = currentDistance
                    lastTime = System.currentTimeMillis()
                    sendLocationUpdateToContacts("Moving towards ${dest.name} - ${currentDistance.toInt()}m away")
                    continue
                }

                val timeElapsed = (System.currentTimeMillis() - lastTime) / 1000f

                if (currentDistance < 50) {
                    _showArrivalConfirmation.value = true
                    _currentDestination.value = null
                    break
                }

                if (timeElapsed > 120 && kotlin.math.abs(currentDistance - lastDistance) < 10) {
                    sendAlertToContacts("Stopped moving towards ${dest.name} for over 2 minutes")
                }

                if (currentDistance > lastDistance + 50) {
                    sendAlertToContacts("Deviated from route to ${dest.name}")
                }

                sendLocationUpdateToContacts("Update: ${currentDistance.toInt()}m from ${dest.name}")

                lastDistance = currentDistance
                lastTime = System.currentTimeMillis()
            }
        }
    }

    fun confirmArrival(isSafe: Boolean) {
        _showArrivalConfirmation.value = false
        if (isSafe) {
            stealthModeSwitchingJob?.cancel()
            _showStealthDecoy.value = false
            _showInfoIcon.value = false
            cancelEmergencyAlarm()
        } else {
            _statusMessage.value = "Continue to safety"
        }
    }

    fun registerUserInteraction() {
        _interactionTimestamp.value = System.currentTimeMillis()
    }

    private fun locationFromPlace(place: SafePlace): Location {
        val loc = Location("dest")
        loc.latitude = place.latitude
        loc.longitude = place.longitude
        return loc
    }

    // Add calculateDistance helper
    private fun calculateDistance(loc1: Location, loc2: Location): Float {
        val results = FloatArray(1)
        Location.distanceBetween(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude, results)
        return results[0]
    }

    // Add startEscapeLocationTracking after updateNearestSafePlaces
    private fun startEscapeLocationTracking() {
        escapeLocationTrackingJob?.cancel()
        escapeLocationTrackingJob = viewModelScope.launch {
            while (_isAlarmActive.value && _currentDestination.value != null) {
                delay(30000) // Every 30 seconds
                val currentLoc = _currentLocation.value ?: continue
                _userLocation.value = currentLoc
                val dest = _currentDestination.value ?: continue
                val destLoc = locationFromPlace(dest)
                val distance = calculateDistance(currentLoc, destLoc)
                // Update distances in safe places if needed, but since it's list, refresh if selected
                updateNearestSafePlaces() // Refresh for current position
                if (distance <= 50f) {
                    endEmergencyIfArrived()
                    break
                }
            }
        }
    }

    // Add endEmergencyIfArrived
    private fun endEmergencyIfArrived() {
        _showArrivalConfirmation.value = true
        _statusMessage.value = "You have reached a safe location!"
        Log.i(TAG, "Emergency ended - arrived at safe place")
    }

    private fun sendLocationUpdateToContacts(message: String) {
        val loc = _currentLocation.value
        val fullMessage = "$message${loc?.let { "\nLocation: https://maps.google.com/?q=${it.latitude},${it.longitude}" } ?: ""}"

        _emergencyContacts.value.forEach { contact ->
            sendSMS(contact, fullMessage, false)
        }
    }

    private fun sendAlertToContacts(message: String) {
        val fullMessage = "URGENT ALERT: $message\nPlease check immediately!"
        _emergencyContacts.value.forEach { contact ->
            sendSMS(contact, fullMessage, true)
        }
    }

    // --- End emergency path 2 ---

    // New stealth mode variables
    private var stealthModeSwitchingJob: Job? = null
    private val _showStealthDecoy = MutableStateFlow(false)
    val showStealthDecoy: StateFlow<Boolean> = _showStealthDecoy.asStateFlow()

    // Info icon for decoy mode
    private val _showInfoIcon = MutableStateFlow(false)
    val showInfoIcon: StateFlow<Boolean> = _showInfoIcon.asStateFlow()

    // New StateFlow for decoy manual switching
    private val _showDecoyAvailable = MutableStateFlow(false)
    val showDecoyAvailable: StateFlow<Boolean> = _showDecoyAvailable.asStateFlow()

    /**
     * Start automatic UI switching for stealth mode
     * Alternates between escape UI and home screen every 10 seconds
     * This confuses attackers while guiding victim to safety
     */
    private fun startStealthModeSwitching() {
        // No longer used - manual switching only
        Log.i(TAG, "startStealthModeSwitching called but no action - manual mode")
    }

    /**
     * Call when info icon in decoy mode is clicked.
     * Switches back to escape UI from decoy mode.
     */
    fun onInfoIconClicked() {
        if (_showStealthDecoy.value && _isAlarmActive.value) {
            _showStealthDecoy.value = false
            Log.i(TAG, "ℹ️ Info icon clicked - switching back to escape UI")
            registerUserInteraction()
        }
    }

    /**
     * Manual decoy switch: call to activate decoy home screen (stealth mode)
     */
    fun switchToDecoy() {
        if (_isAlarmActive.value) {
            _showStealthDecoy.value = true
            Log.i(TAG, "ℹ️ Switching to decoy home screen")
        }
    }

    override fun onCleared() {
        super.onCleared()
        questionTimerJob?.cancel()
        escalationMonitorJob?.cancel()
        autoRetriggerJob?.cancel()
        continuousLocationJob?.cancel()
        secondQuestionTimerJob?.cancel()
        recordingJob?.cancel()
        journeyMonitoringJob?.cancel()
        stealthModeSwitchingJob?.cancel()
        safePlacesPreFetchJob?.cancel()
        stopLoudAlarm()
        stopRecording()
        _showStealthDecoy.value = false
        _showInfoIcon.value = false
        _showDecoyAvailable.value = false
        escapeLocationTrackingJob?.cancel()

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
