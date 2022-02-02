package org.nunocky.sudokusolver.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.database.SudokuRepository
import javax.inject.Inject

@HiltViewModel
class EditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SudokuRepository
) : ViewModel() {

    val entityId = savedStateHandle.getLiveData("entityId", 0L)

    var currentValue = MutableLiveData(0)

    val sudokuSolver = org.nunocky.sudokulib.SudokuSolver()

    fun loadSudoku(id: Long): SudokuEntity {
        if (id == 0L) {
            return SudokuEntity()
        } else {
            repository.findById(id).let { ent ->
                ent ?: throw RuntimeException("entity $id not found")
                return ent
            }
        }
    }

    fun saveSudoku(id: Long, cells: String): Long {
        val entity = repository.findById(id) ?: SudokuEntity(id = 0)

        entity.cells = cells
        if (entity.id == 0L) {
            entity.id = repository.insert(entity)
        } else {
            repository.update(entity)
        }
        return entity.id
    }
}