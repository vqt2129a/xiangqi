package com.quangthanh.xiangqi.application.usecase

import com.quangthanh.xiangqi.application.dto.GameUpdate
import com.quangthanh.xiangqi.application.exception.GameNotFoundException
import com.quangthanh.xiangqi.domain.exception.NothingToUndoException
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.repository.GameRepository

/** Đi lại: số nước bị lùi tùy chế độ chơi, xem [UndoPolicy]. */
class UndoMoveUseCase(private val repository: GameRepository) {

    suspend operator fun invoke(gameId: GameId): GameUpdate {
        val game = repository.findById(gameId) ?: throw GameNotFoundException(gameId)
        val plies = UndoPolicy.pliesToUndo(game)
        if (plies == 0) throw NothingToUndoException(gameId)

        repeat(plies) { game.undoLastMove() }
        repository.save(game)
        return game.toUpdate()
    }
}
