package org.nunocky.sudokulib

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.*

private const val TAG = "SudokuSolverTest"

@RunWith(AndroidJUnit4::class)
class SudokuSolverTest {

    // ユニットテスト内で LiveDataを評価する
    // http://y-anz-m.blogspot.com/2018/06/livedata-unittest.html
    @get:Rule
    val rule = InstantTaskExecutorRule()

    //    private lateinit var appContext: Context
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
//        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        solver = SudokuSolver().apply {
            callback = solverCallback
        }
    }

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

                solver.load(line)

                // 問題が間違っている可能性もある
                assertTrue(solver.isValid)

                solver.trySolve()

                // 解決した、かつ配置が正当であることを確認
                assertTrue(solver.isSolved())
                assertTrue(solver.isValid)
            } while (!done)
        }
    }
}