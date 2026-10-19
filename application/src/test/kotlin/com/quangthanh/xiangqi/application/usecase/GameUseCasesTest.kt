package com.quangthanh.xiangqi.application.usecase

import com.quangthanh.xiangqi.application.exception.GameChangedException
import com.quangthanh.xiangqi.application.exception.GameNotFoundException
import com.quangthanh.xiangqi.application.exception.NotYourTurnException
import com.quangthanh.xiangqi.application.fakes.InMemoryGameRepository
import com.quangthanh.xiangqi.domain.event.GameEvent
import com.quangthanh.xiangqi.domain.exception.NothingToUndoException
import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.GameResult
import com.quangthanh.xiangqi.domain.model.GameStatus
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.domain.model.Square
import com.quangthanh.xiangqi.domain.model.WinReason
import com.quangthanh.xiangqi.domain.rules.MoveGenerator
import com.quangthanh.xiangqi.domain.service.ComputerPlayer
import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameUseCasesTest {

    private val repository = InMemoryGameRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-19T08:00:00Z"), ZoneOffset.UTC)

    /** Máy giả: luôn đi nước hợp lệ đầu tiên. */
    private var computer = ComputerPlayer { position, _ -> MoveGenerator.legalMoves(position).first() }

    private val startNewGame = StartNewGameUseCase(repository, clock)
    private val playMove = PlayMoveUseCase(repository)
    private val playComputerMove get() = PlayComputerMoveUseCase(repository) { p, d -> computer.chooseMove(p, d) }
    private val undoMove = UndoMoveUseCase(repository)
    private val resign = ResignUseCase(repository)
    private val openGame = OpenGameUseCase(repository)
    private val getHistory = GetGameHistoryUseCase(repository)

    private val vsComputerAsRed = GameMode.AgainstComputer(Side.RED, Difficulty.EASY)

    @Test
    fun `new game starts from the initial position and is saved`() = runTest {
        val state = startNewGame(GameMode.TwoPlayers)

        assertEquals(32, state.pieces.size)
        assertEquals(44, state.legalMoves.values.sumOf { it.size })
        assertFalse(state.canUndo)
        assertEquals(state, openGame.latestUnfinished())
    }

    @Test
    fun `starting a new game resigns the unfinished one for the human`() = runTest {
        val abandoned = startNewGame(vsComputerAsRed)
        startNewGame(GameMode.TwoPlayers)

        val status = openGame(abandoned.gameId).status
        assertEquals(GameStatus.Finished(GameResult.Win(Side.BLACK, WinReason.RESIGNATION)), status)
    }

    @Test
    fun `human move is saved and reported as events`() = runTest {
        val game = startNewGame(GameMode.TwoPlayers)
        val update = playMove(game.gameId, Square.parse("h2"), Square.parse("e2"))

        assertTrue(update.events.single() is GameEvent.MovePlayed)
        assertEquals(Side.BLACK, update.state.sideToMove)
        assertEquals(1, openGame(game.gameId).moveCount)
    }

    @Test
    fun `human cannot move during the computer turn`() = runTest {
        val game = startNewGame(GameMode.AgainstComputer(Side.BLACK, Difficulty.EASY))

        assertTrue(game.isComputerTurn)
        assertTrue(game.legalMoves.isEmpty())
        assertFailsWith<NotYourTurnException> { playMove(game.gameId, Square.parse("h2"), Square.parse("e2")) }
    }

    @Test
    fun `computer answers the human move`() = runTest {
        val game = startNewGame(vsComputerAsRed)
        val afterHuman = playMove(game.gameId, Square.parse("h2"), Square.parse("e2"))
        assertTrue(afterHuman.state.isComputerTurn)

        val afterComputer = playComputerMove(game.gameId)
        assertEquals(2, afterComputer.state.moveCount)
        assertEquals(Side.RED, afterComputer.state.sideToMove)
        assertFailsWith<NotYourTurnException> { playComputerMove(game.gameId) }
    }

    @Test
    fun `computer move is discarded when the game changed while it was thinking`() = runTest {
        val game = startNewGame(vsComputerAsRed)
        playMove(game.gameId, Square.parse("h2"), Square.parse("e2"))
        computer = ComputerPlayer { position, _ ->
            undoMove(game.gameId) // người chơi bấm "đi lại" trong lúc máy đang nghĩ
            MoveGenerator.legalMoves(position).first()
        }

        assertFailsWith<GameChangedException> { playComputerMove(game.gameId) }
        assertEquals(0, openGame(game.gameId).moveCount)
    }

    @Test
    fun `undo against the computer goes back to the human turn`() = runTest {
        val game = startNewGame(vsComputerAsRed)
        playMove(game.gameId, Square.parse("h2"), Square.parse("e2"))
        playComputerMove(game.gameId)

        val update = undoMove(game.gameId)
        assertEquals(0, update.state.moveCount)
        assertEquals(Side.RED, update.state.sideToMove)
        assertEquals(2, update.events.count { it is GameEvent.MoveUndone })
    }

    @Test
    fun `nothing to undo when the computer opened the game`() = runTest {
        val game = startNewGame(GameMode.AgainstComputer(Side.BLACK, Difficulty.EASY))
        val afterComputer = playComputerMove(game.gameId)

        assertFalse(afterComputer.state.canUndo)
        assertFailsWith<NothingToUndoException> { undoMove(game.gameId) }
    }

    @Test
    fun `undo between two players takes back a single move`() = runTest {
        val game = startNewGame(GameMode.TwoPlayers)
        playMove(game.gameId, Square.parse("h2"), Square.parse("e2"))
        playMove(game.gameId, Square.parse("h9"), Square.parse("g7"))

        assertEquals(1, undoMove(game.gameId).state.moveCount)
    }

    @Test
    fun `resigning against the computer always resigns for the human`() = runTest {
        val game = startNewGame(GameMode.AgainstComputer(Side.BLACK, Difficulty.HARD))
        val update = resign(game.gameId)

        assertEquals(GameStatus.Finished(GameResult.Win(Side.RED, WinReason.RESIGNATION)), update.state.status)
        assertTrue(update.events.single() is GameEvent.GameFinished)
    }

    @Test
    fun `history shows the record against each difficulty`() = runTest {
        val lost = startNewGame(vsComputerAsRed)
        resign(lost.gameId)
        startNewGame(GameMode.AgainstComputer(Side.BLACK, Difficulty.HARD))
        val ongoing = startNewGame(GameMode.TwoPlayers) // ván trước bị bỏ dở nên tính thua

        val history = getHistory()
        assertEquals(3, history.games.size)
        assertEquals(1, history.recordByDifficulty.getValue(Difficulty.EASY).losses)
        assertEquals(1, history.recordByDifficulty.getValue(Difficulty.HARD).losses)
        assertEquals(0, history.recordByDifficulty.getValue(Difficulty.MEDIUM).played)
        assertEquals(null, history.games.single { it.gameId == ongoing.gameId }.result)
    }

    @Test
    fun `unknown game is reported`() = runTest {
        assertFailsWith<GameNotFoundException> { openGame(GameId("missing")) }
    }
}
