package com.quangthanh.xiangqi.application.usecase

import com.quangthanh.xiangqi.application.dto.GameState
import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.Game
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.Participants
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.domain.repository.GameRepository
import java.time.Clock

sealed interface GameMode {
    data object TwoPlayers : GameMode
    data class AgainstComputer(val humanSide: Side, val difficulty: Difficulty) : GameMode
}

/**
 * Bắt đầu ván mới. Ứng dụng chỉ cho một ván dở dang tại một thời điểm: ván đang chơi dở
 * bị xử thua cho bên bỏ cuộc (bên người khi đánh với máy, bên đang tới lượt khi 2 người chơi).
 */
class StartNewGameUseCase(
    private val repository: GameRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(mode: GameMode): GameState {
        repository.findLatestUnfinished()?.let { abandoned ->
            abandoned.resign(abandoned.participants.humanSideAgainstComputer ?: abandoned.sideToMove)
            repository.save(abandoned)
        }

        val participants = when (mode) {
            GameMode.TwoPlayers -> Participants.twoPlayers()
            is GameMode.AgainstComputer -> Participants.againstComputer(mode.humanSide, mode.difficulty)
        }
        val game = Game.start(GameId.generate(), participants, clock.instant())
        repository.save(game)
        return game.toState()
    }
}
