package org.nunocky.sudokulib

import junit.framework.TestCase
import org.junit.Before
import org.junit.Test

class SolverTest {
    private lateinit var solver: SudokuSolver

    @Before
    fun setUp() {
        solver = SudokuSolver().apply {
            callback = solverCallback
        }
    }

    @Test
    fun testSolveEasy() {
        solver.load(targetEasy)
        TestCase.assertTrue(solver.isValid)

        val solved = solver.trySolve(METHOD.ONLY_STANDARD)
        TestCase.assertTrue(solved)
        TestCase.assertTrue(solver.isValid)
    }

    @Test
    fun testSolveMedium() {
        solver.load(targetMedium)
        TestCase.assertTrue(solver.isValid)

        val solved = solver.trySolve(METHOD.ONLY_STANDARD)
        TestCase.assertTrue(solved)
        TestCase.assertTrue(solver.isValid)
    }

    @Test
    fun testSolveHard() {
        solver.load(targetHard)
        TestCase.assertTrue(solver.isValid)

        val solved = solver.trySolve(METHOD.STANDARD_AND_DFS)
        TestCase.assertTrue(solved)
        TestCase.assertTrue(solver.isValid)
    }

    @Test
    fun testDFS() {
        solver.load(targetEasy)
        TestCase.assertTrue(solver.isValid)

        val solved = solver.trySolve(METHOD.ONLY_DFS)
        TestCase.assertTrue(solved)
        TestCase.assertTrue(solver.isValid)
    }

    @Test
    fun testDFS2() {
        solver.load(targetMedium)
        TestCase.assertTrue(solver.isValid)

        val solved = solver.trySolve(METHOD.ONLY_DFS)
        TestCase.assertTrue(solved)
        TestCase.assertTrue(solver.isValid)
    }

    @Test
    fun testDFS3() {
        solver.load(targetHard)
        TestCase.assertTrue(solver.isValid)

        val solved = solver.trySolve(METHOD.ONLY_DFS)
        TestCase.assertTrue(solved)
        TestCase.assertTrue(solver.isValid)
    }

    @Test
    fun testNoSolution() {
        solver.load(targetNoSolution)
        TestCase.assertTrue(solver.isValid)

        val solved = solver.trySolve(METHOD.STANDARD_AND_DFS)
        TestCase.assertFalse(solved)
//        TestCase.assertTrue(solver.isValid)
    }

    @Test
    fun testDFS5() {
        solver.load(
            "008960040" +
                    "409000003" +
                    "561370098" +
                    "190753004" +
                    "000210000" +
                    "000000507" +
                    "984500760" +
                    "000049080" +
                    "013820400"
        )
        TestCase.assertTrue(solver.isValid)

        val solved = solver.trySolve(METHOD.ONLY_DFS)
        TestCase.assertTrue(solved)
        TestCase.assertTrue(solver.isValid)
    }

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
}

private const val targetEasy =
    "001037020" +
            "006090530" +
            "092000170" +
            "000603082" +
            "000978000" +
            "980201000" +
            "014000860" +
            "038010200" +
            "060380400"

private const val targetMedium =
    "000008500" +
            "000609012" +
            "670100040" +
            "000700201" +
            "030000050" +
            "104005000" +
            "010007026" +
            "580402000" +
            "006300000"

private const val targetHard =
    "006038075" +
            "000040000" +
            "000790200" +
            "005000090" +
            "089000310" +
            "020000400" +
            "008065000" +
            "000080000" +
            "570320900"

private const val targetNoSolution =
    "000080109" +
            "902300040" +
            "005901700" +
            "100800067" +
            "000000000" +
            "520004001" +
            "001007900" +
            "060103508" +
            "703090002"
