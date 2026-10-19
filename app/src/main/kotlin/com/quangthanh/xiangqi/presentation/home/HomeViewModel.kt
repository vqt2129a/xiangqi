package com.quangthanh.xiangqi.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quangthanh.xiangqi.application.dto.GameState
import com.quangthanh.xiangqi.application.usecase.GameMode
import com.quangthanh.xiangqi.application.usecase.OpenGameUseCase
import com.quangthanh.xiangqi.application.usecase.StartNewGameUseCase
import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.Side
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val unfinishedGame: GameState? = null,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val humanSide: Side = Side.RED,
    /** Chế độ đang chờ người chơi xác nhận bỏ ván dở. */
    val pendingMode: GameMode? = null,
    /** Mở app mà còn ván dở: hỏi chơi tiếp hay ván mới. */
    val showResumePrompt: Boolean = false,
)

class HomeViewModel(
    private val openGame: OpenGameUseCase,
    private val startNewGame: StartNewGameUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _openGame = Channel<GameId>(Channel.BUFFERED)
    val gameToOpen: Flow<GameId> = _openGame.receiveAsFlow()

    /** Chỉ hỏi "chơi tiếp hay ván mới" một lần khi mở app, không hỏi lại mỗi lần quay về màn hình chính. */
    private var resumePromptHandled = false

    /** Người chơi đã chọn "Ván mới" ở lời hỏi đầu, nên khi bắt đầu ván mới không cần hỏi bỏ ván dở thêm lần nữa. */
    private var abandonConfirmed = false

    /** Gọi mỗi khi quay lại màn hình chính, vì ván dở có thể vừa kết thúc. */
    fun refresh() {
        viewModelScope.launch {
            val unfinished = openGame.latestUnfinished()
            val askToResume = unfinished != null && !resumePromptHandled
            resumePromptHandled = true
            _uiState.update { it.copy(unfinishedGame = unfinished, showResumePrompt = it.showResumePrompt || askToResume) }
        }
    }

    /** Mở từ thông báo "chơi tiếp": vào thẳng ván dở gần nhất (nếu còn), không cần hỏi nữa. */
    fun resumeLatestGame() {
        resumePromptHandled = true
        viewModelScope.launch {
            val unfinished = openGame.latestUnfinished() ?: return@launch
            _uiState.update { it.copy(unfinishedGame = unfinished, showResumePrompt = false) }
            _openGame.send(unfinished.gameId)
        }
    }

    /** Chọn "Ván mới" ở lời hỏi đầu: đóng lời hỏi để người chơi chọn chế độ. */
    fun skipResume() {
        abandonConfirmed = true
        _uiState.update { it.copy(showResumePrompt = false) }
    }

    fun selectDifficulty(difficulty: Difficulty) = _uiState.update { it.copy(difficulty = difficulty) }

    fun selectSide(side: Side) = _uiState.update { it.copy(humanSide = side) }

    fun startAgainstComputer() = requestStart(GameMode.AgainstComputer(_uiState.value.humanSide, _uiState.value.difficulty))

    fun startTwoPlayers() = requestStart(GameMode.TwoPlayers)

    fun confirmStart() {
        val mode = _uiState.value.pendingMode ?: return
        _uiState.update { it.copy(pendingMode = null) }
        start(mode)
    }

    fun cancelStart() = _uiState.update { it.copy(pendingMode = null) }

    fun continueGame() {
        _uiState.update { it.copy(showResumePrompt = false) }
        _uiState.value.unfinishedGame?.let { _openGame.trySend(it.gameId) }
    }

    private fun requestStart(mode: GameMode) {
        if (_uiState.value.unfinishedGame != null && !abandonConfirmed) {
            _uiState.update { it.copy(pendingMode = mode) }
        } else {
            start(mode)
        }
    }

    private fun start(mode: GameMode) {
        viewModelScope.launch { _openGame.send(startNewGame(mode).gameId) }
    }
}
