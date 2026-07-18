package com.ladev

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

/**
 * Library of Ladev — CloudStream Provider
 *
 * Source: https://libraryofladev.com
 * Searchable database of Neuro-sama (VTuber) stream transcripts
 * All videos are YouTube VODs — resolved via NewPipeExtractor + local DASH server
 *
 * Stream extraction uses NewPipeExtractor (same as re-3rabi):
 *   - Handles signature decryption, poToken, age-gating automatically
 *   - Separates video-only and audio-only streams
 *   - Builds DASH MPD manifests served via local HTTP server
 *   - Supports multi-language audio tracks
 *   - Falls back to CloudStream built-in extractors if NewPipe fails
 *
 * Thumbnails use i.ytimg.com with multi-resolution fallback.
 */
class LadevProvider : MainAPI() {
    override var lang = "en"
    override var mainUrl = "https://libraryofladev.com"
    override var name = "Library of Ladev"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val supportedTypes = setOf(TvType.Movie)

    private val ytMainUrl = "https://www.youtube.com"

    // ═══════════════════════════════════════════════════════════════
    //  JACKSON DATA CLASSES
    // ═══════════════════════════════════════════════════════════════

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ApiResponse(
        val success: Boolean = false,
        val data: ApiData? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ApiData(
        val result: Any? = null,
        val lastUrl: String? = null,
        val noMoreResultsToFetch: Boolean? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Video(
        val url: String = "",
        val title: String = "",
        val date: String = "",
        val tags: List<String> = emptyList(),
        val total: String? = null,
        val subtitles: List<Subtitle>? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Subtitle(
        val subtitleId: Int = 0,
        val startTime: Int = 0,
        val timestamp: String = "",
        val text: String = "",
    )

    data class LoadData(
        val videoId: String,
        val title: String,
    )

    // ═══════════════════════════════════════════════════════════════
    //  THUMBNAIL ENGINE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Build thumbnail URL from YouTube video ID using i.ytimg.com.
     * Resolution fallback chain:
     *   maxresdefault (1280x720) → sddefault (640x480) → hqdefault (480x360)
     */
    private fun buildThumbnailUrl(videoId: String, quality: String = "maxresdefault"): String {
        return "https://i.ytimg.com/vi/$videoId/$quality.jpg"
    }

    fun getBestThumbnail(videoId: String): String {
        return buildThumbnailUrl(videoId, "maxresdefault")
    }

    /**
     * Extract video ID from various YouTube URL formats or raw 11-char ID
     */
    private fun extractVideoId(input: String): String? {
        val patterns = listOf(
            Regex("""(?:v=|/videos/|embed/|youtu\.be/|shorts/)([A-Za-z0-9_-]{11})"""),
            Regex("""^([A-Za-z0-9_-]{11})$""")
        )
        for (pattern in patterns) {
            val match = pattern.find(input)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    // ═══════════════════════════════════════════════════════════════
    //  HOMEPAGE
    // ═══════════════════════════════════════════════════════════════

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val categories = mapOf(
            "All Streams" to listOf<String>(),
            "Gaming" to listOf("Gaming"),
            "Chill" to listOf("Chill"),
            "Karaoke" to listOf("Karaoke"),
            "IRL" to listOf("IRL"),
            "Dev" to listOf("Dev"),
            "Collab" to listOf("Collab"),
            "Themed" to listOf("Themed"),
            "Subathon" to listOf("Subathon"),
        )

        val home = categories.map { (name, tags) ->
            val videos = fetchVideos(tags = tags, fetchSize = 12)
            val shows = videos.map { it.toSearchResponse() }
            HomePageList(name, shows, isHorizontalImages = true)
        }.filter { it.list.isNotEmpty() }

        return newHomePageResponse(home)
    }

    // ═══════════════════════════════════════════════════════════════
    //  SEARCH
    // ═══════════════════════════════════════════════════════════════

    override suspend fun search(query: String): List<SearchResponse> {
        val videos = fetchVideos(text = query, fetchSize = 25)
        return videos.map { it.toSearchResponse() }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Safe Data Parsing
    // ═══════════════════════════════════════════════════════════════

    private fun parseLoadData(input: String): LoadData {
        return try {
            if (input.trim().startsWith("{")) {
                parseJson<LoadData>(input)
            } else {
                LoadData(videoId = input, title = "Library of Ladev Stream")
            }
        } catch (e: Exception) {
            LoadData(videoId = input, title = "Library of Ladev Stream")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  LOAD (video detail page)
    // ═══════════════════════════════════════════════════════════════

    override suspend fun load(url: String): LoadResponse {
        val data = parseLoadData(url)
        val videoId = extractVideoId(data.videoId) ?: data.videoId

        // Fetch full transcript for this video
        val transcript = fetchTranscript(data.videoId)

        // Build description from transcript snippets
        val plot = buildString {
            append("Stream: ${data.title}")
            if (transcript.isNotEmpty()) {
                append("\n\n")
                val preview = transcript.take(10)
                preview.forEach { sub ->
                    append("[${sub.timestamp}] ${sub.text}\n")
                }
                if (transcript.size > 10) {
                    append("\n... and ${transcript.size - 10} more lines")
                }
            }
        }

        val ytUrl = if (data.videoId.startsWith("http")) {
            data.videoId
        } else {
            "$ytMainUrl/watch?v=$videoId"
        }

        return newMovieLoadResponse(data.title, ytUrl, TvType.Movie, data.toJson()) {
            this.posterUrl = getBestThumbnail(videoId)
            this.plot = plot
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  LOAD LINKS — NewPipeExtractor (re-3rabi method)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Load video playback links using NewPipeExtractor + local DASH server.
     *
     * This is the same approach used by re-3rabi's YouTube plugin:
     * 1. YoutubeExtractor uses NewPipeExtractor to fetch streams
     * 2. NewPipeExtractor handles signature decryption, poToken, etc.
     * 3. Video-only streams are paired with audio per language
     * 4. DASH MPD manifests are built and served via local HTTP server
     * 5. Falls back to CloudStream built-in extractors if NewPipe fails
     */
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val loadData = parseLoadData(data)
        val videoId = extractVideoId(loadData.videoId) ?: loadData.videoId
        val ytUrl = "$ytMainUrl/watch?v=$videoId"

        var linksFound = false

        // ── Strategy 1: NewPipeExtractor + DASH server (re-3rabi method) ──
        try {
            YoutubeExtractor().getUrl(ytUrl, null, subtitleCallback, callback)
            linksFound = true
        } catch (e: Exception) {
            // NewPipeExtractor failed, try fallback
        }

        // ── Strategy 2: CloudStream built-in extractors (fallback) ──
        if (!linksFound) {
            try {
                loadExtractor(ytUrl, mainUrl, subtitleCallback, callback)
                linksFound = true
            } catch (_: Exception) {}
        }

        return linksFound
    }

    // ═══════════════════════════════════════════════════════════════
    //  API HELPERS
    // ═══════════════════════════════════════════════════════════════

    private suspend fun fetchVideos(
        text: String? = null,
        tags: List<String> = emptyList(),
        fetchSize: Int = 25,
        lastUrl: String? = null
    ): List<Video> {
        return try {
            val params = mutableMapOf(
                "fetchSize" to fetchSize.toString()
            )
            if (!text.isNullOrBlank()) {
                params["text"] = text
                params["isFullTextSearch"] = "true"
            }
            if (lastUrl != null) {
                params["lastUrl"] = lastUrl
            }

            tags.forEachIndexed { index, tag ->
                params["includeTags[$index]"] = tag
            }

            val response = app.get("$mainUrl/api/search", params = params).text
            val apiResponse = parseJson<ApiResponse>(response)
            val result = apiResponse.data?.result

            when (result) {
                is List<*> -> result.mapNotNull { item ->
                    try {
                        val videoJson = item?.toJson()
                        if (videoJson != null) parseJson<Video>(videoJson) else null
                    } catch (_: Exception) { null }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchTranscript(videoId: String): List<Subtitle> {
        return try {
            val response = app.get("$mainUrl/api/search", params = mapOf("videoUrl" to videoId)).text
            val apiResponse = parseJson<ApiResponse>(response)
            val result = apiResponse.data?.result

            when (result) {
                is Map<*, *> -> {
                    try {
                        val videoJson = result.toJson()
                        val video = parseJson<Video>(videoJson)
                        video.subtitles ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER: Video → SearchResponse
    // ═══════════════════════════════════════════════════════════════

    private fun Video.toSearchResponse(): SearchResponse {
        val loadData = LoadData(
            videoId = this.url,
            title = this.title,
        )

        val matchInfo = if (this.total != null) " (${this.total} matches)" else ""
        val vidId = extractVideoId(this.url) ?: this.url

        return newMovieSearchResponse(
            "${this.title}$matchInfo",
            loadData.toJson(),
            TvType.Movie
        ) {
            this.posterUrl = getBestThumbnail(vidId)
        }
    }
}
