package org.nunocky.sudokusolver.ui.main

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.database.SudokuRepository
import org.nunocky.sudokusolver.database.SudokuEntity
import javax.inject.Inject

@HiltViewModel
class EditViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: SudokuRepository) : ViewModel() {

    var currentValue = MutableLiveData(0)
    val entity = MutableLiveData<SudokuEntity?>(null)

    val sudokuSolver = org.nunocky.sudokulib.SudokuSolver()
    val isValid = sudokuSolver.isValid

    fun setNewSudoku() {
        val ent = SudokuEntity()
        entity.postValue(ent)
    }

    fun loadSudoku(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        val ent = repository.findById(id)
        entity.postValue(ent)
    }

    fun saveSudoku(cells: String) = viewModelScope.launch(Dispatchers.IO) {
        entity.value?.let {
            it.cells = cells
            if (it.id == 0L) {
                it.id = repository.insert(it)
            } else {
                repository.update(it)
            }
            entity.postValue(it)
        }
    }
}