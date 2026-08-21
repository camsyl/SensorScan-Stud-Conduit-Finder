package com.example

import com.example.data.MagnetometerSampleNormalizer
import com.example.data.RealDetectionEngine
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealDetectionEngineTest {

    @Test
    fun `uncalibrated magnetometer bias is subtracted`() {
        val normalized = MagnetometerSampleNormalizer.normalize(
            floatArrayOf(31f, -12f, 45f, 3f, -2f, 5f),
            isUncalibrated = true
        )

        assertEquals(28f, normalized[0], 0.001f)
        assertEquals(-10f, normalized[1], 0.001f)
        assertEquals(40f, normalized[2], 0.001f)
    }

    @Test
    fun `stationary calibration learns baseline and robust noise floor`() {
        val engine = RealDetectionEngine(
            calibrationDurationNanos = 500_000_000L,
            minimumCalibrationSamples = 40
        )
        var reading = engine.processMagnetic(30f, 40f, 0f, 0L, true, "test")
        for (index in 1..120) {
            val noise = if (index % 2 == 0) 0.08f else -0.08f
            reading = engine.processMagnetic(30f + noise, 40f, 0f, index * 5_000_000L, true, "test")
        }

        assertTrue(reading.isCalibrationReady)
        assertEquals(50f, reading.ambientBaseline, 0.2f)
        assertTrue(reading.noiseFloorMicroTesla >= 0.15f)
        assertTrue(reading.signalToNoise < 2f)
    }

    @Test
    fun `motion is exposed so detections can be rejected`() {
        val engine = calibratedEngine()
        engine.updateGyroscope(1.2f, 0f, 0f)

        val reading = engine.processMagnetic(40f, 40f, 0f, 900_000_000L, true, "test")

        assertFalse(reading.isMotionStable)
    }

    @Test
    fun `sustained 60 hertz field is isolated spectrally`() {
        val engine = calibratedEngine()
        var reading = engine.processMagnetic(30f, 40f, 0f, 700_000_000L, true, "test")
        for (index in 1..260) {
            val timeSeconds = index / 200.0
            val ac = (2.0 * sin(2.0 * PI * 60.0 * timeSeconds)).toFloat()
            reading = engine.processMagnetic(
                30f + ac,
                40f,
                0f,
                700_000_000L + index * 5_000_000L,
                true,
                "test"
            )
        }

        assertTrue(reading.isAcDetectionSupported)
        assertEquals(60f, reading.acFrequencyHz ?: 0f, 0.1f)
        assertTrue(reading.acSignalToNoise > 8f)
    }

    @Test
    fun `slow magnetometer never claims AC frequency support`() {
        val engine = calibratedEngine()
        var reading = engine.processMagnetic(30f, 40f, 0f, 700_000_000L, true, "test")
        for (index in 1..100) {
            val timeSeconds = index / 80.0
            val ac = sin(2.0 * PI * 60.0 * timeSeconds).toFloat()
            reading = engine.processMagnetic(
                30f + ac,
                40f,
                0f,
                700_000_000L + index * 12_500_000L,
                true,
                "test"
            )
        }

        assertFalse(reading.isAcDetectionSupported)
        assertNull(reading.acFrequencyHz)
    }

    @Test
    fun `slow magnetic sweep is not mistaken for mains frequency`() {
        val engine = calibratedEngine()
        var reading = engine.processMagnetic(30f, 40f, 0f, 700_000_000L, true, "test")
        for (index in 1..260) {
            val timeSeconds = index / 200.0
            val sweepMotion = (3.0 * sin(2.0 * PI * 5.0 * timeSeconds)).toFloat()
            reading = engine.processMagnetic(
                30f + sweepMotion,
                40f,
                0f,
                700_000_000L + index * 5_000_000L,
                true,
                "test"
            )
        }

        assertTrue(reading.isAcDetectionSupported)
        assertTrue(reading.acSignalToNoise < 5f)
    }

    private fun calibratedEngine(): RealDetectionEngine {
        val engine = RealDetectionEngine(
            calibrationDurationNanos = 400_000_000L,
            minimumCalibrationSamples = 40
        )
        for (index in 0..100) {
            engine.processMagnetic(30f, 40f, 0f, index * 5_000_000L, true, "test")
        }
        return engine
    }
}
