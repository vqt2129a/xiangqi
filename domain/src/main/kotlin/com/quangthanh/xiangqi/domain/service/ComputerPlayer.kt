package com.quangthanh.xiangqi.domain.service

import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Position

/**
 * Port cho đối thủ máy. Domain chỉ biết "máy chọn một nước đi", còn thuật toán tìm kiếm
 * nằm ở tầng infrastructure và có thể thay bằng engine khác.
 *
 * Hàm suspend để bên cài đặt chạy trên luồng nền và hủy được khi người chơi rời ván.
 */
fun interface ComputerPlayer {
    suspend fun chooseMove(position: Position, difficulty: Difficulty): Move
}
