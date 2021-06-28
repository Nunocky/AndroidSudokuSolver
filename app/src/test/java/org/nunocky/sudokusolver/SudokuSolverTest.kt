package org.nunocky.sudokusolver

import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.nunocky.sudokusolver.solver.Cell
import org.nunocky.sudokusolver.solver.SudokuSolver

class SudokuSolverTest {

    private lateinit var solver: SudokuSolver

    private val solverCallback = object : SudokuSolver.ProgressCallback {
        override fun onProgress(cells: List<Cell>) {
            val sb = StringBuffer()

            sb.appendLine("----")
            for (y in 0 until 9) {
                sb.appendLine(
                    String.format(
                        "%d %d %d %d %d %d %d %d %d",
                        cells[9 * y + 0].value,
                        cells[9 * y + 1].value,
                        cells[9 * y + 2].value,
                        cells[9 * y + 3].value,
                        cells[9 * y + 4].value,
                        cells[9 * y + 5].value,
                        cells[9 * y + 6].value,
                        cells[9 * y + 7].value,
                        cells[9 * y + 8].value,
                    )
                )
            }

            println(sb.toString())
        }
    }

    @Before
    fun setUp() {
        solver = SudokuSolver()
    }

    @Test
    fun testSolveEasy() {
        solver.setup(targetEasy)
        solver.callback = solverCallback

        val solved = solver.trySolve()
        assertTrue(solved)
    }

    @Test
    fun testSolveMedium() {
        solver.setup(targetMedium)
        solver.callback = solverCallback

        val solved = solver.trySolve()
        assertTrue(solved)
    }

    @Test
    fun testSolveHard() {
        solver.setup(targetHard)
        solver.callback = solverCallback

        val solved = solver.trySolve()
        assertTrue(solved)
    }


    @Test
    fun testSolveHardStep() {
        solver.setup(targetHard)
        solver.callback = solverCallback

        while (!solver.isSolved()) {
            val valueChanged = solver.execStep()
            if (!valueChanged) {
                break
            }
        }

        assertTrue(solver.isSolved())
    }

//    @Test
//    fun TestDepthFirstSearch() {
//        val solved = solver.depthFirstSearch(0)
//        assertTrue(solved)
//    }

    companion object {
        private val targetEasy = listOf(
            0, 0, 1, 0, 3, 7, 0, 2, 0,
            0, 0, 6, 0, 9, 0, 5, 3, 0,
            0, 9, 2, 0, 0, 0, 1, 7, 0,
            0, 0, 0, 6, 0, 3, 0, 8, 2,
            0, 0, 0, 9, 7, 8, 0, 0, 0,
            9, 8, 0, 2, 0, 1, 0, 0, 0,
            0, 1, 4, 0, 0, 0, 0, 8, 6,
            0, 3, 8, 0, 1, 0, 2, 0, 0,
            0, 6, 0, 3, 8, 0, 4, 0, 0
        )

        private val targetMedium = listOf(
            0, 0, 0, 0, 0, 8, 5, 0, 0,
            0, 0, 0, 6, 0, 9, 0, 1, 2,
            6, 7, 0, 1, 0, 0, 0, 4, 0,
            0, 0, 0, 7, 0, 0, 2, 0, 1,
            0, 3, 0, 0, 0, 0, 0, 5, 0,
            1, 0, 4, 0, 0, 5, 0, 0, 0,
            0, 1, 0, 0, 0, 7, 0, 2, 6,
            5, 8, 0, 4, 0, 2, 0, 0, 0,
            0, 0, 6, 3, 0, 0, 0, 0, 0
        )

        private val targetHard = listOf(
            0, 0, 6, 0, 3, 8, 0, 7, 5,
            0, 0, 0, 0, 4, 0, 0, 0, 0,
            0, 0, 0, 7, 9, 0, 2, 0, 0,
            0, 0, 5, 0, 0, 0, 0, 9, 0,
            0, 8, 9, 0, 0, 0, 3, 1, 0,
            0, 2, 0, 0, 0, 0, 4, 0, 0,
            0, 0, 8, 0, 6, 5, 0, 0, 0,
            0, 0, 0, 0, 8, 0, 0, 0, 0,
            5, 7, 0, 3, 2, 0, 9, 0, 0
        )
    }
}