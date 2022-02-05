package org.nunocky.sudokusolver.adapter

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import org.nunocky.sudokusolver.database.SudokuEntity

class DataBindingAdapter {
    companion object {
        @JvmStatic
        @BindingAdapter("sudokuList")
        fun setSudokuList(recyclerView: RecyclerView, newList: List<SudokuEntity>?) {
            if (newList != null) {
                //(recyclerView.adapter as SudokuListAdapter).updateList(newList)
                (recyclerView.adapter as SudokuListAdapter).submitList(newList)
            }
        }
    }
}