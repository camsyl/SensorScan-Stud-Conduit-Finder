package com.example.haptic

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manages tactile feedback with scaled tick effects and hazard warnings.
 */
class HapticFeedbackManager(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var lastVibrateTime = 0L
    var isHapticsEnabled: Boolean = true

    /**
     * Trigger tactile tick feedback scaled by proximity percentage (0..100%).
     */
    fun triggerProximityFeedback(proximityPercent: Float, isDirectCenter: Boolean) {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return

        val now = System.currentTimeMillis()
        
        if (isDirectCenter) {
            // Strong continuous heartbeat when right over the drywall screw/stud center
            if (now - lastVibrateTime > 120) {
                lastVibrateTime = now
                vibrateDirectCenter()
            }
        } else if (proximityPercent > 20f) {
            // Pulse interval shortens as proximity nears 100% (from 450ms down to 100ms)
            val intervalMs = (450 - (proximityPercent / 100f) * 330).toLong().coerceAtLeast(80)
            if (now - lastVibrateTime > intervalMs) {
                lastVibrateTime = now
                vibrateTick(proximityPercent)
            }
        }
    }

    /**
     * Trigger urgent dual buzz for live AC wire / conduit detection.
     */
    fun triggerAcHazardAlert() {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return
        val now = System.currentTimeMillis()
        if (now - lastVibrateTime > 300) {
            lastVibrateTime = now
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        }
    }

    private fun vibrateTick(proximityPercent: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use subtle tick effect
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitude = (50 + (proximityPercent / 100f) * 180).toInt().coerceIn(1, 255)
                vibrator?.vibrate(VibrationEffect.createOneShot(18, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15)
            }
        } catch (_: Throwable) { }
    }

    private fun vibrateDirectCenter() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, 255))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (_: Throwable) { }
    }

    fun stop() {
        vibrator?.cancel()
    }
}
