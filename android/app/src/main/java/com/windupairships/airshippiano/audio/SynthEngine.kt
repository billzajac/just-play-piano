package com.windupairships.airshippiano.audio

/**
 * JNI bridge to TinySoundFont + Oboe native audio engine.
 * Each MIDI device is assigned a unique channel (0-15, skipping 9/drums).
 */
class SynthEngine {
    private var nativeHandle: Long = 0

    init {
        System.loadLibrary("airshippiano_synth")
    }

    fun create(sampleRate: Int = 44100): Boolean {
        nativeHandle = nativeCreate(sampleRate)
        return nativeHandle != 0L
    }

    fun destroy() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle)
            nativeHandle = 0
        }
    }

    fun loadSoundFont(path: String): Boolean {
        if (nativeHandle == 0L) return false
        return nativeLoadSoundFont(nativeHandle, path)
    }

    fun noteOn(channel: Int, note: Int, velocity: Int) {
        if (nativeHandle != 0L) nativeNoteOn(nativeHandle, channel, note, velocity)
    }

    fun noteOff(channel: Int, note: Int) {
        if (nativeHandle != 0L) nativeNoteOff(nativeHandle, channel, note)
    }

    fun controlChange(channel: Int, controller: Int, value: Int) {
        if (nativeHandle != 0L) nativeControlChange(nativeHandle, channel, controller, value)
    }

    fun setGain(gain: Float) {
        if (nativeHandle != 0L) nativeSetGain(nativeHandle, gain)
    }

    fun startAudio(): Boolean {
        if (nativeHandle == 0L) return false
        return nativeStartAudio(nativeHandle)
    }

    fun stopAudio() {
        if (nativeHandle != 0L) nativeStopAudio(nativeHandle)
    }

    private external fun nativeCreate(sampleRate: Int): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeLoadSoundFont(handle: Long, path: String): Boolean
    private external fun nativeNoteOn(handle: Long, channel: Int, note: Int, velocity: Int)
    private external fun nativeNoteOff(handle: Long, channel: Int, note: Int)
    private external fun nativeControlChange(handle: Long, channel: Int, controller: Int, value: Int)
    private external fun nativeSetGain(handle: Long, gain: Float)
    private external fun nativeStartAudio(handle: Long): Boolean
    private external fun nativeStopAudio(handle: Long)
}
