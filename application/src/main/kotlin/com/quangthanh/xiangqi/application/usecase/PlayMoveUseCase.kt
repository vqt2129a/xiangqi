package com.quangthanh.xiangqi.application.usecase

import com.quangthanh.xiangqi.application.dto.GameUpdate
import com.quangthanh.xiangqi.application.exception.GameNotFoundException
import com.quangthanh.xiangqi.application.exception.NotYourTurnException
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Player
import com.quangthanh.xiangqi.domain.model.Square
import com.quangthanh.xiangqi.domain.repository.GameRepository

/** Người chơi đi một nước. Luật cờ do aggregate [com.quangthanh.xiangqi.domain.model.Game] kiểm tra. */
class PlayMoveUseCase(private val repository: GameRepository) {

    suspend operator fun invoke(gameId: GameId, from: Square, to: Square): GameUpdate {
        val game = repository.findById(gameId) ?: throw GameNotFoundException(gameId)
        if (game.playerToMove !is Player.Human) throw NotYourTurnException(gameId)

        game.play(Move(from, to))
        repository.save(game)
        return game.toUpdate()
    }
}
