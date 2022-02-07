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

    // 解析機の状態
    enum class Status {
        INIT, // 初期状態、データにロードしていない
        READY, // データをロードして解析が可能な状態
        WORKING, // 解析実行中
        SUCCESS, // 解析成功 (終了)
        FAILED, // 解析失敗 (終了)
        INTERRUPTED, // 解析を中断した (終了)
        ERROR // エラーが発生した (終了)
    }

    val solverStatus = MutableLiveData(Status.INIT)
    val elapsedTime = MutableLiveData(0L)
    val elapsedTimeStr = MediatorLiveData<String>()
    val canReset = MediatorLiveData<Boolean>() // リセット・編集可能
    val canStart = MediatorLiveData<Boolean>() // 解析可能
    val steps = MutableLiveData(0)

    val entityId = savedStateHandle.getLiveData("entityId", 0L)
    val stepSpeed = savedStateHandle.getLiveData("stepSpeed", preference.stepSpeed)
    val solverMethod = savedStateHandle.getLiveData("solverMethod", preference.solverMethod)

    private var startTime = 0L
    private var currentTime = 0L

    private var solverJob: Job = Job().apply { cancel() }
    private var timerJob: Job = Job().apply { cancel() }

    val solver = SudokuSolver()

    init {
        canReset.addSource(solverStatus) {
            canReset.value = (it != Status.INIT && it != Status.WORKING)
        }

        canStart.addSource(solverStatus) {
            canStart.value = (it == Status.READY)
        }

        elapsedTimeStr.addSource(elapsedTime) {
            elapsedTimeStr.value = it.toTimeStr()
        }
    }

    fun loadSudoku(id: Long) {
        solverStatus.postValue(Status.INIT)
        val entity = repository.findById(id)
        if (entity != null) {
            solver.load(entity.cells)
            solverStatus.postValue(Status.READY)
        } else {
            solverStatus.postValue(Status.ERROR)
        }
    }

    /**
     * 解析開始
     */
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
            elapsedTime.postValue(solverElapsedTime)
        }
    }

//    fun stopSolver() = viewModelScope.launch(Dispatchers.IO) {
//        solverJob.cancel()
//        stopTimer()
//    }
    /**
     * 解析停止
     */
    fun stopSolver() {
        solverJob.cancel()
        stopTimer()
    }

    /**
     * カウンタの開始
     */
    private fun startTimer() {
        timerJob = viewModelScope.launch(Dispatchers.IO) {
            startTime = System.currentTimeMillis()
            currentTime = System.currentTimeMillis()
            elapsedTime.postValue(0L)

            while (isActive) {
                currentTime = System.currentTimeMillis()
                elapsedTime.postValue((currentTime - startTime))
                delay(100)
            }
        }
    }

    /**
     * カウンタの停止
     */
    private fun stopTimer() {
        timerJob.cancel()
    }

    /**
     * 難易度の更新
     */
    fun updateDifficulty(difficulty: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.findById(entityId.value!!)?.let { entity ->
            entity.difficulty = difficulty
            repository.update(entity)
        }
    }
}

/**
 * Long型を時間形式に変換
 * TODO Utilsに移動する
 */
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