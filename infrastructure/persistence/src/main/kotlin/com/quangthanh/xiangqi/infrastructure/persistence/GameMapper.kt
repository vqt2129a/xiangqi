package com.quangthanh.xiangqi.infrastructure.persistence

import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.Game
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Participants
import com.quangthanh.xiangqi.domain.model.Player
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.domain.notation.Fen
import com.quangthanh.xiangqi.infrastructure.persistence.entity.GameEntity
import com.quangthanh.xiangqi.infrastructure.persistence.entity.GameWithMoves
import com.quangthanh.xiangqi.infrastructure.persistence.entity.MoveEntity
import java.time.Instant

/** Chuyển đổi giữa aggregate của domain và bảng dữ liệu. Domain không biết gì về lớp này. */
internal object GameMapper {

    fun toEntity(game: Game) = GameEntity(
        id = game.id.value,
        redPlayer = encode(game.participants.red),
        blackPlayer = encode(game.participants.black),
        startingFen = Fen.format(game.startingPosition),
        resignedBy = game.resignedBy?.name,
        finished = game.isFinished,
        startedAt = game.startedAt.toEpochMilli(),
    )

    fun toMoveEntities(game: Game): List<MoveEntity> =
        game.moves.mapIndexed { ply, played -> MoveEntity(game.id.value, ply, played.move.toString()) }

    fun toDomain(record: GameWithMoves): Game = with(record.game) {
        Game.restore(
            id = GameId(id),
            participants = Participants(decode(redPlayer), decode(blackPlayer)),
            startedAt = Instant.ofEpochMilli(startedAt),
            startingPosition = Fen.parse(startingFen),
            moves = record.moves.sortedBy { it.ply }.map { Move.parse(it.move) },
            resignedBy = resignedBy?.let(Side::valueOf),
        )
    }

    private fun encode(player: Player): String = when (player) {
        Player.Human -> HUMAN
        is Player.Computer -> COMPUTER_PREFIX + player.difficulty.name
    }

    private fun decode(value: String): Player = when {
        value == HUMAN -> Player.Human
        value.startsWith(COMPUTER_PREFIX) -> Player.Computer(Difficulty.valueOf(value.removePrefix(COMPUTER_PREFIX)))
        else -> throw IllegalStateException("Unknown player: $value")
    }

    private const val HUMAN = "HUMAN"
    private const val COMPUTER_PREFIX = "COMPUTER_"
}
