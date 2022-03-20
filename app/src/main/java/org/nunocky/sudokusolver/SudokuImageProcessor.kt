package org.nunocky.sudokusolver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.*
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream

class SudokuImageProcessor constructor(
    context: Context
) {
    class BoardNotFound : Exception()

    /**
     * PyTorch Mobileのモデル
     */
    private val ptModel by lazy {
        LiteModuleLoader.load(
            assetFilePath(
                context,
                "mobile_model.ptl"
            )
        )
    }

    /**
     * アセットからファイルにコピーしてそのパスを返す
     */
    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
            return file.absolutePath
        }
    }

    /**
     * 数独ボード画像の解析処理
     *
     * @param bmp 数独ボードの写っている画像
     * @return 成功時、 正規化されたボード画像と解析結果文字列の Pair、失敗したら BoardNotFound
     */
    fun process(bmp: Bitmap): Result<Pair<Bitmap, String>> {
        val strBuffer = StringBuffer()
        // 数独ボードの正規化
        val board = normalizeSudokuBoard(bmp) ?: return Result.failure(BoardNotFound())

        // 9x9のマス目に切り出して解析
        for (y in 0 until 9) {
            for (x in 0 until 9) {
                val cellBitMap =
                    Bitmap.createBitmap(board, x * 64, y * 64, 64, 64)
                val num = readCellNumber(cellBitMap)
                strBuffer.append("$num")
            }
        }
        return Result.success(board to strBuffer.toString())
    }

    private fun normalizeSudokuBoard(bmp: Bitmap): Bitmap? {

        val srcBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true)

        // ARGB -> HSVに変換
        val srcMat = Mat()
        Utils.bitmapToMat(srcBitmap, srcMat)

        val hsvMat = Mat()
        cvtColor(srcMat, hsvMat, COLOR_RGB2HSV) //  COLOR_BGR2HSV)

        val gaussMat = Mat()
        GaussianBlur(hsvMat, gaussMat, Size(9.0, 9.0), 3.0)

        // S成分だけ見る
        val hsvMatArray = mutableListOf<Mat>()
        Core.split(gaussMat, hsvMatArray)

        val sMat = hsvMatArray[1]

        // 二値化
        val binMat = Mat()
        threshold(sMat, binMat, 40.0, 255.0, Imgproc.THRESH_BINARY)

        // 物体検出、頂点数の多い順に並べ替え
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        findContours(
            binMat,
            contours,
            hierarchy,
            RETR_LIST,
            CHAIN_APPROX_NONE
        )
        contours.sortByDescending { it.total() }

        // 一番大きい矩形をボードと判断する
        val biggestArea = contours[0]

        val squares = mutableListOf<MatOfPoint2f>()
        findSquares(biggestArea, squares)

        if (squares.size == 0) {
            return null
        }

        val s = squares[0]
        val approxf1 = MatOfPoint()
        s.convertTo(approxf1, CvType.CV_32S);

        val contourTemp = mutableListOf<MatOfPoint>()
        contourTemp.add(approxf1)

        val frameRect = sortFrameRect(squares[0])

        val boardImageSize = 640 - 64.0

        // TODO LeftTop, RightTop, RightBottom, LeftBottomの順に修正
        val transformDstRect = MatOfPoint2f(
            Point(boardImageSize, 0.0),
            Point(0.0, 0.0),
            Point(0.0, boardImageSize),
            Point(boardImageSize, boardImageSize)
        )
        val projectMatrix = getPerspectiveTransform(frameRect, transformDstRect)

        val warpedMat = Mat()

        warpPerspective(
            binMat,
            warpedMat,
            projectMatrix,
            Size(boardImageSize, boardImageSize)
        )

        val warpedBmp = Bitmap.createBitmap(
            boardImageSize.toInt(),
            boardImageSize.toInt(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(warpedMat, warpedBmp)

        return warpedBmp
    }

    private fun findSquares(
        contours: MatOfPoint,
        squares: MutableList<MatOfPoint2f>,
        areaThreshold: Int = 1000
    ) {
        squares.clear()
        val curve = MatOfPoint2f(*contours.toArray())

        val arcLen = arcLength(curve, true)
        val approx = MatOfPoint2f()
        approxPolyDP(curve, approx, arcLen * 0.02, true)

        val area = kotlin.math.abs(contourArea(approx))

        val sz = approx.size().area()
        val convex = isContourConvex(MatOfPoint(*approx.toArray()))
        if (sz == 4.0 && areaThreshold < area && convex) {
            var maxCosine = 0.0
            for (j in 2 until 5) {
                val pt1 = approx.get(j % 4, 0)
                val pt2 = approx.get(j - 2, 0)
                val pt0 = approx.get(j - 1, 0)
                val cosine = kotlin.math.abs(angle(pt1, pt2, pt0))
                maxCosine = maxCosine.coerceAtLeast(cosine)
            }

            if (maxCosine < 0.3) {
                squares.add(approx)
            }
        }
    }

    private fun angle(pt1: DoubleArray, pt2: DoubleArray, pt0: DoubleArray): Double {
        val dx1 = pt1[0] - pt0[0]
        val dy1 = pt1[1] - pt0[1]
        val dx2 = pt2[0] - pt0[0]
        val dy2 = pt2[1] - pt0[1]
        val v = kotlin.math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2))
        return (dx1 * dx2 + dy1 * dy2) / v
    }

    private fun sortFrameRect(square: MatOfPoint2f): MatOfPoint2f {
        val srcPoints = square.toArray()

        val sortByX = srcPoints.clone()
        sortByX.sortBy { it.x }

        val topLeft = if (sortByX[0].y < sortByX[1].y) sortByX[0] else sortByX[1]
        val bottomLeft = if (topLeft == sortByX[0]) sortByX[1] else sortByX[0]
        val topRight = if (sortByX[2].y < sortByX[3].y) sortByX[2] else sortByX[3]
        val bottomRight = if (topRight == sortByX[2]) sortByX[3] else sortByX[2]

        return MatOfPoint2f(topRight, topLeft, bottomLeft, bottomRight)
    }

    private fun readCellNumber(cellBitMap: Bitmap): Int {
        val inputTensor = createTensor(cellBitMap)

        val outputTensor = ptModel.forward(IValue.from(inputTensor)).toTensor()
        val scores = outputTensor.dataAsFloatArray

        var maxScore = -Float.MAX_VALUE
        var maxScoreIndex = -1
        for (i in scores.indices) {
            if (scores[i] > maxScore) {
                maxScore = scores[i]
                maxScoreIndex = i
            }
        }
        return maxScoreIndex
    }

    private fun createTensor(bitmap: Bitmap): Tensor {
        val width = bitmap.width
        val height = bitmap.height
        val buffer = IntArray(width * height)
        bitmap.getPixels(buffer, 0, width, 0, 0, width, height)
        val floatArray = FloatArray(width * height)
        for (i in 0 until width * height) {
            val hsv = FloatArray(3)
            Color.colorToHSV(buffer[i], hsv)
            floatArray[i] = hsv[2]
        }
        return Tensor.fromBlob(floatArray, longArrayOf(1, 1, width.toLong(), height.toLong()))
    }
}