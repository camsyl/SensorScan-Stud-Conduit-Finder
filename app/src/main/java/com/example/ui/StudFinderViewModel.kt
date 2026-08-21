package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioFeedbackManager
import com.example.data.AudioMode
import com.example.data.FerrousDetectionState
import com.example.data.MagneticReading
import com.example.data.ProPlanTier
import com.example.data.ScanMode
import com.example.data.SensitivityLevel
import com.example.data.SensorRepository
import com.example.data.SweepPoint
import com.example.haptic.HapticFeedbackManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudFinderUiState(
    val reading: MagneticReading = MagneticReading(),
    val peakDelta: Float = 0.0f,
    val sensitivity: SensitivityLevel = SensitivityLevel.MEDIUM,
    val scanMode: ScanMode = ScanMode.DUAL,
    val audioMode: AudioMode = AudioMode.CONTINUOUS_TONE,
    val isHapticsEnabled: Boolean = true,
    val isHardwareAvailable: Boolean = true,
    val sweepHistory: List<SweepPoint> = emptyList(),
    val isCalibrating: Boolean = false,
    val confirmedFastenerCount: Int = 0,
    val isEducationalSheetVisible: Boolean = false,
    val messageBanner: String? = null,
    val isProUnlocked: Boolean = false,
    val isProDialogVisible: Boolean = false,
    val selectedTier: ProPlanTier = ProPlanTier.YEARLY,
    val isInterstitialAdVisible: Boolean = false,
    val interstitialSecondsRemaining: Int = 2,
    val canDismissInterstitial: Boolean = false,
    val monthlyBuyUrl: String = ProPlanTier.MONTHLY.stripeBuyUrl,
    val yearlyBuyUrl: String = ProPlanTier.YEARLY.stripeBuyUrl,
    val lifetimeBuyUrl: String = ProPlanTier.LIFETIME.stripeBuyUrl
)

class StudFinderViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorRepository = SensorRepository(application)
    private val audioFeedbackManager = AudioFeedbackManager()
    private val hapticFeedbackManager = HapticFeedbackManager(application)

    private val _uiState = MutableStateFlow(
        StudFinderUiState(
            isHardwareAvailable = sensorRepository.isHardwareAvailable,
            isCalibrating = sensorRepository.isHardwareAvailable
        )
    )
    val uiState: StateFlow<StudFinderUiState> = _uiState.asStateFlow()

    private val sweepPoints = ArrayDeque<SweepPoint>(250)
    private var lastSweepRecordTime = 0L
    private val sweepWindowMillis = 5000L // 5 seconds history
    private var lastTareTimestamp = 0L
    private var tareCountSinceLastAd = 0
    private var interstitialTimerJob: Job? = null

    // Time-based AC confirmation avoids device-rate-dependent frame counters.
    private var acCandidateSinceNanos = 0L
    private var acClearSinceNanos = 0L
    private var isAcHazardDebounced = false

    // A fastener is confirmed only after a strong, stable spatial peak rises and falls.
    private var isTrackingFastenerPeak = false
    private var fastenerPeakStartNanos = 0L
    private var trackedFastenerPeakDelta = 0f
    private var confirmedFastenerUntilNanos = 0L
    private var lastFastenerConfirmationNanos = 0L
    private var confirmedFastenerCount = 0

    init {
        audioFeedbackManager.setAudioMode(_uiState.value.audioMode)
        hapticFeedbackManager.isHapticsEnabled = _uiState.value.isHapticsEnabled

        viewModelScope.launch {
            sensorRepository.sensorState.collect { rawReading ->
                processReading(rawReading)
            }
        }

        // Schedule periodic interstitial ad trigger for free tier (e.g. every ~75 seconds)
        viewModelScope.launch {
            while (true) {
                delay(75_000L)
                if (!_uiState.value.isProUnlocked && !_uiState.value.isInterstitialAdVisible && !_uiState.value.isProDialogVisible) {
                    showInterstitialAd()
                }
            }
        }
    }

    private fun processReading(reading: MagneticReading) {
        val currentState = _uiState.value
        val sensitivity = currentState.sensitivity
        val acCapabilityKnown = reading.isCalibrationReady && reading.sampleRateHz > 0f
        val scanMode = if (acCapabilityKnown &&
            !reading.isAcDetectionSupported &&
            currentState.scanMode != ScanMode.FERROUS_ONLY
        ) {
            ScanMode.FERROUS_ONLY
        } else {
            currentState.scanMode
        }

        val readingUsable = reading.sensorError == null &&
            reading.isCalibrationReady &&
            reading.isSensorReliable &&
            reading.isMotionStable

        val anomalyThreshold = maxOf(
            sensitivity.minimumAnomalyMicroTesla,
            reading.noiseFloorMicroTesla * sensitivity.anomalySignalToNoise
        )
        val centerThreshold = maxOf(
            sensitivity.minimumCenterMicroTesla,
            reading.noiseFloorMicroTesla * sensitivity.centerSignalToNoise
        )

        val proximityPercent = if (readingUsable && scanMode != ScanMode.AC_EMF_ONLY) {
            ((reading.signalToNoise / sensitivity.maxSignalToNoise) * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }

        val centerCandidate = readingUsable &&
            reading.deltaMagnitude >= centerThreshold &&
            reading.signalToNoise >= sensitivity.centerSignalToNoise
        updateFastenerPeak(reading, centerCandidate)

        val fastenerConfirmed = reading.timestampNanos <= confirmedFastenerUntilNanos
        val detectionState = when {
            !readingUsable || scanMode == ScanMode.AC_EMF_ONLY -> FerrousDetectionState.CLEAR
            fastenerConfirmed -> FerrousDetectionState.CENTER_TARGET
            reading.deltaMagnitude >= anomalyThreshold &&
                reading.signalToNoise >= sensitivity.anomalySignalToNoise -> FerrousDetectionState.ANOMALY
            else -> FerrousDetectionState.CLEAR
        }

        val rawAcCandidate = scanMode != ScanMode.FERROUS_ONLY &&
            readingUsable &&
            reading.isAcDetectionSupported &&
            reading.acFrequencyHz != null &&
            reading.acSignalToNoise >= sensitivity.acSignalToNoiseThreshold
        updateAcConfirmation(reading.timestampNanos, rawAcCandidate)
        val isAcHazard = isAcHazardDebounced && scanMode != ScanMode.FERROUS_ONLY

        val enrichedReading = reading.copy(
            proximityPercent = proximityPercent,
            detectionState = detectionState,
            isAcHazardActive = isAcHazard
        )

        // Update peak delta
        val newPeak = if (readingUsable) maxOf(currentState.peakDelta, reading.deltaMagnitude) else currentState.peakDelta

        // Oscilloscope historical record (downsampled to ~30-40 Hz for smooth performance)
        val now = System.currentTimeMillis()
        if (now - lastSweepRecordTime >= 35) {
            lastSweepRecordTime = now
            sweepPoints.addLast(
                SweepPoint(
                    timestampMillis = now,
                    deltaB = reading.deltaMagnitude,
                    acSignalToNoise = reading.acSignalToNoise,
                    proximityPercent = proximityPercent
                )
            )

            // Prune points older than 5 seconds
            val cutoff = now - sweepWindowMillis
            while (sweepPoints.isNotEmpty() && sweepPoints.first().timestampMillis < cutoff) {
                sweepPoints.removeFirst()
            }
        }

        _uiState.update { state ->
            state.copy(
                reading = enrichedReading,
                scanMode = scanMode,
                peakDelta = newPeak,
                sweepHistory = sweepPoints.toList(),
                isCalibrating = reading.sensorError == null && !reading.isCalibrationReady,
                confirmedFastenerCount = confirmedFastenerCount
            )
        }

        // Audio & Haptic feedback triggers
        audioFeedbackManager.updateProximity(proximityPercent, isAcHazard && readingUsable)

        if (isAcHazard && readingUsable) {
            hapticFeedbackManager.triggerAcHazardAlert()
        } else if (scanMode != ScanMode.AC_EMF_ONLY) {
            hapticFeedbackManager.triggerProximityFeedback(
                proximityPercent = proximityPercent,
                isDirectCenter = detectionState == FerrousDetectionState.CENTER_TARGET
            )
        }
    }

    private fun updateFastenerPeak(reading: MagneticReading, centerCandidate: Boolean) {
        if (!reading.isMotionStable || !reading.isCalibrationReady || !reading.isSensorReliable) {
            isTrackingFastenerPeak = false
            fastenerPeakStartNanos = 0L
            trackedFastenerPeakDelta = 0f
            return
        }

        if (centerCandidate) {
            if (!isTrackingFastenerPeak) {
                isTrackingFastenerPeak = true
                fastenerPeakStartNanos = reading.timestampNanos
                trackedFastenerPeakDelta = reading.deltaMagnitude
            } else {
                trackedFastenerPeakDelta = maxOf(trackedFastenerPeakDelta, reading.deltaMagnitude)
            }
            return
        }

        if (!isTrackingFastenerPeak) return
        val peakDuration = reading.timestampNanos - fastenerPeakStartNanos
        val hasFallenFromPeak = reading.deltaMagnitude <= trackedFastenerPeakDelta * PEAK_RELEASE_RATIO
        if (hasFallenFromPeak && peakDuration >= MINIMUM_PEAK_DURATION_NANOS) {
            val separatedFromPrevious = reading.timestampNanos - lastFastenerConfirmationNanos >=
                MINIMUM_CONFIRMATION_SEPARATION_NANOS
            if (separatedFromPrevious) {
                confirmedFastenerCount++
                lastFastenerConfirmationNanos = reading.timestampNanos
            }
            confirmedFastenerUntilNanos = reading.timestampNanos + CONFIRMED_DISPLAY_NANOS
        } else if (peakDuration < MAXIMUM_PEAK_DURATION_NANOS) {
            return
        }
        isTrackingFastenerPeak = false
        fastenerPeakStartNanos = 0L
        trackedFastenerPeakDelta = 0f
    }

    private fun updateAcConfirmation(timestampNanos: Long, candidate: Boolean) {
        if (candidate) {
            acClearSinceNanos = 0L
            if (acCandidateSinceNanos == 0L) acCandidateSinceNanos = timestampNanos
            if (timestampNanos - acCandidateSinceNanos >= AC_CONFIRMATION_NANOS) {
                isAcHazardDebounced = true
            }
        } else {
            acCandidateSinceNanos = 0L
            if (acClearSinceNanos == 0L) acClearSinceNanos = timestampNanos
            if (timestampNanos - acClearSinceNanos >= AC_RELEASE_NANOS) {
                isAcHazardDebounced = false
            }
        }
    }

    private fun resetDetectionConfirmation() {
        acCandidateSinceNanos = 0L
        acClearSinceNanos = 0L
        isAcHazardDebounced = false
        isTrackingFastenerPeak = false
        fastenerPeakStartNanos = 0L
        trackedFastenerPeakDelta = 0f
        confirmedFastenerUntilNanos = 0L
        lastFastenerConfirmationNanos = 0L
        confirmedFastenerCount = 0
        sweepPoints.clear()
    }

    fun onResume() {
        sensorRepository.startListening()
        audioFeedbackManager.start()
    }

    fun onPause() {
        sensorRepository.stopListening()
        audioFeedbackManager.stop()
        hapticFeedbackManager.stop()
    }

    fun tareZero() {
        sensorRepository.tareZeroCalibration()
        resetDetectionConfirmation()
        _uiState.update {
            it.copy(
                peakDelta = 0.0f,
                isCalibrating = sensorRepository.isHardwareAvailable,
                confirmedFastenerCount = 0
            )
        }
        
        // Haptic tap on tare calibration
        hapticFeedbackManager.triggerProximityFeedback(100f, true)

        // Increment tare usage counter; trigger interstitial ad after 5 calibration actions on free tier
        if (!_uiState.value.isProUnlocked) {
            tareCountSinceLastAd++
            if (tareCountSinceLastAd >= 5) {
                tareCountSinceLastAd = 0
                showInterstitialAd()
            }
        }
    }

    fun showInterstitialAd() {
        if (_uiState.value.isProUnlocked) return
        interstitialTimerJob?.cancel()
        _uiState.update {
            it.copy(
                isInterstitialAdVisible = true,
                interstitialSecondsRemaining = 2,
                canDismissInterstitial = false
            )
        }

        interstitialTimerJob = viewModelScope.launch {
            var remaining = 2
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _uiState.update { it.copy(interstitialSecondsRemaining = remaining) }
            }
            _uiState.update {
                it.copy(
                    canDismissInterstitial = true,
                    interstitialSecondsRemaining = 0
                )
            }
        }
    }

    fun dismissInterstitialAd() {
        if (_uiState.value.canDismissInterstitial) {
            interstitialTimerJob?.cancel()
            _uiState.update {
                it.copy(
                    isInterstitialAdVisible = false,
                    canDismissInterstitial = false
                )
            }
        }
    }

    fun setSelectedTier(tier: ProPlanTier) {
        _uiState.update { it.copy(selectedTier = tier) }
    }

    fun setSensitivity(sensitivity: SensitivityLevel) {
        resetDetectionConfirmation()
        _uiState.update { it.copy(sensitivity = sensitivity) }
    }

    fun setScanMode(scanMode: ScanMode) {
        resetDetectionConfirmation()
        _uiState.update { state ->
            val acUnavailable = state.reading.isCalibrationReady &&
                state.reading.sampleRateHz > 0f &&
                !state.reading.isAcDetectionSupported
            val availableMode = if (acUnavailable && scanMode != ScanMode.FERROUS_ONLY) {
                ScanMode.FERROUS_ONLY
            } else {
                scanMode
            }
            state.copy(scanMode = availableMode)
        }
    }

    fun setAudioMode(audioMode: AudioMode) {
        audioFeedbackManager.setAudioMode(audioMode)
        _uiState.update { it.copy(audioMode = audioMode) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        hapticFeedbackManager.isHapticsEnabled = enabled
        _uiState.update { it.copy(isHapticsEnabled = enabled) }
    }

    fun resetPeak() {
        _uiState.update { it.copy(peakDelta = 0.0f) }
    }

    fun showEducationalSheet(show: Boolean) {
        _uiState.update { it.copy(isEducationalSheetVisible = show) }
    }

    fun setProDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isProDialogVisible = visible) }
    }

    fun unlockPro() {
        _uiState.update {
            it.copy(
                isProUnlocked = true,
                isProDialogVisible = false,
                isInterstitialAdVisible = false,
                messageBanner = "PRO Plan Activated - All Ads Removed!"
            )
        }
        hapticFeedbackManager.triggerProximityFeedback(100f, true)
    }

    fun updateStripeLinks(monthlyUrl: String, yearlyUrl: String, lifetimeUrl: String) {
        _uiState.update {
            it.copy(
                monthlyBuyUrl = monthlyUrl.ifBlank { it.monthlyBuyUrl },
                yearlyBuyUrl = yearlyUrl.ifBlank { it.yearlyBuyUrl },
                lifetimeBuyUrl = lifetimeUrl.ifBlank { it.lifetimeBuyUrl }
            )
        }
    }

    fun restorePurchases() {
        _uiState.update {
            it.copy(
                isProUnlocked = true,
                isProDialogVisible = false,
                isInterstitialAdVisible = false,
                messageBanner = "Purchases Restored Successfully"
            )
        }
        hapticFeedbackManager.triggerProximityFeedback(80f, false)
    }

    fun dismissMessageBanner() {
        _uiState.update { it.copy(messageBanner = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioFeedbackManager.stop()
        hapticFeedbackManager.stop()
        sensorRepository.stopListening()
    }

    private companion object {
        const val MINIMUM_PEAK_DURATION_NANOS = 90_000_000L
        const val MAXIMUM_PEAK_DURATION_NANOS = 2_500_000_000L
        const val MINIMUM_CONFIRMATION_SEPARATION_NANOS = 1_500_000_000L
        const val CONFIRMED_DISPLAY_NANOS = 1_200_000_000L
        const val AC_CONFIRMATION_NANOS = 500_000_000L
        const val AC_RELEASE_NANOS = 800_000_000L
        const val PEAK_RELEASE_RATIO = 0.72f
    }
}
