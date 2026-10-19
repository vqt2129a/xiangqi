package com.quangthanh.xiangqi.infrastructure.firebase

import com.quangthanh.xiangqi.domain.model.Game
import com.quangthanh.xiangqi.domain.repository.GameRepository

/**
 * Decorator offline-first: mọi đọc/ghi đi vào kho trong máy ([local], Room) như cũ,
 * và mỗi khi một ván kết thúc thì gửi thêm một bản sao lên cloud ở nền.
 * Use case chỉ thấy [GameRepository], không biết có cloud phía sau.
 */
class CloudBackupGameRepository(private val local: GameRepository) : GameRepository by local {

    override suspend fun save(game: Game) {
        local.save(game)
        if (game.isFinished) CloudBackup.uploadFinishedGame(game)
    }
}
