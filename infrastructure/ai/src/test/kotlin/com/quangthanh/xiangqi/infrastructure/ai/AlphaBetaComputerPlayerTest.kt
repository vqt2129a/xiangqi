package com.quangthanh.xiangqi.infrastructure.ai

import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.Position
import com.quangthanh.xiangqi.domain.notation.Fen
import com.quangthanh.xiangqi.domain.rules.MoveGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AlphaBetaComputerPlayerTest {

    private val player = AlphaBetaComputerPlayer(Dispatchers.Default, Random(1))

    @Test
    fun `finds mate in one`() = runBlocking {
        val position = Fen.parse("4k4/R8/9/9/9/9/9/9/9/3K4R w")
        for (difficulty in listOf(Difficulty.MEDIUM, Difficulty.HARD)) {
            assertEquals("i0i9", player.chooseMove(position, difficulty).toString(), "difficulty $difficulty")
        }
    }

    @Test
    fun `wins within two moves when it can`() = runBlocking {
        // Đen chỉ còn tướng d9. Đỏ có hai xe và tướng e0 khóa cột e (lộ mặt tướng).
        val position = Fen.parse("3k5/9/9/9/9/9/9/9/R8/4K3R w")
        val move = player.chooseMove(position, Difficulty.HARD)
        val after = position.after(move)
        // Sau nước của máy, mọi cách đỡ của Đen đều dẫn tới bị chiếu bí ngay nước sau.
        assertTrue(MoveGenerator.legalMoves(after).all { reply -> canMateInOne(after.after(reply)) }, "move $move")
    }

    @Test
    fun `takes a free chariot`() = runBlocking {
        // Xe đen ở e5 không được bảo vệ, pháo đỏ e2 ăn được qua ngòi tốt e3.
        val position = Fen.parse("5k3/9/9/9/4r4/9/4P4/4C4/9/3K5 w")
        assertEquals("e2e5", player.chooseMove(position, Difficulty.MEDIUM).toString())
    }

    @Test
    fun `easy level only plays legal moves`() = runBlocking {
        var position = Position.INITIAL
        repeat(10) {
            val move = player.chooseMove(position, Difficulty.EASY)
            assertTrue(MoveGenerator.isLegal(position, move))
            position = position.after(move)
        }
    }

    @Test
    fun `hard level respects its time budget`() = runBlocking {
        val elapsed = measureTimeMillis { player.chooseMove(Position.INITIAL, Difficulty.HARD) }
        assertTrue(elapsed < 4_000, "took $elapsed ms")
    }

    @Test
    fun `search stops when the coroutine is cancelled`() = runBlocking {
        val thinking = async(Dispatchers.Default) { player.chooseMove(Position.INITIAL, Difficulty.HARD) }
        Thread.sleep(100)
        val elapsed = measureTimeMillis {
            thinking.cancel()
            assertFailsWith<CancellationException> { thinking.await() }
        }
        assertTrue(elapsed < 500, "cancellation took $elapsed ms")
    }

    private fun canMateInOne(position: Position): Boolean =
        MoveGenerator.legalMoves(position).any { MoveGenerator.legalMoves(position.after(it)).isEmpty() }
}
