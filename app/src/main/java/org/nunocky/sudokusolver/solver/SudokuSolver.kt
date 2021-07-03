package org.nunocky.sudokusolver.solver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SudokuSolver {

    interface ProgressCallback {
        fun onProgress(cells: List<Cell>)
        fun onFocusGroup(groupIndex: Int) {}
        fun onUnfocusGroup(groupIndex: Int) {}
        //fun onCellFocused(cellId: Int) {}
        //fun onCellUnfocused(cellId: Int) {}
        //fun onSelectGroup(groupId: Int) {}
        //fun onUnselectGroup(groupId: Int) {}
        //fun onCellUpdated(cellId: Int, num: Int?, candidates: List<Int>?) {}
    }

    var callback: ProgressCallback? = null
    val cells = ArrayList<Cell>()
    private val groups = ArrayList<Group>()

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
    fun setup(numbers: List<Int>) {
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
    fun setup(numbersStr: String) {
        val list = numbersStr.toCharArray().map { c -> c.code - '0'.code }
        if (list.size != 81) {
            throw IllegalArgumentException("format error")
        }
        setup(list)
    }

    /**
     * 問題を最後まで自動で解く
     *
     */
    fun trySolve(): Boolean {
        var n = 0
        while (!isSolved()) {
            val valueChanged = execStep()
            n += 1
            if (!valueChanged) {
                break
            }
        }

        if (!isSolved()) {
            // 深さ優先探索
            depthFirstSearch()
        }

        return isSolved()
    }

    /**
     * 試行
     */
    fun execStep(): Boolean {
        var valueChanged = false

        // 基本フィルタ (確定候補をもとにふるい落とす)
        valueChanged = valueChanged or filter0()

        // TODO onFocusGroup, onUnfocusGroupの実装。 フォーカス時の背景色設定
        groups.forEachIndexed() { index, g ->
//            callback?.onFocusGroup(index)
            for (n in 2..4) {
                valueChanged = valueChanged or filterCombination(g, n)
            }
            valueChanged = valueChanged or filterLastOne(g)
//            callback?.onUnfocusGroup(index)
        }

        groups.forEach { g ->
            valueChanged = valueChanged or filterLastOne(g)
        }

        cells.forEach { cell ->
            valueChanged = valueChanged or filterOneCandidate(cell)
        }

        callback?.onProgress(cells)
        return valueChanged
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

    /**
     * あるセルの要素が確定しているとき、そのセルが属するすべてのグループでそのセルの値は候補から外れる
     */
    private fun filter0(): Boolean {
        var valueChanged = false
        for (cell in cells) {
            if (cell.isFixed) {
                continue
            }

            // cellが所属しているグループの各セルについて、確定していたらそのセルの値を候補から除外
            for (group in cell.groups) {
                for (c in group.cells) {
                    if (c.value == 0) {
                        continue
                    }

                    // TODO このif文の判定はなくても良いかも。直接 removeで良くない?
//                    if (cell.candidates.contains(c.value)) {
                    cell.candidates.remove(c.value)
//                    }
                }
            }

            // 確定したら更新
            if (cell.candidates.count() == 1) {
                cell.value = cell.candidates.first()
                valueChanged = true
            }
        }

        return valueChanged
    }

    /**
     *  同一の n個の候補を持つセルがグループ内で n個存在するなら、それら
     *  のセルでその候補値を専有できる → グループ内のそれら以外のセルで、候補値は除外できる
     */
    private fun filterCombination(group: Group, n: Int): Boolean {
        var valueChanged = false

        for (i in 0 until 9) {
            val cellAry = mutableSetOf<Cell>()

            if (group.cells.elementAt(i).candidates.size != n) {
                continue
            }

            // 最初のペア発見
            cellAry.add(group.cells.elementAt(i))

            // 2個目以降のペアを見つける
            for (j in i + 1 until 9) {
                val c = group.cells.elementAt(j)
                if (c.candidates == cellAry.first().candidates) {
                    cellAry.add(c)
                }
            }

            // ペアが n個のときだけ処理
            if (cellAry.size != n) {
                continue
            }

            for (cell in group.cells) {
                if (cellAry.contains(cell)) {
                    continue
                }

                cellAry.first().candidates.forEach { v ->
                    cell.candidates.remove(v)
                }

                valueChanged = valueChanged or filterOneCandidate(cell)
            }
        }

        return valueChanged
    }

    /**
     * 候補が一つだけのセルは値が確定する
     */
    private fun filterOneCandidate(cell: Cell): Boolean {
        var valueChanged = false

        if (cell.candidates.size == 1) {
            cell.value = cell.candidates.first()
            valueChanged = true
        }

        return valueChanged
    }

    /**
     * グループ内で未確定の値が一つだけのとき、未確定のセルはその値で確定する
     */
    private fun filterLastOne(group: Group): Boolean {
        var valueChanged = false

        var unFixedCell: Cell? = null
        val candidates = mutableSetOf(1, 2, 3, 4, 5, 6, 7, 8, 9)

        for (cell in group.cells) {
            if (cell.isFixed) {
                candidates.remove(cell.value)
            } else {
                unFixedCell = cell
            }
        }

        if (candidates.size == 1) {
            unFixedCell?.value = candidates.first()
            valueChanged = true
        }

        return valueChanged
    }

    /**
     * 深さ優先探索による解決を試みる
     */
    fun depthFirstSearch(n: Int = 0): Boolean {

        // すべての Cellが fixed なら解決
        if (cells.filter { it.isFixed }.size == cells.size) {
            return true
        }

        val cell = cells[n]
        val candidatesBak = cell.candidates.toSet()

        // fixedなら進む
        if (cell.isFixed) {
            callback?.onProgress(cells)
            return depthFirstSearch(n + 1)
        }

        // 候補を置いてみて矛盾がなければ進む
        for (v in cell.candidates) {
            cell.value = v
            if (calcIsValid()) {
                callback?.onProgress(cells)
                val solved = depthFirstSearch(n + 1)
                if (solved) {
                    callback?.onProgress(cells)
                    return true
                }
            }
        }

        // どの候補も当てはまらなかったので状態を元に戻してfalseを返す
        cell.value = 0
        cell.candidates = candidatesBak.toMutableSet()
        return false
    }

    // 数の配置が正しいか
    private val _isValid = MutableLiveData(false)
    val isValid: LiveData<Boolean> = _isValid

    private fun calcIsValid(): Boolean {
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
