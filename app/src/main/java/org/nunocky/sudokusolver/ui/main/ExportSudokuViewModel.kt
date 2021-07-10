package org.nunocky.sudokusolver.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.nunocky.sudokusolver.MyApplication
import org.nunocky.sudokusolver.SudokuRepository

class ExportSudokuViewModel(
    application: Application,
    private val repository: SudokuRepository
) : AndroidViewModel(application) {
    class Factory(private val application: Application, private val repository: SudokuRepository) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ExportSudokuViewModel(application, repository) as T
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @ExperimentalCoroutinesApi
    suspend fun execExport(uri: Uri) = withContext(Dispatchers.IO) {
        val app = getApplication() as MyApplication
        app.contentResolver.openOutputStream(uri).use { oStream ->
            oStream?.bufferedWriter()?.use { writer ->
                val list = repository.findAll().await()

                list.forEach { entity ->
                    writer.write(entity.cells)
                    writer.newLine()
                }
            }
        }
    }
}

@ExperimentalCoroutinesApi
private suspend fun <T> LiveData<T>.await(): T {
    return withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            val observer = object : Observer<T> {
                override fun onChanged(value: T) {
                    removeObserver(this)
                    continuation.resume(value, onCancellation = {})
                }
            }

            observeForever(observer)

            continuation.invokeOnCancellation {
                removeObserver(observer)
            }
        }
    }
}
