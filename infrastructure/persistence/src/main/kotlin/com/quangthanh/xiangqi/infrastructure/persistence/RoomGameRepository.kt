package com.quangthanh.xiangqi.infrastructure.persistence

import android.content.Context
import android.util.Log
import com.quangthanh.xiangqi.domain.model.Game
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.repository.GameRepository
import com.quangthanh.xiangqi.infrastructure.persistence.dao.GameDao
import com.quangthanh.xiangqi.infrastructure.persistence.entity.GameWithMoves
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dựng lại ván cờ nghĩa là đi lại và kiểm tra luật từng nước, tốn CPU,
 * nên phần chuyển đổi chạy trên [mappingDispatcher] thay vì luồng giao diện.
 */
class RoomGameRepository(
    private val dao: GameDao,
    private val mappingDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : GameRepository {

    override suspend fun save(game: Game) {
        dao.save(GameMapper.toEntity(game), GameMapper.toMoveEntities(game))
    }

    override suspend fun findById(id: GameId): Game? =
        dao.findById(id.value)?.let { withContext(mappingDispatcher) { GameMapper.toDomain(it) } }

    override suspend fun findLatestUnfinished(): Game? =
        dao.findLatestUnfinished()?.let { withContext(mappingDispatcher) { restoreOrNull(it) } }

    /** Một ván bị hỏng dữ liệu không được làm hỏng cả màn hình lịch sử, nên bỏ qua và ghi log. */
    override suspend fun findAll(): List<Game> {
        val records = dao.findAll()
        return withContext(mappingDispatcher) { records.mapNotNull(::restoreOrNull) }
    }

    private fun restoreOrNull(record: GameWithMoves): Game? =
        runCatching { GameMapper.toDomain(record) }
            .onFailure { Log.w(TAG, "Skipping corrupted game ${record.game.id}", it) }
            .getOrNull()

    companion object {
        private const val TAG = "RoomGameRepository"

        /** Tầng ngoài chỉ nhận về [GameRepository]; Room là chi tiết nằm gọn trong module này. */
        fun create(context: Context): GameRepository = RoomGameRepository(ChessDatabase.create(context).gameDao())
    }
}
