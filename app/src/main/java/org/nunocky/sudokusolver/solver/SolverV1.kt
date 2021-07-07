package org.nunocky.sudokusolver.solver

import java.util.*

class SolverV1(
    private val parent: SudokuSolver,
    private val cells: ArrayList<Cell>,
    private val groups: ArrayList<Group>,
    private val callback: SudokuSolver.ProgressCallback?
) : SudokuSolver.Algorithm {

    private fun isSolved() = parent.isSolved()

    override fun trySolve(): Boolean {
        for (difficulty in 1..3) {

            if (isSolved()) {
                break
            }

            parent.difficulty = difficulty + SudokuSolver.DIFFICULTY_UNDEF

            var valueChanged = true

            while (!isSolved() && valueChanged) {
                valueChanged = false

                // EASY
                cells.forEach { cell ->
                    valueChanged = valueChanged or filter0(cell)
                }
                callback?.onProgress(cells)

                cells.forEach { cell ->
                    valueChanged = valueChanged or filterOneCandidate(cell)
                }
//                callback?.onProgress(cells)

                groups.forEach {
                    valueChanged = valueChanged or filterLastOneCellInGroup(it)
                }
//                callback?.onProgress(cells)

                // MEDIUM
                if (1 < difficulty) {
                    groups.forEach { g ->
                        valueChanged = valueChanged or filterGroupCellX(g)
                    }
                }
//                callback?.onProgress(cells)

                // HARD : Combination filter
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

            if (isSolved()) {
                return true
            }
        }

        return isSolved()
    }

    /**
     * あるセルの要素が確定しているとき、そのセルが属するすべてのグループでそのセルの値は候補から外れる
     */
    private fun filter0(cell: Cell): Boolean {
        var changed = false

        if (cell.isFixed) {
            changed = changed or sweepCandidateForCell(cell, cell.value)
        }

        return changed
    }

    /**
     * あるセルについて、候補が一つだけのときは値が確定する
     */
    private fun filterOneCandidate(cell: Cell): Boolean {
        var changed = false

        if (cell.candidates.size == 1) {
            val fixedNum = cell.candidates.first()
            cell.value = fixedNum
            sweepCandidateForCell(cell, fixedNum)

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
            unFixedCell?.let { cell ->
                val fixedNum = candidates.first()
                cell.value = fixedNum
                sweepCandidateForCell(cell, fixedNum)
                changed = true
            }
        }

        return changed
    }

    /**
     * あるグループにおいて、 候補 nを持つセルが1つしかない → そのセルの値は nで確定する
     */
    private fun filterGroupCellX(group: Group): Boolean {
        var changed = false

        for (n in 1..9) {
            val tmpCells = group.cells.filter {
                it.candidates.contains(n)
            }

            if (tmpCells.size == 1) {
                val c = tmpCells.first()
                c.value = n
                changed = changed or sweepCandidateForCell(c, n)
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
                    if (cell.candidates.contains(v)) {
                        cell.candidates.remove(v)
                        changed = true
                    }
                }

                changed = changed or filterOneCandidate(cell)
            }
        }

        return changed
    }

    /**
     * cellの所属するグループに対して、すべてのセルから候補 nを取り除く
     */
    private fun sweepCandidateForCell(cell: Cell, n: Int): Boolean {
        var changed = false
        for (g in cell.groups) {
            for (c in g.cells) {
                if (c == cell) {
                    continue
                }

                if (c.candidates.contains(n)) {
                    c.candidates.remove(n)
                    changed = true
                }
            }
        }

        return changed
    }
}