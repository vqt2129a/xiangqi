package com.quangthanh.xiangqi.application.usecase

import com.quangthanh.xiangqi.application.dto.GameUpdate
import com.quangthanh.xiangqi.application.exception.GameChangedException
import com.quangthanh.xiangqi.application.exception.GameNotFoundException
import com.quangthanh.xiangqi.application.exception.NotYourTurnException
import com.quangthanh.xiangqi.domain.exception.GameAlreadyFinishedException
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.Player
import com.quangthanh.xiangqi.domain.repository.GameRepository
import com.quangthanh.xiangqi.domain.service.ComputerPlayer

/**
 * Máy đi một nước. Máy có thể nghĩ vài giây, nên trước khi lưu phải kiểm tra lại
 * ván cờ không bị thay đổi trong lúc đó (optimistic concurrency).
 */
class PlayComputerMoveUseCase(
    private val repository: GameRepository,
    private val computerPlayer: ComputerPlayer,
) {
    suspend operator fun invoke(gameId: GameId): GameUpdate {
        val game = repository.findById(gameId) ?: throw GameNotFoundException(gameId)
        if (game.isFinished) throw GameAlreadyFinishedException(gameId)
        val computer = game.playerToMove as? Player.Computer ?: throw NotYourTurnException(gameId)

        val move = computerPlayer.chooseMove(game.position, computer.difficulty)

        val latest = repository.findById(gameId) ?: throw GameNotFoundException(gameId)
        if (latest.moves != game.moves || latest.status != game.status) throw GameChangedException(gameId)

        game.play(move)
        repository.save(game)
        return game.toUpdate()
    }
}
