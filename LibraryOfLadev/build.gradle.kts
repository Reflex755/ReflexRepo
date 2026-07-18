version = 3

cloudstream {
    language = "en"
    description = "Library of Ladev - Neuro-sama stream transcript search with YouTube playback"
    authors = listOf("ErrorCode26","Reflex1337")

    status = 1
    tvTypes = listOf(
        "Movie",
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=libraryofladev.com&sz=%size%"

    isCrossPlatform = true
}

android {
    namespace = "com.ladev"

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.Blatzar:NiceHttp:0.4.18")
    implementation("org.jsoup:jsoup:1.22.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")

    // NewPipeExtractor  handles YouTube stream extraction + signature decryption
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.25.2")
    implementation("com.google.protobuf:protobuf-javalite:3.25.1")
    implementation("org.mozilla:rhino:1.7.14")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.github.spotbugs:spotbugs-annotations:4.8.2")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
}
