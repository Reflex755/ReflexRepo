package com.divicast

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class DiviCastTest {
    private val provider = DiviCastProvider()

    @Test
    fun testSearch() = runBlocking {
        val results = provider.search("Batman")
        assertTrue("Search should return results", results.isNotEmpty())
        println("Found ${results.size} search results:")
        results.forEach { println(" - ${it.name} (${it.url}) | Poster: ${it.posterUrl}") }
    }

    @Test
    fun testLoad() = runBlocking {
        val results = provider.search("Batman")
        assertTrue("Search should return results", results.isNotEmpty())
        val firstResult = results.first()
        
        val loadResponse = provider.load(firstResult.url)
        assertTrue("LoadResponse should not be null", loadResponse != null)
        
        when (loadResponse) {
            is MovieLoadResponse -> {
                println("Loaded Movie details:")
                println("  Title: ${loadResponse.name}")
                println("  Plot: ${loadResponse.plot}")
                println("  Poster: ${loadResponse.posterUrl}")
                println("  Data Payload length: ${loadResponse.dataUrl.length}")
            }
            is TvSeriesLoadResponse -> {
                println("Loaded TV Series details:")
                println("  Title: ${loadResponse.name}")
                println("  Plot: ${loadResponse.plot}")
                println("  Poster: ${loadResponse.posterUrl}")
                println("  Episodes count: ${loadResponse.episodes.size}")
                loadResponse.episodes.forEach { ep ->
                    println("    - Season ${ep.season} Episode ${ep.episode}: ${ep.name} (Payload length: ${ep.data.length})")
                }
            }
            else -> println("Loaded other LoadResponse: $loadResponse")
        }
    }

    @Test
    fun testLoadLinks() = runBlocking {
        val results = provider.search("Batman")
        assertTrue("Search should return results", results.isNotEmpty())
        val firstResult = results.first()
        val loadResponse = provider.load(firstResult.url)
        
        val dataUrl = when (loadResponse) {
            is MovieLoadResponse -> loadResponse.dataUrl
            is TvSeriesLoadResponse -> loadResponse.episodes.first().data
            else -> throw Exception("Unknown LoadResponse type")
        }
        
        val links = mutableListOf<ExtractorLink>()
        val subtitles = mutableListOf<com.lagradost.cloudstream3.SubtitleFile>()
        
        println("Resolving links for: ${loadResponse.name}")
        val linkSuccess = provider.loadLinks(dataUrl, false, { sub ->
            println("Extracted Subtitle: ${sub.lang} -> ${sub.url}")
            subtitles.add(sub)
        }, { link ->
            println("Extracted Link: ${link.name} [Quality: ${link.quality}] -> ${link.url}")
            links.add(link)
        })
        
        assertTrue("Link resolution should succeed", linkSuccess)
        assertTrue("Should extract at least one link", links.isNotEmpty())
    }

    @Test
    fun testHomepage() = runBlocking {
        for (section in provider.mainPage.take(3)) {
            println("Testing homepage section: ${section.name} (${section.data})")
            val request = com.lagradost.cloudstream3.MainPageRequest(section.name, section.data, true)
            val response = provider.getMainPage(1, request)
            assertTrue("Homepage response should not be null", response != null)
            assertTrue("Homepage response should have items", response.items.isNotEmpty())
            println("Successfully parsed ${response.items.size} items for ${section.name}")
        }
    }
}
