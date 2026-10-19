package com.quangthanh.xiangqi.application.fakes

import com.quangthanh.xiangqi.domain.model.Game
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Participants
import com.quangthanh.xiangqi.domain.model.Position
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.domain.repository.GameRepository
import java.time.Instant

/** Lưu ván cờ giống cách DB thật làm: chỉ giữ dữ liệu, mỗi lần tải là dựng lại aggregate mới. */
class InMemoryGameRepository : GameRepository {

    private data class Stored(
        val id: GameId,
        val participants: Participants,
        val startedAt: Instant,
        val startingPosition: Position,
        val moves: List<Move>,
        val resignedBy: Side?,
        val finished: Boolean,
    )

    private val games = linkedMapOf<GameId, Stored>()

    override suspend fun save(game: Game) {
        games[game.id] = Stored(
            game.id, game.participants, game.startedAt, game.startingPosition,
            game.moves.map { it.move }, game.resignedBy, game.isFinished,
        )
    }

    override suspend fun findById(id: GameId): Game? = games[id]?.toGame()

    override suspend fun findLatestUnfinished(): Game? =
        games.values.filterNot { it.finished }.maxByOrNull { it.startedAt }?.toGame()

    override suspend fun findAll(): List<Game> = games.values.map { it.toGame() }

    private fun Stored.toGame() = Game.restore(id, participants, startedAt, startingPosition, moves, resignedBy)
}
