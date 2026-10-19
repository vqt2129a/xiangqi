package com.quangthanh.xiangqi.presentation.audio

import kotlinx.coroutines.flow.StateFlow

/** Âm thanh trong ván cờ. ViewModel chỉ biết interface này, nên test được mà không cần Android. */
interface GameSounds {
    fun pieceSelected()
    fun pieceMoved()
}

interface AudioSettings {
    val effectsEnabled: StateFlow<Boolean>
    val musicEnabled: StateFlow<Boolean>
    fun setEffectsEnabled(enabled: Boolean)
    fun setMusicEnabled(enabled: Boolean)
}
