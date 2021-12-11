package org.nunocky.sudokusolver.ui.main

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.database.SudokuRepository
import javax.inject.Inject

@HiltViewModel
class EditViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: SudokuRepository
) : ViewModel() {

    var currentValue = MutableLiveData(0)

    //val entity = MutableLiveData<SudokuEntity?>(null)
    private val entityId = savedStateHandle.getLiveData<Long>("entityId")
    val entity = MediatorLiveData<SudokuEntity>()

    val sudokuSolver = org.nunocky.sudokulib.SudokuSolver()
    val isValid = sudokuSolver.isValid

    init {
        entity.addSource(entityId) {
            viewModelScope.launch(Dispatchers.IO) {
                if (it == 0L) {
                    setNewSudoku()
                } else {
                    loadSudoku(it)
                }
            }
        }
    }

    private fun setNewSudoku() {
        val ent = SudokuEntity()
        entity.postValue(ent)
    }

    private fun loadSudoku(id: Long) {
        repository.findById(id).let { ent ->
            entity.postValue(ent)
        }
    }

    fun saveSudoku(cells: String) : Long{
        // TODO もうちょっとなんとかする
        var id = 0L
        entity.value?.let {
            it.cells = cells
            if (it.id == 0L) {
                it.id = repository.insert(it)
                id = it.id
            } else {
                repository.update(it)
                id = it.id
            }
            entity.postValue(it)
        }

        return id
    }
}