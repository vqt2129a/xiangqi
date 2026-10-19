package com.quangthanh.xiangqi.domain.event

import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.GameResult
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Piece
import com.quangthanh.xiangqi.domain.model.Side

/** Domain event: những điều đã xảy ra trong ván cờ, để tầng ngoài phản ứng (âm thanh, thông báo...). */
sealed interface GameEvent {
    val gameId: GameId

    data class MovePlayed(override val gameId: GameId, val move: Move, val piece: Piece, val captured: Piece?) : GameEvent

    data class MoveUndone(override val gameId: GameId, val move: Move) : GameEvent

    data class GeneralInCheck(override val gameId: GameId, val side: Side) : GameEvent

    data class GameFinished(override val gameId: GameId, val result: GameResult) : GameEvent
}
