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

    // 解読状態
    // 注 : solverStatusと solverStatusFlowは同時に変化するわけではない
    private var _solverStatus: SolverStatus = SolverStatus.INIT
    val solverStatus = MutableStateFlow(SolverStatus.INIT)

    // 解析ステップ数
    private val _steps = MutableStateFlow(0)
    val steps = _steps.asLiveData()

    // 解析に要した時間
    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime = _elapsedTime.asLiveData()

    // リセットボタンの enable状態
    private val _canReset = solverStatus.map {
        it != SolverStatus.INIT && it != SolverStatus.WORKING
    }
    val canReset = _canReset.asLiveData()

    // スタートボタンの enable状態
    private val _canStart = solverStatus.map {
        it == SolverStatus.READY
    }
    val canStart = _canStart.asLiveData()

    val entityId = savedStateHandle.getLiveData("entityId", 0L)
    val stepSpeed = savedStateHandle.getLiveData("stepSpeed", preference.stepSpeed)
    val solverMethod = savedStateHandle.getLiveData("solverMethod", preference.solverMethod)

    private var startTime = 0L
    private var currentTime = 0L

    private var timerJob: Job = Job().apply { cancel() }

    val solver = SudokuSolver()

    /**
     * 指定 id の問題をロードする (非同期処理)
     *
     * @param id entity is of sudoku]
     * @param dispatcher コルーチンを実行するコンテキスト
     * @param callback 処理完了時に実行するコールバック
     *
     */
    fun loadSudoku(id: Long, dispatcher: CoroutineDispatcher, callback: () -> Unit) {
        if (id == 0L) {
            return
        }

        viewModelScope.launch(dispatcher) {
            _solverStatus = SolverStatus.INIT
            _elapsedTime.value = 0
            _steps.value = 0

            solverStatus.value = _solverStatus

            val entity = repository.findById(id)
            if (entity != null) {
                solver.load(entity.cells)
                _solverStatus = SolverStatus.READY
                solverStatus.value = _solverStatus
            } else {
                _solverStatus = SolverStatus.ERROR
                solverStatus.value = _solverStatus
            }

            withContext(Dispatchers.Main) {
                callback()
            }
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
                    solverStatus.value = _solverStatus
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
                _steps.value += 1

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
                if (_solverStatus != SolverStatus.WORKING) {
                    _solverStatus = SolverStatus.WORKING
                    solverStatus.value = SolverStatus.WORKING
                }
                trySend(cells.joinToString(""))
            }

            override fun onComplete(success: Boolean) {
                stopTimer()
                _solverStatus = SolverStatus.SUCCESS
                channel.close()
            }

            override fun onInterrupted() {
                stopTimer()
                _solverStatus = SolverStatus.INTERRUPTED
                channel.close()
            }

            override fun onSolverError() {
                stopTimer()
                _solverStatus = SolverStatus.ERROR
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
            _elapsedTime.value = 0L

            while (isActive) {
                currentTime = System.currentTimeMillis()
                _elapsedTime.value = currentTime - startTime
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