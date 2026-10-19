package com.quangthanh.xiangqi.domain.notation

import com.quangthanh.xiangqi.domain.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FenTest {

    @Test
    fun `initial FEN describes the initial position`() {
        assertEquals(Position.INITIAL, Fen.parse(Fen.INITIAL))
        assertEquals(Fen.INITIAL, Fen.format(Position.INITIAL))
    }

    @Test
    fun `format and parse round trip`() {
        val fen = "2bak4/4a4/4b4/p3C3p/2p6/6P2/P3n3P/4B4/4A4/3AK1B2 b"
        assertEquals(fen, Fen.format(Fen.parse(fen)))
    }

    @Test
    fun `a position without both generals is rejected`() {
        assertFailsWith<IllegalArgumentException> { Fen.parse("9/9/9/9/9/9/9/9/9/4K4 w") }
    }
}
