package com.quangthanh.xiangqi.infrastructure.ai

import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Position
import com.quangthanh.xiangqi.domain.service.ComputerPlayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Cài đặt port [ComputerPlayer]. Việc tính toán chạy trên [dispatcher] (mặc định là luồng nền
 * dành cho tác vụ nặng CPU) nên không làm đơ giao diện, và dừng ngay khi coroutine bị hủy.
 */
class AlphaBetaComputerPlayer(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val random: Random = Random.Default,
) : ComputerPlayer {

    override suspend fun chooseMove(position: Position, difficulty: Difficulty): Move = withContext(dispatcher) {
        val job = coroutineContext.job
        val searcher = Searcher(EngineBoard.from(position), SearchSettings.of(difficulty), random, isCancelled = { !job.isActive })
        val move = searcher.search()
        ensureActive()
        check(move != 0) { "No legal move in position" }
        EngineBoard.toDomainMove(move)
    }
}
