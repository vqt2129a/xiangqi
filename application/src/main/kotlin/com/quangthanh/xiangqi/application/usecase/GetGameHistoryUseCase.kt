package com.quangthanh.xiangqi.application.usecase

import com.quangthanh.xiangqi.application.dto.GameHistory
import com.quangthanh.xiangqi.application.dto.GameSummary
import com.quangthanh.xiangqi.application.dto.Outcome
import com.quangthanh.xiangqi.application.dto.Record
import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.Game
import com.quangthanh.xiangqi.domain.model.GameResult
import com.quangthanh.xiangqi.domain.model.GameStatus
import com.quangthanh.xiangqi.domain.repository.GameRepository

/** Lịch sử ván đấu và thành tích thắng/thua với máy theo từng mức độ khó. */
class GetGameHistoryUseCase(private val repository: GameRepository) {

    suspend operator fun invoke(): GameHistory {
        val summaries = repository.findAll()
            .sortedByDescending { it.startedAt }
            .map { it.toSummary() }

        val record = Difficulty.entries.associateWith { difficulty ->
            val outcomes = summaries
                .filter { it.participants.computer?.difficulty == difficulty }
                .mapNotNull { it.outcomeForHuman }
            Record(
                wins = outcomes.count { it == Outcome.WIN },
                losses = outcomes.count { it == Outcome.LOSS },
                draws = outcomes.count { it == Outcome.DRAW },
            )
        }
        return GameHistory(summaries, record)
    }

    private fun Game.toSummary(): GameSummary {
        val result = (status as? GameStatus.Finished)?.result
        val humanSide = participants.humanSideAgainstComputer
        val outcome = when {
            result == null || humanSide == null -> null
            result is GameResult.Draw -> Outcome.DRAW
            result is GameResult.Win && result.winner == humanSide -> Outcome.WIN
            else -> Outcome.LOSS
        }
        return GameSummary(id, startedAt, participants, moves.size, result, outcome)
    }
}
