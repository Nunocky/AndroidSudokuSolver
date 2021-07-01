package org.nunocky.sudokusolver.ui.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.SudokuRepository
import org.nunocky.sudokusolver.database.SudokuEntity

class EditViewModel(private val repository: SudokuRepository) : ViewModel() {
    companion object {
        private const val TAG = "EditViewModel"
    }

    class Factory(private val repository: SudokuRepository) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return EditViewModel(repository) as T
        }
    }

    var currentValue = MutableLiveData(0)
    var entity = MutableLiveData<SudokuEntity?>(null)

    fun setNewSudoku() {
        val ent = SudokuEntity()
        entity.postValue(ent)
    }

    fun loadSudoku(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        val ent = repository.findById(id)
        entity.postValue(ent)
    }

    fun saveSudoku(cells: String) = viewModelScope.launch(Dispatchers.IO) {
        Log.d(TAG, cells)

        entity.value?.let {
            it.cells = cells
            if (it.id == 0L) {
                it.id = repository.insert(it)
            } else {
                repository.update(it)
            }
        }
    }
}