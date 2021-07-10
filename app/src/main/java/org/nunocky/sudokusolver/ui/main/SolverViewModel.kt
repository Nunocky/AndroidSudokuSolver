package org.nunocky.sudokusolver.ui.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.nunocky.sudokusolver.SudokuRepository
import org.nunocky.sudokulib.Cell
import org.nunocky.sudokulib.SudokuSolver

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
    val elapsedTime = MutableLiveData("00:00.000")
    val stepSpeed = MutableLiveData(0)
    val solverMethod = MutableLiveData(1)

    private var startTime = 0L
    private var currentTime = 0L

    private var solverJob: Job = Job().apply { cancel() }
    private var timerJob: Job = Job().apply { cancel() }

    val solver = org.nunocky.sudokulib.SudokuSolver()

    fun loadSudoku(entityId: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.findById(entityId)?.let { entity ->
            solver.load(entity.cells)
        }
    }

    fun startSolver(callback: org.nunocky.sudokulib.SudokuSolver.ProgressCallback) {
        solverJob = viewModelScope.launch(Dispatchers.IO) {
            inProgress.postValue(Status.WORKING)

            startTimer()

            val result = kotlin.runCatching {
                solver.callback = object : org.nunocky.sudokulib.SudokuSolver.ProgressCallback {
                    override fun onProgress(cells: List<org.nunocky.sudokulib.Cell>) {
                        if (!isActive) {
                            throw InterruptedException()
                        }
                        callback.onProgress(cells)
                    }

                    override fun onComplete(success: Boolean) {
                        callback.onComplete(success)
                    }
                }

                solver.trySolve(solverMethod.value ?: 0)
            }

            stopTimer()

            val solverElapsedTime = solver.getElapsedTime()
            elapsedTime.postValue(solverElapsedTime.toTimeStr())
            inProgress.postValue(Status.DONE)

            if (result.isSuccess) {

            } else {
                Log.d(TAG, "error")
            }
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