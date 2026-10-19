package com.quangthanh.xiangqi.application.port

import com.quangthanh.xiangqi.domain.model.Game

/** Port kho ván đấu trên cloud của tài khoản đang đăng nhập. Chỉ chứa ván đã kết thúc. */
interface CloudGameStore {
    suspend fun downloadFinishedGames(): List<Game>

    /** Ghi đè theo id ván, nên tải lên lại một ván cũng không tạo bản trùng. */
    suspend fun upload(game: Game)
}
