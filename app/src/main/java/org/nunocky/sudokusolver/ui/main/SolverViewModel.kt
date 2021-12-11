package org.nunocky.sudokusolver.ui.main

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import org.nunocky.sudokulib.SudokuSolver
import org.nunocky.sudokusolver.Preference
import org.nunocky.sudokusolver.database.SudokuRepository
import javax.inject.Inject

@HiltViewModel
class SolverViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: SudokuRepository,
    private val preference: Preference
) : ViewModel() {

    enum class Status {
        INIT,
        WORKING,
        SUCCESS,
        FAILED,
        INTERRUPTED,
        ERROR
    }

    val solverStatus = MutableLiveData(Status.INIT)
    val elapsedTime = MutableLiveData("00:00.000")
    val stepSpeed = savedStateHandle.getLiveData<Int>("stepSpeed", preference.stepSpeed)
    val solverMethod = savedStateHandle.getLiveData<Int>("solverMethod", preference.solverMethod)

    private var startTime = 0L
    private var currentTime = 0L

    private var solverJob: Job = Job().apply { cancel() }
    private var timerJob: Job = Job().apply { cancel() }

    val solver = SudokuSolver()

    val entityId = savedStateHandle.getLiveData("entityId", 0L)

//    fun loadSudoku(entityId: Long) = viewModelScope.launch(Dispatchers.IO) {
//    fun loadSudoku() = viewModelScope.launch(Dispatchers.IO) {
//        repository.findById(entityId)?.let { entity ->
//            solver.load(entity.cells)
//        }
//    }
    suspend fun loadSudoku(entityId: Long) {
        repository.findById(entityId)?.let { entity ->
            solver.load(entity.cells)
        }
    }

    val solverReady = MediatorLiveData<Boolean>()

    init {
        solverReady.addSource(entityId) {
            viewModelScope.launch(Dispatchers.IO) {
                if (it != 0L) {
                    val entity = repository.findById(it)
                    if (entity != null) {
                        solver.load(entity.cells)
                        solverReady.postValue(true)
                    } else {
                        throw IllegalArgumentException("load failed")
                    }
                }
            }
        }
    }

    fun startSolver(callback: SudokuSolver.ProgressCallback) {
        solverJob = viewModelScope.launch(Dispatchers.IO) {
            solverStatus.postValue(Status.WORKING)

            startTimer()

            runCatching {
                solver.callback = object : SudokuSolver.ProgressCallback {
                    override fun onProgress(cells: List<org.nunocky.sudokulib.Cell>) {
                        if (!isActive) {
                            solverStatus.postValue(Status.INTERRUPTED)
                            throw InterruptedException()
                        }
                        callback.onProgress(cells)
                    }

                    override fun onComplete(success: Boolean) {
                        if (success) {
                            solverStatus.postValue(Status.SUCCESS)
                        } else {
                            solverStatus.postValue(Status.FAILED)
                        }
                        callback.onComplete(success)
                    }
                }

                solver.trySolve(solverMethod.value ?: 0)
            }.onFailure {
                when (it) {
                    is SudokuSolver.SolverError -> {
                        solverStatus.postValue(Status.ERROR)
                        callback.onSolverError()
                    }

                    is InterruptedException -> {
                        solverStatus.postValue(Status.INTERRUPTED)
                        callback.onInterrupted()
                    }
                }
            }

            stopTimer()
            val solverElapsedTime = solver.getElapsedTime()
            elapsedTime.postValue(solverElapsedTime.toTimeStr())
        }
    }

//    fun stopSolver() = viewModelScope.launch(Dispatchers.IO) {
//        solverJob.cancel()
//        stopTimer()
//    }
    suspend fun stopSolver() {
        solverJob.cancel()
        stopTimer()
    }

//    fun resetSolver() = viewModelScope.launch(Dispatchers.IO) {
//        solverStatus.postValue(Status.INIT)
//    }
    suspend fun resetSolver() {
        solverStatus.postValue(Status.INIT)
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch(Dispatchers.IO) {
            startTime = System.currentTimeMillis()
            currentTime = System.currentTimeMillis()
            elapsedTime.postValue("0")

            while (isActive) {
                currentTime = System.currentTimeMillis()
                elapsedTime.postValue((currentTime - startTime).toTimeStr())
                delay(100)
            }
        }
    }

    private fun stopTimer() {
        timerJob.cancel()
    }

    fun updateDifficulty(entityId: Long, difficulty: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.findById(entityId)?.let { entity ->
            entity.difficulty = difficulty
            repository.update(entity)
        }
    }

    companion object {
        private const val TAG = "SolverViewModel"
    }
}

private fun Long.toTimeStr(): String {
    val milsecs = this % 1000
    var second = this / 1000 // second

    val hour = second / 3600
    second -= 3600 * hour

    val minute = second / 60
    second -= 60 * minute

    return if (0 < hour) {
        String.format("%02d:%02d:%02d.%03d", hour, minute, milsecs)
    } else {
        String.format("%02d:%02d.%03d", minute, second, milsecs)
    }
}