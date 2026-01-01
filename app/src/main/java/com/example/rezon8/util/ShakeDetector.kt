package com.mossglen.lithos.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * ShakeDetector - Detects shake gestures using the accelerometer.
 *
 * Used for:
 * - Extending sleep timer when shaking the phone
 *
 * Features:
 * - Configurable sensitivity
 * - Cooldown period to prevent multiple triggers
 * - Auto-starts/stops with lifecycle
 */
class ShakeDetector(
    private val context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    companion object {
        private const val TAG = "ShakeDetector"

        // Shake detection thresholds
        private const val SHAKE_THRESHOLD_GRAVITY = 2.5f  // Moderate shake
        private const val SHAKE_SLOP_TIME_MS = 500        // Minimum time between shakes
        private const val SHAKE_COUNT_RESET_TIME_MS = 3000 // Reset shake count after 3 seconds

        // Required consecutive shakes for a valid shake gesture
        private const val MIN_SHAKE_COUNT = 2
    }

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private var shakeTimestamp: Long = 0
    private var shakeCount: Int = 0
    private var lastShakeTime: Long = 0

    private var isListening = false

    /**
     * Start listening for shake gestures.
     */
    fun start() {
        if (isListening) return

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) {
            sensorManager?.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
            isListening = true
            Log.d(TAG, "Started shake detection")
        } else {
            Log.w(TAG, "No accelerometer sensor available")
        }
    }

    /**
     * Stop listening for shake gestures.
     */
    fun stop() {
        if (!isListening) return

        sensorManager?.unregisterListener(this)
        isListening = false
        shakeCount = 0
        Log.d(TAG, "Stopped shake detection")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate gravity-adjusted acceleration
        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // Calculate total g-force
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            val now = System.currentTimeMillis()

            // Ignore shakes too close together
            if (now - shakeTimestamp < SHAKE_SLOP_TIME_MS) {
                return
            }

            // Reset shake count if too much time has passed
            if (now - lastShakeTime > SHAKE_COUNT_RESET_TIME_MS) {
                shakeCount = 0
            }

            shakeTimestamp = now
            lastShakeTime = now
            shakeCount++

            Log.d(TAG, "Shake detected: count=$shakeCount, gForce=$gForce")

            // Trigger callback after minimum shake count
            if (shakeCount >= MIN_SHAKE_COUNT) {
                shakeCount = 0  // Reset count after triggering
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
