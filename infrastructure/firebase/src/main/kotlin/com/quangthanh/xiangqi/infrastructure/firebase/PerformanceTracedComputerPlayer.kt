package com.quangthanh.xiangqi.infrastructure.firebase

import com.google.firebase.perf.FirebasePerformance
import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Position
import com.quangthanh.xiangqi.domain.service.ComputerPlayer
import kotlinx.coroutines.CancellationException

/**
 * Decorator: bọc một [ComputerPlayer] bất kỳ để đo thời gian máy nghĩ bằng Firebase Performance.
 * Engine AI không biết mình đang bị đo; muốn bỏ đo thì chỉ cần không bọc ở composition root.
 * Trên Firebase Console, mục Performance > Custom traces sẽ thấy trace "ai_choose_move" theo từng độ khó.
 */
class PerformanceTracedComputerPlayer(private val delegate: ComputerPlayer) : ComputerPlayer {

    override suspend fun chooseMove(position: Position, difficulty: Difficulty): Move {
        val trace = FirebasePerformance.getInstance().newTrace(TRACE_NAME)
        trace.putAttribute(ATTR_DIFFICULTY, difficulty.name)
        trace.start()
        try {
            return delegate.chooseMove(position, difficulty).also { trace.putAttribute(ATTR_OUTCOME, "moved") }
        } catch (e: CancellationException) {
            // Người chơi đi lại hoặc thoát ván trong lúc máy nghĩ: vẫn ghi lại để không làm lệch số liệu.
            trace.putAttribute(ATTR_OUTCOME, "cancelled")
            throw e
        } finally {
            trace.stop()
        }
    }

    private companion object {
        const val TRACE_NAME = "ai_choose_move"
        const val ATTR_DIFFICULTY = "difficulty"
        const val ATTR_OUTCOME = "outcome"
    }
}
