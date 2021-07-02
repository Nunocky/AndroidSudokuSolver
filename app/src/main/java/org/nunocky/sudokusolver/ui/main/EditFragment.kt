package org.nunocky.sudokusolver.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import org.nunocky.sudokusolver.MyApplication
import org.nunocky.sudokusolver.R
import org.nunocky.sudokusolver.SudokuRepository
import org.nunocky.sudokusolver.databinding.FragmentEditBinding
import org.nunocky.sudokusolver.solver.Cell
import org.nunocky.sudokusolver.view.NumberCellView

/**
 * A simple [Fragment] subclass.
 * Use the [EditFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EditFragment : Fragment() {
    private val args: EditFragmentArgs by navArgs()
    private lateinit var binding: FragmentEditBinding

    //    private val viewModel: EditViewModel by viewModels()
    private val viewModel: EditViewModel by viewModels {
        val app = (requireActivity().application as MyApplication)
        val appDatabase = app.appDatabase
        EditViewModel.Factory(SudokuRepository(appDatabase))
    }

    private var currentCell: NumberCellView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // binding.sudokuBoardView.borderColor = Color.BLUE
        binding.sudokuBoardView.showCandidates = false

        binding.sudokuBoardView.cellViews.forEach { cellView ->
            cellView.setOnClickListener(cellClickedListener)
        }

        binding.tb0.setOnCheckedChangeListener(tbListener)
        binding.tb1.setOnCheckedChangeListener(tbListener)
        binding.tb2.setOnCheckedChangeListener(tbListener)
        binding.tb3.setOnCheckedChangeListener(tbListener)
        binding.tb4.setOnCheckedChangeListener(tbListener)
        binding.tb5.setOnCheckedChangeListener(tbListener)
        binding.tb6.setOnCheckedChangeListener(tbListener)
        binding.tb7.setOnCheckedChangeListener(tbListener)
        binding.tb8.setOnCheckedChangeListener(tbListener)
        binding.tb9.setOnCheckedChangeListener(tbListener)

        viewModel.currentValue.observe(requireActivity()) {
            currentCell?.fixedNum = it
        }

        if (args.entityId == 0L) {
            // new Item
            viewModel.setNewSudoku()
        } else {
            viewModel.loadSudoku(args.entityId)
        }

        viewModel.entity.observe(requireActivity()) {
            it?.cells?.let { cells ->
                val cellList = ArrayList<Cell>()
                cells.toCharArray().forEach {
                    val cell = Cell().apply {
                        value = it - '0'
                    }
                    cellList.add(cell)
                }
                binding.sudokuBoardView.updateCells(cellList)
            }
        }
    }

    private val cellClickedListener = View.OnClickListener {
        val currentSelectedCellIndex = (it as NumberCellView).index

        binding.sudokuBoardView.cellViews.forEachIndexed { n, numberCellView ->
            if (n == currentSelectedCellIndex) {
                numberCellView.setBackgroundColor(Color.parseColor("#ffffb0"))
                currentCell = numberCellView
                viewModel.currentValue.value = numberCellView.fixedNum
            } else {
                numberCellView.setBackgroundColor(Color.WHITE)
            }
        }
    }

    private val tbListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                return@OnCheckedChangeListener
            }

            when (buttonView?.id) {
                binding.tb0.id -> {
                    viewModel.currentValue.value = 0
                }
                binding.tb1.id -> {
                    viewModel.currentValue.value = 1
                }
                binding.tb2.id -> {
                    viewModel.currentValue.value = 2
                }
                binding.tb3.id -> {
                    viewModel.currentValue.value = 3
                }
                binding.tb4.id -> {
                    viewModel.currentValue.value = 4
                }
                binding.tb5.id -> {
                    viewModel.currentValue.value = 5
                }
                binding.tb6.id -> {
                    viewModel.currentValue.value = 6
                }
                binding.tb7.id -> {
                    viewModel.currentValue.value = 7
                }
                binding.tb8.id -> {
                    viewModel.currentValue.value = 8
                }
                binding.tb9.id -> {
                    viewModel.currentValue.value = 9
                }
            }
        }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_save) {
            val cells =
                binding.sudokuBoardView.cellViews.joinToString("") { it.fixedNum.toString() }
            viewModel.saveSudoku(cells)
        }

        return super.onOptionsItemSelected(item)
    }
}