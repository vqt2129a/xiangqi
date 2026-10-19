package com.quangthanh.xiangqi.application.usecase

import com.quangthanh.xiangqi.application.dto.GameUpdate
import com.quangthanh.xiangqi.application.exception.GameNotFoundException
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.repository.GameRepository

/** Đầu hàng: khi đánh với máy thì luôn là người đầu hàng, khi 2 người chơi thì là bên đang tới lượt. */
class ResignUseCase(private val repository: GameRepository) {

    suspend operator fun invoke(gameId: GameId): GameUpdate {
        val game = repository.findById(gameId) ?: throw GameNotFoundException(gameId)
        game.resign(game.participants.humanSideAgainstComputer ?: game.sideToMove)
        repository.save(game)
        return game.toUpdate()
    }
}
