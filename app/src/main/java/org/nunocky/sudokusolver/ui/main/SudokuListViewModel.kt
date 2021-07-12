package org.nunocky.sudokusolver.ui.main

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.database.SudokuRepository
import org.nunocky.sudokusolver.database.SudokuEntity

class SudokuListViewModel(private val repository: SudokuRepository) : ViewModel() {
    class Factory(private val repository: SudokuRepository) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SudokuListViewModel(repository) as T
        }
    }

    val filterImpossible = MutableLiveData(true)
    val filterUnTested = MutableLiveData(true)
    val filterEasy = MutableLiveData(true)
    val filterMedium = MutableLiveData(true)
    val filterHard = MutableLiveData(true)
    val filterExtreme = MutableLiveData(true)

    val filter = MediatorLiveData<SudokuRepository.Filter?>()

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

    fun deleteItem(entity: SudokuEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(entity)
    }

    fun deleteItems(ids: List<Long>) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteByIds(ids)
    }
}