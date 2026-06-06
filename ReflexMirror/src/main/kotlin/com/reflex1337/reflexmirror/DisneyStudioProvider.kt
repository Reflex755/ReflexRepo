package com.reflex1337.reflexmirror

import android.content.Context
import com.reflex1337.reflexmirror.entities.EpisodesData
import com.reflex1337.reflexmirror.entities.PostData
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.APIHolder.unixTime

open class DisneyStudioProvider(
    private val studio: String,
    displayName: String
) : MainAPI() {
    companion object {
        var context: Context? = null
    }
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama
    )
    override var lang = "en"

    override var mainUrl = "https://net52.cc"
    override var name = displayName

    override val hasMainPage = true
    private var cookie_value = ""
    private val headers = mapOf(
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "Accept-Language" to "en-IN,en-US;q=0.9,en;q=0.8",
        "Cache-Control" to "max-age=0",
        "Connection" to "keep-alive",
        "sec-ch-ua" to "\"Not(A:Brand\";v=\"8\", \"Chromium\";v=\"144\", \"Android WebView\";v=\"144\"",
        "sec-ch-ua-mobile" to "?0",
        "sec-ch-ua-platform" to "\"Android\"",
        "Sec-Fetch-Dest" to "document",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "same-origin",
        "Sec-Fetch-User" to "?1",
        "Upgrade-Insecure-Requests" to "1",
        "User-Agent" to "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, Gecko) Version/4.0 Chrome/144.0.7559.132 Safari/537.36 /OS.Gatu v3.0",
        "X-Requested-With" to "XMLHttpRequest"
    )

    private fun buildCookies(): Map<String, String> {
        val cookies = mutableMapOf(
            "t_hash_t" to cookie_value,
            "ott" to "dp",
            "hd" to "on"
        )
        if (studio.isNotEmpty()) {
            cookies["studio"] = studio
        }
        return cookies
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        cookie_value = if (cookie_value.isEmpty()) bypass(mainUrl) else cookie_value
        val document = app.get(
            "$mainUrl/mobile/home?app=1",
            cookies = buildCookies(),
            headers = headers,
            referer = "$mainUrl/mobile/home?app=1",
        ).document
        val items = document.select(".tray-container, #top10").map {
            it.toHomePageList()
        }
        return newHomePageResponse(items, false)
    }

    private fun Element.toHomePageList(): HomePageList {
        val name = select("h2, span").text()
        val items = select("article, .top10-post").mapNotNull {
            it.toSearchResult()
        }
        return HomePageList(name, items, isHorizontalImages = false)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val id = selectFirst("a")?.attr("data-post") ?: attr("data-post")

        return newAnimeSearchResponse("", Id(id).toJson()) {
            posterUrl = "https://imgcdn.kim/hs/v/$id.jpg"
            posterHeaders = mapOf("Referer" to "$mainUrl/home")
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        cookie_value = if (cookie_value.isEmpty()) bypass(mainUrl) else cookie_value
        val id = parseJson<Id>(url).id
        val data = app.get(
            "$mainUrl/mobile/hs/post.php?id=$id&t=${APIHolder.unixTime}",
            headers = headers,
            referer = "$mainUrl/home",
            cookies = buildCookies()
        ).parsed<PostData>()

        val episodes = arrayListOf<Episode>()
        val title = data.title
        val castList = data.cast?.split(",")?.map { it.trim() } ?: emptyList()
        val cast = castList.map { ActorData(Actor(it)) }
        val genre = data.genre?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }

        val rating = data.match?.replace("IMDb ", "")
        val parsedRunTime = convertRuntimeToMinutes(data.runtime.toString())

        val suggest = data.suggest?.map { item ->
            newAnimeSearchResponse("", Id(item.id).toJson()) {
                posterUrl = "https://imgcdn.kim/hs/v/${item.id}.jpg"
                posterHeaders = mapOf("Referer" to "$mainUrl/home")
            }
        }

        if (data.episodes.first() == null) {
            episodes.add(newEpisode(LoadData(title, id).toJson()) {
                name = data.title
            })
        } else {
            data.episodes.filterNotNull().forEach { item ->
                episodes.add(newEpisode(LoadData(title, item.id).toJson()) {
                    name = item.t
                    episode = item.ep.replace("E", "").toIntOrNull()
                    season = item.s.replace("S", "").toIntOrNull()
                    posterUrl = "https://imgcdn.kim/hsepimg/150/${item.id}.jpg"
                    runTime = item.time.replace("m", "").toIntOrNull()
                })
            }

            if (data.nextPageShow == 1) {
                episodes.addAll(getEpisodes(title, id, data.nextPageSeason!!, 2))
            }

            data.season?.dropLast(1)?.amap { seasonItem ->
                episodes.addAll(getEpisodes(title, id, seasonItem.id, 1))
            }
        }

        val type = if (data.episodes.first() == null) TvType.Movie else TvType.TvSeries

        return newTvSeriesLoadResponse(title, url, type, episodes) {
            posterUrl = "https://imgcdn.kim/hs/v/$id.jpg"
            backgroundPosterUrl = "https://imgcdn.kim/hs/h/$id.jpg"
            posterHeaders = mapOf("Referer" to "$mainUrl/home")
            plot = data.desc
            year = data.year.toIntOrNull()
            tags = genre
            actors = cast
            score = Score.from10(rating)
            duration = parsedRunTime
            contentRating = data.ua
            recommendations = suggest
        }
    }

    private suspend fun getEpisodes(
        title: String, eid: String, sid: String, page: Int
    ): List<Episode> {
        val episodes = arrayListOf<Episode>()
        var pg = page
        while (true) {
            val data = app.get(
                "$mainUrl/mobile/hs/episodes.php?s=$sid&series=$eid&t=${APIHolder.unixTime}&page=$pg",
                headers = headers,
                referer = "$mainUrl/home",
                cookies = buildCookies()
            ).parsed<EpisodesData>()
            
            data.episodes?.filterNotNull()?.forEach { item ->
                episodes.add(newEpisode(LoadData(title, item.id).toJson()) {
                    name = item.t
                    episode = item.ep.replace("E", "").toIntOrNull()
                    season = item.s.replace("S", "").toIntOrNull()
                    posterUrl = "https://imgcdn.kim/hsepimg/${item.id}.jpg"
                    runTime = item.time.replace("m", "").toIntOrNull()
                })
            }
            if (data.nextPageShow == 0) break
            pg++
        }
        return episodes
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val apiBase = resolveApiUrl()
        val id = parseJson<LoadData>(data).id
        val response = app.get(
            "$apiBase/newtv/player.php?id=$id",
            headers = buildNewTvHeaders("hs", mapOf("Usertoken" to ""))
        ).parsed<NewTvPlayerResponse>()

        if (response.status != "ok" || response.video_link.isNullOrBlank()) return false

        callback.invoke(
            newExtractorLink(name, name, response.video_link, type = ExtractorLinkType.M3U8) {
                referer = response.referer ?: apiBase
            }
        )

        return true
    }
}

data class Id(
    val id: String
)

data class LoadData(
    val title: String,
    val id: String
)