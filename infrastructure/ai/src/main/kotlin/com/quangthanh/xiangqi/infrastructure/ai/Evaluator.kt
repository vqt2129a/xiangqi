package com.quangthanh.xiangqi.infrastructure.ai

import com.quangthanh.xiangqi.infrastructure.ai.EngineBoard.Companion.ADVISOR
import com.quangthanh.xiangqi.infrastructure.ai.EngineBoard.Companion.CANNON
import com.quangthanh.xiangqi.infrastructure.ai.EngineBoard.Companion.CHARIOT
import com.quangthanh.xiangqi.infrastructure.ai.EngineBoard.Companion.ELEPHANT
import com.quangthanh.xiangqi.infrastructure.ai.EngineBoard.Companion.FILES
import com.quangthanh.xiangqi.infrastructure.ai.EngineBoard.Companion.HORSE
import com.quangthanh.xiangqi.infrastructure.ai.EngineBoard.Companion.SOLDIER
import com.quangthanh.xiangqi.infrastructure.ai.EngineBoard.Companion.SQUARES
import kotlin.math.abs
import kotlin.math.min

/**
 * Hàm đánh giá thế cờ: giá trị quân cộng điểm vị trí. Điểm tính theo đơn vị 1/100 con tốt,
 * và luôn nhìn từ phía bên đang đi (dương là bên đang đi có lợi).
 */
internal object Evaluator {

    fun evaluate(board: EngineBoard): Int {
        var score = 0
        for (sq in 0 until SQUARES) {
            val piece = board[sq]
            if (piece == 0) continue
            val side = if (piece > 0) 1 else -1
            val type = abs(piece)
            score += side * (pieceValue(type) + positionalBonus(type, sq, side))
        }
        return score * board.side
    }

    fun pieceValue(type: Int): Int = when (type) {
        ADVISOR, ELEPHANT -> 200
        HORSE -> 400
        CHARIOT -> 900
        CANNON -> 450
        SOLDIER -> 100
        else -> 0 // Tướng không bao giờ bị ăn nên không cần giá trị.
    }

    private fun positionalBonus(type: Int, sq: Int, side: Int): Int {
        val file = sq % FILES
        val rank = sq / FILES
        val advance = if (side > 0) rank else 9 - rank // 0 = hàng cuối nhà mình, 9 = hàng cuối đối phương
        val centrality = 4 - abs(file - 4)
        return when (type) {
            // Tốt qua sông mạnh gần gấp đôi, nhưng xuống tận hàng cuối thì gần như vô dụng.
            SOLDIER -> when {
                advance < 5 -> 0
                advance == 9 -> 30
                else -> 90 + centrality * 10 + (advance - 5) * 10
            }
            HORSE -> centrality * 8 + min(advance, 7) * 6
            CHARIOT -> centrality * 4 + if (advance >= 5) 20 else 0
            CANNON -> if (file == 4) 20 else centrality * 3
            else -> 0
        }
    }
}
