package com.example.soloproject

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import kotlin.math.abs
import kotlin.math.sqrt

//This listen to sensor events
class ShakeDetector(
    private val onShakeStarted: () -> Unit,
    private val onShakeStopped: () -> Unit
) : SensorEventListener {

    private var isShaking = false
    private val handler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable {
        if (isShaking) {
            isShaking = false
            onShakeStopped()
        }
    }

    //This listens to sensor changes and handles the values
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        if (abs(acceleration) > 12f) {
            if (!isShaking) {
                isShaking = true
                onShakeStarted()
            }
            handler.removeCallbacks(stopRunnable)
            handler.postDelayed(stopRunnable, 500L)
        }
    }

    //This is for changes in accuracy, not used
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
