package org.nunocky.sudokusolver.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.nunocky.sudokusolver.MyApplication
import org.nunocky.sudokusolver.SudokuRepository
import org.nunocky.sudokusolver.adapter.SudokuListAdapter
import org.nunocky.sudokusolver.databinding.FragmentSudokuListBinding

/**
 * 登録した問題一覧
 */
class SudokuListFragment : Fragment() {
    private lateinit var binding: FragmentSudokuListBinding

    private val viewModel: SudokuListViewModel by viewModels {
        val app = (requireActivity().application as MyApplication)
        val appDatabase = app.appDatabase
        SudokuListViewModel.Factory(SudokuRepository(appDatabase))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSudokuListBinding.inflate(inflater, container, false)
        //binding.viewModel = viewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SudokuListAdapter(emptyList())
        binding.recyclerView.layoutManager =
            GridLayoutManager(requireActivity(), 2, RecyclerView.VERTICAL, false)

        binding.recyclerView.adapter = adapter

        adapter.listener = object : SudokuListAdapter.OnItemClickListener {
            override fun onItemClicked(view: View, position: Int) {
                val entity = adapter.list[position]
                val action =
                    SudokuListFragmentDirections.actionSudokuListFragmentToEditFragment(entity.id)
                findNavController().navigate(action)
            }
        }

        binding.floatingActionButton.setOnClickListener {
            val action = SudokuListFragmentDirections.actionSudokuListFragmentToEditFragment(0)
            findNavController().navigate(action)
        }
    }
}

