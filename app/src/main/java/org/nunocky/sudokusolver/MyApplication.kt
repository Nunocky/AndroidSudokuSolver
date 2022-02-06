package org.nunocky.sudokusolver

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import dagger.hilt.android.HiltAndroidApp
import org.nunocky.sudokusolver.service.CleanupService
import java.io.File
import javax.inject.Singleton

@Singleton
@HiltAndroidApp
class MyApplication : Application(), Application.ActivityLifecycleCallbacks {

    private var startedCount = 0
    var isRunningInForeground = false

    override fun onCreate() {
        super.onCreate()

        val imageDir = File("${filesDir}/images")
        imageDir.mkdir()

        registerActivityLifecycleCallbacks(this)
        startService(Intent(this, DestroyingService::class.java))
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        if (startedCount == 0) {
            isRunningInForeground = true
            onActivityEnteredInForeground(activity)
        }
        startedCount += 1
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        startedCount -= 1
        if (startedCount == 0) {
            isRunningInForeground = false
            onActivityEnteredInBackground(activity)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    // アプリがフォアグラウンドになった時
    private fun onActivityEnteredInForeground(activity: Activity?) {
    }

    // アプリがバックグラウンドになった時
    private fun onActivityEnteredInBackground(activity: Activity?) {
        // 不要サムネイル削除 Service起動
        val intent = Intent(this, CleanupService::class.java)
        startService(intent)
    }

    /**
     * Destroy検出用のServiceクラス
     */
    class DestroyingService : Service() {
        override fun onBind(intent: Intent?): IBinder? {
            return null
        }
    }

}

// Androidのアプリ終了(タスクキル)を検知する方法(onDestroy)
// https://mt312.com/1241