package org.nunocky.sudokusolver

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val imageDir = File("${filesDir}/images")
        imageDir.mkdir()
    }
}