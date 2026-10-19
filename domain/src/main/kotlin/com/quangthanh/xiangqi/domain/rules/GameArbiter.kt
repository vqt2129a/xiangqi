package com.quangthanh.xiangqi.domain.rules

import com.quangthanh.xiangqi.domain.model.DrawReason
import com.quangthanh.xiangqi.domain.model.GameResult
import com.quangthanh.xiangqi.domain.model.GameStatus
import com.quangthanh.xiangqi.domain.model.Position
import com.quangthanh.xiangqi.domain.model.WinReason

/**
 * Trọng tài: quyết định ván cờ đã kết thúc chưa sau mỗi nước đi.
 *
 * Luật hòa ở đây là bản đơn giản hóa. Luật châu Á đầy đủ về chiếu dai, đuổi dai
 * (bên nào phạm luật thì bị xử thua) rất phức tạp, nên lặp lại thế cờ 3 lần được tính là hòa.
 */
object GameArbiter {

    const val REPETITION_LIMIT = 3
    const val NO_CAPTURE_PLY_LIMIT = 120

    fun evaluate(positionHistory: List<Position>, pliesSinceLastCapture: Int): GameStatus {
        val current = positionHistory.last()
        val side = current.sideToMove

        if (!MoveGenerator.hasAnyLegalMove(current)) {
            val reason = if (CheckDetector.isInCheck(current.board, side)) WinReason.CHECKMATE else WinReason.STALEMATE
            return GameStatus.Finished(GameResult.Win(side.opponent, reason))
        }
        val drawReason = when {
            positionHistory.count { it == current } >= REPETITION_LIMIT -> DrawReason.REPETITION
            pliesSinceLastCapture >= NO_CAPTURE_PLY_LIMIT -> DrawReason.MOVE_LIMIT
            current.board.pieces().none { it.piece.type.canAttackAcrossRiver } -> DrawReason.INSUFFICIENT_MATERIAL
            else -> null
        }
        return drawReason?.let { GameStatus.Finished(GameResult.Draw(it)) } ?: GameStatus.InProgress
    }
}
