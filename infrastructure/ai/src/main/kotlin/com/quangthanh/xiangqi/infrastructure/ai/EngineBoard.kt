package com.quangthanh.xiangqi.infrastructure.ai

import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.PieceType
import com.quangthanh.xiangqi.domain.model.Position
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.domain.model.Square
import kotlin.math.abs
import kotlin.random.Random

/**
 * Bàn cờ tối ưu cho tìm kiếm: mảng 90 số nguyên, đi/lùi nước tại chỗ (make/unmake) thay vì tạo object mới.
 * Quân Đỏ là số dương, quân Đen là số âm; [side] = 1 khi Đỏ đi, -1 khi Đen đi.
 * Luật đi được viết lại cho nhanh và được test đối chiếu với domain (perft, so sánh nước đi).
 */
internal class EngineBoard private constructor(private val squares: IntArray, side: Int) {

    var side: Int = side
        private set

    /** Khóa Zobrist của thế cờ hiện tại, cập nhật tăng dần sau mỗi nước. */
    var hash: Long = 0L
        private set

    private val generals = IntArray(2)
    private val capturedStack = IntArray(MAX_STACK)
    private val hashStack = LongArray(MAX_STACK)
    private var stackSize = 0

    init {
        for (sq in 0 until SQUARES) {
            val piece = squares[sq]
            if (piece == 0) continue
            hash = hash xor Zobrist.piece(piece, sq)
            if (abs(piece) == GENERAL) generals[sideIndex(piece)] = sq
        }
        if (side < 0) hash = hash xor Zobrist.blackToMove
    }

    operator fun get(sq: Int): Int = squares[sq]

    fun make(move: Int) {
        val from = moveFrom(move)
        val to = moveTo(move)
        val piece = squares[from]
        val captured = squares[to]
        hashStack[stackSize] = hash
        capturedStack[stackSize] = captured
        stackSize++

        hash = hash xor Zobrist.piece(piece, from) xor Zobrist.piece(piece, to) xor Zobrist.blackToMove
        if (captured != 0) hash = hash xor Zobrist.piece(captured, to)
        squares[to] = piece
        squares[from] = 0
        if (abs(piece) == GENERAL) generals[sideIndex(piece)] = to
        side = -side
    }

    fun unmake(move: Int) {
        val from = moveFrom(move)
        val to = moveTo(move)
        stackSize--
        side = -side
        val piece = squares[to]
        squares[from] = piece
        squares[to] = capturedStack[stackSize]
        hash = hashStack[stackSize]
        if (abs(piece) == GENERAL) generals[sideIndex(piece)] = from
    }

    /** Sinh nước giả hợp lệ của bên đang đi vào [out], trả về số nước. Bên gọi tự loại nước để tướng bị chiếu. */
    fun generateMoves(out: IntArray, capturesOnly: Boolean): Int {
        var n = 0
        for (from in 0 until SQUARES) {
            val piece = squares[from]
            if (piece == 0 || (piece > 0) != (side > 0)) continue
            val f = from % FILES
            val r = from / FILES
            when (abs(piece)) {
                GENERAL -> for (d in ORTHOGONAL) {
                    val tf = f + d[0]
                    val tr = r + d[1]
                    if (inPalace(tf, tr, side)) n = add(out, n, from, square(tf, tr), capturesOnly)
                }

                ADVISOR -> for (d in DIAGONAL) {
                    val tf = f + d[0]
                    val tr = r + d[1]
                    if (inPalace(tf, tr, side)) n = add(out, n, from, square(tf, tr), capturesOnly)
                }

                ELEPHANT -> for (d in DIAGONAL) {
                    val tf = f + 2 * d[0]
                    val tr = r + 2 * d[1]
                    if (onBoard(tf, tr) && isHomeRank(tr, side) && squares[square(f + d[0], r + d[1])] == 0) {
                        n = add(out, n, from, square(tf, tr), capturesOnly)
                    }
                }

                HORSE -> for (d in ORTHOGONAL) {
                    val lf = f + d[0]
                    val lr = r + d[1]
                    if (!onBoard(lf, lr) || squares[square(lf, lr)] != 0) continue
                    for (s in -1..1 step 2) {
                        val tf = if (d[0] == 0) f + s else f + 2 * d[0]
                        val tr = if (d[0] == 0) r + 2 * d[1] else r + s
                        if (onBoard(tf, tr)) n = add(out, n, from, square(tf, tr), capturesOnly)
                    }
                }

                CHARIOT -> for (d in ORTHOGONAL) {
                    var tf = f + d[0]
                    var tr = r + d[1]
                    while (onBoard(tf, tr)) {
                        val target = square(tf, tr)
                        n = add(out, n, from, target, capturesOnly)
                        if (squares[target] != 0) break
                        tf += d[0]
                        tr += d[1]
                    }
                }

                CANNON -> for (d in ORTHOGONAL) {
                    var tf = f + d[0]
                    var tr = r + d[1]
                    var jumped = false
                    while (onBoard(tf, tr)) {
                        val target = square(tf, tr)
                        if (!jumped) {
                            if (squares[target] == 0) {
                                if (!capturesOnly) out[n++] = encode(from, target)
                            } else {
                                jumped = true
                            }
                        } else if (squares[target] != 0) {
                            if ((squares[target] > 0) != (side > 0)) out[n++] = encode(from, target)
                            break
                        }
                        tf += d[0]
                        tr += d[1]
                    }
                }

                SOLDIER -> {
                    if (onBoard(f, r + side)) n = add(out, n, from, square(f, r + side), capturesOnly)
                    if (!isHomeRank(r, side)) {
                        if (f > 0) n = add(out, n, from, from - 1, capturesOnly)
                        if (f < FILES - 1) n = add(out, n, from, from + 1, capturesOnly)
                    }
                }
            }
        }
        return n
    }

    fun isInCheck(sideToCheck: Int): Boolean {
        val target = generals[sideIndex(sideToCheck)]
        val attacker = -sideToCheck
        val f = target % FILES
        val r = target / FILES

        // Xe, pháo và lộ mặt tướng.
        for (d in ORTHOGONAL) {
            var tf = f + d[0]
            var tr = r + d[1]
            var screen = false
            while (onBoard(tf, tr)) {
                val piece = squares[square(tf, tr)]
                if (piece != 0) {
                    val own = (piece > 0) == (attacker > 0)
                    val type = abs(piece)
                    if (!screen) {
                        if (own && (type == CHARIOT || (type == GENERAL && d[0] == 0))) return true
                        screen = true
                    } else {
                        if (own && type == CANNON) return true
                        break
                    }
                }
                tf += d[0]
                tr += d[1]
            }
        }

        // Mã: chân mã nằm sát con mã, theo hướng dài của nước nhảy.
        for (d in HORSE_JUMPS) {
            val hf = f + d[0]
            val hr = r + d[1]
            if (!onBoard(hf, hr) || squares[square(hf, hr)] != HORSE * attacker) continue
            val leg = if (abs(d[1]) == 2) square(hf, hr - d[1] / 2) else square(hf - d[0] / 2, hr)
            if (squares[leg] == 0) return true
        }

        // Tốt: ăn thẳng, và ăn ngang khi đã qua sông.
        if (onBoard(f, r - attacker) && squares[square(f, r - attacker)] == SOLDIER * attacker) return true
        if (!isHomeRank(r, attacker)) {
            if (f > 0 && squares[target - 1] == SOLDIER * attacker) return true
            if (f < FILES - 1 && squares[target + 1] == SOLDIER * attacker) return true
        }
        return false
    }

    private fun add(out: IntArray, n: Int, from: Int, to: Int, capturesOnly: Boolean): Int {
        val target = squares[to]
        if (target != 0 && (target > 0) == (side > 0)) return n
        if (capturesOnly && target == 0) return n
        out[n] = encode(from, to)
        return n + 1
    }

    companion object {
        const val FILES = 9
        const val SQUARES = 90
        private const val MAX_STACK = 512

        const val GENERAL = 1
        const val ADVISOR = 2
        const val ELEPHANT = 3
        const val HORSE = 4
        const val CHARIOT = 5
        const val CANNON = 6
        const val SOLDIER = 7

        private val ORTHOGONAL = arrayOf(intArrayOf(0, 1), intArrayOf(0, -1), intArrayOf(1, 0), intArrayOf(-1, 0))
        private val DIAGONAL = arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1))
        private val HORSE_JUMPS = arrayOf(
            intArrayOf(1, 2), intArrayOf(-1, 2), intArrayOf(1, -2), intArrayOf(-1, -2),
            intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(-2, 1), intArrayOf(-2, -1),
        )

        fun encode(from: Int, to: Int): Int = (from shl 8) or to
        fun moveFrom(move: Int): Int = move ushr 8
        fun moveTo(move: Int): Int = move and 0xFF

        fun square(file: Int, rank: Int): Int = rank * FILES + file
        private fun onBoard(file: Int, rank: Int) = file in 0..8 && rank in 0..9
        private fun inPalace(file: Int, rank: Int, side: Int) = file in 3..5 && if (side > 0) rank in 0..2 else rank in 7..9
        fun isHomeRank(rank: Int, side: Int) = if (side > 0) rank <= 4 else rank >= 5
        private fun sideIndex(pieceOrSide: Int) = if (pieceOrSide > 0) 0 else 1

        fun from(position: Position): EngineBoard {
            val squares = IntArray(SQUARES)
            for ((sq, piece) in position.board.pieces()) {
                val code = when (piece.type) {
                    PieceType.GENERAL -> GENERAL
                    PieceType.ADVISOR -> ADVISOR
                    PieceType.ELEPHANT -> ELEPHANT
                    PieceType.HORSE -> HORSE
                    PieceType.CHARIOT -> CHARIOT
                    PieceType.CANNON -> CANNON
                    PieceType.SOLDIER -> SOLDIER
                }
                squares[square(sq.file, sq.rank)] = if (piece.side == Side.RED) code else -code
            }
            return EngineBoard(squares, if (position.sideToMove == Side.RED) 1 else -1)
        }

        fun toDomainMove(move: Int): Move {
            val from = moveFrom(move)
            val to = moveTo(move)
            return Move(Square(from % FILES, from / FILES), Square(to % FILES, to / FILES))
        }
    }
}

/** Bảng số ngẫu nhiên 64-bit cho mỗi cặp (quân, ô); XOR lại với nhau thành khóa băm của thế cờ. */
internal object Zobrist {
    private val random = Random(20261019)
    private val keys = LongArray(15 * EngineBoard.SQUARES) { random.nextLong() }
    val blackToMove: Long = random.nextLong()

    fun piece(piece: Int, square: Int): Long = keys[(piece + 7) * EngineBoard.SQUARES + square]
}
