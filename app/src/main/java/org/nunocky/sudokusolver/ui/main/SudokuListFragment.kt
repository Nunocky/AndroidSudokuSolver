package org.nunocky.sudokusolver.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.nunocky.sudokusolver.databinding.FragmentSudokuListBinding
import org.nunocky.sudokusolver.solver.SudokuSolver

/**
 * 登録した問題一覧
 */
class SudokuListFragment : Fragment() {
    private lateinit var binding: FragmentSudokuListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSudokuListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        val solver = SudokuSolver()
        solver.setup(targetEasy)

        binding.board.updateCells(solver.cells)

    }
}