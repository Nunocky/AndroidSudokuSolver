package org.nunocky.sudokusolver.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nunocky.sudokusolver.CalenderJsonAdapter
import org.nunocky.sudokusolver.MyApplication
import org.nunocky.sudokusolver.SudokuJsonAdapterFactory
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.database.SudokuRepository
import java.util.*

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
    suspend fun execExport(uri: Uri) = withContext(Dispatchers.IO) {
        val app = getApplication() as MyApplication
        app.contentResolver.openOutputStream(uri).use { oStream ->
            oStream?.bufferedWriter()?.use { writer ->
                val list = repository.findAll()

                list.forEach { entity ->
                    writer.write(entity.cells)
                    writer.newLine()
                }
            }
        }
    }

    suspend fun execExportJson(uri: Uri) = withContext(Dispatchers.IO) {

        val app = getApplication() as MyApplication

        app.contentResolver.openOutputStream(uri).use { oStream ->
            oStream?.bufferedWriter()?.use { writer ->

                val builder = Moshi.Builder()
                    .add(SudokuJsonAdapterFactory.INSTANCE)
                    .add(Calendar::class.java, CalenderJsonAdapter())
                    .build()

                val dataType =
                    Types.newParameterizedType(List::class.java, SudokuEntity::class.java)
                val adapter = builder.adapter<List<SudokuEntity>>(dataType)
                val list = adapter.toJson(repository.findAll().toList())
                writer.write(list)
            }
        }


    }
}
