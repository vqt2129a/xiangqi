package com.quangthanh.xiangqi.domain.model

/** Thế cờ: bàn cờ cộng với bên đến lượt. Hai thế cờ bằng nhau khi cả hai yếu tố đều trùng. */
data class Position(val board: Board, val sideToMove: Side) {

    fun after(move: Move): Position = Position(board.apply(move), sideToMove.opponent)

    companion object {
        val INITIAL = Position(Board.initial(), Side.RED)
    }
}
