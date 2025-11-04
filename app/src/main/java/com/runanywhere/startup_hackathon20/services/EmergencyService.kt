package com.runanywhere.startup_hackathon20.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.runanywhere.startup_hackathon20.MainActivity
import com.runanywhere.startup_hackathon20.R
import java.io.File

/**
 * Foreground service for emergency monitoring
 * Handles:
 * - Background location tracking
 * - Audio evidence recording
 * - Persistent emergency alert
 */
class EmergencyService : Service() {

    companion object {
        private const val TAG = "EmergencyService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "emergency_channel"

        const val ACTION_START_EMERGENCY = "action_start_emergency"
        const val ACTION_STOP_EMERGENCY = "action_stop_emergency"
        const val ACTION_START_RECORDING = "action_start_recording"
        const val ACTION_STOP_RECORDING = "action_stop_recording"

        const val EXTRA_SESSION_ID = "extra_session_id"
    }

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentSessionId: String? = null
    private var isRecording = false
    private var recordingFile: File? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "EmergencyService created")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_EMERGENCY -> {
                    currentSessionId = it.getStringExtra(EXTRA_SESSION_ID)
                    startEmergencyMode()
                }

                ACTION_STOP_EMERGENCY -> {
                    stopEmergencyMode()
                }

                ACTION_START_RECORDING -> {
                    startAudioRecording()
                }

                ACTION_STOP_RECORDING -> {
                    stopAudioRecording()
                }
            }
        }

        return START_STICKY
    }

    private fun startEmergencyMode() {
        Log.i(TAG, "Starting emergency mode: $currentSessionId")

        // Start foreground service with notification
        val notification = createNotification("Emergency Active", "Monitoring your safety...")
        startForeground(NOTIFICATION_ID, notification)

        // Start location tracking
        startLocationTracking()
    }

    private fun stopEmergencyMode() {
        Log.i(TAG, "Stopping emergency mode")

        stopLocationTracking()
        stopAudioRecording()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // 10 seconds
        ).apply {
            setMinUpdateIntervalMillis(5000L) // 5 seconds
            setMaxUpdateDelayMillis(15000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    onLocationUpdate(location)
                }
            }
        }

        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )

        Log.i(TAG, "Location tracking started")
    }

    private fun stopLocationTracking() {
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
        Log.i(TAG, "Location tracking stopped")
    }

    private fun onLocationUpdate(location: Location) {
        Log.d(TAG, "Location update: ${location.latitude}, ${location.longitude}")

        // Broadcast location to ViewModel
        val intent = Intent("com.runanywhere.startup_hackathon20.LOCATION_UPDATE").apply {
            putExtra("latitude", location.latitude)
            putExtra("longitude", location.longitude)
            putExtra("accuracy", location.accuracy)
            putExtra("timestamp", location.time)
            putExtra("session_id", currentSessionId)
        }
        sendBroadcast(intent)

        // Update notification with location
        val notification = createNotification(
            "Emergency Active",
            "Last location: ${location.latitude.format()}, ${location.longitude.format()}"
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startAudioRecording() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Audio recording permission not granted")
            return
        }

        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }

        try {
            // Create evidence directory
            val evidenceDir = File(filesDir, "evidence/$currentSessionId")
            if (!evidenceDir.exists()) {
                evidenceDir.mkdirs()
            }

            recordingFile = File(evidenceDir, "audio_${System.currentTimeMillis()}.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(recordingFile?.absolutePath)

                prepare()
                start()
            }

            isRecording = true
            Log.i(TAG, "Audio recording started: ${recordingFile?.absolutePath}")

            // Update notification
            val notification = createNotification(
                "🔴 Recording Evidence",
                "Audio recording in progress..."
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio recording", e)
            isRecording = false
        }
    }

    private fun stopAudioRecording() {
        if (!isRecording) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            Log.i(TAG, "Audio recording stopped: ${recordingFile?.absolutePath}")

            // Broadcast recording saved
            recordingFile?.let { file ->
                val intent = Intent("com.runanywhere.startup_hackathon20.RECORDING_SAVED").apply {
                    putExtra("file_path", file.absolutePath)
                    putExtra("session_id", currentSessionId)
                }
                sendBroadcast(intent)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recording", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical emergency notifications"
                setShowBadge(true)
                enableVibration(true)
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
        stopAudioRecording()
        Log.d(TAG, "EmergencyService destroyed")
    }
}

private fun Double.format(): String = String.format("%.4f", this)
