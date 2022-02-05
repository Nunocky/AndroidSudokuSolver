package org.nunocky.sudokusolver

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.nunocky.sudokusolver.database.SudokuRepository
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@AndroidEntryPoint
class CleanupService : Service(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    @Inject
    lateinit var repository: SudokuRepository

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        (job + Dispatchers.Default).cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        launch {
            withContext(Dispatchers.IO) {
                deleteUnusedFiles()
            }
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun deleteUnusedFiles() {
        val filesToBeDeleted: MutableList<String> = mutableListOf()
        val imageDir = File(filesDir, IMAGEDIR)

        imageDir.listFiles()?.map {
            it.name
        }?.let {
            filesToBeDeleted.addAll(it)
        }

        // サムネイルをリストから削除
        repository.findAll().forEach {
            if (!it.thumbnail.isNullOrBlank()) {
                filesToBeDeleted.remove(it.thumbnail)
            }
        }

        // ファイル削除
        filesToBeDeleted.forEach {
            File(imageDir, it).delete()
        }
    }

}