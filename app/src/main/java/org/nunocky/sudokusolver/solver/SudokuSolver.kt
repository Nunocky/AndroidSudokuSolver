package org.nunocky.sudokusolver.solver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SudokuSolver {
    companion object {
        const val DIFFICULTY_IMPOSSIBLE = -1
        const val DIFFICULTY_UNDEF = 0
        const val DIFFICULTY_EASY = 1
        const val DIFFICULTY_MEDIUM = 2
        const val DIFFICULTY_HARD = 3
        const val DIFFICULTY_EXTREME = 3
    }

    interface Algorithm {
        fun trySolve(): Boolean
    }

    interface ProgressCallback {
        fun onProgress(cells: List<Cell>)
        fun onFocusGroup(groupIndex: Int) {}
        fun onUnfocusGroup(groupIndex: Int) {}

        //fun onCellFocused(cellId: Int) {}
        //fun onCellUnfocused(cellId: Int) {}
        //fun onSelectGroup(groupId: Int) {}
        //fun onUnselectGroup(groupId: Int) {}
        //fun onCellUpdated(cellId: Int, num: Int?, candidates: List<Int>?) {}
        fun onComplete(success: Boolean) {}
    }

    var difficulty = DIFFICULTY_UNDEF
    var callback: ProgressCallback? = null
    val cells = ArrayList<Cell>()
    val groups = ArrayList<Group>()

    init {
        repeat(81) {
            cells.add(Cell())
        }

        // 横方向のグループ
        for (i in 0 until 9) {
            val p = i * 9
            groups.add(
                Group(
                    setOf(
                        cells[p + 0],
                        cells[p + 1],
                        cells[p + 2],
                        cells[p + 3],
                        cells[p + 4],
                        cells[p + 5],
                        cells[p + 6],
                        cells[p + 7],
                        cells[p + 8]
                    )
                )
            )
        }

        // 縦方向のグループ
        for (i in 0 until 9) {
            groups.add(
                Group(
                    setOf(
                        cells[0 + i],
                        cells[9 + i],
                        cells[18 + i],
                        cells[27 + i],
                        cells[36 + i],
                        cells[45 + i],
                        cells[54 + i],
                        cells[63 + i],
                        cells[72 + i]
                    )
                )
            )
        }

        // 3x3のグループ
        for (y in 0 until 3) {
            for (x in 0 until 3) {
                val leftTop = y * 27 + x * 3
                groups.add(
                    Group(
                        setOf(
                            cells[leftTop + 0], cells[leftTop + 1], cells[leftTop + 2],
                            cells[leftTop + 9], cells[leftTop + 9 + 1], cells[leftTop + 9 + 2],
                            cells[leftTop + 18], cells[leftTop + 18 + 1], cells[leftTop + 18 + 2]
                        )
                    )
                )
            }
        }

        cells.forEach { cell ->
            // cellにグループを関連付ける
            cell.groups = groups.filter { group ->
                group.cells.contains(cell)
            }.toSet()

            // cellに自分自身を関連付ける
            cell.parent = this
        }
    }

    /**
     * 解決すべき問題をセットする
     *
     * @param numbers 各セルのあたい。未確定は0、確定していたら1~9
     * @throws IllegalArgumentException 0~9以外の数字を指定しようとすると発生
     */
    fun load(numbers: List<Int>) {
        if (numbers.size != cells.size) {
            throw IllegalArgumentException()
        }

        for (n in 0 until cells.size) {
            if (numbers[n] !in 0..9) {
                throw IllegalArgumentException()
            }
            cells[n].candidates = mutableSetOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            cells[n].value = numbers[n] // 1~9なら candidatesは空集合にセットされる
        }

        _isValid.postValue(calcIsValid())
    }

    /**
     * 数字の文字列から問題をセット
     * @param numbersStr 0~9の数字からなる 81文字の文字列
     */
    fun load(numbersStr: String) {
        val list = numbersStr.toCharArray().map { c -> c.code - '0'.code }
        if (list.size != 81) {
            throw IllegalArgumentException("format error")
        }
        load(list)
    }

    /**
     * 問題を最後まで自動で解く
     *
     */
    fun trySolve(): Boolean {
        //val algorithm = SolverV0(this, cells, groups, callback)
        val algorithm = SolverV1(this, cells, groups, callback)
        val algorithm_dfs = SolverDFS(this, cells, groups, callback)

        var retVal = algorithm.trySolve()

//        if (!retVal) {
//            difficulty = DIFFICULTY_EXTREME
//            retVal = algorithm_dfs.trySolve()
//            if (!retVal) {
//                difficulty = DIFFICULTY_IMPOSSIBLE
//            }
//        }

        callback?.onComplete(retVal)
        return retVal
    }

    /**
     * 問題は解決したか
     */
    fun isSolved(): Boolean {
        cells.forEach {
            if (!it.isFixed) {
                return false
            }
        }

        return true
    }

    // 数の配置が正しいか
    private val _isValid = MutableLiveData(false)

    // TODO private val _isValid = calcIsValid() みたいに書けない?

    val isValid: LiveData<Boolean> = _isValid

    internal fun calcIsValid(): Boolean {
        groups.forEach { group ->
            val numbers = mutableSetOf<Int>()
            group.cells.forEach { cell ->
                if (cell.value != 0 && numbers.contains(cell.value)) {
                    return false
                }
                numbers.add(cell.value)
            }
        }

        return true
    }

    /**
     * cellからのデータ変更通知を受ける
     */
    internal fun notifyDataChanged() {
        _isValid.postValue(calcIsValid())
    }
}
