package com.reflex1337.reflexmirror.entities

data class SearchData(
    val head: String,
    val searchResult: List<SearchResult>,
    val type: Int
)
