package org.nunocky.sudokusolver.solver

import android.util.Log

class SolverDFS(
    private val parent: SudokuSolver,
    private val cells: ArrayList<Cell>,
    private val groups: ArrayList<Group>,
    private val callback: SudokuSolver.ProgressCallback?
) : SudokuSolver.Algorithm {
    companion object {
        private const val TAG = "DFS"
    }

    override fun trySolve(): Boolean {
        setup()
        return depthFirstSearch()
    }

    //private fun isSolved() = parent.isSolved()

    /**
     * 深さ優先探索による解決を試みる
     * TODO 探査順を変える。残り候補の少ないセルを先に処理すれば速くなるはず
     */

    private lateinit var cellList: List<Cell>
    private var maxDepth = 0

    private fun setup() {
        // 基本的なフィルタをかける (明らかに必要でない候補をへらす)
        for (cell in cells) {
            if (cell.isFixed) {
                sweepCandidateForCell(cell, cell.value)
            }
        }

        // 基本的なフィルタで残り候補1となったものは確定させておく
        for (cell in cells) {
            if (cell.candidates.size == 1) {
                cell.value = cell.candidates.first()
            }
        }

        // TODO sortedBy, sortedByDescendingとどちらが速いのだろう
        cellList = cells.filter { 0 < it.candidates.size }.sortedBy { it.candidates.size }
        Log.d(TAG, "${cellList.size}")

        maxDepth = 0
    }

    private fun depthFirstSearch(depth: Int = 0): Boolean {
        maxDepth = depth.coerceAtLeast(maxDepth)

        // 終端確認
        if (parent.isSolved()) {
            return true
        }

        val cell = cellList[depth]
        val candidatesBak = cell.candidates.toSet()

        val ary = cell.candidates.toIntArray()
        for (v in ary) {

            // 候補を置いてみる
            cell.value = v

            // 矛盾がなければ進む
            if (parent.calcIsValid()) {
                Log.d(
                    TAG,
                    "${depth}/${cellList.size} [$maxDepth]: set cell ${cell.id} (${cell.id % 9}, ${cell.id / 9}) to $v"
                )
                callback?.onProgress(cells)
                val solved = depthFirstSearch(depth + 1)

                if (solved) {
                    return true
                }
            }

            // だめならもとに戻して次の候補を試す
            cell.value = 0
            cell.candidates = candidatesBak.toMutableSet()
        }

        Log.d(TAG, "${depth}/${cellList.size} : fail")
        return false
    }

    /**
     * 確定セルをもとに不要な候補を除外する
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