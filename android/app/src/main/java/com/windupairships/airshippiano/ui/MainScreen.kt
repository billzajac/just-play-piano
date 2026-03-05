package com.windupairships.airshippiano.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.windupairships.airshippiano.soundfont.SoundFontState

@Composable
fun MainScreen(viewModel: PianoViewModel = viewModel()) {
    val soundFontState by viewModel.soundFontManager.state.collectAsState()

    when (val state = soundFontState) {
        is SoundFontState.Checking -> DownloadScreen(progress = null)
        is SoundFontState.Downloading -> DownloadScreen(progress = state.progress)
        is SoundFontState.Ready -> PianoScreen(viewModel = viewModel)
        is SoundFontState.Error -> ErrorScreen(
            message = state.message,
            onRetry = { viewModel.retrySoundFont() }
        )
    }
}
