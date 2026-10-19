package com.quangthanh.xiangqi.domain.rules

import com.quangthanh.xiangqi.domain.model.Board
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Piece
import com.quangthanh.xiangqi.domain.model.PieceType
import com.quangthanh.xiangqi.domain.model.Position
import com.quangthanh.xiangqi.domain.model.Square

/**
 * Domain service sinh nước đi theo luật cờ tướng.
 *
 * Nước "giả hợp lệ" (pseudo-legal) là nước đúng cách đi của quân. Nước hợp lệ còn phải
 * không để tướng của mình bị chiếu sau khi đi, kể cả lộ mặt tướng.
 */
object MoveGenerator {

    fun legalMoves(position: Position): List<Move> =
        position.board.piecesOf(position.sideToMove).flatMap { legalMovesFrom(position, it.square) }

    fun legalMovesFrom(position: Position, from: Square): List<Move> {
        val piece = position.board[from] ?: return emptyList()
        if (piece.side != position.sideToMove) return emptyList()
        return pseudoLegalMoves(position.board, from, piece).filter { leavesGeneralSafe(position, it) }
    }

    fun isLegal(position: Position, move: Move): Boolean {
        val piece = position.board[move.from] ?: return false
        return piece.side == position.sideToMove &&
            move in pseudoLegalMoves(position.board, move.from, piece) &&
            leavesGeneralSafe(position, move)
    }

    fun hasAnyLegalMove(position: Position): Boolean =
        position.board.piecesOf(position.sideToMove).any { legalMovesFrom(position, it.square).isNotEmpty() }

    private fun leavesGeneralSafe(position: Position, move: Move): Boolean =
        !CheckDetector.isInCheck(position.board.apply(move), position.sideToMove)

    private fun pseudoLegalMoves(board: Board, from: Square, piece: Piece): List<Move> {
        val side = piece.side
        val targets: List<Square> = when (piece.type) {
            PieceType.GENERAL -> ORTHOGONAL.mapNotNull { (df, dr) -> from.offset(df, dr) }.filter { it.isInPalaceOf(side) }

            PieceType.ADVISOR -> DIAGONAL.mapNotNull { (df, dr) -> from.offset(df, dr) }.filter { it.isInPalaceOf(side) }

            // Tượng đi chéo 2 ô, bị cản nếu có quân ở giữa ("mắt tượng"), và không được qua sông.
            PieceType.ELEPHANT -> DIAGONAL.mapNotNull { (df, dr) ->
                val eye = from.offset(df, dr) ?: return@mapNotNull null
                if (!board.isEmpty(eye)) return@mapNotNull null
                from.offset(2 * df, 2 * dr)?.takeIf { it.isOnHomeSideOf(side) }
            }

            // Mã đi 1 ô thẳng rồi 1 ô chéo, bị cản nếu ô thẳng đầu tiên có quân ("cản chân mã").
            PieceType.HORSE -> ORTHOGONAL.flatMap { (df, dr) ->
                val leg = from.offset(df, dr)
                if (leg == null || !board.isEmpty(leg)) return@flatMap emptyList()
                if (df == 0) listOfNotNull(from.offset(-1, 2 * dr), from.offset(1, 2 * dr))
                else listOfNotNull(from.offset(2 * df, -1), from.offset(2 * df, 1))
            }

            PieceType.CHARIOT -> ORTHOGONAL.flatMap { (df, dr) -> slide(board, from, df, dr, mustJump = false) }

            // Pháo đi như xe nhưng muốn ăn quân thì phải nhảy qua đúng một quân làm "ngòi".
            PieceType.CANNON -> ORTHOGONAL.flatMap { (df, dr) -> slide(board, from, df, dr, mustJump = true) }

            // Tốt chỉ tiến; sau khi qua sông được đi ngang.
            PieceType.SOLDIER -> buildList {
                from.offset(0, side.forward)?.let(::add)
                if (!from.isOnHomeSideOf(side)) {
                    from.offset(-1, 0)?.let(::add)
                    from.offset(1, 0)?.let(::add)
                }
            }
        }
        return targets.filter { board[it]?.side != side }.map { Move(from, it) }
    }

    private fun slide(board: Board, from: Square, df: Int, dr: Int, mustJump: Boolean): List<Square> {
        val result = mutableListOf<Square>()
        var square = from.offset(df, dr)
        while (square != null && board.isEmpty(square)) {
            result += square
            square = square.offset(df, dr)
        }
        if (square == null) return result
        if (!mustJump) {
            result += square // Xe dừng ở quân đầu tiên; bên gọi sẽ loại nếu là quân mình.
            return result
        }
        square = square.offset(df, dr)
        while (square != null && board.isEmpty(square)) square = square.offset(df, dr)
        if (square != null) result += square
        return result
    }

    internal val ORTHOGONAL = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
    internal val DIAGONAL = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
}
