// use an integer for version numbers
version = 1


cloudstream {
    description = "Watch trending movies and TV shows online in one fast, modern streaming hub with smart search, watchlists, and seamless playback."
    authors = listOf("Reflex1337")

    /**
    * Status int as the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta only
    * */
    status = 1 // will be 3 if unspecified

    tvTypes = listOf(
        "Movie",
        "TvSeries",
    )
    language = "en"
    iconUrl = "https://bingebox.to/favicon.svg?__WB_REVISION__=f341fd6cbda5b20b61e162c2ec35ae39"

    isCrossPlatform = false
}
