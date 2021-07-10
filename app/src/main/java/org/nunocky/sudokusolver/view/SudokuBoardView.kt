package org.nunocky.sudokusolver.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.CHAIN_PACKED
import org.nunocky.sudokusolver.R
import org.nunocky.sudokulib.Cell

class SudokuBoardView : ConstraintLayout {
    val cellViews = ArrayList<NumberCellView>()

    constructor(context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context, attrs, defStyle)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyle: Int) {
        setupSudokuBase(context, this)
        setCellStyles()

        context.theme.obtainStyledAttributes(attrs, R.styleable.SudokuBoardView, defStyle, 0)
            .apply {
                try {
                    borderColor =
                        getColor(R.styleable.SudokuBoardView_borderColor, Color.BLACK)
                    textColor = getColor(R.styleable.SudokuBoardView_textColor, Color.BLACK)
                    updatedTextColor =
                        getColor(R.styleable.SudokuBoardView_updatedTextColor, Color.RED)
                    showCandidates =
                        getBoolean(R.styleable.SudokuBoardView_showCandidates, true)
                    candidateColor =
                        getColor(R.styleable.SudokuBoardView_candidateColor, Color.LTGRAY)
                } finally {
                    recycle()
                }
            }
    }

    private fun setupSudokuBase(context: Context, base: ConstraintLayout) {
        val rows = 9
        val cols = 9

        var upperCellArray: ArrayList<NumberCellView>? = null
        repeat(rows) { row ->
            val lineArray = ArrayList<NumberCellView>()

            repeat(cols) { col ->
                val leftCell: NumberCellView? =
                    if (lineArray.isNotEmpty()) lineArray[col - 1] else null

                val cell = NumberCellView(context).apply {
                    id = View.generateViewId()
                    index = row * rows + col
                    fixedNum = 0
                    candidates = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9).toIntArray()
                }
                base.addView(cell)
                cellViews.add(cell)

                val params = cell.layoutParams as LayoutParams
                val leftParams = leftCell?.layoutParams as LayoutParams?

                params.width = LayoutParams.MATCH_CONSTRAINT
                params.height = LayoutParams.MATCH_CONSTRAINT
                params.dimensionRatio = "w1:1"

                // 左右関係
                if (leftCell == null) {
                    params.startToStart = LayoutParams.PARENT_ID
                } else {
                    params.horizontalChainStyle = CHAIN_PACKED
                    params.startToEnd = leftCell.id
                    leftParams?.endToStart = cell.id
                }

                if (col == cols - 1) {
                    params.horizontalChainStyle = CHAIN_PACKED
                    if (leftCell != null) {
                        params.startToEnd = leftCell.id
                    }
                    params.endToEnd = LayoutParams.PARENT_ID
                    leftParams?.endToStart = cell.id
                }

                // 上下関係
                val upperCell = if (upperCellArray != null) upperCellArray!![col] else null
                val upperCellParams = upperCell?.layoutParams as LayoutParams?

                if (upperCellArray == null) {
                    params.topToTop = LayoutParams.PARENT_ID
                } else {
                    params.verticalChainStyle = CHAIN_PACKED
                    if (upperCell != null) {
                        params.topToBottom = upperCell.id
                    }
                    upperCellParams?.bottomToTop = cell.id
                    upperCell?.layoutParams = upperCellParams
                }

                if (row == rows - 1) {
                    if (upperCellArray != null) {
                        if (upperCell != null) {
                            params.topToBottom = upperCell.id
                        }
                        upperCellParams?.bottomToTop = cell.id
                    }

                    params.verticalChainStyle = CHAIN_PACKED
                    params.bottomToBottom = LayoutParams.PARENT_ID
                }

                cell.layoutParams = params

                if (leftCell != null) {
                    leftCell.layoutParams = leftParams
                }

                if (upperCell != null) {
                    upperCell.layoutParams = upperCellParams
                }

                lineArray.add(cell)
            }

            upperCellArray = lineArray
        }
    }

    private fun setCellStyles() {
        cellViews.forEach { cellView ->
            cellView.topBorderStyle = BorderStyle.NORMAL
            cellView.rightBorderStyle = BorderStyle.NORMAL
            cellView.bottomBorderStyle = BorderStyle.NORMAL
            cellView.leftBorderStyle = BorderStyle.NORMAL

            // 1行目
            if (cellView.index / 9 == 0) {
                cellView.topBorderStyle = BorderStyle.BOLD
            }

            // 左端
            if (cellView.index % 9 == 0) {
                cellView.leftBorderStyle = BorderStyle.BOLD
            }

            // 右端
            if (cellView.index % 9 == 8) {
                cellView.rightBorderStyle = BorderStyle.BOLD
            }

            // 3, 6行目
            if ((cellView.index % 9 == 2) || (cellView.index % 9 == 5)) {
                cellView.rightBorderStyle = BorderStyle.BOLD
            }

            // 3, 6列目
            if ((cellView.index % 9 == 3) || (cellView.index % 9 == 6)) {
                cellView.rightBorderStyle = BorderStyle.NONE
            }

            if ((cellView.index in 18..26) || (cellView.index in 45..53)) {
                cellView.bottomBorderStyle = BorderStyle.BOLD
            }

            if ((cellView.index in 27..35) || (cellView.index in 54..62)) {
                cellView.topBorderStyle = BorderStyle.NONE
            }

            // 8行目
            if (cellView.index / 9 == 8) {
                cellView.bottomBorderStyle = BorderStyle.BOLD
            }
        }
    }

    fun updateCells(cells: List<org.nunocky.sudokulib.Cell>) {
        cellViews.forEachIndexed { n, cellView ->
            cellView.onFocus = false
            cellView.fixedNum = cells[n].value
            cellView.candidates = cells[n].candidates.toIntArray()
        }
        invalidate()
    }

    var borderColor: Int = Color.BLACK
        set(newValue) {
            field = newValue
            cellViews.forEach {
                it.borderColor = newValue
            }
        }

    var textColor: Int = Color.BLACK
        set(newValue) {
            field = newValue
            cellViews.forEach {
                it.textColor = newValue
            }
        }

    var updatedTextColor: Int = Color.RED
        set(newValue) {
            field = newValue
            cellViews.forEach {
                it.updatedTextColor = newValue
            }
        }

    var showCandidates: Boolean = true
        set(value) {
            field = value
            cellViews.forEach {
                it.showCandidates = value
            }
        }

    var candidateColor: Int = Color.LTGRAY
        set(newValue) {
            field = newValue
            cellViews.forEach {
                it.candidateColor = newValue
            }
        }

    var updated: Boolean = false
        set(value) {
            cellViews.forEach { cellView ->
                cellView.updated = value
            }
           field = value
        }
}
