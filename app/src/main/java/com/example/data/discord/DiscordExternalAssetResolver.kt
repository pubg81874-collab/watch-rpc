package com.example.data.discord

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves external HTTP(S) image URLs to Discord's `mp:external/...` media proxy asset format
 * via Discord's `/applications/{appId}/external-assets` endpoint so posters and Play/Pause badges
 * render reliably inside Discord Rich Presence cards.
 */
class DiscordExternalAssetResolver(
    private val httpClient: OkHttpClient,
) {
    private val cache = ConcurrentHashMap<String, String>()

    suspend fun resolveAsset(
        token: String,
        applicationId: String,
        rawUrl: String,
    ): String? = withContext(Dispatchers.IO) {
        val clean = rawUrl.trim()
        if (clean.isEmpty()) return@withContext null
        if (clean.startsWith("mp:") || !clean.startsWith("http", ignoreCase = true)) {
            return@withContext clean
        }

        val cacheKey = "$applicationId|$clean"
        cache[cacheKey]?.let { return@withContext it }

        runCatching {
            val payload = JSONObject().apply {
                put("urls", JSONArray().put(clean))
            }
            val request = Request.Builder()
                .url("https://discord.com/api/v9/applications/$applicationId/external-assets")
                .header("Authorization", token.trim())
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@runCatching null
                }
                val array = JSONArray(body)
                val first = array.optJSONObject(0) ?: return@runCatching null
                val externalPath = first.optString("external_asset_path").takeIf { it.isNotBlank() }
                    ?: return@runCatching null
                val resolved = if (externalPath.startsWith("mp:")) externalPath else "mp:$externalPath"
                cache[cacheKey] = resolved
                resolved
            }
        }.getOrNull()
    }
}
