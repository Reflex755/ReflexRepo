package com.ladev

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class LadevPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(LadevProvider())
    }
}
