package com.example.data.local

import com.example.model.WatchPresenceConfig
import kotlinx.coroutines.flow.Flow

class RpcPresetRepository(
    private val dao: RpcPresetDao,
) {
    val presets: Flow<List<RpcPresetEntity>> = dao.observeAllPresets()

    suspend fun savePreset(label: String, config: WatchPresenceConfig): Long =
        dao.insertPreset(RpcPresetEntity.fromConfig(label, config))

    suspend fun deletePreset(id: Long) {
        dao.deletePresetById(id)
    }
}
