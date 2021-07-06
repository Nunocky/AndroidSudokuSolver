package org.nunocky.sudokusolver.solver

import java.util.*

class SolverV1(
    private val parent: SudokuSolver,
    private val cells: ArrayList<Cell>,
    private val groups: ArrayList<Group>,
    private val callback: SudokuSolver.ProgressCallback?
) : SudokuSolver.Algorithm {

    override fun trySolve(): Boolean {
        // 最も基本的なフィルタ。 レベルは EASY
        for (difficulty in 1..3) {
            if (parent.isSolved()) {
                break
            }
            parent.difficulty = difficulty

            var valueChanged = true

            while (!parent.isSolved() && valueChanged) {
                valueChanged = false

                // EASY
                cells.forEach { cell ->
                    valueChanged = valueChanged or filter0(cell)
                }

                cells.forEach { cell ->
                    valueChanged = valueChanged or filterOneCandidate(cell)
                }

                groups.forEach {
                    valueChanged = valueChanged or filterLastOneCellInGroup(it)
                }

                // TODO このフィルタにバグ
                if (1 < difficulty) {
                    groups.forEach {
                        valueChanged = valueChanged or filterGroupCellX(it)
                    }
                }

                // Combination filter
                if (2 < difficulty) {
                    for (group in groups) {
                        for (n in 2..4) {
                            valueChanged = valueChanged or filterCombination(group, n)
                        }
                    }
                }

                callback?.onProgress(cells)

                // 解析に間違いがあったら停止
                if (!parent.calcIsValid()) {
                    return false
                }
            }

            if (parent.isSolved()) {
                return true
            }
        }

        return parent.isSolved()
    }

    /**
     * あるセルの要素が確定しているとき、そのセルが属するすべてのグループでそのセルの値は候補から外れる
     */
    private fun filter0(cell: Cell): Boolean {
        if (!cell.isFixed) {
            return false
        }

        var changed = false
        val fixedNum = cell.value

        // cellが所属しているグループの各セルについて、確定していなければそのセルの候補から除外
        cell.groups.forEach { group ->
            for (c in group.cells) {
                if (c.candidates.contains(fixedNum)) {
                    c.candidates.remove(fixedNum)
                    changed = true
                }
            }
        }

        return changed
    }

    /**
     * あるセルについて、候補が一つだけのときは値が確定する
     */
    private fun filterOneCandidate(cell: Cell): Boolean {
        var changed = false

        if (cell.candidates.size == 1) {
            cell.value = cell.candidates.first()
            changed = true
        }

        return changed
    }

    /**
     * あるグループについて、グループ内で未確定のセルが一つだけのとき、そのセルの値は確定する
     */
    private fun filterLastOneCellInGroup(group: Group): Boolean {
        var changed = false

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
            changed = true
        }

        return changed
    }

    /**
     * あるグループにおいて、 候補 nを持つセルが1つしかない → そのセルの値は nで確定する
     */
    private fun filterGroupCellX(group: Group): Boolean {
        var changed = false

        // TODO ここにバグがある
        for (n in 1..9) {
            val tmpCells = ArrayList<Cell>()
            for (cell in group.cells) {
                if (cell.candidates.contains(n)) {
                    tmpCells.add(cell)
                }
            }

            if (tmpCells.count() == 1) {
                tmpCells.first().value = n
                changed = true
            }
        }

        return changed
    }

    /**
     *  あるグループにおいて、同一の n個の候補を持つセルが n個存在するなら、それら
     *  のセルでその候補値を専有できる → グループ内のそれら以外のセルで、候補値は除外できる
     */
    private fun filterCombination(group: Group, n: Int): Boolean {
        var changed = false

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

                changed = changed or filterOneCandidate(cell)
            }
        }

        return changed
    }
}