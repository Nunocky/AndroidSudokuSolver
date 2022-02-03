package org.nunocky.sudokusolver.ui.main

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.database.SudokuRepository
import javax.inject.Inject

@HiltViewModel
class SudokuListViewModel @Inject constructor(
//    val savesStateHandle: SavedStateHandle,
    val repository: SudokuRepository
) : ViewModel() {

    val filterImpossible = MutableLiveData(true)
    val filterUnTested = MutableLiveData(true)
    val filterEasy = MutableLiveData(true)
    val filterMedium = MutableLiveData(true)
    val filterHard = MutableLiveData(true)
    val filterExtreme = MutableLiveData(true)
    val filter = MediatorLiveData<SudokuRepository.Filter?>()
    val isActionMode = MutableLiveData(false)

    init {
        arrayOf(
            filterImpossible,
            filterUnTested,
            filterEasy,
            filterMedium,
            filterHard,
            filterExtreme
        ).forEach {
            filter.addSource(it) {
                filter.postValue(
                    SudokuRepository.Filter(
                        checkImpossible = filterImpossible.value ?: true,
                        checkUntested = filterUnTested.value ?: true,
                        checkEasy = filterEasy.value ?: true,
                        checkMedium = filterMedium.value ?: true,
                        checkHard = filterHard.value ?: true,
                        checkExtreme = filterExtreme.value ?: true,
                    )
                )
            }
        }
    }

    val sudokuList =
        Transformations.switchMap(filter) {
            if (it != null) {
                val difficulties = it.toIntArray()
                repository.findByDifficulties(difficulties)
            } else {
                repository.findAllAsLiveData()
            }
        }

    private var deletedItems: List<SudokuEntity>? = null

    fun deleteItems(ids: List<Long>) = viewModelScope.launch(Dispatchers.IO) {
        deletedItems = repository.findByIds(ids)
        repository.deleteByIds(ids)
    }

    fun restoreDeletedItems() = viewModelScope.launch(Dispatchers.IO) {
        deletedItems?.let { them ->
            repository.insert(them)
        }
    }
}