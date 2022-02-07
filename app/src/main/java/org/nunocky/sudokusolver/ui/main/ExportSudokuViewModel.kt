package org.nunocky.sudokusolver.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nunocky.sudokusolver.adapter.CalenderJsonAdapter
import org.nunocky.sudokusolver.adapter.SudokuJsonAdapterFactory
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.database.SudokuRepository
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ExportSudokuViewModel @Inject constructor(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repository: SudokuRepository
) : ViewModel() {
//    class Factory(private val application: Application, private val repository: SudokuRepository) :
//        ViewModelProvider.NewInstanceFactory() {
//        @Suppress("unchecked_cast")
//        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
//            return ExportSudokuViewModel(application, repository) as T
//        }
//    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun execExport(uri: Uri) = withContext(Dispatchers.IO) {
        //val app = applicationContext as MyApplication
        application.contentResolver.openOutputStream(uri).use { oStream ->
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

        //val app = getApplication() as MyApplication
        application.contentResolver.openOutputStream(uri).use { oStream ->
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
