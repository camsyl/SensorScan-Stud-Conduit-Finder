package com.example.data

/**
 * Operating scan mode for the stud and conduit detector.
 */
enum class ScanMode(val label: String, val description: String) {
    DUAL("Dual Mode", "Detect both ferrous fasteners and AC electrical lines simultaneously"),
    FERROUS_ONLY("Ferrous Studs", "Focus on drywall screws, nails, and metal framing"),
    AC_EMF_ONLY("AC Electrical", "Focus on live 50/60Hz electromagnetic conduit and wiring")
}

/**
 * Sensitivity preset adjusting full-scale range in micro-Teslas (µT)
 * and AC variance trigger thresholds.
 */
enum class SensitivityLevel(
    val label: String,
    val maxDeltaMicroTesla: Float, // Full scale for 100% proximity
    val anomalyThresholdMicroTesla: Float,
    val centerThresholdMicroTesla: Float,
    val acVarianceThreshold: Float
) {
    FINE(
        label = "Fine (Deep / Small Screws)",
        maxDeltaMicroTesla = 4.0f,
        anomalyThresholdMicroTesla = 0.8f,
        centerThresholdMicroTesla = 2.5f,
        acVarianceThreshold = 0.6f
    ),
    MEDIUM(
        label = "Medium (Standard Drywall)",
        maxDeltaMicroTesla = 10.0f,
        anomalyThresholdMicroTesla = 1.8f,
        centerThresholdMicroTesla = 6.0f,
        acVarianceThreshold = 1.2f
    ),
    COARSE(
        label = "Coarse (Surface Metal / High Noise)",
        maxDeltaMicroTesla = 22.0f,
        anomalyThresholdMicroTesla = 4.0f,
        centerThresholdMicroTesla = 14.0f,
        acVarianceThreshold = 2.5f
    )
}

/**
 * Categorical detection state for Ferrous anomaly.
 */
enum class FerrousDetectionState(val label: String, val levelCode: Int) {
    CLEAR("Ambient / Clear", 0),
    ANOMALY("Ferrous Anomaly Detected", 1),
    CENTER_TARGET("Direct Center (Screw/Stud)", 2)
}

/**
 * Audio feedback sound profile.
 */
enum class AudioMode(val label: String) {
    CONTINUOUS_TONE("Pitch Synth"),
    PULSE_BEEPS("Geiger Pulse"),
    MUTED("Muted")
}

/**
 * Snapshot of instantaneous and filtered sensor physics.
 */
data class MagneticReading(
    val rawX: Float = 0f,
    val rawY: Float = 0f,
    val rawZ: Float = 0f,
    val rawMagnitude: Float = 0f, // |B| in µT
    val filteredMagnitude: Float = 0f, // EMA filtered magnitude
    val ambientBaseline: Float = 0f, // Tared baseline
    val deltaMagnitude: Float = 0f, // ΔB = |filtered - baseline|
    val acVariance: Float = 0f, // Sliding window variance (AC EMF)
    val acPeakToPeak: Float = 0f, // Sliding window max - min (µT)
    val proximityPercent: Float = 0f, // 0..100%
    val detectionState: FerrousDetectionState = FerrousDetectionState.CLEAR,
    val isAcHazardActive: Boolean = false,
    val timestampNanos: Long = 0L
)

/**
 * Data point for the 5-second oscilloscope sweep chart.
 */
data class SweepPoint(
    val timestampMillis: Long,
    val deltaB: Float,
    val acVariance: Float,
    val proximityPercent: Float
)
