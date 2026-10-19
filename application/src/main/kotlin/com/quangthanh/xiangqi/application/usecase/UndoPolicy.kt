package com.quangthanh.xiangqi.application.usecase

import com.quangthanh.xiangqi.domain.model.Game
import com.quangthanh.xiangqi.domain.model.Player

/**
 * Quy tắc "đi lại" của ứng dụng (khác với luật cờ nên đặt ở tầng application):
 * - Hai người chơi: lùi đúng 1 nước.
 * - Đánh với máy: lùi về lượt gần nhất của người, tức là bỏ luôn nước máy vừa đáp lại.
 */
internal object UndoPolicy {

    fun pliesToUndo(game: Game): Int {
        val moveCount = game.moves.size
        if (game.resignedBy != null || moveCount == 0) return 0
        if (!game.participants.isAgainstComputer) return 1

        return if (game.playerToMove is Player.Human) {
            // Máy vừa đi xong. Cần ít nhất 1 nước của người phía trước, nếu không thì không có gì để lùi
            // (trường hợp máy cầm Đỏ và mới đi nước đầu tiên).
            if (moveCount >= 2) 2 else 0
        } else {
            // Người vừa đi (ví dụ chiếu bí máy, hoặc máy chưa kịp đáp): chỉ lùi nước của người.
            1
        }
    }
}
