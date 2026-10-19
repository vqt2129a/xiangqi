package com.quangthanh.xiangqi.presentation.game

import com.quangthanh.xiangqi.application.fakes.InMemoryGameRepository
import com.quangthanh.xiangqi.application.usecase.GameMode
import com.quangthanh.xiangqi.application.usecase.OpenGameUseCase
import com.quangthanh.xiangqi.application.usecase.PlayComputerMoveUseCase
import com.quangthanh.xiangqi.application.usecase.PlayMoveUseCase
import com.quangthanh.xiangqi.application.usecase.ResignUseCase
import com.quangthanh.xiangqi.application.usecase.StartNewGameUseCase
import com.quangthanh.xiangqi.application.usecase.UndoMoveUseCase
import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.GameStatus
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Position
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.domain.model.Square
import com.quangthanh.xiangqi.domain.rules.MoveGenerator
import com.quangthanh.xiangqi.domain.service.ComputerPlayer
import com.quangthanh.xiangqi.presentation.audio.GameSounds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    /** Máy giả: đi nước hợp lệ đầu tiên; nếu đặt [gate] thì "nghĩ" cho tới khi gate hoàn thành. */
    private class FakeComputer : ComputerPlayer {
        var gate: CompletableDeferred<Unit>? = null
        var wasCancelled = false

        override suspend fun chooseMove(position: Position, difficulty: Difficulty): Move {
            try {
                gate?.await()
            } catch (e: CancellationException) {
                wasCancelled = true
                throw e
            }
            return MoveGenerator.legalMoves(position).first()
        }
    }

    private class FakeSounds : GameSounds {
        var selected = 0
        var moved = 0
        override fun pieceSelected() { selected++ }
        override fun pieceMoved() { moved++ }
    }

    private val repository = InMemoryGameRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-19T08:00:00Z"), ZoneOffset.UTC)
    private val computer = FakeComputer()
    private val sounds = FakeSounds()
    private val startNewGame = StartNewGameUseCase(repository, clock)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun newViewModel(mode: GameMode): GameViewModel {
        val gameId = startNewGame(mode).gameId
        return viewModelFor(gameId)
    }

    private fun viewModelFor(gameId: GameId) = GameViewModel(
        gameId,
        OpenGameUseCase(repository),
        PlayMoveUseCase(repository),
        PlayComputerMoveUseCase(repository, computer),
        UndoMoveUseCase(repository),
        ResignUseCase(repository),
        startNewGame,
        sounds,
    )

    private fun GameViewModel.tap(vararg squares: String) = squares.forEach { onSquareTapped(Square.parse(it)) }

    @Test
    fun `selecting a piece shows its targets and tapping a target plays the move`() = runTest {
        val viewModel = newViewModel(GameMode.TwoPlayers)

        viewModel.tap("h2")
        assertEquals(Square.parse("h2"), viewModel.uiState.value.selected)
        assertTrue(Square.parse("e2") in viewModel.uiState.value.targets)
        assertEquals(1, sounds.selected)

        viewModel.tap("e2")
        val state = viewModel.uiState.value
        assertEquals(1, state.game!!.moveCount)
        assertNull(state.selected)
        assertEquals(1, sounds.moved)
    }

    @Test
    fun `opponent pieces cannot be selected`() = runTest {
        val viewModel = newViewModel(GameMode.TwoPlayers)
        viewModel.tap("h7")
        assertNull(viewModel.uiState.value.selected)
        assertEquals(0, sounds.selected)
    }

    @Test
    fun `computer answers right after the human move`() = runTest {
        val viewModel = newViewModel(GameMode.AgainstComputer(Side.RED, Difficulty.EASY))
        viewModel.tap("h2", "e2")

        val state = viewModel.uiState.value
        assertEquals(2, state.game!!.moveCount)
        assertFalse(state.isComputerThinking)
        assertEquals(2, sounds.moved)
    }

    @Test
    fun `undo while the computer is thinking cancels its search`() = runTest {
        computer.gate = CompletableDeferred()
        val viewModel = newViewModel(GameMode.AgainstComputer(Side.RED, Difficulty.HARD))
        viewModel.tap("h2", "e2")
        assertTrue(viewModel.uiState.value.isComputerThinking)

        viewModel.undo()

        assertTrue(computer.wasCancelled)
        assertFalse(viewModel.uiState.value.isComputerThinking)
        assertEquals(0, viewModel.uiState.value.game!!.moveCount)
    }

    @Test
    fun `new game asks for confirmation only when a game is in progress`() = runTest {
        val viewModel = newViewModel(GameMode.TwoPlayers)
        val opened = mutableListOf<GameId>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.gameToOpen.collect { opened += it } }
        val firstGame = viewModel.uiState.value.game!!.gameId

        viewModel.tap("h2", "e2")
        viewModel.requestNewGame()
        assertTrue(viewModel.uiState.value.confirmNewGame)
        assertTrue(opened.isEmpty())

        viewModel.confirmNewGame()
        assertFalse(viewModel.uiState.value.confirmNewGame)
        assertEquals(1, opened.size)
        assertTrue(OpenGameUseCase(repository)(firstGame).status is GameStatus.Finished)
    }

    @Test
    fun `check is announced and resigning shows the result`() = runTest {
        val viewModel = newViewModel(GameMode.TwoPlayers)
        val messages = mutableListOf<GameMessage>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.messages.collect { messages += it } }

        // Pháo đỏ vào giữa, sĩ đen lên e8 làm ngòi, pháo ăn tốt e6: chiếu tướng.
        viewModel.tap("h2", "e2", "d9", "e8", "e2", "e6")
        assertEquals(listOf(GameMessage.CHECK), messages)

        viewModel.resign()
        assertTrue(viewModel.uiState.value.showResult)
        assertTrue(viewModel.uiState.value.game!!.status is GameStatus.Finished)
    }
}
