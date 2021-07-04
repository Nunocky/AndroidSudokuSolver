package org.nunocky.sudokusolver

import android.content.Context
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.nunocky.sudokusolver.solver.Cell
import org.nunocky.sudokusolver.solver.SudokuSolver
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SudokuSolverTest {

    // ユニットテスト内で LiveDataを評価する
    // http://y-anz-m.blogspot.com/2018/06/livedata-unittest.html
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var appContext: Context
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

            Log.d(TAG, sb.toString())
        }
    }

    @Before
    fun setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        solver = SudokuSolver().apply {
            callback = solverCallback
        }
    }

    // TODO 解けない問題に対して正しい状態を返すテスト

    @Test
    fun testSolveEasy() {
        solver.setup(targetEasy)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))

        val solved = solver.trySolve()
        assertTrue(solved)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))
    }

    @Test
    fun testSolveMedium() {
        solver.setup(targetMedium)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))

        val solved = solver.trySolve()
        assertTrue(solved)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))
    }

    @Test
    fun testSolveHard() {
        solver.setup(targetHard)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))

        val solved = solver.trySolve()
        assertTrue(solved)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))
    }


    @Test
    fun testSolveHardStep() {
        solver.setup(targetHard)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))

        while (!solver.isSolved()) {
            val valueChanged = solver.execStep()
            if (!valueChanged) {
                break
            }
            assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))
        }

        assertTrue(solver.isSolved())
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))
    }

//    @Test
//    fun TestDepthFirstSearch() {
//        val solved = solver.depthFirstSearch(0)
//        assertTrue(solved)
//    }

    /**
     * アセット内のテキストファイルに格納された問題をすべて解く
     */
    @Test
    fun testSolveFile() {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        ctx.assets.open("testData.txt").use { iStream ->

            val streamReader = BufferedReader(InputStreamReader(iStream))

            var done = false
            var line: String?
            do {
                line = streamReader.readLine()
                if (line == null) {
                    done = true
                    continue
                }

                if (line.startsWith("#") || line.isBlank()) {
                    // #で始まる行はコメント -> スキップ
                    // 空行も無視
                    continue
                }

                if (line.length != 81) {
                    Log.d(TAG, "$line is not a valid text, skip")
                    continue
                }

                solver.setup(line)

                // 問題が間違っている可能性もある
                assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))

                val sw = true // 自動で解くかステップ実行するか

                if (sw) {
                    solver.trySolve()
                } else {
                    while (!solver.isSolved()) {
                        val valueChanged = solver.execStep()
                        if (!valueChanged) {
                            break
                        }

                        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))
                    }

                    if (!solver.isSolved()) {
                        // 深さ優先探索
                        solver.depthFirstSearch()
                    }
                }

                // 解決した、かつ配置が正当であることを確認
                assertTrue(solver.isSolved())
                assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))
            } while (!done)
        }
    }

    companion object {
        private const val TAG = "SudokuSolverTest"
        private const val targetEasy =
            "001037020006090530092000170000603082000978000980201000014000860038010200060380400"

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
    }
}