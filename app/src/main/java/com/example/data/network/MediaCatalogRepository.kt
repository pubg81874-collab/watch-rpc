package com.example.data.network

import com.example.model.MediaEpisode
import com.example.model.MediaSearchResult
import com.example.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class MediaCatalogRepository(
    private val httpClient: OkHttpClient,
) {
    suspend fun search(query: String, type: MediaType): Result<List<MediaSearchResult>> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return@withContext Result.success(emptyList())
            runCatching {
                when (type) {
                    MediaType.SERIES -> searchTvMaze(trimmed)
                    MediaType.ANIME -> searchJikanAnime(trimmed)
                    MediaType.MOVIE -> searchItunesMovies(trimmed)
                }
            }
        }

    suspend fun fetchEpisodes(show: MediaSearchResult): Result<List<MediaEpisode>> =
        withContext(Dispatchers.IO) {
            runCatching {
                when (show.mediaType) {
                    MediaType.SERIES -> fetchTvMazeEpisodes(show.externalId)
                    MediaType.ANIME -> fetchJikanEpisodes(show.externalId, show.watchUrl)
                    MediaType.MOVIE -> emptyList()
                }
            }
        }

    private fun searchTvMaze(query: String): List<MediaSearchResult> {
        val url = "https://api.tvmaze.com/search/shows".toHttpUrl()
            .newBuilder()
            .addQueryParameter("q", query)
            .build()
        val body = executeGet(url.toString())
        val array = JSONArray(body)
        val results = mutableListOf<MediaSearchResult>()
        for (i in 0 until array.length()) {
            val entry = array.optJSONObject(i) ?: continue
            val show = entry.optJSONObject("show") ?: continue
            val id = show.optInt("id", -1)
            if (id <= 0) continue
            val name = show.optString("name").orEmpty()
            if (name.isBlank()) continue
            val premiered = show.optString("premiered").take(4)
            val runtime = show.optInt("averageRuntime", show.optInt("runtime", 0))
                .takeIf { it > 0 }
            val imageObj = show.optJSONObject("image")
            val poster = imageObj?.optString("original")?.takeIf { it.isNotBlank() }
                ?: imageObj?.optString("medium").orEmpty()
            val summary = stripHtml(show.optString("summary"))
            val showUrl = show.optString("url").orEmpty()
            val ratingVal = show.optJSONObject("rating")?.optDouble("average")
                ?.takeIf { !it.isNaN() && it > 0.0 }
                ?.let { String.format(java.util.Locale.US, "%.1f", it) }

            results += MediaSearchResult(
                externalId = id.toString(),
                mediaType = MediaType.SERIES,
                title = name,
                year = premiered,
                runtimeMinutes = runtime,
                synopsis = summary,
                posterUrl = poster,
                watchUrl = showUrl,
                rating = ratingVal,
            )
        }
        return results
    }

    private fun fetchTvMazeEpisodes(showId: String): List<MediaEpisode> {
        val body = executeGet("https://api.tvmaze.com/shows/$showId/episodes")
        val array = JSONArray(body)
        val episodes = mutableListOf<MediaEpisode>()
        for (i in 0 until array.length()) {
            val ep = array.optJSONObject(i) ?: continue
            val season = ep.optInt("season", 1)
            val number = ep.optInt("number", 0)
            if (number <= 0) continue
            val name = ep.optString("name").ifBlank { "Episode $number" }
            val summary = stripHtml(ep.optString("summary"))
            val runtime = ep.optInt("runtime", 0).takeIf { it > 0 }
            val epUrl = ep.optString("url").orEmpty()
            val still = ep.optJSONObject("image")?.optString("original")
                ?.takeIf { it.isNotBlank() }
                ?: ep.optJSONObject("image")?.optString("medium")?.takeIf { it.isNotBlank() }

            episodes += MediaEpisode(
                id = ep.optInt("id", i).toString(),
                season = season,
                episodeNumber = number,
                title = name,
                synopsis = summary,
                runtimeMinutes = runtime,
                episodeUrl = epUrl,
                stillUrl = still,
            )
        }
        return episodes
    }

    private fun searchJikanAnime(query: String): List<MediaSearchResult> {
        val url = "https://api.jikan.moe/v4/anime".toHttpUrl()
            .newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("limit", "15")
            .addQueryParameter("sfw", "true")
            .build()
        val body = executeGet(url.toString())
        val root = JSONObject(body)
        val data = root.optJSONArray("data") ?: return emptyList()
        val results = mutableListOf<MediaSearchResult>()
        for (i in 0 until data.length()) {
            val anime = data.optJSONObject(i) ?: continue
            val malId = anime.optInt("mal_id", -1)
            if (malId <= 0) continue
            val title = anime.optString("title_english")
                .takeIf { it.isNotBlank() && it != "null" }
                ?: anime.optString("title")
            if (title.isBlank()) continue
            val yearInt = anime.optInt("year", 0)
            val airedFrom = anime.optJSONObject("aired")?.optString("from")?.take(4).orEmpty()
            val year = if (yearInt > 0) yearInt.toString() else airedFrom
            val durationStr = anime.optString("duration").orEmpty()
            val runtime = Regex("(\\d+)\\s*min").find(durationStr)?.groupValues?.getOrNull(1)?.toIntOrNull()
            val images = anime.optJSONObject("images")?.optJSONObject("jpg")
            val poster = images?.optString("large_image_url")?.takeIf { it.isNotBlank() }
                ?: images?.optString("image_url").orEmpty()
            val synopsis = anime.optString("synopsis").takeIf { it != "null" }.orEmpty()
            val malUrl = anime.optString("url").orEmpty()
            val score = anime.optDouble("score")
                .takeIf { !it.isNaN() && it > 0.0 }
                ?.let { String.format(java.util.Locale.US, "%.1f", it) }

            results += MediaSearchResult(
                externalId = malId.toString(),
                mediaType = MediaType.ANIME,
                title = title,
                year = year,
                runtimeMinutes = runtime,
                synopsis = synopsis,
                posterUrl = poster,
                watchUrl = malUrl,
                rating = score,
            )
        }
        return results
    }

    private fun fetchJikanEpisodes(malId: String, seriesUrl: String): List<MediaEpisode> {
        val body = executeGet("https://api.jikan.moe/v4/anime/$malId/episodes")
        val root = JSONObject(body)
        val data = root.optJSONArray("data") ?: return emptyList()
        val episodes = mutableListOf<MediaEpisode>()
        for (i in 0 until data.length()) {
            val ep = data.optJSONObject(i) ?: continue
            val epNum = ep.optInt("mal_id", i + 1)
            val title = ep.optString("title").takeIf { it.isNotBlank() && it != "null" }
                ?: "Episode $epNum"
            val epUrl = ep.optString("url").takeIf { it.isNotBlank() && it != "null" }
                ?: "$seriesUrl/episode/$epNum"
            val aired = ep.optString("aired").take(10).takeIf { it.isNotBlank() && it != "null" }
            val fallbackDesc = if (aired != null) "Aired $aired • Episode $epNum" else "Episode $epNum"

            episodes += MediaEpisode(
                id = epNum.toString(),
                season = 1,
                episodeNumber = epNum,
                title = title,
                synopsis = fallbackDesc,
                runtimeMinutes = 24,
                episodeUrl = epUrl,
            )
        }
        return episodes
    }

    private fun searchItunesMovies(query: String): List<MediaSearchResult> {
        val url = "https://itunes.apple.com/search".toHttpUrl()
            .newBuilder()
            .addQueryParameter("media", "movie")
            .addQueryParameter("entity", "movie")
            .addQueryParameter("limit", "20")
            .addQueryParameter("term", query)
            .build()
        val body = executeGet(url.toString())
        val root = JSONObject(body)
        val array = root.optJSONArray("results") ?: return emptyList()
        val results = mutableListOf<MediaSearchResult>()
        for (i in 0 until array.length()) {
            val movie = array.optJSONObject(i) ?: continue
            val trackId = movie.optLong("trackId", -1L)
            val title = movie.optString("trackName").orEmpty()
            if (title.isBlank()) continue
            val year = movie.optString("releaseDate").take(4)
            val runtimeMs = movie.optLong("trackTimeMillis", 0L)
            val runtimeMinutes = if (runtimeMs > 0L) (runtimeMs / 60_000L).toInt().coerceAtLeast(1) else null
            val rawArt = movie.optString("artworkUrl100").orEmpty()
            val poster = rawArt.replace("100x100bb", "600x600bb")
            val synopsis = movie.optString("shortDescription")
                .takeIf { it.isNotBlank() }
                ?: movie.optString("longDescription").orEmpty()
            val watchUrl = movie.optString("trackViewUrl").orEmpty()
            val rating = movie.optString("contentAdvisoryRating").takeIf { it.isNotBlank() }

            results += MediaSearchResult(
                externalId = trackId.toString(),
                mediaType = MediaType.MOVIE,
                title = title,
                year = year,
                runtimeMinutes = runtimeMinutes,
                synopsis = synopsis,
                posterUrl = poster,
                watchUrl = watchUrl,
                rating = rating,
            )
        }
        return results
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "WatchRPC-Android/1.0")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Catalog request failed (HTTP ${response.code})")
            }
            return response.body?.string() ?: throw IllegalStateException("Empty response body")
        }
    }

    private fun stripHtml(raw: String?): String {
        if (raw.isNullOrBlank() || raw == "null") return ""
        return raw
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
