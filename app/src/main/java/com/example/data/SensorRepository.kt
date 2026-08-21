package com.example.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * SensorRepository handles 3-axis magnetometer hardware integration,
 * Exponential Moving Average (EMA) low-pass filtering for ferrous detection,
 * sliding-window variance calculations for AC electromagnetic conduit detection,
 * and Tare / Zero baseline calibration.
 */
class SensorRepository(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    
    // Attempt to acquire UNCALIBRATED magnetometer first to avoid OS hard-iron auto-centering,
    // falling back to standard CALIBRATED magnetic field sensor.
    private val magnetometer: Sensor? = sensorManager?.let { sm ->
        sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)
            ?: sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    val isHardwareAvailable: Boolean = magnetometer != null

    private val _sensorState = MutableStateFlow(MagneticReading())
    val sensorState: StateFlow<MagneticReading> = _sensorState.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSimulatedMode = MutableStateFlow(false)
    val isSimulatedMode: StateFlow<Boolean> = _isSimulatedMode.asStateFlow()

    // DSP Filter Constants & State
    private val alphaEma = 0.15f // EMA low-pass filter coefficient for DC ferrous detection
    private var filteredMagnitude = 0f
    private var ambientBaseline = 45.0f // Standard Earth field baseline in µT (~30..60 µT)
    private var isInitialized = false

    // Sliding window buffer for AC EMF high-rate variance calculation (50 samples ~ 200-300ms)
    private val windowCapacity = 50
    private val slidingWindow = FloatArray(windowCapacity)
    private var windowIndex = 0
    private var windowCount = 0

    private var simulationJob: Job? = null
    private val repoScope = CoroutineScope(Dispatchers.Default)

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event ?: return

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            processSensorValues(x, y, z, event.timestamp)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // No-op
        }
    }

    private fun processSensorValues(x: Float, y: Float, z: Float, timestampNanos: Long) {
        val rawMagnitude = sqrt(x * x + y * y + z * z)

        if (!isInitialized) {
            filteredMagnitude = rawMagnitude
            ambientBaseline = rawMagnitude
            isInitialized = true
        } else {
            // Exponential Moving Average (EMA) Low-Pass Filter:
            // B_filtered = α * |B_raw| + (1 - α) * B_filtered_prev
            filteredMagnitude = alphaEma * rawMagnitude + (1.0f - alphaEma) * filteredMagnitude
        }

        // Relative magnetic delta from calibrated tare baseline: ΔB = |filtered - baseline|
        val deltaMagnitude = abs(filteredMagnitude - ambientBaseline)

        // Slide window for AC variance calculation
        slidingWindow[windowIndex] = rawMagnitude
        windowIndex = (windowIndex + 1) % windowCapacity
        if (windowCount < windowCapacity) {
            windowCount++
        }

        // Calculate standard deviation / sample variance and peak-to-peak differential
        var sum = 0f
        var minVal = Float.MAX_VALUE
        var maxVal = Float.MIN_VALUE

        for (i in 0 until windowCount) {
            val v = slidingWindow[i]
            sum += v
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
        }

        val mean = if (windowCount > 0) sum / windowCount else 0f
        var varianceSum = 0f
        for (i in 0 until windowCount) {
            val diff = slidingWindow[i] - mean
            varianceSum += diff * diff
        }

        val variance = if (windowCount > 1) varianceSum / (windowCount - 1) else 0f
        val peakToPeak = if (windowCount > 0 && maxVal >= minVal) maxVal - minVal else 0f

        _sensorState.value = MagneticReading(
            rawX = x,
            rawY = y,
            rawZ = z,
            rawMagnitude = rawMagnitude,
            filteredMagnitude = filteredMagnitude,
            ambientBaseline = ambientBaseline,
            deltaMagnitude = deltaMagnitude,
            acVariance = variance,
            acPeakToPeak = peakToPeak,
            timestampNanos = timestampNanos
        )
    }

    /**
     * Start high-rate hardware sensor polling.
     */
    fun startListening() {
        if (_isListening.value) return

        if (magnetometer != null && sensorManager != null && !_isSimulatedMode.value) {
            val registered = try {
                sensorManager.registerListener(
                    sensorEventListener,
                    magnetometer,
                    SensorManager.SENSOR_DELAY_FASTEST
                )
            } catch (_: SecurityException) {
                try {
                    sensorManager.registerListener(
                        sensorEventListener,
                        magnetometer,
                        SensorManager.SENSOR_DELAY_GAME
                    )
                } catch (_: Throwable) {
                    sensorManager.registerListener(
                        sensorEventListener,
                        magnetometer,
                        SensorManager.SENSOR_DELAY_UI
                    )
                }
            } catch (_: Throwable) {
                false
            }

            if (registered) {
                _isListening.value = true
            } else {
                startSimulation()
            }
        } else {
            // If no hardware or simulated mode requested, run realistic DSP simulation
            startSimulation()
        }
    }

    /**
     * Stop sensor polling to save power when UI is backgrounded.
     */
    fun stopListening() {
        if (sensorManager != null) {
            try {
                sensorManager.unregisterListener(sensorEventListener)
            } catch (_: Exception) { }
        }
        stopSimulation()
        _isListening.value = false
    }

    /**
     * Zero / Tare Calibration:
     * Sets current filtered magnetic reading as the ambient baseline reference.
     */
    fun tareZeroCalibration() {
        if (filteredMagnitude > 0f) {
            ambientBaseline = filteredMagnitude
        } else {
            ambientBaseline = _sensorState.value.rawMagnitude
        }

        // Reset sliding window for fresh AC baseline
        windowIndex = 0
        windowCount = 0

        val current = _sensorState.value
        _sensorState.value = current.copy(
            ambientBaseline = ambientBaseline,
            deltaMagnitude = 0f
        )
    }

    /**
     * Toggle hardware simulation mode (allows testing stud / conduit detection on emulators or bench testing).
     */
    fun setSimulationMode(enabled: Boolean) {
        _isSimulatedMode.value = enabled
        if (_isListening.value) {
            stopListening()
            startListening()
        }
    }

    private fun startSimulation() {
        stopSimulation()
        _isListening.value = true
        simulationJob = repoScope.launch {
            var simTime = 0.0
            val baseField = 48.0f

            while (isActive) {
                simTime += 0.05
                // Synthesize a sweeping path passing over a drywall screw and an AC wire periodically
                // Every 6 seconds: 0..2s clear, 2..3s approaching stud, 3s direct screw center, 4..5s AC conduit
                val cycle = (simTime % 7.0)
                
                var simFerrousDelta = 0.0f
                var simAcNoise = 0.0f

                if (cycle in 1.5..3.5) {
                    // Approaching and passing stud/screw: Gaussian bell peak
                    val dist = (cycle - 2.5) * 3.0
                    simFerrousDelta = (8.5 * kotlin.math.exp(-dist * dist)).toFloat()
                }

                if (cycle in 4.0..5.8) {
                    // Passing live AC line: 50/60 Hz electromagnetic oscillation
                    val acEnvelope = (sin((cycle - 4.0) * Math.PI / 1.8)).toFloat()
                    simAcNoise = (sin(simTime * 60.0) * 3.8 * acEnvelope).toFloat()
                }

                val simulatedRaw = baseField + simFerrousDelta + simAcNoise + (Math.random().toFloat() * 0.2f - 0.1f)
                val x = baseField * 0.6f + simFerrousDelta * 0.8f
                val y = baseField * 0.3f + simAcNoise
                val z = sqrt(abs(simulatedRaw * simulatedRaw - x * x - y * y))

                processSensorValues(x, y, z, System.nanoTime())
                delay(20) // ~50 Hz update rate for simulation
            }
        }
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }
}
