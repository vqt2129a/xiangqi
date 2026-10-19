package com.quangthanh.xiangqi.presentation.settings

import com.quangthanh.xiangqi.presentation.audio.AudioSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private class FakeAudio : AudioSettings {
        override val musicEnabled = MutableStateFlow(true)
        override val effectsEnabled = MutableStateFlow(true)
        override fun setMusicEnabled(enabled: Boolean) { musicEnabled.value = enabled }
        override fun setEffectsEnabled(enabled: Boolean) { effectsEnabled.value = enabled }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `saving applies both choices and updates the state`() {
        val audio = FakeAudio()
        val viewModel = SettingsViewModel(audio)

        viewModel.saveAudio(musicEnabled = false, effectsEnabled = true)

        assertEquals(false, audio.musicEnabled.value)
        assertEquals(AudioUiState(musicEnabled = false, effectsEnabled = true), viewModel.audioState.value)
    }
}
