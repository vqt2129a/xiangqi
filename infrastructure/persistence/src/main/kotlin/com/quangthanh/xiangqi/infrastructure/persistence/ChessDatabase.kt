package com.quangthanh.xiangqi.infrastructure.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.quangthanh.xiangqi.infrastructure.persistence.dao.GameDao
import com.quangthanh.xiangqi.infrastructure.persistence.entity.GameEntity
import com.quangthanh.xiangqi.infrastructure.persistence.entity.MoveEntity

@Database(entities = [GameEntity::class, MoveEntity::class], version = 1, exportSchema = true)
abstract class ChessDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        fun create(context: Context): ChessDatabase =
            Room.databaseBuilder(context.applicationContext, ChessDatabase::class.java, "xiangqi.db").build()
    }
}
