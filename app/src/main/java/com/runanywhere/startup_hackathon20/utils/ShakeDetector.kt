package com.runanywhere.startup_hackathon20.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * ShakeDetector - Detects shake gestures using accelerometer
 * Inspired by Motorola's flashlight shake activation
 *
 * Usage:
 * ```
 * val shakeDetector = ShakeDetector(context) {
 *     // Shake detected - trigger emergency
 * }
 * shakeDetector.start()
 * // Later...
 * shakeDetector.stop()
 * ```
 */
class ShakeDetector(
    context: Context,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    companion object {
        private const val TAG = "ShakeDetector"

        // Shake detection parameters (tuned for deliberate shakes)
        private const val SHAKE_THRESHOLD = 15.0f // Acceleration force threshold (m/s²)
        private const val SHAKE_COUNT_THRESHOLD = 3 // Number of shakes required
        private const val SHAKE_TIME_WINDOW = 800L // Time window for shake sequence (ms)
        private const val SHAKE_COOLDOWN = 2000L // Cooldown between shake activations (ms)
    }

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime = 0L
    private var shakeCount = 0
    private var firstShakeTime = 0L
    private var lastTriggerTime = 0L

    private var isEnabled = false

    init {
        if (accelerometer == null) {
            Log.w(TAG, "⚠️ No accelerometer sensor available on this device")
        } else {
            Log.i(TAG, "✅ ShakeDetector initialized with accelerometer")
        }
    }

    /**
     * Start listening for shake gestures
     */
    fun start() {
        if (accelerometer == null) {
            Log.w(TAG, "Cannot start - no accelerometer available")
            return
        }

        if (isEnabled) {
            Log.d(TAG, "ShakeDetector already running")
            return
        }

        isEnabled = true
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI // UI delay is sufficient for shake detection
        )
        Log.i(
            TAG,
            "🎯 ShakeDetector started - shake phone ${SHAKE_COUNT_THRESHOLD}x to trigger emergency"
        )
    }

    /**
     * Stop listening for shake gestures
     */
    fun stop() {
        if (!isEnabled) return

        isEnabled = false
        sensorManager.unregisterListener(this)
        resetShakeDetection()
        Log.i(TAG, "🛑 ShakeDetector stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val currentTime = System.currentTimeMillis()

        // Check cooldown period to prevent accidental double-triggers
        if (currentTime - lastTriggerTime < SHAKE_COOLDOWN) {
            return
        }

        // Calculate acceleration magnitude (excluding gravity)
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate total acceleration (magnitude of acceleration vector)
        val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        // Remove gravity (9.81 m/s²) to get actual movement acceleration
        val accelerationWithoutGravity = acceleration - SensorManager.GRAVITY_EARTH

        // Detect shake if acceleration exceeds threshold
        if (accelerationWithoutGravity > SHAKE_THRESHOLD) {
            val timeSinceLastShake = currentTime - lastShakeTime

            // If this is the first shake or within time window
            if (shakeCount == 0) {
                // First shake in sequence
                firstShakeTime = currentTime
                shakeCount = 1
                lastShakeTime = currentTime
                Log.d(TAG, "🔔 Shake detected (1/${SHAKE_COUNT_THRESHOLD})")
            } else if (timeSinceLastShake > 100) { // Debounce (100ms between individual shakes)
                val timeSinceFirstShake = currentTime - firstShakeTime

                if (timeSinceFirstShake <= SHAKE_TIME_WINDOW) {
                    // Valid shake within time window
                    shakeCount++
                    lastShakeTime = currentTime
                    Log.d(TAG, "🔔 Shake detected (${shakeCount}/${SHAKE_COUNT_THRESHOLD})")

                    // Check if we reached the threshold
                    if (shakeCount >= SHAKE_COUNT_THRESHOLD) {
                        // SHAKE DETECTED - TRIGGER EMERGENCY!
                        Log.i(TAG, "🚨 SHAKE SEQUENCE COMPLETED - TRIGGERING EMERGENCY!")
                        lastTriggerTime = currentTime
                        resetShakeDetection()
                        onShakeDetected()
                    }
                } else {
                    // Time window exceeded, reset and start new sequence
                    Log.d(TAG, "⏱️ Shake sequence timeout - restarting")
                    firstShakeTime = currentTime
                    shakeCount = 1
                    lastShakeTime = currentTime
                }
            }
        }

        // Auto-reset if time window exceeded
        if (shakeCount > 0 && (currentTime - firstShakeTime) > SHAKE_TIME_WINDOW) {
            resetShakeDetection()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for shake detection
    }

    private fun resetShakeDetection() {
        shakeCount = 0
        firstShakeTime = 0L
        lastShakeTime = 0L
    }

    /**
     * Check if shake detection is supported on this device
     */
    fun isSupported(): Boolean = accelerometer != null

    /**
     * Check if shake detection is currently active
     */
    fun isActive(): Boolean = isEnabled
}
