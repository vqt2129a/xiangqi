package com.quangthanh.xiangqi.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quangthanh.xiangqi.presentation.audio.AudioSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AudioUiState(val musicEnabled: Boolean, val effectsEnabled: Boolean)

/** Cài đặt âm thanh dùng chung cho màn hình chính và màn hình chơi. */
class SettingsViewModel(private val audio: AudioSettings) : ViewModel() {

    val audioState: StateFlow<AudioUiState> = combine(audio.musicEnabled, audio.effectsEnabled, ::AudioUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AudioUiState(audio.musicEnabled.value, audio.effectsEnabled.value))

    fun saveAudio(musicEnabled: Boolean, effectsEnabled: Boolean) {
        audio.setMusicEnabled(musicEnabled)
        audio.setEffectsEnabled(effectsEnabled)
    }
}
