package com.quangthanh.xiangqi.infrastructure.persistence

import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.Game
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Participants
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.infrastructure.persistence.entity.GameWithMoves
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class GameMapperTest {

    @Test
    fun `game survives a round trip through the database records`() {
        val game = Game.start(
            GameId("g1"),
            Participants.againstComputer(Side.BLACK, Difficulty.HARD),
            Instant.parse("2026-10-19T08:00:00Z"),
        )
        listOf("h2e2", "h9g7", "h0g2").forEach { game.play(Move.parse(it)) }
        game.resign(Side.BLACK)

        // Room không đảm bảo thứ tự của @Relation, nên đảo ngược để chắc mapper tự sắp xếp.
        val record = GameWithMoves(GameMapper.toEntity(game), GameMapper.toMoveEntities(game).reversed())
        val restored = GameMapper.toDomain(record)

        assertEquals(game.id, restored.id)
        assertEquals(game.participants, restored.participants)
        assertEquals(game.startedAt, restored.startedAt)
        assertEquals(game.moves, restored.moves)
        assertEquals(game.status, restored.status)
        assertEquals(true, record.game.finished)
    }
}
