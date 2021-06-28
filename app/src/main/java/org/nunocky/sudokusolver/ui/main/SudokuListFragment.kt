package org.nunocky.sudokusolver.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.CHAIN_PACKED
import androidx.fragment.app.Fragment
import org.nunocky.sudokusolver.databinding.FragmentSudokuListBinding
import org.nunocky.sudokusolver.view.NumberCellView

/**
 * 登録した問題一覧
 */
class SudokuListFragment : Fragment() {
    private lateinit var binding: FragmentSudokuListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSudokuListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val baseLayout = binding.base
        setupSudokuBase(requireActivity(), baseLayout)
    }

    private fun setupSudokuBase(context: Context, base: ConstraintLayout) {
        val rows = 9
        val cols = 9
        var lastLineArray: ArrayList<NumberCellView>? = null
        repeat(rows) { row ->
            val lineArray = ArrayList<NumberCellView>()

            var leftCell: NumberCellView? = null
            repeat(cols) { col ->
                val cell = NumberCellView(context)
                cell.id = View.generateViewId()
                base.addView(cell)

                // test
                cell.fixedNum = ((col + row) % 9) + 1
                cell.candidates = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9).toIntArray()

                val params = cell.layoutParams as ConstraintLayout.LayoutParams
                val leftParams = leftCell?.layoutParams as ConstraintLayout.LayoutParams?

                params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                params.height = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                params.dimensionRatio = "w1:1"

                // 左右関係
                when (col) {
                    0 -> {
                        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    }
                    cols - 1 -> {
                        params.horizontalChainStyle = CHAIN_PACKED
                        params.leftToRight = leftCell!!.id
                        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        leftParams?.rightToLeft = cell.id
                    }
                    else -> {
                        params.horizontalChainStyle = CHAIN_PACKED
                        params.leftToRight = leftCell!!.id
                        leftParams?.rightToLeft = cell.id
                    }
                }

                // 上下関係
                when (row) {
                    0 -> {
                        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    }
                    rows - 1 -> {
                        params.verticalChainStyle = CHAIN_PACKED
                        params.topToBottom = lastLineArray!![row].id
                        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                        val upperCell = lastLineArray!![col]

                        val upperCellParams =
                            upperCell.layoutParams as ConstraintLayout.LayoutParams

                        upperCellParams.bottomToTop = cell.id
                        upperCell.layoutParams = upperCellParams

                    }
                    else -> {
                        params.verticalChainStyle = CHAIN_PACKED
                        params.topToBottom = lastLineArray!![col].id

                        val upperCell = lastLineArray!![col]

                        val upperCellParams =
                            upperCell.layoutParams as ConstraintLayout.LayoutParams

                        upperCellParams.bottomToTop = cell.id
                        upperCell.layoutParams = upperCellParams
                    }
                }

                cell.layoutParams = params
                leftCell?.layoutParams = leftParams

                lineArray.add(cell)
                leftCell = cell
            }

            leftCell = null
            lastLineArray = lineArray

//        base.requestLayout()
        }
    }
}