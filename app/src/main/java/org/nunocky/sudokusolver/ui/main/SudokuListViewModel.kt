package org.nunocky.sudokusolver.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.SudokuRepository
import org.nunocky.sudokusolver.database.SudokuEntity

class SudokuListViewModel(private val repository: SudokuRepository) : ViewModel() {
    class Factory(private val repository: SudokuRepository) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SudokuListViewModel(repository) as T
        }
    }

    val sudokuList = repository.findAll()

    fun deleteItem(entity: SudokuEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(entity)
    }
}