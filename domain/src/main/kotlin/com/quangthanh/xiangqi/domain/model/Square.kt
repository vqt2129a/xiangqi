package com.quangthanh.xiangqi.domain.model

/**
 * Một giao điểm trên bàn cờ, ký hiệu theo chuẩn ICCS: cột a..i (file 0..8), hàng 0..9 (rank).
 * Hàng 0 là hàng cuối của bên Đỏ, hàng 9 là hàng cuối của bên Đen.
 */
data class Square(val file: Int, val rank: Int) {

    init {
        require(file in 0 until FILES && rank in 0 until RANKS) { "Square ($file, $rank) is outside the board" }
    }

    fun offset(dFile: Int, dRank: Int): Square? {
        val f = file + dFile
        val r = rank + dRank
        return if (f in 0 until FILES && r in 0 until RANKS) Square(f, r) else null
    }

    /** Ô nằm trên phần sân nhà (chưa qua sông) của [side]. */
    fun isOnHomeSideOf(side: Side): Boolean =
        if (side == Side.RED) rank <= RIVER_RANK_RED else rank > RIVER_RANK_RED

    fun isInPalaceOf(side: Side): Boolean =
        file in PALACE_FILES && if (side == Side.RED) rank in 0..2 else rank in 7..9

    override fun toString(): String = "${'a' + file}$rank"

    companion object {
        const val FILES = 9
        const val RANKS = 10
        private const val RIVER_RANK_RED = 4
        private val PALACE_FILES = 3..5

        val ALL: List<Square> = (0 until RANKS).flatMap { rank -> (0 until FILES).map { file -> Square(file, rank) } }

        /** Đọc ký hiệu ICCS, ví dụ "e0" hoặc "h2". */
        fun parse(text: String): Square {
            require(text.length == 2 && text[0] in 'a'..'i' && text[1] in '0'..'9') { "Invalid square: $text" }
            return Square(text[0] - 'a', text[1] - '0')
        }
    }
}
