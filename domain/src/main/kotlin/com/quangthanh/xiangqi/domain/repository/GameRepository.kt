package com.quangthanh.xiangqi.domain.repository

import com.quangthanh.xiangqi.domain.model.Game
import com.quangthanh.xiangqi.domain.model.GameId

/** Domain chỉ định nghĩa cần lưu/tải ván cờ thế nào; lưu bằng Room, Firebase hay bộ nhớ là việc của tầng ngoài. */
interface GameRepository {
    suspend fun save(game: Game)

    suspend fun findById(id: GameId): Game?

    suspend fun findLatestUnfinished(): Game?

    /** Tất cả ván cờ, mới nhất trước. */
    suspend fun findAll(): List<Game>
}
