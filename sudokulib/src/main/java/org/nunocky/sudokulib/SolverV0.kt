package org.nunocky.sudokulib

class SolverV0(
    private val parent: SudokuSolver,
    private val cells: ArrayList<Cell>,
    private val groups: ArrayList<Group>,
    private val callback: SudokuSolver.ProgressCallback?
) : SudokuSolver.Algorithm {

    override fun trySolve(): Boolean {
        var n = 0
        while (!parent.isSolved()) {
            val valueChanged = execStep()
            n += 1
            if (!valueChanged) {
                break
            }
        }

//        if (!parent.isSolved()) {
//            // 深さ優先探索
//            depthFirstSearch()
//        }

        val result = parent.isSolved()
//        callback?.onComplete(result)
        return result
    }

    /**
     * 試行
     */
    private fun execStep(): Boolean {
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

                    cell.candidates.remove(c.value)
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

//    /**
//     * 深さ優先探索による解決を試みる
//     * TODO DFSを分離する
//     * TODO 探査順を変える。残り候補の少ないセルを先に処理すれば速くなるはず
//     */
//    private fun depthFirstSearch(n: Int = 0): Boolean {
//        // すべての Cellが fixed なら解決
//        if (cells.filter { it.isFixed }.size == cells.size) {
//            return true
//        }
//
//        val cell = cells[n]
//        val candidatesBak = cell.candidates.toSet()
//
//        // fixedなら進む
//        if (cell.isFixed) {
//            callback?.onProgress(cells)
//            return depthFirstSearch(n + 1)
//        }
//
//        // 候補を置いてみて矛盾がなければ進む
//        for (v in cell.candidates) {
//            cell.value = v
//            if (parent.calcIsValid()) {
//                callback?.onProgress(cells)
//                val solved = depthFirstSearch(n + 1)
//                if (solved) {
//                    callback?.onProgress(cells)
//                    return true
//                }
//            }
//        }
//
//        // どの候補も当てはまらなかったので状態を元に戻してfalseを返す
//        cell.value = 0
//        cell.candidates = candidatesBak.toMutableSet()
//        return false
//    }
}