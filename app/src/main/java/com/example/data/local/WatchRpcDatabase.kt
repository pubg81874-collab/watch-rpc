package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RpcPresetEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WatchRpcDatabase : RoomDatabase() {
    abstract fun rpcPresetDao(): RpcPresetDao

    companion object {
        @Volatile
        private var instance: WatchRpcDatabase? = null

        fun getInstance(context: Context): WatchRpcDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WatchRpcDatabase::class.java,
                    "watch_rpc.db",
                ).build().also { instance = it }
            }
    }
}
