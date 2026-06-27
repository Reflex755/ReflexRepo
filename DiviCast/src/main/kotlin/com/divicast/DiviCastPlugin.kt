package com.divicast

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class DiviCastPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(DiviCastProvider())
    }
}
