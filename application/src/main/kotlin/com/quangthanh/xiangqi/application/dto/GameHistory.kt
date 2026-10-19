package com.quangthanh.xiangqi.application.dto

import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.GameResult
import com.quangthanh.xiangqi.domain.model.Participants
import java.time.Instant

enum class Outcome { WIN, LOSS, DRAW }

data class GameSummary(
    val gameId: GameId,
    val startedAt: Instant,
    val participants: Participants,
    val moveCount: Int,
    /** null khi ván còn đang chơi. */
    val result: GameResult?,
    /** Thắng/thua nhìn từ phía người chơi; null với ván 2 người hoặc ván chưa xong. */
    val outcomeForHuman: Outcome?,
)

data class Record(val wins: Int, val losses: Int, val draws: Int) {
    val played: Int get() = wins + losses + draws
}

data class GameHistory(
    val games: List<GameSummary>,
    /** Thành tích khi đánh với máy, theo từng mức độ khó. */
    val recordByDifficulty: Map<Difficulty, Record>,
)
