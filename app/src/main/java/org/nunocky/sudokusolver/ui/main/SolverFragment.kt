package org.nunocky.sudokusolver.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.nunocky.sudokusolver.MyApplication
import org.nunocky.sudokusolver.R
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
            viewModel.startSolver(callback)
//            startSolve()
        }

        binding.btnReset.setOnClickListener {
            reset()
        }

        binding.btnStop.setOnClickListener {
            stopSolve()
        }
    }

    private fun loadSudoku() = lifecycleScope.launch {
        viewModel.loadSudoku(args.entityId).join()

        binding.sudokuBoard.cellViews.forEachIndexed { n, cellView ->
            cellView.apply {
                fixedNum = viewModel.solver.cells[n].value
                if (fixedNum != 0) {
                    setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.fixedCell))
                    candidates = IntArray(0)
                    showCandidates = false
                } else {
                    candidates = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9).toIntArray()
                }
            }
        }
        binding.sudokuBoard.updated = false
    }

//    private fun startSolve() = lifecycleScope.launch {
//        viewModel.startSolver(callback)
//    }

    private fun stopSolve() = lifecycleScope.launch {
        viewModel.stopSolver()
    }

    private fun reset() = lifecycleScope.launch {
        loadSudoku().join()
        viewModel.resetSolver()
    }

    private val callback = object : SudokuSolver.ProgressCallback {
        override fun onProgress(cells: List<Cell>) {
            binding.sudokuBoard.updated = false
            binding.sudokuBoard.cellViews.forEachIndexed { n, cellView ->
                cellView.fixedNum = cells[n].value
                cellView.candidates = cells[n].candidates.toIntArray()
            }

            runBlocking {
                // TODO ノーウェイト / ウェイトあり くらいの区分で良さそう
                delay((viewModel.stepSpeed.value ?: 1) * 100L)
            }
        }

        override fun onComplete(success: Boolean) {
            val message = if (success)
                requireActivity().resources.getString(R.string.solver_success)
            else
                requireActivity().resources.getString(R.string.solver_fail)

            val bgColor = if (success)
                ContextCompat.getColor(requireContext(), R.color.solverSuccess)
            else
                ContextCompat.getColor(requireContext(), R.color.solverFail)

            val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            snackBar.view.setBackgroundColor(bgColor)

            snackBar.show()
        }
    }
}

