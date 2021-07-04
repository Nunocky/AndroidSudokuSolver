package org.nunocky.sudokusolver.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.nunocky.sudokusolver.SudokuRepository
import org.nunocky.sudokusolver.solver.Cell
import org.nunocky.sudokusolver.solver.SudokuSolver

class SolverViewModel(private val repository: SudokuRepository) : ViewModel() {
    class Factory(private val repository: SudokuRepository) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SolverViewModel(repository) as T
        }
    }

    enum class Status {
        INIT,
        WORKING,
        DONE
    }

    val inProgress = MutableLiveData(Status.INIT)
    val elapsedTime = MutableLiveData("")
    private var startTime = 0L
    private var currentTime = 0L

    private var solverJob: Job = Job().apply { cancel() }
    private var timerJob: Job = Job().apply { cancel() }

    val stepSpeed = MutableLiveData(0)

    val solver = SudokuSolver()

    fun loadSudoku(entityId: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.findById(entityId)?.let { entity ->
            solver.load(entity.cells)
        }
    }

    fun startSolver(callback: SudokuSolver.ProgressCallback) {
        solverJob = viewModelScope.launch(Dispatchers.IO) {
            inProgress.postValue(Status.WORKING)

            startTimer()

            val success = kotlin.runCatching {
                solver.callback = object : SudokuSolver.ProgressCallback {
                    override fun onProgress(cells: List<Cell>) {
                        if (!isActive) {
                            throw InterruptedException()
                        }
                        callback.onProgress(cells)
                    }

                    override fun onComplete(success: Boolean) {
                        callback.onComplete(success)
                    }
                }
                solver.trySolve()
            }

            stopTimer()
            inProgress.postValue(Status.DONE)
        }
    }

    fun stopSolver() = viewModelScope.launch(Dispatchers.IO) {
        solverJob.cancel()
        stopTimer()
    }

    fun resetSolver() = viewModelScope.launch(Dispatchers.IO) {
        inProgress.postValue(Status.INIT)
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch(Dispatchers.IO) {
            startTime = System.currentTimeMillis()
            currentTime = System.currentTimeMillis()
            elapsedTime.postValue("0")

            while (isActive) {
                currentTime = System.currentTimeMillis()
                elapsedTime.postValue((currentTime - startTime).toTimeStr())
                delay(250)
            }
        }
    }

    private fun stopTimer() {
        timerJob.cancel()
    }
}

private fun Long.toTimeStr(): String {
    var second = this / 1000 // second

    val hour = second / 3600
    second -= 3600 * hour

    val minute = second / 60
    second -= 60 * minute

    return if (0 < hour) {
        String.format("%02d:%02d:%02d", hour, minute, second)
    } else {
        String.format("%02d:%02d", minute, second)
    }
}