package com.quangthanh.xiangqi.application.usecase

import com.quangthanh.xiangqi.application.port.CloudGameStore
import com.quangthanh.xiangqi.domain.repository.GameRepository

data class SyncResult(val uploaded: Int, val restored: Int)

/**
 * Hợp nhất lịch sử giữa máy và cloud: ván đã xong chỉ có trong máy thì tải lên,
 * ván chỉ có trên cloud (chơi ở máy khác hoặc trước khi cài lại app) thì khôi phục về máy.
 * Không ván nào bị xóa, nên đồng bộ bao nhiêu lần cũng an toàn.
 */
class SyncCloudGamesUseCase(
    private val local: GameRepository,
    private val cloud: CloudGameStore,
) {
    suspend operator fun invoke(): SyncResult {
        val localGames = local.findAll()
        val cloudGames = cloud.downloadFinishedGames()
        val localIds = localGames.map { it.id }.toSet()
        val cloudIds = cloudGames.map { it.id }.toSet()

        val toUpload = localGames.filter { it.isFinished && it.id !in cloudIds }
        toUpload.forEach { cloud.upload(it) }

        val toRestore = cloudGames.filter { it.id !in localIds }
        toRestore.forEach { local.save(it) }

        return SyncResult(uploaded = toUpload.size, restored = toRestore.size)
    }
}
