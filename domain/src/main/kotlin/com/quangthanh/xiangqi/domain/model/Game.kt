package com.quangthanh.xiangqi.domain.model

import com.quangthanh.xiangqi.domain.event.GameEvent
import com.quangthanh.xiangqi.domain.exception.GameAlreadyFinishedException
import com.quangthanh.xiangqi.domain.exception.IllegalMoveException
import com.quangthanh.xiangqi.domain.exception.NothingToUndoException
import com.quangthanh.xiangqi.domain.rules.CheckDetector
import com.quangthanh.xiangqi.domain.rules.GameArbiter
import com.quangthanh.xiangqi.domain.rules.MoveGenerator
import java.time.Instant

/**
 * Aggregate root của ván cờ. Mọi thay đổi trạng thái đều phải đi qua [play], [undoLastMove]
 * hoặc [resign], nên ván cờ không bao giờ rơi vào trạng thái sai luật.
 *
 * Mỗi thay đổi được ghi lại thành [GameEvent]; tầng application lấy ra bằng [pullEvents].
 */
class Game private constructor(
    val id: GameId,
    val participants: Participants,
    val startedAt: Instant,
    startingPosition: Position,
) {
    private val positions = mutableListOf(startingPosition)
    private val playedMoves = mutableListOf<PlayedMove>()
    private val pendingEvents = mutableListOf<GameEvent>()

    var status: GameStatus = GameArbiter.evaluate(positions, pliesSinceLastCapture = 0)
        private set

    var resignedBy: Side? = null
        private set

    val startingPosition: Position get() = positions.first()
    val position: Position get() = positions.last()
    val sideToMove: Side get() = position.sideToMove
    val playerToMove: Player get() = participants.playerOf(sideToMove)
    val moves: List<PlayedMove> get() = playedMoves.toList()
    val lastMove: PlayedMove? get() = playedMoves.lastOrNull()
    val isFinished: Boolean get() = status is GameStatus.Finished
    val isInCheck: Boolean get() = CheckDetector.isInCheck(position.board, sideToMove)

    fun legalMoves(): List<Move> = if (isFinished) emptyList() else MoveGenerator.legalMoves(position)

    fun play(move: Move) {
        ensureInProgress()
        if (!MoveGenerator.isLegal(position, move)) throw IllegalMoveException(move)

        val board = position.board
        val played = PlayedMove(move, piece = board[move.from]!!, captured = board[move.to])
        playedMoves += played
        positions += position.after(move)
        status = GameArbiter.evaluate(positions, pliesSinceLastCapture())

        pendingEvents += GameEvent.MovePlayed(id, move, played.piece, played.captured)
        when (val current = status) {
            is GameStatus.Finished -> pendingEvents += GameEvent.GameFinished(id, current.result)
            GameStatus.InProgress -> if (isInCheck) pendingEvents += GameEvent.GeneralInCheck(id, sideToMove)
        }
    }

    /** Đi lại một nước. Được phép cả khi ván đã hết do chiếu bí hay hòa, nhưng không được sau khi đã đầu hàng. */
    fun undoLastMove() {
        if (resignedBy != null) throw GameAlreadyFinishedException(id)
        if (playedMoves.isEmpty()) throw NothingToUndoException(id)

        val undone = playedMoves.removeAt(playedMoves.lastIndex)
        positions.removeAt(positions.lastIndex)
        status = GameArbiter.evaluate(positions, pliesSinceLastCapture())
        pendingEvents += GameEvent.MoveUndone(id, undone.move)
    }

    fun resign(side: Side) {
        ensureInProgress()
        resignedBy = side
        val result = GameResult.Win(side.opponent, WinReason.RESIGNATION)
        status = GameStatus.Finished(result)
        pendingEvents += GameEvent.GameFinished(id, result)
    }

    fun pullEvents(): List<GameEvent> = pendingEvents.toList().also { pendingEvents.clear() }

    private fun ensureInProgress() {
        if (isFinished) throw GameAlreadyFinishedException(id)
    }

    private fun pliesSinceLastCapture(): Int =
        playedMoves.size - (playedMoves.indexOfLast { it.captured != null } + 1)

    companion object {

        fun start(
            id: GameId,
            participants: Participants,
            startedAt: Instant,
            startingPosition: Position = Position.INITIAL,
        ): Game = Game(id, participants, startedAt, startingPosition)

        /**
         * Dựng lại ván cờ đã lưu bằng cách đi lại từng nước từ thế ban đầu.
         * Nhờ vậy dữ liệu lưu trữ chỉ cần danh sách nước đi, và dữ liệu hỏng sẽ bị phát hiện ngay.
         */
        fun restore(
            id: GameId,
            participants: Participants,
            startedAt: Instant,
            startingPosition: Position,
            moves: List<Move>,
            resignedBy: Side?,
        ): Game {
            val game = Game(id, participants, startedAt, startingPosition)
            moves.forEach(game::play)
            resignedBy?.let(game::resign)
            game.pendingEvents.clear()
            return game
        }
    }
}
