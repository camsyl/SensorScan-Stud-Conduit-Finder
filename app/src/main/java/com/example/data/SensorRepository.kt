package com.example.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the real Android sensor lifecycle. Unavailable hardware produces an
 * explicit error and never produces generated readings.
 */
class SensorRepository(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val calibratedMagnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val uncalibratedMagnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)
    private val magnetometer = calibratedMagnetometer ?: uncalibratedMagnetometer
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val detectionEngine = RealDetectionEngine()

    val isHardwareAvailable: Boolean = magnetometer != null

    private val _sensorState = MutableStateFlow(
        MagneticReading(
            sensorError = if (magnetometer == null) NO_MAGNETOMETER_MESSAGE else null,
            sensorName = magnetometer?.let { "${it.name} (${it.vendor})" }.orEmpty()
        )
    )
    val sensorState: StateFlow<MagneticReading> = _sensorState.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var sensorReliable = true

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event ?: return
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> if (event.values.size >= 3) {
                    detectionEngine.updateAccelerometer(event.values[0], event.values[1], event.values[2])
                }

                Sensor.TYPE_GYROSCOPE -> if (event.values.size >= 3) {
                    detectionEngine.updateGyroscope(event.values[0], event.values[1], event.values[2])
                }

                Sensor.TYPE_MAGNETIC_FIELD,
                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> {
                    val vector = MagnetometerSampleNormalizer.normalize(
                        event.values,
                        isUncalibrated = event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED
                    )
                    _sensorState.value = detectionEngine.processMagnetic(
                        x = vector[0],
                        y = vector[1],
                        z = vector[2],
                        timestampNanos = event.timestamp,
                        sensorReliable = sensorReliable && event.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE,
                        sensorName = "${event.sensor.name} (${event.sensor.vendor})"
                    )
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD ||
                sensor?.type == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED
            ) {
                sensorReliable = accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE
            }
        }
    }

    /** Starts physical sensing and returns false if the magnetometer cannot be used. */
    fun startListening(): Boolean {
        if (_isListening.value) return true
        val manager = sensorManager
        val magneticSensor = magnetometer
        if (manager == null || magneticSensor == null) {
            publishSensorError(NO_MAGNETOMETER_MESSAGE)
            return false
        }

        val magneticRegistered = try {
            manager.registerListener(
                sensorEventListener,
                magneticSensor,
                MAGNETIC_SAMPLING_PERIOD_MICROS
            )
        } catch (_: SecurityException) {
            false
        } catch (_: Throwable) {
            false
        }

        if (!magneticRegistered) {
            publishSensorError("The phone's magnetometer could not be started. Real detection is unavailable.")
            return false
        }

        accelerometer?.let {
            try {
                manager.registerListener(sensorEventListener, it, MOTION_SAMPLING_PERIOD_MICROS)
            } catch (_: Throwable) {
                // Magnetometer detection remains available without this motion sensor.
            }
        }
        gyroscope?.let {
            try {
                manager.registerListener(sensorEventListener, it, MOTION_SAMPLING_PERIOD_MICROS)
            } catch (_: Throwable) {
                // Accelerometer-only motion gating is still useful.
            }
        }

        _isListening.value = true
        detectionEngine.beginCalibration()
        return true
    }

    fun stopListening() {
        try {
            sensorManager?.unregisterListener(sensorEventListener)
        } catch (_: Throwable) {
            // Already stopped or sensor service unavailable.
        }
        _isListening.value = false
    }

    fun tareZeroCalibration() {
        if (!isHardwareAvailable || !_isListening.value) return
        detectionEngine.beginCalibration()
        _sensorState.value = _sensorState.value.copy(
            deltaMagnitude = 0f,
            signalToNoise = 0f,
            isCalibrationReady = false,
            calibrationProgress = 0f,
            detectionState = FerrousDetectionState.CLEAR,
            isAcHazardActive = false
        )
    }

    private fun publishSensorError(message: String) {
        _isListening.value = false
        _sensorState.value = _sensorState.value.copy(
            sensorError = message,
            isCalibrationReady = false,
            isAcDetectionSupported = false,
            proximityPercent = 0f,
            detectionState = FerrousDetectionState.CLEAR,
            isAcHazardActive = false
        )
    }

    private companion object {
        const val MAGNETIC_SAMPLING_PERIOD_MICROS = 5_000
        const val MOTION_SAMPLING_PERIOD_MICROS = 10_000
        const val NO_MAGNETOMETER_MESSAGE =
            "No physical magnetometer was found on this phone. SensorScan cannot perform real detection."
    }
}
