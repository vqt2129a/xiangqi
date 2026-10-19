package com.quangthanh.xiangqi.domain.model

import com.quangthanh.xiangqi.domain.event.GameEvent
import com.quangthanh.xiangqi.domain.exception.GameAlreadyFinishedException
import com.quangthanh.xiangqi.domain.exception.IllegalMoveException
import com.quangthanh.xiangqi.domain.exception.NothingToUndoException
import com.quangthanh.xiangqi.domain.fen
import com.quangthanh.xiangqi.domain.move
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameTest {

    private fun newGame(position: Position = Position.INITIAL) =
        Game.start(GameId("test"), Participants.twoPlayers(), Instant.EPOCH, position)

    @Test
    fun `a legal move switches the turn and records an event`() {
        val game = newGame()
        game.play(move("h2e2"))

        assertEquals(Side.BLACK, game.sideToMove)
        assertEquals(Piece(PieceType.CANNON, Side.RED), game.position.board[Square.parse("e2")])
        val event = assertIs<GameEvent.MovePlayed>(game.pullEvents().single())
        assertNull(event.captured)
        assertTrue(game.pullEvents().isEmpty(), "events are pulled only once")
    }

    @Test
    fun `illegal moves are rejected`() {
        val game = newGame()
        assertFailsWith<IllegalMoveException> { game.play(move("b0b2")) } // mã không đi thẳng
        assertFailsWith<IllegalMoveException> { game.play(move("h7e7")) } // chưa tới lượt Đen
        assertEquals(Position.INITIAL, game.position)
    }

    @Test
    fun `checkmate ends the game`() {
        // Xe a8 khóa hàng 8, xe i0 lên i9 chiếu. Tướng đen không chạy được sang d9 vì lộ mặt tướng.
        val game = newGame(fen("4k4/R8/9/9/9/9/9/9/9/3K4R w"))
        game.play(move("i0i9"))

        assertEquals(GameStatus.Finished(GameResult.Win(Side.RED, WinReason.CHECKMATE)), game.status)
        assertTrue(game.pullEvents().any { it is GameEvent.GameFinished })
        assertFailsWith<GameAlreadyFinishedException> { game.play(move("e9e8")) }
    }

    @Test
    fun `a side with no legal move loses even without being in check`() {
        val game = newGame(fen("4k4/R8/9/9/9/9/9/9/5R3/3K5 b"))
        assertEquals(GameStatus.Finished(GameResult.Win(Side.RED, WinReason.STALEMATE)), game.status)
    }

    @Test
    fun `check is announced while the game goes on`() {
        val game = newGame(fen("4k4/9/9/9/9/9/9/9/9/R2K5 w"))
        game.play(move("a0a9"))

        assertEquals(GameStatus.InProgress, game.status)
        assertTrue(game.isInCheck)
        assertTrue(game.pullEvents().contains(GameEvent.GeneralInCheck(game.id, Side.BLACK)))
    }

    @Test
    fun `repeating the same position three times is a draw`() {
        val game = newGame()
        repeat(2) { listOf("b0c2", "b9c7", "c2b0", "c7b9").forEach { game.play(move(it)) } }

        assertEquals(GameStatus.Finished(GameResult.Draw(DrawReason.REPETITION)), game.status)
    }

    @Test
    fun `only generals and defenders left is a draw`() {
        val game = newGame(fen("3a1k3/4a4/9/9/9/9/9/9/9/3AKA3 w"))
        assertEquals(GameStatus.Finished(GameResult.Draw(DrawReason.INSUFFICIENT_MATERIAL)), game.status)
    }

    @Test
    fun `undo restores the previous position and reopens a finished game`() {
        val game = newGame(fen("4k4/R8/9/9/9/9/9/9/9/3K4R w"))
        val before = game.position
        game.play(move("i0i9"))
        game.undoLastMove()

        assertEquals(before, game.position)
        assertEquals(GameStatus.InProgress, game.status)
        assertFailsWith<NothingToUndoException> { game.undoLastMove() }
    }

    @Test
    fun `resigning hands the win to the opponent and cannot be undone`() {
        val game = newGame()
        game.play(move("h2e2"))
        game.resign(Side.BLACK)

        assertEquals(GameStatus.Finished(GameResult.Win(Side.RED, WinReason.RESIGNATION)), game.status)
        assertFailsWith<GameAlreadyFinishedException> { game.undoLastMove() }
    }

    @Test
    fun `restore replays the stored moves without emitting events`() {
        val original = newGame()
        listOf("h2e2", "h9g7", "h0g2").forEach { original.play(move(it)) }

        val restored = Game.restore(
            original.id, original.participants, original.startedAt, original.startingPosition,
            original.moves.map { it.move }, resignedBy = null,
        )

        assertEquals(original.position, restored.position)
        assertEquals(original.moves, restored.moves)
        assertTrue(restored.pullEvents().isEmpty())
    }
}
