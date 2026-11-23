package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RpcPresetDao {
    @Query("SELECT * FROM rpc_presets ORDER BY updatedAtMs DESC")
    fun observeAllPresets(): Flow<List<RpcPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: RpcPresetEntity): Long

    @Query("DELETE FROM rpc_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)
}
