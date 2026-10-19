package com.quangthanh.xiangqi.application.usecase

import com.quangthanh.xiangqi.application.dto.GameState
import com.quangthanh.xiangqi.application.exception.GameNotFoundException
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.repository.GameRepository

class OpenGameUseCase(private val repository: GameRepository) {

    /** Mở một ván theo id (ví dụ từ màn hình lịch sử). */
    suspend operator fun invoke(gameId: GameId): GameState =
        (repository.findById(gameId) ?: throw GameNotFoundException(gameId)).toState()

    /** Ván đang chơi dở gần nhất để "Chơi tiếp", hoặc null nếu không có. */
    suspend fun latestUnfinished(): GameState? = repository.findLatestUnfinished()?.toState()
}
