package com.guesswho.movetracker

import android.app.Application
import android.content.Context

@Suppress("unused")
class MoveTrackerApplication : Application() {

    init {
        INSTANCE = this
    }

    companion object {

        private var INSTANCE: MoveTrackerApplication? = null

        fun applicationContext(): Context? {
            return INSTANCE?.applicationContext
        }

    }

}