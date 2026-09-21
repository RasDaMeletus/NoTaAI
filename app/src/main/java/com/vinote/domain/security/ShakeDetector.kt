package com.vinote.domain.security

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects rapid device shakes (e.g. 3 shakes within 1.5 seconds)
 * used for triggering NoTa Panic Mode (hiding all amounts).
 */
class ShakeDetector(
    context: Context,
    private val requiredShakeCount: Int = 3,
    private val timeWindowMs: Long = 1500L,
    private val accelerationThreshold: Float = 12.5f,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeCount = 0
    private var firstShakeTimestamp = 0L
    private var lastShakeTimestamp = 0L

    fun startListening() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        // Threshold for a vigorous shake
        if (gForce > (accelerationThreshold / SensorManager.GRAVITY_EARTH)) {
            val now = System.currentTimeMillis()

            // Debounce rapid events from the same direction movement
            if (now - lastShakeTimestamp < 200L) return

            if (firstShakeTimestamp == 0L || now - firstShakeTimestamp > timeWindowMs) {
                firstShakeTimestamp = now
                shakeCount = 1
            } else {
                shakeCount++
            }

            lastShakeTimestamp = now

            if (shakeCount >= requiredShakeCount) {
                shakeCount = 0
                firstShakeTimestamp = 0L
                onShakeDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
