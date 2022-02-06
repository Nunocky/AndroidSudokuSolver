package org.nunocky.sudokusolver.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.nunocky.sudokusolver.IMAGEDIR
import org.nunocky.sudokusolver.R
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.databinding.SudokuListItemBinding
import org.nunocky.sudokusolver.ui.main.SudokuListViewModel
import java.io.File

private val diffCallback = object : DiffUtil.ItemCallback<SudokuEntity>() {
    override fun areItemsTheSame(oldItem: SudokuEntity, newItem: SudokuEntity) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: SudokuEntity, newItem: SudokuEntity) =
        oldItem == newItem
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val binding = SudokuListItemBinding.bind(view)

    fun getItemIdDetails(): ItemDetailsLookup.ItemDetails<Long> =
        object : ItemDetailsLookup.ItemDetails<Long>() {
            // https://stackoverflow.com/questions/63068519/getadapterposition-is-deprecated
            override fun getPosition(): Int = bindingAdapterPosition  // adapterPosition
            override fun getSelectionKey(): Long = itemId
        }

    fun bindTo(
        entity: SudokuEntity, listener: OnItemClickListener?,
        viewLifecycleOwner: LifecycleOwner,
        viewModel: SudokuListViewModel,
        isActivated: Boolean = false
    ) {
        entity.isChecked = isActivated

        itemView.setOnClickListener {
            listener?.onItemClicked(it, entity)
        }

        val context = binding.imageView.context
        val bitmap: Bitmap? = if (entity.thumbnail.isNullOrBlank()) {
            BitmapFactory.decodeResource(context.resources, R.drawable.noimage)
        } else {
            val imageDir = File(context.filesDir, IMAGEDIR)
            val file = File(imageDir, entity.thumbnail!!)
            BitmapFactory.decodeFile(file.absolutePath)
        }

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.imageView.setImageBitmap(bitmap)
        binding.sudokuEntity = entity

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

        binding.executePendingBindings()
    }
}

interface OnItemClickListener {
    fun onItemClicked(view: View, entity: SudokuEntity)
}

class SudokuListAdapter(
    private val viewLifecycleOwner: LifecycleOwner,
    private val viewModel: SudokuListViewModel
) :
    ListAdapter<SudokuEntity, ViewHolder>(diffCallback), OnItemClickListener {

    var listener: OnItemClickListener? = null
    var tracker: SelectionTracker<Long>? = null

    init {
        setHasStableIds(true)
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(getItem(position), this, viewLifecycleOwner, viewModel)
    }

    // ロングタップ時の選択操作に必要
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)

        // TODO ロングタップ時の処理 アニメーションでフィルタがたたまれたときに余計なアイテムもロングタップ状態になるのに対応する
        val entity = getItem(position)
        val selected = tracker?.isSelected(entity.id) ?: false
        holder.bindTo(entity, this, viewLifecycleOwner, viewModel, selected)
    }

    // ロングタップ時の選択操作に必要
    override fun getItemId(position: Int) = getItem(position).id

    /**
     * ViewHolderから受け取ったイベントをリスナーに返す
     */
    override fun onItemClicked(view: View, entity: SudokuEntity) {
        listener?.onItemClicked(view, entity)
    }
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