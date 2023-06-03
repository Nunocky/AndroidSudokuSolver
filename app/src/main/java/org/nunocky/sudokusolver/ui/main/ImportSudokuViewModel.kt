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
import org.nunocky.sudokulib.DIFFICULTY
import org.nunocky.sudokulib.SudokuSolver
import org.nunocky.sudokusolver.adapter.CalenderJsonAdapter
import org.nunocky.sudokusolver.adapter.SudokuJsonAdapterFactory
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.database.SudokuRepository
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class ImportSudokuViewModel @Inject constructor(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repository: SudokuRepository
) : ViewModel() {
//    class Factory(private val application: Application, private val repository: SudokuRepository) :
//        ViewModelProvider.NewInstanceFactory() {
//        @Suppress("unchecked_cast")
//        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
//            return ImportSudokuViewModel(application, repository) as T
//        }
//    }

    suspend fun execImport(uri: Uri) = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            readAsJson(uri)
        }.onFailure {
            readAsText(uri)
        }
    }

    private fun readAsJson(uri: Uri) {
        //val app = getApplication() as MyApplication

        application.contentResolver.openInputStream(uri).use { iStream ->
            if (iStream == null) {
                throw IOException()
            }

            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)
            val out = ByteArrayOutputStream()

            var numRead: Int
            do {
                numRead = iStream.read(buffer, 0, buffer.size)
                if (0 < numRead) {
                    out.write(buffer, 0, numRead)
                }
            } while (0 < numRead)

            val jsonString = out.toString("UTF-8")

            val builder = Moshi.Builder()
                .add(SudokuJsonAdapterFactory.INSTANCE)
                .add(Calendar::class.java, CalenderJsonAdapter())
                .build()

            val dataType =
                Types.newParameterizedType(List::class.java, SudokuEntity::class.java)
            val adapter = builder.adapter<List<SudokuEntity>>(dataType)

            val list = adapter.fromJson(jsonString)

            list?.let {
                list.forEach { entity ->
                    entity.id = 0
                    entity.difficulty = DIFFICULTY.UNDEF
                    entity.createdAt = Calendar.getInstance()
                }

                repository.insert(list)
            }
        }
    }

    private fun readAsText(uri: Uri) {
        //val app = getApplication() as MyApplication
        application.contentResolver.openInputStream(uri).use { iStream ->
            iStream?.bufferedReader()?.use { reader ->

                val list = ArrayList<SudokuEntity>()

                var line: String? = ""
                do {
                    line = reader.readLine()?.trim()
                    if (line?.length == 81) {
                        list.add(
                            SudokuEntity(
                                id = 0,
                                cells = line,
                                difficulty = DIFFICULTY.UNDEF

                            )
                        )
                    }
                } while (line?.isNotEmpty() == true)

                repository.insert(list)
            }
        }
    }

}