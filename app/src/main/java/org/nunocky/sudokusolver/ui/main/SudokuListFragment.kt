package org.nunocky.sudokusolver.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.nunocky.sudokusolver.databinding.FragmentSudokuListBinding

/**
 * 登録した問題一覧
 */
class SudokuListFragment : Fragment() {
    private lateinit var binding: FragmentSudokuListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSudokuListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.numberCellView.candidates = arrayOf(1, 5, 9).toIntArray()
        binding.numberCellView.fixedNum = 8
    }

    override fun onResume() {
        super.onResume()
    }

}