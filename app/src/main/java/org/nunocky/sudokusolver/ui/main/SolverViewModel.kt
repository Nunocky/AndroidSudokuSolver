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

    val elapsedTime = MutableLiveData(0L)

    // solverStatusと solverStatusFlowは同時に変化するわけではない
    private var solverStatus: SolverStatus = SolverStatus.INIT
    val solverStatusFlow = MutableStateFlow(SolverStatus.INIT)

    // リセットボタンの enable状態
    private val _canResetFlow = solverStatusFlow.map {
        it != SolverStatus.INIT && it != SolverStatus.WORKING
    }

    val canReset = _canResetFlow.asLiveData()

    // スタートボタンの enable状態
    private val _canStartFlow = solverStatusFlow.map {
        it == SolverStatus.READY
    }

    val canStart = _canStartFlow.asLiveData()

    val stepsFlow = MutableStateFlow(0)
    val steps = stepsFlow.asLiveData()

    val entityId = savedStateHandle.getLiveData("entityId", 0L)
    val stepSpeed = savedStateHandle.getLiveData("stepSpeed", preference.stepSpeed)
    val solverMethod = savedStateHandle.getLiveData("solverMethod", preference.solverMethod)

    private var startTime = 0L
    private var currentTime = 0L

    private var timerJob: Job = Job().apply { cancel() }

    val solver = SudokuSolver()

    fun loadSudoku(id: Long) {
        solverStatus = SolverStatus.INIT
        solverStatusFlow.value = solverStatus
        val entity = repository.findById(id)
        if (entity != null) {
            solver.load(entity.cells)
            solverStatus = SolverStatus.READY
            solverStatusFlow.value = solverStatus
        } else {
            solverStatus = SolverStatus.ERROR
            solverStatusFlow.value = solverStatus
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
                    solverStatusFlow.value = solverStatus
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
                if (solverStatus != SolverStatus.WORKING) {
                    solverStatus = SolverStatus.WORKING
                    solverStatusFlow.value = SolverStatus.WORKING
                }
                trySend(cells.joinToString(""))
            }

            override fun onComplete(success: Boolean) {
                stopTimer()
                solverStatus = SolverStatus.SUCCESS
                channel.close()
            }

            override fun onInterrupted() {
                stopTimer()
                solverStatus = SolverStatus.INTERRUPTED
                channel.close()
            }

            override fun onSolverError() {
                stopTimer()
                solverStatus = SolverStatus.ERROR
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