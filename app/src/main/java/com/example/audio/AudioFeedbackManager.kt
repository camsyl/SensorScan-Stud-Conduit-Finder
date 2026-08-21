package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.AudioMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Low-latency audio synthesizer using Android AudioTrack.
 * Generates continuous sine wave pitch modulation (200 Hz to 1200 Hz)
 * or rhythmic Geiger clicks according to stud/screw proximity.
 */
class AudioFeedbackManager {

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var synthJob: Job? = null
    private val audioScope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var targetFrequency = 200.0 // Hz
    @Volatile
    private var currentFrequency = 200.0

    @Volatile
    private var currentVolume = 0.0f // 0.0..1.0
    @Volatile
    private var currentProximity = 0.0f // 0..100%

    @Volatile
    private var audioMode: AudioMode = AudioMode.CONTINUOUS_TONE

    fun setAudioMode(mode: AudioMode) {
        audioMode = mode
    }

    /**
     * Update tone pitch and volume based on detected proximity (0..100%).
     */
    fun updateProximity(proximityPercent: Float, isAcHazard: Boolean) {
        currentProximity = proximityPercent.coerceIn(0f, 100f)

        if (audioMode == AudioMode.MUTED || (currentProximity < 3f && !isAcHazard)) {
            currentVolume = 0f
            return
        }

        // Map 0..100% to 200 Hz .. 1200 Hz frequency scale
        val normalized = (currentProximity / 100f).coerceIn(0f, 1f)
        val baseFreq = 200.0 + (1000.0 * (normalized * normalized)) // Quadratic curve for fine resolution near target
        
        targetFrequency = if (isAcHazard) {
            // Distinct alert warble on AC line
            baseFreq + 150.0
        } else {
            baseFreq
        }

        // Set volume with subtle ramp
        currentVolume = if (normalized > 0.05f || isAcHazard) 0.6f else 0.0f
    }

    fun start() {
        if (isPlaying) return

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = (minBufferSize * 2).coerceAtLeast(2048)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isPlaying = true
            startAudioStream(bufferSize / 2)
        } catch (_: Exception) {
            isPlaying = false
        }
    }

    fun stop() {
        isPlaying = false
        synthJob?.cancel()
        synthJob = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) { }
        audioTrack = null
    }

    private fun startAudioStream(chunkSize: Int) {
        synthJob = audioScope.launch {
            try {
                val buffer = ShortArray(chunkSize)
                var phase = 0.0
                var pulsePhase = 0.0

                while (isActive && isPlaying) {
                    val track = audioTrack
                    if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                        kotlinx.coroutines.delay(100)
                        continue
                    }

                    // Smoothly slew frequency to avoid phase discontinuities
                    val freqStep = (targetFrequency - currentFrequency) * 0.08
                    currentFrequency += freqStep

                    val vol = currentVolume
                    val mode = audioMode

                    if (vol <= 0.001f || mode == AudioMode.MUTED) {
                        buffer.fill(0)
                        // Write silence and brief delay to avoid CPU spinning
                        try {
                            track.write(buffer, 0, chunkSize)
                        } catch (_: Throwable) { }
                        kotlinx.coroutines.delay(20)
                    } else {
                        val phaseIncrement = (2.0 * Math.PI * currentFrequency) / sampleRate

                        for (i in 0 until chunkSize) {
                            phase += phaseIncrement
                            if (phase > 2.0 * Math.PI) {
                                phase -= 2.0 * Math.PI
                            }

                            var sampleVal = 0.0

                            when (mode) {
                                AudioMode.CONTINUOUS_TONE -> {
                                    sampleVal = sin(phase) * vol
                                }
                                AudioMode.PULSE_BEEPS -> {
                                    // Geiger click / pulse rate proportional to proximity
                                    val pulseRateHz = 1.0 + (currentProximity / 100f) * 18.0 // 1 Hz to 19 Hz clicks
                                    pulsePhase += (2.0 * Math.PI * pulseRateHz) / sampleRate
                                    if (pulsePhase > 2.0 * Math.PI) pulsePhase -= 2.0 * Math.PI

                                    // Short burst on each pulse cycle
                                    val isPulseActive = sin(pulsePhase) > 0.85
                                    if (isPulseActive) {
                                        sampleVal = sin(phase) * vol
                                    }
                                }
                                AudioMode.MUTED -> {
                                    sampleVal = 0.0
                                }
                            }

                            val sampleShort = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(
                                Short.MIN_VALUE.toInt(),
                                Short.MAX_VALUE.toInt()
                            ).toShort()

                            buffer[i] = sampleShort
                        }

                        try {
                            track.write(buffer, 0, chunkSize)
                        } catch (_: Throwable) {
                            break
                        }
                    }
                }
            } catch (_: Throwable) {
                // Ignore audio stream exceptions gracefully
            }
        }
    }
}
