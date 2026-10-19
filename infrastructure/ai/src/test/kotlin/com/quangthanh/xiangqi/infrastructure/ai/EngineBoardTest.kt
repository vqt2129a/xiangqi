package com.quangthanh.xiangqi.infrastructure.ai

import com.quangthanh.xiangqi.domain.model.Position
import com.quangthanh.xiangqi.domain.notation.Fen
import com.quangthanh.xiangqi.domain.rules.MoveGenerator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/** Engine viết lại luật cờ để chạy nhanh, nên phải chứng minh nó đi y hệt domain. */
class EngineBoardTest {

    @Test
    fun `perft matches reference values`() {
        val board = EngineBoard.from(Position.INITIAL)
        assertEquals(44, perft(board, 1))
        assertEquals(1_920, perft(board, 2))
        assertEquals(79_666, perft(board, 3))
        assertEquals(3_290_240, perft(board, 4))
    }

    @Test
    fun `legal moves match the domain rules in random games`() {
        val random = Random(42)
        repeat(40) {
            var position = Position.INITIAL
            repeat(80) {
                val expected = MoveGenerator.legalMoves(position).map { it.toString() }.toSet()
                val actual = engineLegalMoves(EngineBoard.from(position)).map { EngineBoard.toDomainMove(it).toString() }.toSet()
                assertEquals(expected, actual, "Mismatch in ${Fen.format(position)}")
                if (expected.isEmpty()) return@repeat
                position = position.after(MoveGenerator.legalMoves(position).random(random))
            }
        }
    }

    @Test
    fun `make and unmake restore the position and its hash`() {
        val board = EngineBoard.from(Fen.parse("2bak4/4a4/4b4/p3C3p/2p6/6P2/P3n3P/4B4/4A4/3AK1B2 w"))
        val hash = board.hash
        for (move in engineLegalMoves(board)) {
            board.make(move)
            board.unmake(move)
            assertEquals(hash, board.hash)
        }
    }

    @Test
    fun `incremental hash equals hash computed from scratch`() {
        val position = Position.INITIAL
        val board = EngineBoard.from(position)
        val move = engineLegalMoves(board).first()
        board.make(move)
        assertEquals(EngineBoard.from(position.after(EngineBoard.toDomainMove(move))).hash, board.hash)
    }

    private fun engineLegalMoves(board: EngineBoard): List<Int> {
        val buffer = IntArray(128)
        val count = board.generateMoves(buffer, capturesOnly = false)
        return (0 until count).map { buffer[it] }.filter { move ->
            board.make(move)
            val legal = !board.isInCheck(-board.side)
            board.unmake(move)
            legal
        }
    }

    private fun perft(board: EngineBoard, depth: Int): Long {
        if (depth == 0) return 1
        var nodes = 0L
        for (move in engineLegalMoves(board)) {
            board.make(move)
            nodes += perft(board, depth - 1)
            board.unmake(move)
        }
        return nodes
    }
}
