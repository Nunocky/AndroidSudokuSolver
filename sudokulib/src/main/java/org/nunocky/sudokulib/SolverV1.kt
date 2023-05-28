package org.nunocky.sudokulib

import java.util.*

class SolverV1(
    private val parent: SudokuSolver,
    private val cells: ArrayList<Cell>,
    private val groups: ArrayList<Group>,
    private val callback: SudokuSolver.ProgressCallback?
) : SudokuSolver.Algorithm {

    private fun isSolved() = parent.isSolved()

    private val basicFilters = ArrayList<SudokuFilter>()
    private val mediumFilters = ArrayList<SudokuFilter>()
    private val hardFilters = ArrayList<SudokuFilter>()

    init {
        for (cell in cells) {
            basicFilters.add(Filter0(this, cell))
        }

        for (cell in cells) {
            basicFilters.add(FilterOneCandidate(this, cell))
        }

        for (group in groups) {
            basicFilters.add(FilterLastOneCellInGroup(this, group))
        }

        mediumFilters.add(Filter3Lines(this, listOf(groups[0], groups[1], groups[2])))
        mediumFilters.add(Filter3Lines(this, listOf(groups[3], groups[4], groups[5])))
        mediumFilters.add(Filter3Lines(this, listOf(groups[6], groups[7], groups[8])))
        mediumFilters.add(Filter3Lines(this, listOf(groups[9], groups[10], groups[11])))
        mediumFilters.add(Filter3Lines(this, listOf(groups[12], groups[13], groups[14])))
        mediumFilters.add(Filter3Lines(this, listOf(groups[15], groups[16], groups[17])))

        for (group in groups) {
            for (n in 2..7) {
                hardFilters.add(FilterCombination(this, group, n))
            }
        }
    }

    private fun execEasyFilter(): Boolean {
        var changed = false
        var shouldRepeat: Boolean
        shouldRepeat = true
        do {
            for (filter in basicFilters) {
                shouldRepeat = filter.exec()
                changed = changed or shouldRepeat

                if (!parent.isValid) {
                    throw SudokuSolver.SolverError()
                }
            }
        } while (shouldRepeat)

        callback?.onProgress(cells)
        return changed
    }

    override fun trySolve(): Boolean {

        for (difficulty in arrayOf(DIFFICULTY.MEDIUM, DIFFICULTY.HARD)) {
            parent.difficulty = difficulty

            var valueChanged = true
            var shouldRefresh: Boolean

            while (!isSolved() && valueChanged) {
                valueChanged = false

                // EASY
//                execEasyFilter()

                // MEDIUM
                if (DIFFICULTY.EASY < difficulty) {
                    shouldRefresh = true
                    for (filter in mediumFilters) {
                        if (shouldRefresh) {
                            execEasyFilter()
                        }
                        shouldRefresh = filter.exec()
                        valueChanged = valueChanged or shouldRefresh

                        if (!parent.isValid) {
                            throw SudokuSolver.SolverError()
                        }
                    }
                    callback?.onProgress(cells)
                }

                // HARD
                if (DIFFICULTY.MEDIUM < difficulty) {
                    shouldRefresh = true
                    for (filter in hardFilters) {
                        if (shouldRefresh) {
                            execEasyFilter()
                        }
                        shouldRefresh = filter.exec()
                        valueChanged = valueChanged or shouldRefresh

                        if (!parent.isValid) {
                            throw SudokuSolver.SolverError()
                        }
                    }
                    callback?.onProgress(cells)
                }

                if (isSolved()) {
                    return true
                }
            }

        }

        return isSolved()
    }
}

/**
 * フィルタの基本クラス
 */
private abstract class SudokuFilter(protected val parent: SolverV1) {
    abstract fun exec(): Boolean
}

/**
 * 対象セルの値が nに確定していたら、そのセルが所属する他グループの未確定セルから要素 nを削除する
 */
private class Filter0(parent: SolverV1, private val cell: Cell) : SudokuFilter(parent) {
    override fun exec(): Boolean {
        var changed = false

        var shouldRepeat: Boolean
        do {
            shouldRepeat = false
            if (cell.isFixed) {
                val n = cell.value
                cell.groups.forEach { g ->
                    g.cells.forEach { c ->
                        if (c.candidates.contains(n)) {
                            c.candidates.remove(n)
                            changed = true
                        }

                        // そのセルの候補が残り1だったらそのセルは確定。そのときはもう一度このフィルタを繰り返す
                        if (c.candidates.size == 1) {
                            c.value = c.candidates.first()
                            shouldRepeat = true
                        }
                    }
                }
            }
        } while (shouldRepeat)
        return changed
    }
}

/**
 * あるセルについて、候補が一つだけのときは値が確定する
 */
private class FilterOneCandidate(parent: SolverV1, private val cell: Cell) : SudokuFilter(parent) {
    override fun exec(): Boolean {
        var changed = false

        if (cell.candidates.size == 1) {
            cell.value = cell.candidates.first()
            changed = true
        }
        return changed
    }
}

/**
 * あるグループについて、グループ内で未確定のセルが一つだけのとき、そのセルの値は確定する
 */
private class FilterLastOneCellInGroup(parent: SolverV1, private val group: Group) :
    SudokuFilter(parent) {
    override fun exec(): Boolean {
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
                changed = true
            }
        }

        return changed
    }
}

/**
 *  あるグループにおいて、同一の n個の候補を持つセルが n個存在するなら、それら
 *  のセルでその候補値を専有できる → グループ内のそれら以外のセルで、候補値は除外できる
 */
private class FilterCombination(parent: SolverV1, private val group: Group, private val n: Int) :
    SudokuFilter(parent) {
    override fun exec(): Boolean {
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
            }
        }

        return changed
    }
}

/**
 * 数字 n に対して、与えられた3つのグループについて
 * + 他の2列で nが確定している
 * + 残りの列に候補 n のセルがただ一つだけ存在する
 * 上記を満たすとき、そのセルは nで確定する
 */
private class Filter3Lines(
    parent: SolverV1,
    private val group: List<Group>
) :
    SudokuFilter(parent) {
    override fun exec(): Boolean {
        arrayOf(
            Triple(0, 1, 2),
            Triple(1, 2, 0),
            Triple(2, 0, 1),
        ).forEach { t ->
            for (n in 1..9) {
                val x = group[t.first].cells.any { it.value == n }
                val y = group[t.second].cells.any { it.value == n }
                val z = group[t.third].cells.filter { it.candidates.contains(n) }

                if (x and y && z.size == 1) {
                    z.first().value = n
                    return true
                }
            }
        }

        return false
    }
}

