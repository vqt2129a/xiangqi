package com.quangthanh.xiangqi.domain.notation

import com.quangthanh.xiangqi.domain.model.Board
import com.quangthanh.xiangqi.domain.model.Piece
import com.quangthanh.xiangqi.domain.model.PieceType
import com.quangthanh.xiangqi.domain.model.Position
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.domain.model.Square

/**
 * Ký hiệu FEN cho cờ tướng, ví dụ thế khai cuộc:
 * `rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w`.
 * Chữ hoa là quân Đỏ, chữ thường là quân Đen; hàng đầu tiên là hàng 9 (phía Đen).
 */
object Fen {

    const val INITIAL = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w"

    fun parse(fen: String): Position {
        val fields = fen.trim().split(' ')
        val rows = fields[0].split('/')
        require(rows.size == Square.RANKS) { "FEN must have ${Square.RANKS} ranks: $fen" }

        val placement = mutableMapOf<Square, Piece>()
        rows.forEachIndexed { rowIndex, row ->
            val rank = Square.RANKS - 1 - rowIndex
            var file = 0
            for (char in row) {
                if (char.isDigit()) {
                    file += char.digitToInt()
                } else {
                    require(file < Square.FILES) { "Too many squares on rank $rank: $fen" }
                    placement[Square(file, rank)] = pieceOf(char)
                    file++
                }
            }
            require(file == Square.FILES) { "Rank $rank must have ${Square.FILES} squares: $fen" }
        }

        val sideToMove = when (fields.getOrNull(1)) {
            null, "w", "r" -> Side.RED
            "b" -> Side.BLACK
            else -> throw IllegalArgumentException("Unknown side to move in FEN: $fen")
        }
        return Position(Board.of(placement), sideToMove)
    }

    fun format(position: Position): String {
        val rows = (Square.RANKS - 1 downTo 0).map { rank ->
            buildString {
                var empty = 0
                for (file in 0 until Square.FILES) {
                    val piece = position.board[Square(file, rank)]
                    if (piece == null) {
                        empty++
                    } else {
                        if (empty > 0) append(empty).also { empty = 0 }
                        append(charOf(piece))
                    }
                }
                if (empty > 0) append(empty)
            }
        }
        val side = if (position.sideToMove == Side.RED) "w" else "b"
        return rows.joinToString("/") + " " + side
    }

    private fun pieceOf(char: Char): Piece {
        val type = when (char.lowercaseChar()) {
            'k' -> PieceType.GENERAL
            'a' -> PieceType.ADVISOR
            'b', 'e' -> PieceType.ELEPHANT
            'n', 'h' -> PieceType.HORSE
            'r' -> PieceType.CHARIOT
            'c' -> PieceType.CANNON
            'p' -> PieceType.SOLDIER
            else -> throw IllegalArgumentException("Unknown piece '$char' in FEN")
        }
        return Piece(type, if (char.isUpperCase()) Side.RED else Side.BLACK)
    }

    private fun charOf(piece: Piece): Char {
        val char = when (piece.type) {
            PieceType.GENERAL -> 'k'
            PieceType.ADVISOR -> 'a'
            PieceType.ELEPHANT -> 'b'
            PieceType.HORSE -> 'n'
            PieceType.CHARIOT -> 'r'
            PieceType.CANNON -> 'c'
            PieceType.SOLDIER -> 'p'
        }
        return if (piece.side == Side.RED) char.uppercaseChar() else char
    }
}
