package org.nunocky.sudokusolver.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.SudokuRepository
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokulib.SudokuSolver

class EditViewModel(private val repository: SudokuRepository) : ViewModel() {

    class Factory(private val repository: SudokuRepository) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return EditViewModel(repository) as T
        }
    }

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