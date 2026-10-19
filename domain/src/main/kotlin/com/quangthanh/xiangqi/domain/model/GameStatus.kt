package com.quangthanh.xiangqi.domain.model

import java.util.UUID

@JvmInline
value class GameId(val value: String) {
    companion object {
        fun generate() = GameId(UUID.randomUUID().toString())
    }
}

sealed interface GameStatus {
    data object InProgress : GameStatus
    data class Finished(val result: GameResult) : GameStatus
}

sealed interface GameResult {
    data class Win(val winner: Side, val reason: WinReason) : GameResult
    data class Draw(val reason: DrawReason) : GameResult
}

enum class WinReason {
    CHECKMATE,

    /** Cờ tướng khác cờ vua: bên hết nước đi (dù không bị chiếu) là bên thua. */
    STALEMATE,
    RESIGNATION,
}

enum class DrawReason {
    REPETITION,

    /** Quá 60 nước (120 lượt đi) không có quân nào bị ăn. */
    MOVE_LIMIT,

    /** Cả hai bên không còn quân nào qua sông tấn công được. */
    INSUFFICIENT_MATERIAL,
}
