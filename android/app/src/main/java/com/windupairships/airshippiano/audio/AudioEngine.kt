package com.windupairships.airshippiano.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

class AudioEngine(context: Context) {

    private val synth = SynthEngine()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val deviceChannels = mutableMapOf<String, Int>()
    private var nextChannel = 0
    private var audioFocusGranted = false
    private var soundFontLoaded = false

    // Channels 0-15, skip 9 (GM drum channel)
    private val availableChannels = (0..15).filter { it != 9 }

    var volume: Float = 0.8f
        set(value) {
            field = value
            // 12dB gain boost at full volume (4.0x linear), scaled by volume
            synth.setGain(value * 4.0f)
        }

    fun start(): Boolean {
        if (!requestAudioFocus()) return false
        if (!synth.create()) return false
        if (!synth.startAudio()) {
            synth.destroy()
            return false
        }
        synth.setGain(volume * 4.0f)
        return true
    }

    fun loadSoundFont(path: String): Boolean {
        soundFontLoaded = synth.loadSoundFont(path)
        if (soundFontLoaded) {
            // Disable reverb/chorus on all channels
            for (ch in 0..15) {
                if (ch == 9) continue
                synth.controlChange(ch, 91, 0) // reverb
                synth.controlChange(ch, 93, 0) // chorus
            }
        }
        return soundFontLoaded
    }

    fun ensureChannel(deviceName: String): Int {
        return deviceChannels.getOrPut(deviceName) {
            val channel = availableChannels.getOrElse(nextChannel) { 0 }
            nextChannel = (nextChannel + 1).coerceAtMost(availableChannels.size - 1)
            channel
        }
    }

    fun noteOn(note: Int, velocity: Int, device: String = "") {
        val channel = ensureChannel(device)
        synth.noteOn(channel, note, velocity)
    }

    fun noteOff(note: Int, device: String = "") {
        val channel = ensureChannel(device)
        synth.noteOff(channel, note)
    }

    fun sustainPedal(value: Int, device: String = "") {
        val channel = ensureChannel(device)
        synth.controlChange(channel, 64, value)
    }

    fun controlChange(controller: Int, value: Int, device: String = "") {
        val channel = ensureChannel(device)
        synth.controlChange(channel, controller, value)
    }

    fun stop() {
        synth.stopAudio()
        synth.destroy()
        abandonAudioFocus()
    }

    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setOnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // Duck volume on transient loss
                    synth.setGain(volume * 1.0f)
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    synth.setGain(volume * 4.0f)
                }
            }
        }
        .build()

    private fun requestAudioFocus(): Boolean {
        val result = audioManager.requestAudioFocus(audioFocusRequest)
        audioFocusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return audioFocusGranted
    }

    private fun abandonAudioFocus() {
        if (audioFocusGranted) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
            audioFocusGranted = false
        }
    }
}
