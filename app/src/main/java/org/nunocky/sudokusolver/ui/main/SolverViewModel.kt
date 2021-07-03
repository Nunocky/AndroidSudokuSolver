package org.nunocky.sudokusolver.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.nunocky.sudokusolver.SudokuRepository
import org.nunocky.sudokusolver.solver.Cell
import org.nunocky.sudokusolver.solver.SudokuSolver
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SolverViewModel(private val repository: SudokuRepository) : ViewModel() {
    class Factory(private val repository: SudokuRepository) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SolverViewModel(repository) as T
        }
    }

    val inProgress = MutableLiveData(false)
    val elapsedTime = MutableLiveData("")
    private var startTime = 0L
    private var currentTime = 0L
    private var timerJob: Job = Job().apply { cancel() }

    val stepSpeed = MutableLiveData(3)
    val message = MutableLiveData("")

    private val speeds = arrayOf(0L, 100L, 200L, 500L, 1000L) // ステップ実行時の待ち時間

    val solver = SudokuSolver()

    fun loadSudoku(entityId: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.findById(entityId)?.let { entity ->
            solver.setup(entity.cells)
        }
    }

    fun startSolve(callback: SudokuSolver.ProgressCallback) =
        viewModelScope.launch(Dispatchers.IO) {
            inProgress.postValue(true)
            message.postValue("solving...")

            startTimer()
            val success = solve(callback)
            stopTimer()

            message.postValue(
                if (success) {
                    "solved!"
                } else {
                    "unsolved"
                }
            )
            inProgress.postValue(false)
        }

    private suspend fun solve(callback: SudokuSolver.ProgressCallback) =
        suspendCoroutine<Boolean> { continuation ->

            solver.callback = object : SudokuSolver.ProgressCallback {
                override fun onProgress(cells: List<Cell>) {
                    callback.onProgress(cells)
                    runBlocking {
                        delay(speeds[stepSpeed.value ?: 3]) // TODO delayをどうやって入れる?
                    }
                }
            }
//            solver.trySolve()

            while (!solver.isSolved()) {
                val valueChanged = solver.execStep()
                if (!valueChanged) {
                    break
                }
            }

            if (!solver.isSolved()) {
                // 深さ優先探索
                solver.depthFirstSearch()
            }

            continuation.resume(solver.isSolved())
        }

    private fun startTimer() {
        timerJob = viewModelScope.launch(Dispatchers.IO) {
            startTime = System.currentTimeMillis()
            currentTime = System.currentTimeMillis()
            elapsedTime.postValue("0")

            while (isActive) {
                currentTime = System.currentTimeMillis()
                elapsedTime.postValue("${currentTime - startTime}") // TODO フォーマット
                delay(250)
            }
        }
    }

    private fun stopTimer() {
        timerJob.cancel()
    }
}