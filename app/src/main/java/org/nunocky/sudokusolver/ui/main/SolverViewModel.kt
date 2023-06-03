package org.nunocky.sudokusolver.ui.main

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.nunocky.sudokulib.Cell
import org.nunocky.sudokulib.DIFFICULTY
import org.nunocky.sudokulib.METHOD
import org.nunocky.sudokulib.SudokuSolver
import org.nunocky.sudokusolver.Preference
import org.nunocky.sudokusolver.database.SudokuRepository
import javax.inject.Inject

@HiltViewModel
class SolverViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
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
    val solverMethodIndex =
        savedStateHandle.getLiveData("solverMethodIndex", preference.solverMethodIndex)

    val solverMethod = MediatorLiveData<METHOD>().apply {
        fun update(index: Int) {
            this.value = when (index) {
                0 -> METHOD.ONLY_STANDARD
                1 -> METHOD.STANDARD_AND_DFS
                else -> METHOD.ONLY_DFS
            }
        }

        // 画面遷移直後に値がセットされないのでこんな実装をしているがどうにかならないのか
        update(preference.solverMethodIndex)

        addSource(solverMethodIndex) {
            update(it)
        }
    }

    private var startTime = 0L
    private var currentTime = 0L

    val solver = SudokuSolver()
    private var timerJob: Job = Job().apply { cancel() }
    private var solverJob: Job = Job().apply { cancel() }

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
            solverStatus.value = _solverStatus
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
     * TODO コールバックは複数のメソッドを用意する?
     */
    fun startSolve(dispatcher: CoroutineDispatcher, callback: (List<Cell>) -> Unit) {
        if (solverJob.isActive) {
            return
        }

        solverJob = viewModelScope.launch(dispatcher) {
            startTimer()

            val solvingFlow = solverFlow()
                .buffer(Channel.UNLIMITED)
                .onCompletion {
                    // 中断時に別の所でセットされるため
                    if (_solverStatus == SolverStatus.SUCCESS) {
                        solverStatus.value = _solverStatus
                    }
                }.catch {
                    _solverStatus = SolverStatus.INTERRUPTED
                    solverStatus.value = _solverStatus
                }

            solvingFlow.collect { cellStr ->
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
    private fun solverFlow(): Flow<String> = callbackFlow {
        solver.callback = object : SudokuSolver.ProgressCallback {
            override fun onProgress(cells: List<Cell>) {
                if (!isActive) {
                    channel.close()
                    throw InterruptedException()
                }

                if (_solverStatus != SolverStatus.WORKING) {
                    _solverStatus = SolverStatus.WORKING
                    solverStatus.value = SolverStatus.WORKING
                }
                trySend(cells.joinToString(""))
            }

            override fun onComplete(success: Boolean) {
                stopTimer()
                if (isActive) {
                    _solverStatus = if (success) {
                        SolverStatus.SUCCESS
                    } else {
                        SolverStatus.FAILED
                    }
                }
                channel.close()
            }
        }

        solver.trySolve(solverMethod.value ?: METHOD.ONLY_STANDARD)

        awaitClose {
            solver.callback = null
        }
    }

    /**
     * 解析停止
     */
    fun stopSolver() {
        viewModelScope.launch {
            stopTimer()
            solverJob.cancel()

            // TODO 中断時に解析が終わっているときは、成功として flowの最後の値をボードに反映する
//            if (solver.isSolved()) {
//                _solverStatus = SolverStatus.SUCCESS
//            } else {
            _solverStatus = SolverStatus.INTERRUPTED
//            }

            solverStatus.value = _solverStatus
//            solverJob.join()
        }
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
    private fun stopTimer() {
        timerJob.cancel()
    }

    /**
     * 難易度の更新
     */
    fun updateDifficulty(difficulty: DIFFICULTY) = viewModelScope.launch(Dispatchers.IO) {
        repository.findById(entityId.value!!)?.let { entity ->
            entity.difficulty = difficulty
            repository.update(entity)
        }
    }
}