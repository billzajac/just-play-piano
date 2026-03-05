package com.windupairships.airshippiano.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.windupairships.airshippiano.audio.AudioEngine
import com.windupairships.airshippiano.midi.MidiManager
import com.windupairships.airshippiano.soundfont.SoundFontManager
import com.windupairships.airshippiano.soundfont.SoundFontState
import kotlinx.coroutines.launch

class PianoViewModel(application: Application) : AndroidViewModel(application) {

    val soundFontManager = SoundFontManager(application)
    val audioEngine = AudioEngine(application)
    val midiManager = MidiManager(application)

    private var started = false

    init {
        viewModelScope.launch {
            soundFontManager.ensureSoundFont()
        }

        // Watch for soundfont ready to start audio engine
        viewModelScope.launch {
            soundFontManager.state.collect { state ->
                if (state is SoundFontState.Ready && !started) {
                    started = true
                    if (audioEngine.start()) {
                        audioEngine.loadSoundFont(state.path)
                        midiManager.start(audioEngine)
                    }
                }
            }
        }
    }

    fun retrySoundFont() {
        viewModelScope.launch {
            soundFontManager.retry()
        }
    }

    fun setVolume(volume: Float) {
        audioEngine.volume = volume
    }

    override fun onCleared() {
        super.onCleared()
        midiManager.stop()
        audioEngine.stop()
    }
}
