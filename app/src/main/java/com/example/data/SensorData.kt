package com.example.data

/**
 * Operating scan mode for the stud and conduit detector.
 */
enum class ScanMode(val label: String, val description: String) {
    DUAL("Dual Mode", "Detect ferrous fasteners and possible energized 50/60 Hz conductors"),
    FERROUS_ONLY("Fasteners", "Focus on drywall screws, nails, steel conduit, and metal framing"),
    AC_EMF_ONLY("Energized AC", "Look for a sustained 50/60 Hz magnetic signature from current flow")
}

/**
 * Device-independent sensitivity presets. Absolute floors prevent a very quiet
 * sensor from becoming over-sensitive, while sigma/SNR thresholds adapt to the
 * measured noise of each phone.
 */
enum class SensitivityLevel(
    val label: String,
    val maxSignalToNoise: Float,
    val anomalySignalToNoise: Float,
    val centerSignalToNoise: Float,
    val minimumAnomalyMicroTesla: Float,
    val minimumCenterMicroTesla: Float,
    val acSignalToNoiseThreshold: Float
) {
    FINE(
        label = "Fine (quiet walls / small fasteners)",
        maxSignalToNoise = 14.0f,
        anomalySignalToNoise = 3.5f,
        centerSignalToNoise = 7.0f,
        minimumAnomalyMicroTesla = 0.7f,
        minimumCenterMicroTesla = 1.8f,
        acSignalToNoiseThreshold = 5.0f
    ),
    MEDIUM(
        label = "Medium (standard drywall)",
        maxSignalToNoise = 22.0f,
        anomalySignalToNoise = 5.0f,
        centerSignalToNoise = 10.0f,
        minimumAnomalyMicroTesla = 1.1f,
        minimumCenterMicroTesla = 3.0f,
        acSignalToNoiseThreshold = 8.0f
    ),
    COARSE(
        label = "Coarse (high noise / surface metal)",
        maxSignalToNoise = 36.0f,
        anomalySignalToNoise = 8.0f,
        centerSignalToNoise = 16.0f,
        minimumAnomalyMicroTesla = 2.2f,
        minimumCenterMicroTesla = 6.0f,
        acSignalToNoiseThreshold = 12.0f
    )
}

/**
 * Categorical detection state for Ferrous anomaly.
 */
enum class FerrousDetectionState(val label: String, val levelCode: Int) {
    CLEAR("Ambient / Clear", 0),
    ANOMALY("Ferrous Object Candidate", 1),
    CENTER_TARGET("Fastener Peak Confirmed", 2)
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
    val rawMagnitude: Float = 0f,
    val filteredMagnitude: Float = 0f,
    val ambientBaseline: Float = 0f,
    val deltaMagnitude: Float = 0f,
    val noiseFloorMicroTesla: Float = 0f,
    val signalToNoise: Float = 0f,
    val acSignalToNoise: Float = 0f,
    val acFrequencyHz: Float? = null,
    val acFieldStrengthMicroTesla: Float = 0f,
    val sampleRateHz: Float = 0f,
    val isAcDetectionSupported: Boolean = false,
    val isMotionStable: Boolean = true,
    val isCalibrationReady: Boolean = false,
    val calibrationProgress: Float = 0f,
    val isSensorReliable: Boolean = true,
    val sensorError: String? = null,
    val sensorName: String = "",
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
    val acSignalToNoise: Float,
    val proximityPercent: Float
)
