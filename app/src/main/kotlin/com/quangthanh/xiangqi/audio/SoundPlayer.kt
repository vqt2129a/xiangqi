package com.quangthanh.xiangqi.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.core.content.edit
import com.quangthanh.xiangqi.R
import com.quangthanh.xiangqi.presentation.audio.AudioSettings
import com.quangthanh.xiangqi.presentation.audio.GameSounds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hiệu ứng ngắn dùng [SoundPool] (nạp sẵn vào bộ nhớ, phát tức thì), nhạc nền dùng [MediaPlayer] phát lặp.
 * Nhạc chỉ phát khi app đang hiển thị; [onForeground]/[onBackground] được gọi theo vòng đời Activity.
 */
class SoundPlayer(private val context: Context) : GameSounds, AudioSettings {

    private val preferences = context.getSharedPreferences("audio", Context.MODE_PRIVATE)

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val selectSound = soundPool.load(context, R.raw.select, 1)
    private val moveSound = soundPool.load(context, R.raw.click, 1)

    private var music: MediaPlayer? = null
    private var inForeground = false

    private val _effectsEnabled = MutableStateFlow(preferences.getBoolean(KEY_EFFECTS, true))
    override val effectsEnabled: StateFlow<Boolean> = _effectsEnabled.asStateFlow()

    private val _musicEnabled = MutableStateFlow(preferences.getBoolean(KEY_MUSIC, true))
    override val musicEnabled: StateFlow<Boolean> = _musicEnabled.asStateFlow()

    override fun pieceSelected() = playEffect(selectSound)

    override fun pieceMoved() = playEffect(moveSound)

    override fun setEffectsEnabled(enabled: Boolean) {
        _effectsEnabled.value = enabled
        preferences.edit { putBoolean(KEY_EFFECTS, enabled) }
    }

    override fun setMusicEnabled(enabled: Boolean) {
        _musicEnabled.value = enabled
        preferences.edit { putBoolean(KEY_MUSIC, enabled) }
        updateMusic()
    }

    fun onForeground() {
        inForeground = true
        updateMusic()
    }

    fun onBackground() {
        inForeground = false
        updateMusic()
    }

    private fun playEffect(soundId: Int) {
        if (_effectsEnabled.value) soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private fun updateMusic() {
        if (inForeground && _musicEnabled.value) {
            if (music != null) return
            // prepareAsync giải mã file mp3 trên luồng của MediaPlayer, không chặn luồng giao diện lúc mở app.
            music = MediaPlayer().apply {
                context.resources.openRawResourceFd(R.raw.background).use { setDataSource(it) }
                isLooping = true
                setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
                setOnPreparedListener { if (music === it) it.start() }
                prepareAsync()
            }
        } else {
            // Giải phóng hẳn thay vì chỉ tạm dừng, để không giữ bộ giải mã âm thanh khi app ở nền.
            music?.release()
            music = null
        }
    }

    private companion object {
        const val KEY_EFFECTS = "effects_enabled"
        const val KEY_MUSIC = "music_enabled"
        const val MUSIC_VOLUME = 0.4f
    }
}
