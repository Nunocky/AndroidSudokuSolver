package org.nunocky.sudokusolver.ui.main

import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.nunocky.sudokulib.Cell
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
        INIT, // 初期状態、データをロードしていない
        READY, // データをロードして解析が可能な状態
        WORKING, // 解析実行中
        SUCCESS, // 解析成功 (終了)
        FAILED, // 解析失敗 (終了)
        INTERRUPTED, // 解析を中断した (終了)
        ERROR // エラーが発生した (終了)
    }

    private val solverStatusFlow = MutableStateFlow(Status.INIT)

    //    val solverStatus = solverStatusFlow.asLiveData()
    val solverStatus: MutableLiveData<Status> = MutableLiveData<Status>(Status.INIT)

    val elapsedTime = MutableLiveData(0L)
    val canReset = MediatorLiveData<Boolean>() // リセット・編集可能
    val canStart = MediatorLiveData<Boolean>() // 解析可能
    val stepsFlow = MutableStateFlow(0)
    val steps = stepsFlow.asLiveData()

    val entityId = savedStateHandle.getLiveData("entityId", 0L)
    val stepSpeed = savedStateHandle.getLiveData("stepSpeed", preference.stepSpeed)
    val solverMethod = savedStateHandle.getLiveData("solverMethod", preference.solverMethod)

    private var startTime = 0L
    private var currentTime = 0L

    private var timerJob: Job = Job().apply { cancel() }

    val solver = SudokuSolver()

    init {
        canReset.addSource(solverStatus) {
            canReset.value = (it != Status.INIT && it != Status.WORKING)
        }

        canStart.addSource(solverStatus) {
            canStart.value = (it == Status.READY)
        }
    }

    fun loadSudoku(id: Long) {
        solverStatusFlow.value = Status.INIT
        solverStatus.postValue(solverStatusFlow.value)
        val entity = repository.findById(id)
        if (entity != null) {
            solver.load(entity.cells)
            solverStatusFlow.value = Status.READY
            solverStatus.postValue(solverStatusFlow.value)
        } else {
            solverStatusFlow.value = Status.ERROR
            solverStatus.postValue(solverStatusFlow.value)
        }
    }

    /**
     * 解析開始
     * TODO コールバックは複数のメソッドを用意する
     */
    fun startSolve(dispatcher: CoroutineDispatcher, callback: (List<Cell>) -> Unit): Job {
        return viewModelScope.launch(dispatcher) {
            val flow = solverFlow()
                .buffer(Channel.UNLIMITED)
                .onCompletion {
                    solverStatus.postValue(solverStatusFlow.value)
                }

            flow.collect { cellStr ->
                val cells = mutableListOf<Cell>()

                cellStr.forEach { c ->
                    val newCell = Cell()
                    newCell.value = c.digitToInt()
                    cells.add(newCell)
                }

                // ボードの描画
                withContext(Dispatchers.Main) {
                    callback(cells)
                }
                stepsFlow.value += 1

                //val tm = max(30L, viewModel.stepSpeed.value!! * 50L)
                val tm = stepSpeed.value!! * 50L
                delay(tm)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun solverFlow(): Flow<String> = callbackFlow {
        startTimer()

        solver.callback = object : SudokuSolver.ProgressCallback {
            override fun onProgress(cells: List<Cell>) {
                if (solverStatusFlow.value != Status.WORKING) {
                    solverStatusFlow.value = Status.WORKING
                    solverStatus.postValue(Status.WORKING)
                }
                trySend(cells.joinToString(""))
            }

            override fun onComplete(success: Boolean) {
                stopTimer()
                solverStatusFlow.value = Status.SUCCESS
                channel.close()
            }

            override fun onInterrupted() {
                stopTimer()
                solverStatusFlow.value = Status.INTERRUPTED
                channel.close()
            }

            override fun onSolverError() {
                stopTimer()
                solverStatusFlow.value = Status.ERROR
                channel.close()
            }
        }

        runCatching {
            solver.trySolve(solverMethod.value ?: 0)
        }.onFailure {
            Log.d("SolverViewModel", "onFailure")
            when (it) {
                is SudokuSolver.SolverError -> {
                    channel.close()
                }

                is InterruptedException -> {
                    channel.close()
                }
            }
        }

        awaitClose {
            solver.callback = null
        }

    }

    /**
     * 解析停止
     */
    fun stopSolver() {
        timerJob.cancel()
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
    fun stopTimer() {
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