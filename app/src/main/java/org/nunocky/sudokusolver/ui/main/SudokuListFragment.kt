package org.nunocky.sudokusolver.ui.main

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.MyApplication
import org.nunocky.sudokusolver.R
import org.nunocky.sudokusolver.SudokuRepository
import org.nunocky.sudokusolver.adapter.SudokuListAdapter
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.databinding.FragmentSudokuListBinding
import org.nunocky.sudokusolver.ui.dialog.DeleteItemConfirmDialog
import kotlin.coroutines.suspendCoroutine


/**
 * 登録した問題一覧
 */
class SudokuListFragment : Fragment() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: FragmentSudokuListBinding

    private val viewModel: SudokuListViewModel by viewModels {
        val app = (requireActivity().application as MyApplication)
        val appDatabase = app.appDatabase
        SudokuListViewModel.Factory(SudokuRepository(appDatabase))
    }

//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
//        val navController = findNavController(R.id.nav_host_)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSudokuListBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        binding.recyclerView.layoutManager =
            GridLayoutManager(requireActivity(), 2, RecyclerView.VERTICAL, false)

        val adapter = SudokuListAdapter(emptyList())
        binding.recyclerView.adapter = adapter

        adapter.listener = object : SudokuListAdapter.OnItemClickListener {
            override fun onItemClicked(view: View, position: Int) {
                val entity = adapter.list[position]
                val action =
                    SudokuListFragmentDirections.actionSudokuListFragmentToEditFragment(entity.id)
                findNavController().navigate(action)
            }

            override fun onLongClick(view: View, position: Int): Boolean {
                val entity = adapter.list[position]
                confirmDelete(entity)
                return true
            }
        }

        binding.floatingActionButton.setOnClickListener {
            val action = SudokuListFragmentDirections.actionSudokuListFragmentToEditFragment(0)
            findNavController().navigate(action)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_sudoku_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_about) {
            findNavController().navigate(R.id.aboutFragment)
        }

        return super.onOptionsItemSelected(item)
    }

    private fun confirmDelete(entity: SudokuEntity) = lifecycleScope.launch {
        val confirm = openDeleteDialog()

        if (confirm) {
            viewModel.deleteItem(entity)
        }
    }

    private suspend fun openDeleteDialog() = suspendCoroutine<Boolean> { continuation ->
        val dialog = DeleteItemConfirmDialog(continuation)
        dialog.show(parentFragmentManager, "delete")
        // TODO parentとか childとかどう使い分ければいいの
    }

    override fun onResume() {
        super.onResume()
//        (activity as AppCompatActivity).supportActionBar?.title = "Sudoku Solver"
    }
}

