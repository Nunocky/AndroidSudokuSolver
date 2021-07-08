package org.nunocky.sudokusolver.solver

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

        for (group in groups) {
            for (n in 2..7) {
                mediumFilters.add(FilterCombination(this, group, n))
            }
        }

        // TODO add hard filters
        // valueChanged = valueChanged or filterGroupCellX2(cell.groups)
    }

    // TODO : 不正な解析を行ったら例外を発生させる
    override fun trySolve(): Boolean {

        for (difficulty in 0..2) {
            parent.difficulty = difficulty + SudokuSolver.DIFFICULTY_EASY

            var valueChanged = true

            while (!isSolved() && valueChanged) {
                valueChanged = false

                var shouldRepeat: Boolean

                // EASY
                shouldRepeat = true
                do {
                    for (filter in basicFilters) {
                        shouldRepeat = filter.exec()
                        valueChanged = valueChanged or shouldRepeat

                        callback?.onProgress(cells)
                        if (!parent.calcIsValid()) {
                            return false
                        }
                    }
                } while (shouldRepeat)

                // MEDIUM
                if (0 < difficulty) {
                    for (filter in mediumFilters) {
                        valueChanged = valueChanged or filter.exec()

                        callback?.onProgress(cells)
                        if (!parent.calcIsValid()) {
                            return false
                        }
                    }
                }

                // HARD
                if (1 < difficulty) {
                    for (filter in hardFilters) {
                        valueChanged = valueChanged or filter.exec()

                        callback?.onProgress(cells)
                        if (!parent.calcIsValid()) {
                            return false
                        }
                    }
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


// /**
// * あるセルに対して、関連する3つのグループにおいて
// *   + 候補 nを持つセルが1つだけ
// * が共通している
// */
//    private fun filterGroupCellX2(cell: Cell): Boolean {
//        var changed = false
//
//        val ary = IntArray()
//        for (g in cell.groups) {
//            for (n in 1..9) {
//                val tmpCells = groups.flatMap { it.cells }
//
//                if (tmpCells.size == 1) {
//                    val c = tmpCells.first()
//                    fixCell(c, n)
//                    changed = true
//                    ary.put(n)
//                }
//            }
//        }
//
//        return changed
//    }


