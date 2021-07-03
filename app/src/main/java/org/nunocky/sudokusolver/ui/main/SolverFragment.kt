package org.nunocky.sudokusolver.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.MyApplication
import org.nunocky.sudokusolver.SudokuRepository
import org.nunocky.sudokusolver.databinding.FragmentSolverBinding
import org.nunocky.sudokusolver.solver.Cell
import org.nunocky.sudokusolver.solver.SudokuSolver

/**
 * 問題を解く
 *
 *
 */
class SolverFragment : Fragment() {
    private val args: SolverFragmentArgs by navArgs()

    //    private val viewModel: SolverViewModel by viewModels()
    private val viewModel: SolverViewModel by viewModels {
        val app = (requireActivity().application as MyApplication)
        val appDatabase = app.appDatabase
        SolverViewModel.Factory(SudokuRepository(appDatabase))
    }

    private lateinit var binding: FragmentSolverBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSolverBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSudoku()

        binding.btnStart.setOnClickListener {
            startSolve()
        }
    }

    private fun loadSudoku() = lifecycleScope.launch {
        viewModel.loadSudoku(args.entityId).join()

        binding.sudokuBoard.cellViews.forEachIndexed { n, cellView ->
            cellView.fixedNum = viewModel.solver.cells[n].value
            if (cellView.fixedNum != 0) {
                cellView.candidates = IntArray(0)
                cellView.showCandidates = false
            }
        }
    }

    private fun startSolve() = lifecycleScope.launch {
        viewModel.startSolve(callback)
    }

    private val callback = object : SudokuSolver.ProgressCallback {
        override fun onProgress(cells: List<Cell>) {
            binding.sudokuBoard.cellViews.forEachIndexed { n, cellView ->
                //cellView.updated = false // TODO 属性追加
                cellView.fixedNum = cells[n].value
                cellView.candidates = cells[n].candidates.toIntArray()
            }
        }
    }
}

