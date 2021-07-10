package org.nunocky.sudokusolver.ui.main

import android.os.Bundle
import android.view.*
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.MyApplication
import org.nunocky.sudokusolver.R
import org.nunocky.sudokusolver.SudokuRepository
import org.nunocky.sudokusolver.databinding.FragmentEditBinding
import org.nunocky.sudokulib.Cell
import org.nunocky.sudokusolver.view.NumberCellView

class EditFragment : Fragment() {
    private val args: EditFragmentArgs by navArgs()
    private lateinit var binding: FragmentEditBinding

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

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.toolbar)

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

        binding.btnSolve.setOnClickListener {
            saveEntityAndMoveToSolveFragment()
        }

        viewModel.currentValue.observe(requireActivity()) { num ->
            currentCell?.let {
                it.fixedNum = num
                it.updated = false
            }
            // 通知
            val list = binding.sudokuBoardView.cellViews.map { cellView ->
                cellView.fixedNum.toChar().code
            }
            viewModel.sudokuSolver.load(list)
        }

        viewModel.entity.observe(requireActivity()) {
            it?.cells?.let { cells ->
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
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.title = "Edit"

        viewModel.currentValue.value = 0

        // 画面遷移時(前後) で読み込み直す
        if (viewModel.entity.value == null) {
            if (args.entityId == 0L) {
                viewModel.setNewSudoku()
            } else {
                viewModel.loadSudoku(args.entityId)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_save) {
            saveSudoku()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun saveSudoku() = lifecycleScope.launch {
        val cells =
            binding.sudokuBoardView.cellViews.joinToString("") { it.fixedNum.toString() }
        viewModel.saveSudoku(cells).join()
    }

    private fun saveEntityAndMoveToSolveFragment() = lifecycleScope.launch {
        viewModel.entity.value?.let { entity ->
            saveSudoku().join()
            val action = EditFragmentDirections.actionEditFragmentToSolverFragment(entity.id)
            findNavController().navigate(action)
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
}