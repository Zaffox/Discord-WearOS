package com.zaffox.discordwear

import android.app.Application
import com.zaffox.discordwear.api.DiscordRepository

class DiscordWearApp : Application() {
    var repository: DiscordRepository? = null
        private set

    override fun onCreate() {
        super.onCreate()
        UpdateChecker.start(this)
        UpdateChecker.checkNow(this)
    }

    fun initRepository(token: String) {
        repository?.disconnect()
        val repo = DiscordRepository(token, context = this)
        repo.connect()
        repository = repo
    }

    fun clearRepository() {
        repository?.disconnect()
        repository = null
    }

    override fun onTerminate() {
        super.onTerminate()
        repository?.disconnect()
    }
}

val android.content.Context.discordApp: DiscordWearApp
    get() = applicationContext as DiscordWearApp
