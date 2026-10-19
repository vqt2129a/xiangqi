package com.quangthanh.xiangqi.domain.rules

import com.quangthanh.xiangqi.domain.model.Board
import com.quangthanh.xiangqi.domain.model.PieceType
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.domain.model.Square
import kotlin.math.abs
import kotlin.math.sign

/**
 * Kiểm tra tướng có đang bị chiếu không bằng cách nhìn ngược từ vị trí tướng ra,
 * thay vì sinh hết nước đi của đối phương. Sĩ và tượng không bao giờ sang được cung
 * đối phương nên không cần xét.
 */
object CheckDetector {

    fun isInCheck(board: Board, side: Side): Boolean =
        isAttackedBy(board, board.generalOf(side), side.opponent)

    private fun isAttackedBy(board: Board, target: Square, attacker: Side): Boolean =
        attackedAlongLines(board, target, attacker) ||
            attackedByHorse(board, target, attacker) ||
            attackedBySoldier(board, target, attacker)

    /** Xe, pháo và luật "lộ mặt tướng": hai tướng không được đối mặt trên cùng cột mà không có quân chắn. */
    private fun attackedAlongLines(board: Board, target: Square, attacker: Side): Boolean {
        for ((df, dr) in MoveGenerator.ORTHOGONAL) {
            var square = target.offset(df, dr)
            var screenFound = false
            while (square != null) {
                val piece = board[square]
                if (piece != null) {
                    val isAttacker = piece.side == attacker
                    if (!screenFound) {
                        if (isAttacker && piece.type == PieceType.CHARIOT) return true
                        if (isAttacker && piece.type == PieceType.GENERAL && df == 0) return true
                        screenFound = true
                    } else {
                        if (isAttacker && piece.type == PieceType.CANNON) return true
                        break
                    }
                }
                square = square.offset(df, dr)
            }
        }
        return false
    }

    private fun attackedByHorse(board: Board, target: Square, attacker: Side): Boolean {
        for ((df, dr) in HORSE_OFFSETS) {
            val horseSquare = target.offset(df, dr) ?: continue
            val piece = board[horseSquare]
            if (piece == null || piece.side != attacker || piece.type != PieceType.HORSE) continue
            // Chân mã nằm sát con mã, theo hướng dài của nước nhảy về phía mục tiêu.
            val leg = if (abs(dr) == 2) horseSquare.offset(0, -dr.sign) else horseSquare.offset(-df.sign, 0)
            if (leg != null && board.isEmpty(leg)) return true
        }
        return false
    }

    private fun attackedBySoldier(board: Board, target: Square, attacker: Side): Boolean {
        fun isSoldier(square: Square?) =
            square != null && board[square]?.let { it.side == attacker && it.type == PieceType.SOLDIER } == true

        if (isSoldier(target.offset(0, -attacker.forward))) return true
        // Tốt chỉ ăn ngang khi đã qua sông, tức là đang đứng trên sân đối phương.
        return listOf(-1, 1).any { df ->
            val square = target.offset(df, 0)
            isSoldier(square) && !square!!.isOnHomeSideOf(attacker)
        }
    }

    private val HORSE_OFFSETS = listOf(1 to 2, -1 to 2, 1 to -2, -1 to -2, 2 to 1, 2 to -1, -2 to 1, -2 to -1)
}
