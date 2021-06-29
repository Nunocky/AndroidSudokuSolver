package org.nunocky.sudokusolver.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.CHAIN_PACKED
import org.nunocky.sudokusolver.solver.Cell

class SudokuBoardView : ConstraintLayout {
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

    private fun init(context: Context, acttrs: AttributeSet?, defStyle: Int) {
        setupSudokuBase(context, this, acttrs, defStyle)
    }

    private val cellViews = ArrayList<NumberCellView>()

    private fun setupSudokuBase(
        context: Context,
        base: ConstraintLayout,
        acttrs: AttributeSet?,
        defStyle: Int
    ) {
        val rows = 9
        val cols = 9

        var upperCellArray: ArrayList<NumberCellView>? = null
        repeat(rows) { row ->
            val lineArray = ArrayList<NumberCellView>()

            repeat(cols) { col ->
                val leftCell: NumberCellView? =
                    if (lineArray.isNotEmpty()) lineArray[col - 1] else null

                val cell = NumberCellView(context)
                cell.id = View.generateViewId()
                base.addView(cell)
                cellViews.add(cell)

                // test
                //cell.fixedNum = ((col + row) % 9) + 1
                cell.candidates = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9).toIntArray()

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

    fun updateCells(cells: List<Cell>) {
        cellViews.forEachIndexed { n, cellView ->
            cellView.fixedNum = cells[n].value
            cellView.candidates = cells[n].candidates.toIntArray()
        }
        invalidate()
    }
}