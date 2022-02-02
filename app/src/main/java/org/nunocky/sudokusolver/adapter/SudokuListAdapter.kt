package org.nunocky.sudokusolver.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.nunocky.sudokusolver.R
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.databinding.SudokuListItemBinding
import java.io.File

private val diffCallback = object : DiffUtil.ItemCallback<SudokuEntity>() {
    override fun areItemsTheSame(oldItem: SudokuEntity, newItem: SudokuEntity) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: SudokuEntity, newItem: SudokuEntity) =
        oldItem == newItem // check contents
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val binding = SudokuListItemBinding.bind(view)

    fun getItemIdDetails(): ItemDetailsLookup.ItemDetails<Long> =
        object : ItemDetailsLookup.ItemDetails<Long>() {
            // https://stackoverflow.com/questions/63068519/getadapterposition-is-deprecated
            override fun getPosition(): Int = adapterPosition
            override fun getSelectionKey(): Long = itemId
        }

    fun bindTo(entity: SudokuEntity, listener: OnItemClickListener?, isActivated: Boolean = false) {

        itemView.setOnClickListener {
            listener?.onItemClicked(it, entity)
        }

        val context = binding.imageView.context
        val bitmap: Bitmap? = if (entity.thumbnail.isNullOrBlank()) {
            BitmapFactory.decodeResource(context.resources, R.drawable.noimage)
        } else {
            val imageDir = File(context.filesDir, "images")
            val file = File(imageDir, entity.thumbnail!!)
            BitmapFactory.decodeFile(file.absolutePath)
        }

        binding.imageView.setImageBitmap(bitmap)

        val difficulty =
            entity.difficulty ?: org.nunocky.sudokulib.SudokuSolver.DIFFICULTY_UNDEF

        binding.text1.apply {
            val textArray = context.resources.getTextArray(R.array.difficulty)
            this.text = textArray[difficulty]

            val text1Color = when (difficulty) {
                1 -> R.color.difficulty_undef
                2 -> R.color.difficulty_easy
                3 -> R.color.difficulty_medium
                4 -> R.color.difficulty_hard
                5 -> R.color.difficulty_extreme
                else -> R.color.difficulty_impossible
            }

            if (Build.VERSION_CODES.M <= Build.VERSION.SDK_INT) {
                this.setTextColor(ContextCompat.getColor(context, text1Color))
            } else {
                this.setTextColor(context.resources.getColor(text1Color))
            }
        }

        itemView.isActivated = isActivated
    }
}

interface OnItemClickListener {
    //        fun onItemClicked(view: View, position: Int)
    fun onItemClicked(view: View, entity: SudokuEntity)
}

//class SudokuListAdapter(var list: List<SudokuEntity>) :
class SudokuListAdapter :
    ListAdapter<SudokuEntity, ViewHolder>(diffCallback), OnItemClickListener {


    var listener: OnItemClickListener? = null
    var tracker: SelectionTracker<Long>? = null

//    init {
//        setHasStableIds(true)
//    }

//    fun updateList(newList: List<SudokuEntity>) {
//        list = newList
//        notifyDataSetChanged()
//    }

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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(getItem(position), this)
//        holder.itemView.setOnClickListener {
//            listener?.onItemClicked(it, position)
//        }
    }

    /**
     * ViewHolderから受け取ったイベントをリスナーに返す
     */
    override fun onItemClicked(view: View, entity: SudokuEntity) {
        listener?.onItemClicked(view, entity)
    }

//    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
//        super.onBindViewHolder(holder, position, payloads)
//
//        val entity = list[position]
//        val selected = tracker?.isSelected(entity.id) ?: false
//        holder.bind(entity, selected)
//    }

//    override fun getItemCount() = list.size
//
//    override fun getItemId(position: Int) = list[position].id

}

/**
 * ListViewから複数選んで削除するときに使う
 */
class SudokuEntityDetailsLookup(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<Long>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            return (recyclerView.getChildViewHolder(view) as ViewHolder)
                .getItemIdDetails()
        }
        return null
    }
}