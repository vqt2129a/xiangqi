package com.quangthanh.xiangqi.domain.rules

import com.quangthanh.xiangqi.domain.fen
import com.quangthanh.xiangqi.domain.model.Position
import com.quangthanh.xiangqi.domain.move
import com.quangthanh.xiangqi.domain.square
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoveGeneratorTest {

    /**
     * Perft: đếm số thế cờ đạt được sau N nước từ thế khai cuộc. Các con số 44 / 1.920 / 79.666
     * là giá trị chuẩn đã được công nhận; khớp được nghĩa là luật đi của mọi quân đều đúng.
     */
    @Test
    fun `perft from initial position matches reference values`() {
        assertEquals(44, perft(Position.INITIAL, 1))
        assertEquals(1_920, perft(Position.INITIAL, 2))
        assertEquals(79_666, perft(Position.INITIAL, 3))
    }

    @Test
    fun `horse is blocked when its leg is occupied`() {
        val free = fen("4k4/9/9/9/9/9/9/9/9/1N1K5 w")
        assertEquals(setOf("b0a2", "b0c2", "b0d1"), movesFrom(free, "b0"))

        val legBlocked = fen("4k4/9/9/9/9/9/9/9/1P7/1N1K5 w")
        assertEquals(setOf("b0d1"), movesFrom(legBlocked, "b0"))
    }

    @Test
    fun `elephant cannot cross the river and is blocked by its eye`() {
        val position = fen("4k4/9/9/9/9/2B6/9/9/9/3K5 w")
        assertEquals(setOf("c4a2", "c4e2"), movesFrom(position, "c4"))

        val eyeBlocked = fen("4k4/9/9/9/9/2B6/1P7/9/9/3K5 w")
        assertEquals(setOf("c4e2"), movesFrom(eyeBlocked, "c4"))
    }

    @Test
    fun `cannon captures only by jumping over exactly one screen`() {
        val position = fen("4k4/9/9/9/9/9/9/9/3K5/C1P1p4 w")
        val moves = movesFrom(position, "a0")
        assertTrue("a0e0" in moves, "jump over c0 to capture e0")
        assertTrue("a0b0" in moves)
        assertFalse("a0c0" in moves, "cannot capture own piece")
        assertFalse("a0d0" in moves, "cannot stop behind the screen")
    }

    @Test
    fun `soldier moves sideways only after crossing the river`() {
        val home = fen("5k3/9/9/9/9/9/4P4/9/9/3K5 w")
        assertEquals(setOf("e3e4"), movesFrom(home, "e3"))

        val crossed = fen("5k3/9/9/9/4P4/9/9/9/9/3K5 w")
        assertEquals(setOf("e5e6", "e5d5", "e5f5"), movesFrom(crossed, "e5"))
    }

    @Test
    fun `general stays inside the palace`() {
        val position = fen("3k5/9/9/9/9/9/9/9/9/5K3 w")
        assertEquals(setOf("f0e0", "f0f1"), movesFrom(position, "f0"))
    }

    @Test
    fun `a piece shielding the generals from facing each other cannot leave the file`() {
        val position = fen("4k4/9/9/9/4R4/9/9/9/9/4K4 w")
        val moves = movesFrom(position, "e5")
        assertFalse("e5a5" in moves, "moving sideways would let the generals face each other")
        assertTrue("e5e8" in moves, "moving along the file keeps the shield")
    }

    @Test
    fun `only moves that get the general out of check are legal`() {
        // Xe đen e9 chiếu tướng đỏ e0. Tướng không được sang d0 vì sẽ đối mặt tướng đen ở d9.
        val position = fen("3kr4/9/9/9/9/9/9/9/R8/4K4 w")
        val legal = MoveGenerator.legalMoves(position).map { it.toString() }.toSet()
        assertEquals(setOf("a1e1", "e0f0"), legal)
        assertFalse(MoveGenerator.isLegal(position, move("a1a5")))
    }

    private fun movesFrom(position: Position, from: String): Set<String> =
        MoveGenerator.legalMovesFrom(position, square(from)).map { it.toString() }.toSet()

    private fun perft(position: Position, depth: Int): Long {
        if (depth == 0) return 1
        return MoveGenerator.legalMoves(position).sumOf { perft(position.after(it), depth - 1) }
    }
}
