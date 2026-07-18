version = 1

cloudstream {
    description = "Watch movies and TV series from DiviCast"
    authors = listOf("Reflex1337","ErrorCode26")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta-only
     **/
    status = 1

    tvTypes = listOf("Movie", "TvSeries")

    language = "en"

    iconUrl = "https://www.google.com/s2/favicons?domain=divicast.study&sz=64"
}

dependencies {
    // Cloudstream core classes are provided by the app runtime.
    // The `cloudstream` configuration provides stubs for compilation only;
    // they are NOT packaged into the final .cs3 dex.
    cloudstream("com.lagradost:cloudstream3:pre-release")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation("org.mozilla:rhino:1.7.15")
    testImplementation("me.xdrop:fuzzywuzzy:1.4.0")
    testImplementation(files("C:/Users/user/.gradle/caches/cloudstream/cloudstream/cloudstream.jar"))
}
