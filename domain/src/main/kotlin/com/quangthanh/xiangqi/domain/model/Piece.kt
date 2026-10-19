package com.quangthanh.xiangqi.domain.model

enum class PieceType {
    GENERAL,
    ADVISOR,
    ELEPHANT,
    HORSE,
    CHARIOT,
    CANNON,
    SOLDIER;

    /** Quân có thể sang sông tấn công. Khi cả hai bên đều hết loại quân này thì ván cờ hòa. */
    val canAttackAcrossRiver: Boolean
        get() = this == HORSE || this == CHARIOT || this == CANNON || this == SOLDIER
}

data class Piece(val type: PieceType, val side: Side) {
    override fun toString(): String = "$side $type"
}

data class PlacedPiece(val square: Square, val piece: Piece)
