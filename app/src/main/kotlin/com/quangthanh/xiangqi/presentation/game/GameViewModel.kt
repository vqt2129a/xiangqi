package com.quangthanh.xiangqi.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quangthanh.xiangqi.application.dto.GameState
import com.quangthanh.xiangqi.application.dto.GameUpdate
import com.quangthanh.xiangqi.application.exception.ApplicationException
import com.quangthanh.xiangqi.application.exception.GameChangedException
import com.quangthanh.xiangqi.application.usecase.GameMode
import com.quangthanh.xiangqi.application.usecase.OpenGameUseCase
import com.quangthanh.xiangqi.application.usecase.PlayComputerMoveUseCase
import com.quangthanh.xiangqi.application.usecase.PlayMoveUseCase
import com.quangthanh.xiangqi.application.usecase.ResignUseCase
import com.quangthanh.xiangqi.application.usecase.StartNewGameUseCase
import com.quangthanh.xiangqi.application.usecase.UndoMoveUseCase
import com.quangthanh.xiangqi.domain.event.GameEvent
import com.quangthanh.xiangqi.domain.exception.DomainException
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.GameStatus
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.domain.model.Square
import com.quangthanh.xiangqi.presentation.audio.GameSounds
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val game: GameState? = null,
    val selected: Square? = null,
    val isComputerThinking: Boolean = false,
    val showResult: Boolean = false,
    /** Đang chờ người chơi xác nhận bỏ ván dở để mở ván mới. */
    val confirmNewGame: Boolean = false,
) {
    /** Lật bàn cờ để quân của người chơi luôn ở phía dưới. */
    val flipped: Boolean get() = game?.participants?.humanSideAgainstComputer == Side.BLACK
    val targets: List<Square> get() = selected?.let { game?.legalMoves?.get(it) }.orEmpty()

    /** Bên vừa đi nước cuối, để tô khung đúng màu. */
    val lastMoveSide: Side? get() = game?.takeIf { it.lastMove != null }?.sideToMove?.opponent
}

enum class GameMessage { CHECK, ACTION_REJECTED, LOAD_FAILED }

/**
 * Presenter của màn hình chơi cờ: biến thao tác chạm thành lời gọi use case và biến kết quả
 * thành [GameUiState]. Không chứa luật cờ nào; mọi kiểm tra đều do use case và domain làm.
 */
class GameViewModel(
    private val gameId: GameId,
    private val openGame: OpenGameUseCase,
    private val playMove: PlayMoveUseCase,
    private val playComputerMove: PlayComputerMoveUseCase,
    private val undoMove: UndoMoveUseCase,
    private val resign: ResignUseCase,
    private val startNewGame: StartNewGameUseCase,
    private val sounds: GameSounds,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _messages = Channel<GameMessage>(Channel.BUFFERED)
    val messages: Flow<GameMessage> = _messages.receiveAsFlow()

    private val _gameToOpen = Channel<GameId>(Channel.BUFFERED)
    /** Id của ván mới vừa tạo bằng nút "Ván mới", để màn hình chuyển sang ván đó. */
    val gameToOpen: Flow<GameId> = _gameToOpen.receiveAsFlow()

    private var computerJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(game = openGame(gameId)) }
                letComputerMoveIfItsTurn()
            } catch (e: ApplicationException) {
                _messages.send(GameMessage.LOAD_FAILED)
            }
        }
    }

    fun onSquareTapped(square: Square) {
        val state = _uiState.value
        val legalMoves = state.game?.legalMoves ?: return
        val selected = state.selected
        when {
            selected != null && square in legalMoves[selected].orEmpty() -> play(selected, square)
            square in legalMoves && square != selected -> {
                sounds.pieceSelected()
                _uiState.update { it.copy(selected = square) }
            }
            else -> _uiState.update { it.copy(selected = null) }
        }
    }

    fun undo() {
        viewModelScope.launch {
            computerJob?.cancelAndJoin()
            perform { undoMove(gameId) }
            letComputerMoveIfItsTurn()
        }
    }

    fun resign() {
        viewModelScope.launch {
            computerJob?.cancelAndJoin()
            perform { resign(gameId) }
        }
    }

    fun dismissResult() = _uiState.update { it.copy(showResult = false) }

    /**
     * Bấm "Ván mới". Ván đang chơi dở (đã có nước đi) sẽ bị xử thua, nên phải hỏi lại trước;
     * ván chưa đi nước nào hoặc đã kết thúc thì mở ván mới ngay.
     */
    fun requestNewGame() {
        val game = _uiState.value.game ?: return
        if (game.status == GameStatus.InProgress && game.moveCount > 0) {
            _uiState.update { it.copy(confirmNewGame = true) }
        } else {
            startNextGame()
        }
    }

    fun confirmNewGame() {
        _uiState.update { it.copy(confirmNewGame = false, showResult = false) }
        startNextGame()
    }

    fun cancelNewGame() = _uiState.update { it.copy(confirmNewGame = false) }

    /** Ván mới cùng chế độ và độ khó. Ván hiện tại nếu chưa xong sẽ bị xử thua (quy tắc của StartNewGame). */
    private fun startNextGame() {
        val participants = _uiState.value.game?.participants ?: return
        val humanSide = participants.humanSideAgainstComputer
        val computer = participants.computer
        val mode = if (humanSide != null && computer != null) {
            GameMode.AgainstComputer(humanSide, computer.difficulty)
        } else {
            GameMode.TwoPlayers
        }
        viewModelScope.launch {
            computerJob?.cancelAndJoin()
            _gameToOpen.send(startNewGame(mode).gameId)
        }
    }

    private fun play(from: Square, to: Square) {
        _uiState.update { it.copy(selected = null) }
        viewModelScope.launch {
            perform { playMove(gameId, from, to) }
            letComputerMoveIfItsTurn()
        }
    }

    private fun letComputerMoveIfItsTurn() {
        if (_uiState.value.game?.isComputerTurn != true || computerJob?.isActive == true) return
        computerJob = viewModelScope.launch {
            _uiState.update { it.copy(isComputerThinking = true) }
            try {
                perform { playComputerMove(gameId) }
            } finally {
                _uiState.update { it.copy(isComputerThinking = false) }
            }
        }
    }

    private suspend fun perform(action: suspend () -> GameUpdate) {
        try {
            val update = action()
            _uiState.update { it.copy(game = update.state, selected = null) }
            react(update.events)
        } catch (e: GameChangedException) {
            // Người chơi đã đi lại trong lúc máy nghĩ; nước của máy bị bỏ, không cần báo lỗi.
        } catch (e: DomainException) {
            _messages.send(GameMessage.ACTION_REJECTED)
        } catch (e: ApplicationException) {
            _messages.send(GameMessage.ACTION_REJECTED)
        }
    }

    private suspend fun react(events: List<GameEvent>) {
        if (events.any { it is GameEvent.MovePlayed }) sounds.pieceMoved()
        if (events.any { it is GameEvent.GeneralInCheck }) _messages.send(GameMessage.CHECK)
        if (events.any { it is GameEvent.GameFinished }) _uiState.update { it.copy(showResult = true) }
    }
}
