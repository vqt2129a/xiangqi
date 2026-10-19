package com.quangthanh.xiangqi.infrastructure.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.quangthanh.xiangqi.infrastructure.persistence.entity.GameEntity
import com.quangthanh.xiangqi.infrastructure.persistence.entity.GameWithMoves
import com.quangthanh.xiangqi.infrastructure.persistence.entity.MoveEntity

@Dao
interface GameDao {

    /** Ghi ván cờ và toàn bộ nước đi trong một transaction, để không bao giờ lưu dở dang. */
    @Transaction
    suspend fun save(game: GameEntity, moves: List<MoveEntity>) {
        upsertGame(game)
        deleteMoves(game.id)
        insertMoves(moves)
    }

    @Upsert
    suspend fun upsertGame(game: GameEntity)

    @Query("DELETE FROM moves WHERE gameId = :gameId")
    suspend fun deleteMoves(gameId: String)

    @Insert
    suspend fun insertMoves(moves: List<MoveEntity>)

    @Transaction
    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun findById(id: String): GameWithMoves?

    @Transaction
    @Query("SELECT * FROM games WHERE finished = 0 ORDER BY startedAt DESC LIMIT 1")
    suspend fun findLatestUnfinished(): GameWithMoves?

    @Transaction
    @Query("SELECT * FROM games ORDER BY startedAt DESC")
    suspend fun findAll(): List<GameWithMoves>
}
