package org.nunocky.sudokusolver.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.databinding.FragmentSolverBinding
import org.nunocky.sudokusolver.solver.Cell
import org.nunocky.sudokusolver.solver.SudokuSolver

/**
 * 問題を解く
 *
 *
 */
class SolverFragment : Fragment() {
    private val viewModel: SolverViewModel by viewModels()
    private lateinit var binding: FragmentSolverBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSolverBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        binding.btnStart.setOnClickListener {

            lifecycleScope.launch {
                // TODO 前の画面から受け取る
                val targetEasy = listOf(
                    0, 0, 1, 0, 3, 7, 0, 2, 0,
                    0, 0, 6, 0, 9, 0, 5, 3, 0,
                    0, 9, 2, 0, 0, 0, 1, 7, 0,
                    0, 0, 0, 6, 0, 3, 0, 8, 2,
                    0, 0, 0, 9, 7, 8, 0, 0, 0,
                    9, 8, 0, 2, 0, 1, 0, 0, 0,
                    0, 1, 4, 0, 0, 0, 0, 8, 6,
                    0, 3, 8, 0, 1, 0, 2, 0, 0,
                    0, 6, 0, 3, 8, 0, 4, 0, 0
                )

                viewModel.startSolve(targetEasy).join()
            }
        }

        viewModel.cells.observe(requireActivity()) {
            // cellを CellViewに渡し、 CellViewの中で cell.value, cell.candidatesを反映させる
        }
        return binding.root
    }
}

class SolverViewModel : ViewModel() {
    val inProgress = MutableLiveData(false)

    val cells = MutableLiveData<List<Cell>>()

    fun startSolve(ary: List<Int>) = viewModelScope.launch {
        inProgress.postValue(true)

        val solver = SudokuSolver().apply {
            this.callback = object : SudokuSolver.ProgressCallback {
                override fun onProgress(cells: List<Cell>) {
                    this@SolverViewModel.cells.postValue(cells)
                }
            }
        }

        solver.setup(ary)

        while (!solver.isSolved()) {
            val valueChanged = solver.execStep()
            if (!valueChanged) {
                break
            }
        }

        inProgress.postValue(false)
    }
}