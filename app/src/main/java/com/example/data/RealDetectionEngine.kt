package com.example.data

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure signal-processing core for real phone sensors. It deliberately contains
 * no generated-data path so the same implementation can be unit tested.
 */
class RealDetectionEngine(
    private val calibrationDurationNanos: Long = 1_500_000_000L,
    private val minimumCalibrationSamples: Int = 30,
    private val acWindowNanos: Long = 1_000_000_000L,
    private val minimumAcSampleRateHz: Float = 130f
) {
    private data class TimedVector(
        val timestampNanos: Long,
        val x: Float,
        val y: Float,
        val z: Float
    )

    private val calibrationMagnitudes = ArrayList<Float>(256)
    private val acSamples = ArrayDeque<TimedVector>()

    private var calibrationStartedNanos = 0L
    private var baseline = 0f
    private var noiseFloor = MINIMUM_NOISE_FLOOR_MICRO_TESLA
    private var filteredMagnitude = 0f
    private var isFilterInitialized = false
    private var calibrationReady = false

    private var accelerometerAvailable = false
    private var gyroscopeAvailable = false
    private var accelerationDeviationEma = 0f
    private var angularVelocityEma = 0f
    private var lastAcAnalysisNanos = 0L
    private var lastAcAnalysis = AcAnalysis()

    fun beginCalibration() {
        calibrationMagnitudes.clear()
        calibrationStartedNanos = 0L
        calibrationReady = false
        acSamples.clear()
        lastAcAnalysisNanos = 0L
        lastAcAnalysis = AcAnalysis()
    }

    fun updateAccelerometer(x: Float, y: Float, z: Float) {
        accelerometerAvailable = true
        val magnitude = sqrt(x * x + y * y + z * z)
        val deviation = abs(magnitude - STANDARD_GRAVITY)
        accelerationDeviationEma = ema(accelerationDeviationEma, deviation, MOTION_EMA_ALPHA)
    }

    fun updateGyroscope(x: Float, y: Float, z: Float) {
        gyroscopeAvailable = true
        val magnitude = sqrt(x * x + y * y + z * z)
        angularVelocityEma = ema(angularVelocityEma, magnitude, MOTION_EMA_ALPHA)
    }

    fun processMagnetic(
        x: Float,
        y: Float,
        z: Float,
        timestampNanos: Long,
        sensorReliable: Boolean,
        sensorName: String
    ): MagneticReading {
        val magnitude = sqrt(x * x + y * y + z * z)
        filteredMagnitude = if (!isFilterInitialized) {
            isFilterInitialized = true
            magnitude
        } else {
            ema(filteredMagnitude, magnitude, MAGNETIC_EMA_ALPHA)
        }

        val motionStable = isMotionStable()
        if (!calibrationReady && motionStable && sensorReliable) {
            if (calibrationStartedNanos == 0L) calibrationStartedNanos = timestampNanos
            calibrationMagnitudes.add(magnitude)
            val duration = timestampNanos - calibrationStartedNanos
            if (duration >= calibrationDurationNanos && calibrationMagnitudes.size >= minimumCalibrationSamples) {
                finishCalibration()
            }
        } else if (!motionStable && !calibrationReady) {
            calibrationStartedNanos = 0L
            calibrationMagnitudes.clear()
        }

        addAcSample(TimedVector(timestampNanos, x, y, z))
        if (timestampNanos - lastAcAnalysisNanos >= AC_ANALYSIS_INTERVAL_NANOS) {
            lastAcAnalysis = analyzeAcFrequency()
            lastAcAnalysisNanos = timestampNanos
        }
        val acAnalysis = lastAcAnalysis

        val delta = if (calibrationReady) abs(filteredMagnitude - baseline) else 0f
        val signalToNoise = if (calibrationReady) delta / noiseFloor else 0f

        // Very slow baseline tracking is allowed only while stationary and clear.
        if (calibrationReady && motionStable && signalToNoise < 2.0f) {
            baseline = ema(baseline, filteredMagnitude, BASELINE_TRACKING_ALPHA)
        }

        val durationProgress = if (calibrationStartedNanos == 0L) 0f else
            ((timestampNanos - calibrationStartedNanos).toDouble() / calibrationDurationNanos).toFloat()
        val sampleProgress = calibrationMagnitudes.size.toFloat() / minimumCalibrationSamples.toFloat()
        val calibrationProgress = if (calibrationReady) 1f else minOf(durationProgress, sampleProgress).coerceIn(0f, 1f)

        return MagneticReading(
            rawX = x,
            rawY = y,
            rawZ = z,
            rawMagnitude = magnitude,
            filteredMagnitude = filteredMagnitude,
            ambientBaseline = baseline,
            deltaMagnitude = delta,
            noiseFloorMicroTesla = noiseFloor,
            signalToNoise = signalToNoise,
            acSignalToNoise = acAnalysis.signalToNoise,
            acFrequencyHz = acAnalysis.frequencyHz,
            acFieldStrengthMicroTesla = acAnalysis.fieldStrengthMicroTesla,
            sampleRateHz = acAnalysis.sampleRateHz,
            isAcDetectionSupported = acAnalysis.supported,
            isMotionStable = motionStable,
            isCalibrationReady = calibrationReady,
            calibrationProgress = calibrationProgress,
            isSensorReliable = sensorReliable,
            sensorName = sensorName,
            timestampNanos = timestampNanos
        )
    }

    private fun isMotionStable(): Boolean {
        val accelerationStable = !accelerometerAvailable || accelerationDeviationEma <= MAX_ACCELERATION_DEVIATION
        val rotationStable = !gyroscopeAvailable || angularVelocityEma <= MAX_ANGULAR_VELOCITY
        return accelerationStable && rotationStable
    }

    private fun finishCalibration() {
        baseline = median(calibrationMagnitudes)
        val deviations = calibrationMagnitudes.map { abs(it - baseline) }
        noiseFloor = max(MINIMUM_NOISE_FLOOR_MICRO_TESLA, median(deviations) * MAD_TO_SIGMA)
        filteredMagnitude = baseline
        calibrationReady = true
        calibrationMagnitudes.clear()
    }

    private fun addAcSample(sample: TimedVector) {
        acSamples.addLast(sample)
        val cutoff = sample.timestampNanos - acWindowNanos
        while (acSamples.isNotEmpty() && acSamples.first().timestampNanos < cutoff) {
            acSamples.removeFirst()
        }
    }

    private data class AcAnalysis(
        val supported: Boolean = false,
        val sampleRateHz: Float = 0f,
        val signalToNoise: Float = 0f,
        val frequencyHz: Float? = null,
        val fieldStrengthMicroTesla: Float = 0f
    )

    private fun analyzeAcFrequency(): AcAnalysis {
        if (acSamples.size < 32) return AcAnalysis()
        val first = acSamples.first().timestampNanos
        val last = acSamples.last().timestampNanos
        val durationSeconds = (last - first).toDouble() / NANOS_PER_SECOND
        if (durationSeconds < 0.75) return AcAnalysis()

        val sampleRate = ((acSamples.size - 1) / durationSeconds).toFloat()
        if (sampleRate < minimumAcSampleRateHz) return AcAnalysis(sampleRateHz = sampleRate)

        val values = acSamples.toList()
        val meanX = values.map { it.x }.average()
        val meanY = values.map { it.y }.average()
        val meanZ = values.map { it.z }.average()

        fun powerAt(frequencyHz: Double): Double {
            var realX = 0.0
            var imaginaryX = 0.0
            var realY = 0.0
            var imaginaryY = 0.0
            var realZ = 0.0
            var imaginaryZ = 0.0
            values.forEachIndexed { index, sample ->
                val time = (sample.timestampNanos - first).toDouble() / NANOS_PER_SECOND
                val window = 0.5 - 0.5 * cos((2.0 * PI * index) / (values.size - 1))
                val angle = 2.0 * PI * frequencyHz * time
                val cosine = cos(angle)
                val negativeSine = -sin(angle)
                val x = (sample.x - meanX) * window
                val y = (sample.y - meanY) * window
                val z = (sample.z - meanZ) * window
                realX += x * cosine
                imaginaryX += x * negativeSine
                realY += y * cosine
                imaginaryY += y * negativeSine
                realZ += z * cosine
                imaginaryZ += z * negativeSine
            }
            val normalization = values.size.toDouble() * values.size.toDouble()
            return (
                realX * realX + imaginaryX * imaginaryX +
                    realY * realY + imaginaryY * imaginaryY +
                    realZ * realZ + imaginaryZ * imaginaryZ
                ) / normalization
        }

        val power50 = powerAt(50.0)
        val power60 = powerAt(60.0)
        val targetFrequency = if (power60 > power50) 60f else 50f
        val targetPower = max(power50, power60)
        val noisePowers = listOf(25.0, 30.0, 35.0, 40.0, 45.0).map(::powerAt).sorted()
        val noisePower = noisePowers[noisePowers.size / 2].coerceAtLeast(MINIMUM_SPECTRAL_POWER)
        val signalToNoise = (targetPower / noisePower).toFloat()
        val fieldStrength = (2.0 * sqrt(targetPower)).toFloat()

        return AcAnalysis(
            supported = true,
            sampleRateHz = sampleRate,
            signalToNoise = signalToNoise,
            frequencyHz = targetFrequency,
            fieldStrengthMicroTesla = fieldStrength
        )
    }

    private fun median(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2f else sorted[middle]
    }

    private fun ema(previous: Float, current: Float, alpha: Float): Float =
        if (previous == 0f) current else alpha * current + (1f - alpha) * previous

    private companion object {
        const val STANDARD_GRAVITY = 9.80665f
        const val MAGNETIC_EMA_ALPHA = 0.14f
        const val MOTION_EMA_ALPHA = 0.18f
        const val BASELINE_TRACKING_ALPHA = 0.002f
        const val MAX_ACCELERATION_DEVIATION = 0.45f
        const val MAX_ANGULAR_VELOCITY = 0.20f
        const val MINIMUM_NOISE_FLOOR_MICRO_TESLA = 0.15f
        const val MAD_TO_SIGMA = 1.4826f
        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val MINIMUM_SPECTRAL_POWER = 1e-8
        const val AC_ANALYSIS_INTERVAL_NANOS = 100_000_000L
    }
}

/** Converts Android calibrated or uncalibrated events into a calibrated vector. */
object MagnetometerSampleNormalizer {
    fun normalize(values: FloatArray, isUncalibrated: Boolean): FloatArray {
        require(values.size >= 3) { "Magnetometer event must contain at least three values" }
        return if (isUncalibrated && values.size >= 6) {
            floatArrayOf(
                values[0] - values[3],
                values[1] - values[4],
                values[2] - values[5]
            )
        } else {
            floatArrayOf(values[0], values[1], values[2])
        }
    }
}
