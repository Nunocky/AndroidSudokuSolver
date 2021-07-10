package org.nunocky.sudokusolver.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import org.nunocky.sudokusolver.R
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.databinding.SudokuListItemBinding
import org.nunocky.sudokusolver.solver.Cell
import org.nunocky.sudokusolver.solver.SudokuSolver

class SudokuListAdapter(var list: List<SudokuEntity>) :
    RecyclerView.Adapter<SudokuListAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClicked(view: View, position: Int)
    }

    var listener: OnItemClickListener? = null
    var tracker: SelectionTracker<Long>? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = SudokuListItemBinding.bind(view)

        fun getItemIdDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                // https://stackoverflow.com/questions/63068519/getadapterposition-is-deprecated
                override fun getPosition(): Int = adapterPosition
                override fun getSelectionKey(): Long = itemId
            }

        fun bind(entity: SudokuEntity, isActivated: Boolean = false) {

            // TODO ここの処理をコルーチンにできる?
            //  https://stackoverflow.com/questions/64398097/coroutine-inside-viewholder-kotlin
            //  https://engawapg.net/android/180/
            binding.sudokuBoardView.apply {
                showCandidates = false

                val text = entity.cells
                val cellList = ArrayList<Cell>()
                text.toCharArray().forEach {
                    val cell = Cell().apply {
                        value = it - '0'
                    }
                    cellList.add(cell)
                }

                updateCells(cellList)
                binding.sudokuBoardView.cellViews.forEach { cellView ->
                    if (cellView.fixedNum != 0) {
                        cellView.setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.fixedCell
                            )
                        )
                    }
                }

                val difficulty = entity.difficulty ?: SudokuSolver.DIFFICULTY_UNDEF

                binding.text1.apply {
                    val textArray = context.resources.getTextArray(R.array.difficulty)
                    val text1Color = when (difficulty) {
                        1 -> {
                            R.color.difficulty_undef
                        }
                        2 -> {
                            R.color.difficulty_easy
                        }
                        3 -> {
                            R.color.difficulty_medium
                        }
                        4 -> {
                            R.color.difficulty_hard
                        }
                        5 -> {
                            R.color.difficulty_extreme
                        }
                        else -> {
                            R.color.difficulty_impossible
                        }
                    }

                    this.text = textArray[difficulty]
                    if (Build.VERSION_CODES.M <= Build.VERSION.SDK_INT) {
                        this.setTextColor(ContextCompat.getColor(context, text1Color))
                    } else {
                        this.setTextColor(context.resources.getColor(text1Color))
                    }
                }

                updated = false
            }

            itemView.isActivated = isActivated
        }
    }

    init {
        setHasStableIds(true)
    }

    fun updateList(newList: List<SudokuEntity>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(
            inflater.inflate(
                R.layout.sudoku_list_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = list.size

    override fun getItemId(position: Int) = list[position].id

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            listener?.onItemClicked(it, position)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)

        val entity = list[position]
        val selected = tracker?.isSelected(entity.id) ?: false
        holder.bind(entity, selected)
    }
}

class SudokuEntityDetailsLookup(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<Long>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            return (recyclerView.getChildViewHolder(view) as SudokuListAdapter.ViewHolder)
                .getItemIdDetails()
        }
        return null
    }
}