package com.quangthanh.xiangqi.domain.model

data class Move(val from: Square, val to: Square) {

    init {
        require(from != to) { "A move must change square" }
    }

    /** Ký hiệu ICCS, ví dụ "h2e2" là pháo bên phải Đỏ vào giữa. */
    override fun toString(): String = "$from$to"

    companion object {
        fun parse(text: String): Move {
            require(text.length == 4) { "Invalid move: $text" }
            return Move(Square.parse(text.substring(0, 2)), Square.parse(text.substring(2, 4)))
        }
    }
}

/** Nước đã đi, kèm quân đi và quân bị ăn (nếu có), dùng cho lịch sử ván và luật hòa. */
data class PlayedMove(val move: Move, val piece: Piece, val captured: Piece?)
