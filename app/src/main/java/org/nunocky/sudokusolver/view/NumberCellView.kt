package org.nunocky.sudokusolver.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import kotlinx.android.parcel.Parcelize
import org.nunocky.sudokusolver.R

class NumberCellView : View {
    private var canvasWidth: Float = 0f
    private var canvasHeight: Float = 0f

    var index = 0

    var fixedNum = 0
        set(value) {
            if (field != value) {
                updated = true
                invalidate()
            }
            field = value
        }

    var candidates: IntArray = IntArray(0)
        set(value) {
            if (!(field contentEquals value)) {
                updated = true
                invalidate()
            }
            field = value
        }

    private val textPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }

    private val updatedTextPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }

    private val candidatesPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 10f
    }

    private val linePaint = Paint().apply {
        strokeWidth = 2f
        color = Color.BLACK
    }

    private val borderPaint = Paint(ANTI_ALIAS_FLAG).apply {
        strokeWidth = 8f
        color = Color.RED
    }

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {

        context.theme.obtainStyledAttributes(attrs, R.styleable.NumberCellView, defStyle, 0)
            .apply {
                try {
                    borderColor =
                        getColor(R.styleable.NumberCellView_borderColor, Color.BLACK)
                    textColor = getColor(R.styleable.NumberCellView_textColor, Color.BLACK)
                    updatedTextColor =
                        getColor(R.styleable.NumberCellView_updatedTextColor, Color.RED)
                    showCandidates =
                        getBoolean(R.styleable.NumberCellView_showCandidates, true)
                    candidateColor =
                        getColor(R.styleable.NumberCellView_candidateColor, Color.LTGRAY)
                } finally {
                    recycle()
                }
            }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasWidth = w.toFloat()
        canvasHeight = h.toFloat()

        determineFinedNumberTextSize()
        determineCandidatesTextSize()
    }

    private fun determineFinedNumberTextSize() {
        val targetHeight = canvasHeight // * 0.9f
        textPaint.textSize = targetHeight
    }

    private fun determineCandidatesTextSize() {
        val targetHeight = canvasHeight / 3f * 0.75f
        candidatesPaint.textSize = targetHeight
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)

        // draw borders
        borderPaint.strokeWidth = calcBorderWidth(topBorderStyle, canvasWidth)
        canvas?.drawLine(0f, 0f, canvasWidth, 0f, borderPaint)

        borderPaint.strokeWidth = calcBorderWidth(rightBorderStyle, canvasHeight)
        canvas?.drawLine(canvasWidth, 0f, canvasWidth, canvasHeight, borderPaint)

        borderPaint.strokeWidth = calcBorderWidth(bottomBorderStyle, canvasHeight)
        canvas?.drawLine(0f, canvasHeight, canvasWidth, canvasHeight, borderPaint)

        borderPaint.strokeWidth = calcBorderWidth(leftBorderStyle, canvasHeight)
        canvas?.drawLine(0f, 0f, 0f, canvasHeight, borderPaint)

        if (showCandidates) {
            // draw cell lines
            // vertical
            for (lx in 0..2) {
                canvas?.drawLine(
                    canvasWidth / 3f * lx, 0f,
                    canvasWidth / 3f * lx, canvasHeight,
                    candidatesPaint
                )
            }

            // horizontal
            for (ly in 0..2) {
                canvas?.drawLine(
                    0f, canvasHeight / 3f * ly,
                    canvasWidth, canvasHeight / 3f * ly,
                    candidatesPaint
                )
            }

            candidates.forEach { v ->
                if (v in 1..9) {

                    val cellX = ((v - 1) % 3) * (canvasWidth / 3f)
                    val cellY = ((v - 1) / 3) * (canvasHeight / 3f)

                    val centerX = cellX + canvasWidth / 3f / 2f
                    val centerY = cellY + canvasHeight / 3f / 2f
                    val fontMetrics = candidatesPaint.fontMetrics
                    val textWidth = candidatesPaint.measureText("$v")
                    val baseX = centerX - textWidth / 2f
                    val baseY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f

                    canvas?.drawText("$v", baseX, baseY, candidatesPaint)
                }
            }
        }

        // draw fixedNum
        if (fixedNum != 0) {
            val text = "$fixedNum"
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f
            val fontMetrics = textPaint.fontMetrics
            val textWidth = textPaint.measureText(text)
            val baseX = centerX - textWidth / 2f
            val baseY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f

            canvas?.drawText(text, baseX, baseY, textPaint)
        }
    }

    // なんとなく作っていたAndroidのカスタムViewを正しく実装する
    // https://qiita.com/KazaKago/items/758076137e8d4a962dd0

    override fun onSaveInstanceState(): Parcelable {
        val parent = super.onSaveInstanceState()
        return SavedState(parent, fixedNum, candidates)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as? SavedState
        super.onRestoreInstanceState(state)
        savedState ?: return
        this.fixedNum = savedState.fixedNum
        this.candidates = savedState.candidates
    }

    @Parcelize
    private data class SavedState(
        val source: Parcelable?,
        var fixedNum: Int,
        var candidates: IntArray
    ) : BaseSavedState(source) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SavedState

            if (source != other.source) return false
            if (fixedNum != other.fixedNum) return false
            if (!candidates.contentEquals(other.candidates)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = source?.hashCode() ?: 0
            result = 31 * result + fixedNum
            result = 31 * result + candidates.contentHashCode()
            return result
        }
    }

    var borderColor: Int = Color.BLACK
        set(value) {
            if (borderPaint.color != value) {
                borderPaint.color = value
                invalidate()
            }
            field = value
        }

    var textColor: Int = Color.BLACK
        set(value) {
            if (textPaint.color != value) {
                textPaint.color = value
                invalidate()
            }
            field = value
        }

    var updatedTextColor = Color.RED
        set(value) {
            if (updatedTextPaint.color != value) {
                updatedTextPaint.color = value
                invalidate()
            }
            field = value
        }

    var showCandidates: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var candidateColor: Int = Color.LTGRAY
        set(value) {
            if (candidatesPaint.color != value) {
                candidatesPaint.color = value
                invalidate()
            }
            field = value
        }

    var topBorderStyle = BorderStyle.NORMAL
        set(value) {
            field = value
            invalidate()
        }

    var rightBorderStyle = BorderStyle.NORMAL
        set(value) {
            field = value
            invalidate()
        }

    var bottomBorderStyle = BorderStyle.NORMAL
        set(value) {
            field = value
            invalidate()
        }

    var leftBorderStyle = BorderStyle.NORMAL
        set(value) {
            field = value
            invalidate()
        }

    // setSelectedの overrideではうまく動かなかった
    var onFocus = false
        set(value) {
            field = value
            setBackgroundColor(
                if (value) {
                    // TODO 選択・非選択状態の色をアトリビュートで設定
                    Color.parseColor("#ffffb0")
                } else {
                    Color.WHITE
                }
            )
            invalidate()
        }

    var updated = false
        set(value) {
            if (field != value) {
                field = value
                textPaint.color = if (value) updatedTextColor else textColor
                invalidate()
            }
        }

    private fun calcBorderWidth(style: Int, maxLength: Float): Float {
        return when (style) {
            1 -> {
                (maxLength * 0.01f).coerceAtLeast(1f).coerceAtMost(4f)
            }
            2 -> {
                (maxLength * 0.08f).coerceAtLeast(1f).coerceAtMost(8f)
            }
            else -> {
                0f
            }
        }
    }
}