dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// Use an integer for version numbers
version = 1

cloudstream {
    // All of these properties are optional, you can safely remove any of them.

    description = "Discover and watch the latest movies and TV shows on BingeBox. Your ultimate streaming destination with contact support and box office data."
    authors = listOf("Reflex1337")

    /**
    * Status int as one of the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta-only
    **/
    status = 1 // Will be 3 if unspecified

    tvTypes = listOf("Movie")

    requiresResources = true
    language = "en"

    // Random CC logo I found
    iconUrl = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://bingebox.to/&size=256"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}
