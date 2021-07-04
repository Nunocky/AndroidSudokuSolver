package org.nunocky.sudokusolver.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.nunocky.sudokusolver.R
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.databinding.SudokuListItemBinding
import org.nunocky.sudokusolver.solver.Cell

class SudokuListAdapter(var list: List<SudokuEntity>) :
    RecyclerView.Adapter<SudokuListAdapter.SudokuListAdapterViewHolder>() {

    interface OnItemClickListener {
        fun onItemClicked(view: View, position: Int)
        fun onLongClick(view: View, position: Int): Boolean
    }

    var listener: OnItemClickListener? = null

    class SudokuListAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = SudokuListItemBinding.bind(view)
    }

    fun updateList(newList: List<SudokuEntity>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SudokuListAdapterViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return SudokuListAdapterViewHolder(
            inflater.inflate(
                R.layout.sudoku_list_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: SudokuListAdapterViewHolder, position: Int) {
        val entity = list[position]
        val text = entity.cells
        val cellList = ArrayList<Cell>()
        text.toCharArray().forEach {
            val cell = Cell().apply {
                value = it - '0'
            }
            cellList.add(cell)
        }

        holder.binding.sudokuBoardView.apply {
            showCandidates = false
            updateCells(cellList)
            updated = false
        }

        holder.itemView.setOnClickListener {
            listener?.onItemClicked(it, position)
        }

        holder.itemView.setOnLongClickListener {
            listener?.onLongClick(it, position) ?: true
        }
    }
}