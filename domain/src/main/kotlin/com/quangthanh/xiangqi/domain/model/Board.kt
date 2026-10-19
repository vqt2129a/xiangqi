package com.quangthanh.xiangqi.domain.model

/**
 * Value object bất biến: vị trí các quân trên bàn. Mỗi nước đi tạo ra một [Board] mới,
 * nên hai bàn cờ bằng nhau khi các quân giống hệt nhau (dùng cho luật lặp nước).
 */
class Board private constructor(private val cells: Array<Piece?>) {

    operator fun get(square: Square): Piece? = cells[indexOf(square)]

    fun isEmpty(square: Square): Boolean = get(square) == null

    fun pieces(): List<PlacedPiece> =
        Square.ALL.mapNotNull { square -> get(square)?.let { PlacedPiece(square, it) } }

    fun piecesOf(side: Side): List<PlacedPiece> = pieces().filter { it.piece.side == side }

    fun generalOf(side: Side): Square =
        Square.ALL.first { get(it) == Piece(PieceType.GENERAL, side) }

    /** Trả về bàn cờ sau khi di chuyển quân. Không kiểm tra luật, việc đó thuộc về [com.quangthanh.xiangqi.domain.rules.MoveGenerator]. */
    fun apply(move: Move): Board {
        val piece = requireNotNull(get(move.from)) { "No piece on ${move.from}" }
        val next = cells.copyOf()
        next[indexOf(move.to)] = piece
        next[indexOf(move.from)] = null
        return Board(next)
    }

    override fun equals(other: Any?): Boolean = other is Board && cells.contentEquals(other.cells)

    override fun hashCode(): Int = cells.contentHashCode()

    companion object {

        fun of(placement: Map<Square, Piece>): Board {
            for (side in Side.entries) {
                val generals = placement.values.count { it == Piece(PieceType.GENERAL, side) }
                require(generals == 1) { "$side must have exactly one general, found $generals" }
            }
            val cells = arrayOfNulls<Piece>(Square.FILES * Square.RANKS)
            placement.forEach { (square, piece) -> cells[indexOf(square)] = piece }
            return Board(cells)
        }

        fun initial(): Board {
            val backRank = listOf(
                PieceType.CHARIOT, PieceType.HORSE, PieceType.ELEPHANT, PieceType.ADVISOR, PieceType.GENERAL,
                PieceType.ADVISOR, PieceType.ELEPHANT, PieceType.HORSE, PieceType.CHARIOT,
            )
            val placement = buildMap {
                for (side in Side.entries) {
                    // Đỏ ở hàng 0..3, Đen đối xứng ở hàng 9..6.
                    fun rank(r: Int) = if (side == Side.RED) r else Square.RANKS - 1 - r
                    backRank.forEachIndexed { file, type -> put(Square(file, rank(0)), Piece(type, side)) }
                    put(Square(1, rank(2)), Piece(PieceType.CANNON, side))
                    put(Square(7, rank(2)), Piece(PieceType.CANNON, side))
                    for (file in 0 until Square.FILES step 2) put(Square(file, rank(3)), Piece(PieceType.SOLDIER, side))
                }
            }
            return of(placement)
        }

        private fun indexOf(square: Square): Int = square.rank * Square.FILES + square.file
    }
}
