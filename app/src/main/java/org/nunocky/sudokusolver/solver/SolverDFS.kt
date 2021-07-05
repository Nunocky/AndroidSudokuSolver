package org.nunocky.sudokusolver.solver

class SolverDFS(
    private val parent: SudokuSolver,
    private val cells: ArrayList<Cell>,
    private val groups: ArrayList<Group>,
    private val callback: SudokuSolver.ProgressCallback?
) : SudokuSolver.Algorithm {

    override fun trySolve(): Boolean {
        return depthFirstSearch()
    }

    /**
     * 深さ優先探索による解決を試みる
     * TODO 探査順を変える。残り候補の少ないセルを先に処理すれば速くなるはず
     */
    private fun depthFirstSearch(n: Int = 0): Boolean {
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
            if (parent.calcIsValid()) {
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

}