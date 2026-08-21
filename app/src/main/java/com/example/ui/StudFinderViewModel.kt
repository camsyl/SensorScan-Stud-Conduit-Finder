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
    val isSimulatedMode: Boolean = false,
    val sweepHistory: List<SweepPoint> = emptyList(),
    val isCalibrating: Boolean = false,
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
            isSimulatedMode = !sensorRepository.isHardwareAvailable
        )
    )
    val uiState: StateFlow<StudFinderUiState> = _uiState.asStateFlow()

    private val sweepPoints = ArrayDeque<SweepPoint>(250)
    private var lastSweepRecordTime = 0L
    private val sweepWindowMillis = 5000L // 5 seconds history
    private var lastTareTimestamp = 0L
    private var tareCountSinceLastAd = 0
    private var interstitialTimerJob: Job? = null

    // AC EMF Debounce and Hysteresis Filters to prevent phantom 50/60Hz flickering
    private var acHazardConsecutiveHits = 0
    private var isAcHazardDebounced = false
    private var acHazardCooldownCount = 0

    init {
        // If device has no hardware magnetometer, default automatically to simulation demo mode
        if (!sensorRepository.isHardwareAvailable) {
            sensorRepository.setSimulationMode(true)
        }

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
        val scanMode = currentState.scanMode

        // Calculate ferrous proximity percentage based on current sensitivity scale
        val rawProximity = (reading.deltaMagnitude / sensitivity.maxDeltaMicroTesla) * 100f
        val proximityPercent = rawProximity.coerceIn(0f, 100f)

        // Categorical ferrous detection state
        val detectionState = when {
            scanMode == ScanMode.AC_EMF_ONLY -> FerrousDetectionState.CLEAR
            reading.deltaMagnitude >= sensitivity.centerThresholdMicroTesla -> FerrousDetectionState.CENTER_TARGET
            reading.deltaMagnitude >= sensitivity.anomalyThresholdMicroTesla -> FerrousDetectionState.ANOMALY
            else -> FerrousDetectionState.CLEAR
        }

        // AC EMF hazard state with hysteresis & multi-sample confirmation
        // Requires 6 consecutive high-variance frames to trigger (filtering transient jitter)
        // and 8 consecutive clear frames to release, preventing jumping and flickering.
        val rawAcCandidate = when (scanMode) {
            ScanMode.FERROUS_ONLY -> false
            else -> reading.acVariance >= sensitivity.acVarianceThreshold
        }

        if (rawAcCandidate) {
            acHazardConsecutiveHits++
            acHazardCooldownCount = 0
            if (acHazardConsecutiveHits >= 6) {
                isAcHazardDebounced = true
            }
        } else {
            acHazardConsecutiveHits = 0
            acHazardCooldownCount++
            // Release threshold with lower variance hysteresis
            val belowReleaseThreshold = reading.acVariance < (sensitivity.acVarianceThreshold * 0.65f)
            if (acHazardCooldownCount >= 8 || belowReleaseThreshold) {
                isAcHazardDebounced = false
            }
        }

        val isAcHazard = isAcHazardDebounced

        val enrichedReading = reading.copy(
            proximityPercent = proximityPercent,
            detectionState = detectionState,
            isAcHazardActive = isAcHazard
        )

        // Update peak delta
        val newPeak = maxOf(currentState.peakDelta, reading.deltaMagnitude)

        // Oscilloscope historical record (downsampled to ~30-40 Hz for smooth performance)
        val now = System.currentTimeMillis()
        if (now - lastSweepRecordTime >= 35) {
            lastSweepRecordTime = now
            sweepPoints.addLast(
                SweepPoint(
                    timestampMillis = now,
                    deltaB = reading.deltaMagnitude,
                    acVariance = reading.acVariance,
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
                peakDelta = newPeak,
                sweepHistory = sweepPoints.toList()
            )
        }

        // Audio & Haptic feedback triggers
        audioFeedbackManager.updateProximity(proximityPercent, isAcHazard)

        if (isAcHazard) {
            hapticFeedbackManager.triggerAcHazardAlert()
        } else if (scanMode != ScanMode.AC_EMF_ONLY) {
            hapticFeedbackManager.triggerProximityFeedback(
                proximityPercent = proximityPercent,
                isDirectCenter = detectionState == FerrousDetectionState.CENTER_TARGET
            )
        }
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
        _uiState.update { it.copy(peakDelta = 0.0f, isCalibrating = true) }
        
        // Haptic tap on tare calibration
        hapticFeedbackManager.triggerProximityFeedback(100f, true)

        viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            _uiState.update { it.copy(isCalibrating = false) }
        }

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
        _uiState.update { it.copy(sensitivity = sensitivity) }
    }

    fun setScanMode(scanMode: ScanMode) {
        _uiState.update { it.copy(scanMode = scanMode) }
    }

    fun setAudioMode(audioMode: AudioMode) {
        audioFeedbackManager.setAudioMode(audioMode)
        _uiState.update { it.copy(audioMode = audioMode) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        hapticFeedbackManager.isHapticsEnabled = enabled
        _uiState.update { it.copy(isHapticsEnabled = enabled) }
    }

    fun toggleSimulationMode(enabled: Boolean) {
        sensorRepository.setSimulationMode(enabled)
        _uiState.update { it.copy(isSimulatedMode = enabled) }
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
}
