package com.reflex1337.reflexmirror.entities

data class PlayListItem(
    val image: String?,
    val image2: String?,
    val sources: List<Source>,
    val tracks: List<Tracks>?,
    val title: String
)
