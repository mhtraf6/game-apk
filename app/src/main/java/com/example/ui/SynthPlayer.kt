package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.sin

object SynthPlayer {
    private const val SAMPLE_RATE = 22050
    private var musicJob: Job? = null
    private val synthScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMuted = false
    private var isMusicEnabled = true

    fun setMuted(muted: Boolean) {
        this.isMuted = muted
    }

    fun setMusicEnabled(enabled: Boolean) {
        this.isMusicEnabled = enabled
        if (!enabled) {
            stopMusicLoop()
        }
    }

    private fun playTone(frequency: Double, durationMs: Int, volume: Float = 0.5f) {
        if (isMuted) return
        synthScope.launch {
            try {
                val numSamples = (durationMs * SAMPLE_RATE / 1000)
                if (numSamples <= 0) return@launch
                val samples = FloatArray(numSamples)
                
                // Form a clean sine wave with gentle envelope to avoid audio clipping / pops
                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / frequency)
                    val baseSine = sin(angle).toFloat()
                    
                    // Simple linear attack & decay envelope
                    val envelope = when {
                        i < SAMPLE_RATE * 0.01 -> i / (SAMPLE_RATE * 0.01f) // 10ms Attack
                        i > numSamples - (SAMPLE_RATE * 0.02) -> (numSamples - i) / (SAMPLE_RATE * 0.02f) // 20ms Decay
                        else -> 1.0f
                    }
                    samples[i] = baseSine * envelope * volume
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 4)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                audioTrack.play()
                delay(durationMs.toLong() + 30)
                try {
                    audioTrack.stop()
                } catch (e: Exception) {}
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playCorrectSound() {
        if (isMuted) return
        synthScope.launch {
            // Sweet rising major chord arpeggio
            playTone(523.25, 90, 0.3f)  // C5
            delay(70)
            playTone(659.25, 90, 0.3f)  // E5
            delay(70)
            playTone(783.99, 90, 0.3f)  // G5
            delay(70)
            playTone(1046.50, 200, 0.4f) // C6
        }
    }

    fun playWrongSound() {
        if (isMuted) return
        synthScope.launch {
            // Clean soft negative sound arpeggio
            playTone(392.00, 110, 0.35f)  // G4
            delay(90)
            playTone(349.23, 110, 0.35f)  // F4
            delay(90)
            playTone(311.13, 220, 0.4f)   // Eb4
        }
    }

    fun playButtonClickSound() {
        if (isMuted) return
        // Small snappier click sound
        playTone(783.99, 35, 0.2f) // High click tone
    }

    fun playLevelUpSound() {
        if (isMuted) return
        synthScope.launch {
            // Magical arpeggio cascade for celebration
            val levelNotes = listOf(
                392.00, 440.00, 493.88, 523.25, 
                587.33, 659.25, 698.46, 783.99, 
                880.00, 987.77, 1046.50, 1318.51
            )
            for (note in levelNotes) {
                playTone(note, 60, 0.3f)
                delay(40)
            }
        }
    }

    fun startMusicLoop() {
        if (!isMusicEnabled) return
        if (musicJob != null && musicJob?.isActive == true) return
        
        musicJob = synthScope.launch {
            // Calm pentatonic background game music loop
            val melody = listOf(
                Pair(261.63, 600), Pair(329.63, 600), Pair(293.66, 600), Pair(392.00, 600),
                Pair(329.63, 600), Pair(440.00, 600), Pair(392.00, 600), Pair(523.25, 600),
                Pair(440.00, 600), Pair(329.63, 600), Pair(392.00, 600), Pair(293.66, 600),
                Pair(329.63, 600), Pair(261.63, 600), Pair(220.00, 800), Pair(196.00, 800)
            )
            while (isActive && isMusicEnabled) {
                for (pair in melody) {
                    if (!isActive || !isMusicEnabled) break
                    playTone(pair.first, pair.second, 0.05f) // Ultra-soft volume hum
                    delay(800)
                }
                delay(1200)
            }
        }
    }

    fun stopMusicLoop() {
        musicJob?.cancel()
        musicJob = null
    }
}
