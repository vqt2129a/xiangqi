package com.quangthanh.xiangqi.application.dto

import com.quangthanh.xiangqi.domain.event.GameEvent
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.GameStatus
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Participants
import com.quangthanh.xiangqi.domain.model.PlacedPiece
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.domain.model.Square

/**
 * Ảnh chụp chỉ-đọc của ván cờ gửi ra tầng ngoài. Tầng presentation không bao giờ cầm aggregate
 * [com.quangthanh.xiangqi.domain.model.Game], nên không thể đổi trạng thái ván mà không qua use case.
 */
data class GameState(
    val gameId: GameId,
    val participants: Participants,
    val pieces: List<PlacedPiece>,
    val sideToMove: Side,
    val status: GameStatus,
    val lastMove: Move?,
    /** Vị trí tướng đang bị chiếu, để giao diện tô đỏ. */
    val generalInCheck: Square?,
    /** Nước đi hợp lệ, nhóm theo ô xuất phát. Rỗng khi không phải lượt người chơi. */
    val legalMoves: Map<Square, List<Square>>,
    val moveCount: Int,
    val canUndo: Boolean,
    val isComputerTurn: Boolean,
)

/** Kết quả của một thao tác làm thay đổi ván cờ, kèm các domain event vừa xảy ra. */
data class GameUpdate(val state: GameState, val events: List<GameEvent>)
