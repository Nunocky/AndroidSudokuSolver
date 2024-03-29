package org.nunocky.sudokusolver.ui.main

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.CompoundButton
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nunocky.sudokusolver.R
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.databinding.FragmentEditBinding
import org.nunocky.sudokusolver.view.NumberCellView

@AndroidEntryPoint
class EditFragment : Fragment() {
    private val args: EditFragmentArgs by navArgs()
    private lateinit var binding: FragmentEditBinding
    private val viewModel: EditViewModel by viewModels()

    private val navController by lazy { findNavController() }
    private val previousSavedStateHandle by lazy { navController.previousBackStackEntry!!.savedStateHandle }

    private var currentCell: NumberCellView? = null
    private var shouldReturnToList = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            onBackButtonClicked()
        }

        viewModel.entityId.observe(viewLifecycleOwner) { entity ->
            entity?.let {
                loadSudoku(it)
                shouldReturnToList = (it == 0L)
            }
        }

        previousSavedStateHandle.set(KEY_SAVED, false)

        binding.sudokuBoardView.showCandidates = false

        binding.sudokuBoardView.cellViews.forEach { cellView ->
            cellView.setOnClickListener(cellClickedListener)
        }

        binding.numberInput.apply {
            tb0.setOnCheckedChangeListener(tbListener)
            tb1.setOnCheckedChangeListener(tbListener)
            tb2.setOnCheckedChangeListener(tbListener)
            tb3.setOnCheckedChangeListener(tbListener)
            tb4.setOnCheckedChangeListener(tbListener)
            tb5.setOnCheckedChangeListener(tbListener)
            tb6.setOnCheckedChangeListener(tbListener)
            tb7.setOnCheckedChangeListener(tbListener)
            tb8.setOnCheckedChangeListener(tbListener)
            tb9.setOnCheckedChangeListener(tbListener)
        }

        binding.numberInput.tbAC.setOnClickListener {
            clearAllCells()
        }

        viewModel.currentValue.observe(viewLifecycleOwner) { num ->
            currentCell?.let {
                it.fixedNum = num
                it.updated = false
            }
            // 通知
            val list = binding.sudokuBoardView.cellViews.map { cellView ->
                cellView.fixedNum.toChar().code
            }
            viewModel.sudokuSolver.load(list)
            updateBackgroundColor()
        }
    }

    private val cellClickedListener = View.OnClickListener {
        val currentSelectedCellIndex = (it as NumberCellView).index
        currentCell = binding.sudokuBoardView.cellViews[currentSelectedCellIndex]
        currentCell?.onFocus = true

        binding.sudokuBoardView.cellViews.forEachIndexed { n, numberCellView ->
            if (n == currentSelectedCellIndex) {
                viewModel.currentValue.value = numberCellView.fixedNum
            } else {
                numberCellView.onFocus = false
            }
        }
    }

    private val tbListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        val buttonIndex = when (buttonView.id) {
            binding.numberInput.tb1.id -> 1
            binding.numberInput.tb2.id -> 2
            binding.numberInput.tb3.id -> 3
            binding.numberInput.tb4.id -> 4
            binding.numberInput.tb5.id -> 5
            binding.numberInput.tb6.id -> 6
            binding.numberInput.tb7.id -> 7
            binding.numberInput.tb8.id -> 8
            binding.numberInput.tb9.id -> 9
            else -> 0
        }

        if (!isChecked) {
            if (currentCell == null) {
                viewModel.currentValue.value = 0
            } else if (currentCell?.fixedNum == buttonIndex) {
                viewModel.currentValue.value = 0
            }

            return@OnCheckedChangeListener
        }

        viewModel.currentValue.value = buttonIndex

        updateBackgroundColor()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_edit, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_save)?.let {
            it.isEnabled = viewModel.sudokuSolver.isValid
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackButtonClicked()
                true
            }
            R.id.action_save -> {
                saveSudoku()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    /**
     * 指定 idの 数独を読み込み、UIにセットする
     */
    private fun loadSudoku(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val entity = viewModel.loadSudoku(id)

            withContext(Dispatchers.Main) {
                updateUI(entity)
            }
        }
    }

    /**
     * UIを更新する
     */
    private fun updateUI(entity: SudokuEntity) {
        entity.cells.let { cells ->
            val cellList = ArrayList<org.nunocky.sudokulib.Cell>()
            cells.toCharArray().forEach {
                val cell = org.nunocky.sudokulib.Cell().apply {
                    value = it - '0'
                }
                cellList.add(cell)
            }
            binding.sudokuBoardView.updateCells(cellList)
            binding.sudokuBoardView.updated = false
        }
    }

    /**
     *
     */
    private fun saveSudoku() {
        lifecycleScope.launch(Dispatchers.IO) {
            val cells =
                binding.sudokuBoardView.cellViews.joinToString("") { it.fixedNum.toString() }

            val thumbnail = createImage()
            val newId = viewModel.saveSudoku(args.entityId, cells, thumbnail)

            withContext(Dispatchers.Main) {
                shouldReturnToList = false
                previousSavedStateHandle.set(KEY_SAVED, true)
                previousSavedStateHandle.set("entityId", newId)

                // SnackBarを表示して前画面に戻る
                val snackBar = Snackbar.make(
                    binding.root,
                    resources.getString(R.string.saved),
                    Snackbar.LENGTH_SHORT
                )
                snackBar.show()

                findNavController().popBackStack()
            }
        }
    }

    private fun clearAllCells() {
        binding.sudokuBoardView.cellViews.forEach { cellView ->
            cellView.fixedNum = 0
        }

        // 通知
        // TODO コード重複
        val list = binding.sudokuBoardView.cellViews.map { cellView ->
            cellView.fixedNum.toChar().code
        }
        viewModel.sudokuSolver.load(list)
    }

    private fun onBackButtonClicked() {
        if (shouldReturnToList) {
            navController.popBackStack(R.id.sudokuListFragment, false)
        } else {
            previousSavedStateHandle.set(KEY_SAVED, true)

            viewModel.entityId.value?.let { entityId ->
                if (entityId != 0L) {
                    previousSavedStateHandle.set("entityId", entityId)
                }
            }
            navController.popBackStack()
        }
    }

    private fun createImage(): Bitmap {
        binding.sudokuBoardView.visibility = View.INVISIBLE
        binding.sudokuBoardView.cellViews.forEach { cellView ->
            val color = if (cellView.fixedNum != 0) {
                R.color.fixedCell
            } else {
                R.color.white
            }

            cellView.setBackgroundColor(
                ContextCompat.getColor(requireActivity(), color)
            )
        }

        val thumbnail = binding.sudokuBoardView.getThumbNail()

        binding.sudokuBoardView.cellViews.forEach { cellView ->
            val color = R.color.white

            cellView.setBackgroundColor(
                ContextCompat.getColor(requireActivity(), color)
            )
        }

        return thumbnail
    }

    private fun updateBackgroundColor() {
        requireActivity().invalidateMenu()

        if (viewModel.sudokuSolver.isValid) {
            setBGColor(requireActivity(), binding.sudokuBoardView, R.color.board_valid)
        } else {
            if (binding.sudokuBoardView.cellViews
                    .filter { cellView -> cellView.fixedNum != 0 }
                    .count() != 0
            ) {
                setBGColor(requireActivity(), binding.sudokuBoardView, R.color.board_invalid)
                binding.sudokuBoardView.invalidate()
            }
        }
    }


    companion object {
        const val KEY_SAVED = "saved"
    }
}

private fun setBGColor(context: Context, view: View, colorId: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        view.setBackgroundColor(
            context.resources.getColor(
                colorId,
                context.theme
            )
        )
    } else {
        view.setBackgroundColor(context.resources.getColor(colorId))
    }
}