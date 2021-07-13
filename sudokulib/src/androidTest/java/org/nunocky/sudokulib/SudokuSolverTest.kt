package org.nunocky.sudokulib

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.nunocky.sudokusolver.getOrAwaitValue
import java.io.*
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

    @Test
    fun importDatabase() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val currentDBPath: String = appContext.getDatabasePath("appDatabase").absolutePath

        // TODO 実装する
        fail()
    }

    @Test
    fun exportDatabase() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val currentDBPath: String = appContext.getDatabasePath("appDatabase").absolutePath

        arrayOf("").forEach { postfix ->
            val srcPath = currentDBPath + postfix
            val filename = File(srcPath).name
            FileInputStream(File(srcPath)).use { iStream ->
                FileOutputStream(
                    File(
                        appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                        filename
                    )
                ).use { oStream ->
                    var len = 0
                    val buffer = ByteArray(8 * 1024)
                    do {
                        len = iStream.read(buffer, 0, buffer.size)
                        if (0 < len) {
                            oStream.write(buffer, 0, len)
                        }
                    } while (0 < len)
                }
            }
        }
    }


    // TODO 解けない問題に対して正しい状態を返すテスト

    @Test
    fun testSolveEasy() {
        solver.load(targetEasy)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))

        val solved = solver.trySolve()
        assertTrue(solved)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))
    }

    @Test
    fun testSolveMedium() {
        solver.load(targetMedium)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))

        val solved = solver.trySolve()
        assertTrue(solved)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))
    }

    @Test
    fun testSolveHard() {
        solver.load(targetHard)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))

        val solved = solver.trySolve()
        assertTrue(solved)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))
    }

    @Test
    fun testDFS() {
//        solver.load(targetEasy)  // 解けた
//        solver.load(targetMedium)  // 解けた
        solver.load(targetHard) // 解けた
//        solver.load(targetExtreme) // 解けない (´・ω・｀)

        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))

        val solved = solver.trySolve(2)
        assertTrue(solved)
        assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))
    }

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

                solver.load(line)

                // 問題が間違っている可能性もある
                assertTrue(solver.isValid.getOrAwaitValue(100, TimeUnit.MILLISECONDS))

                solver.trySolve()

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

        private const val targetExtreme =
            "000080109" +
                    "902300040" +
                    "005901700" +
                    "100800067" +
                    "000000000" +
                    "520004001" +
                    "001007900" +
                    "060103508" +
                    "703090002"

    }
}