package org.nunocky.sudokusolver.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.nunocky.sudokusolver.SudokuRepository

class SudokuListViewModel(private val repository: SudokuRepository) : ViewModel() {
    class Factory(private val repository: SudokuRepository) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SudokuListViewModel(repository) as T
        }
    }

    val sudokuList = repository.findAll()
}