package com.quangthanh.xiangqi.infrastructure.persistence.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val redPlayer: String,
    val blackPlayer: String,
    /** Thế cờ ban đầu dạng FEN, để sau này hỗ trợ ván bắt đầu từ thế cờ tàn. */
    val startingFen: String,
    val resignedBy: String?,
    /** Lưu thêm để truy vấn "ván đang dở" mà không phải dựng lại từng ván. */
    val finished: Boolean,
    val startedAt: Long,
)

/** Chỉ lưu nước đi; trạng thái bàn cờ được tính lại bằng cách đi lại từ đầu khi tải. */
@Entity(
    tableName = "moves",
    primaryKeys = ["gameId", "ply"],
    foreignKeys = [
        ForeignKey(entity = GameEntity::class, parentColumns = ["id"], childColumns = ["gameId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class MoveEntity(
    val gameId: String,
    val ply: Int,
    /** Ký hiệu ICCS, ví dụ "h2e2". */
    val move: String,
)

data class GameWithMoves(
    @Embedded val game: GameEntity,
    @Relation(parentColumn = "id", entityColumn = "gameId")
    val moves: List<MoveEntity>,
)
