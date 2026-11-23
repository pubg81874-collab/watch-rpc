package com.example

import android.app.Application
import com.example.data.discord.DiscordExternalAssetResolver
import com.example.data.discord.DiscordGatewayClient
import com.example.data.discord.DiscordRpcManager
import com.example.data.local.RpcPreferencesRepository
import com.example.data.local.RpcPresetRepository
import com.example.data.local.WatchRpcDatabase
import com.example.data.network.MediaCatalogRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class WatchRpcApp : Application() {

    lateinit var httpClient: OkHttpClient
        private set

    lateinit var database: WatchRpcDatabase
        private set

    lateinit var presetRepository: RpcPresetRepository
        private set

    lateinit var preferencesRepository: RpcPreferencesRepository
        private set

    lateinit var mediaCatalogRepository: MediaCatalogRepository
        private set

    lateinit var rpcManager: DiscordRpcManager
        private set

    override fun onCreate() {
        super.onCreate()
        httpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // Required for persistent WebSocket streams
            .writeTimeout(15, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        database = WatchRpcDatabase.getInstance(this)
        presetRepository = RpcPresetRepository(database.rpcPresetDao())
        preferencesRepository = RpcPreferencesRepository(this)
        mediaCatalogRepository = MediaCatalogRepository(httpClient)

        val assetResolver = DiscordExternalAssetResolver(httpClient)
        val gatewayClient = DiscordGatewayClient(httpClient, assetResolver)
        rpcManager = DiscordRpcManager(this, gatewayClient, preferencesRepository)
    }
}
