package org.nunocky.sudokulib

class SolverEasy(
    private val sudokuSolver: SudokuSolver,
    private val cells: ArrayList<Cell>,
    private val callback: SudokuSolver.ProgressCallback?
) : SudokuSolver.Algorithm {
    companion object {
        private const val TAG = "Easy"
    }

    override fun trySolve(): Boolean {
        var shouldRepeat: Boolean
        do {
            callback?.onProgress(cells)
            shouldRepeat = false

            for (cell in cells) {
                if (cell.isFixed) {
                    val n = cell.value
                    sudokuSolver.groupOfCells(cell.id).forEach { g ->
                        g.cells.forEach { c ->
                            c.removeCandidate(n)

                            // そのセルの候補が残り1だったらそのセルは確定。そのときはもう一度このフィルタを繰り返す
                            if (c.candidates.size == 1) {
                                c.value = c.candidates.first()
                                shouldRepeat = true
                            }
                        }
                    }
                }
            }
        } while (shouldRepeat)

        callback?.onProgress(cells)
        return sudokuSolver.isSolved()
    }
}