package org.nunocky.sudokusolver.ui.main

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StableIdKeyProvider
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.MyApplication
import org.nunocky.sudokusolver.R
import org.nunocky.sudokusolver.SudokuRepository
import org.nunocky.sudokusolver.adapter.SudokuEntityDetailsLookup
import org.nunocky.sudokusolver.adapter.SudokuListAdapter
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.databinding.FragmentSudokuListBinding
import org.nunocky.sudokusolver.ui.dialog.DeleteItemConfirmDialog
import kotlin.coroutines.suspendCoroutine


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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

        adapter = SudokuListAdapter(emptyList())
        binding.recyclerView.adapter = adapter

        adapter.listener = object : SudokuListAdapter.OnItemClickListener {
            override fun onItemClicked(view: View, position: Int) {
                val entity = adapter.list[position]
                val action =
                    SudokuListFragmentDirections.actionSudokuListFragmentToEditFragment(entity.id)
                findNavController().navigate(action)
            }
        }

        tracker = SelectionTracker.Builder(
            "sudoku-selection-id",
            binding.recyclerView,
            StableIdKeyProvider(binding.recyclerView),
            SudokuEntityDetailsLookup(binding.recyclerView),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()

        tracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
//            override fun onSelectionRefresh() {
//                super.onSelectionRefresh()
//            }
//
//            override fun onSelectionRestored() {
//                super.onSelectionRestored()
//            }
//
//            override fun onItemStateChanged(key: Long, selected: Boolean) {
//                super.onItemStateChanged(key, selected)
//            }

            override fun onSelectionChanged() {
                super.onSelectionChanged()
                when {
                    tracker.hasSelection() && actionMode == null -> {
                        actionMode =
                            (requireActivity() as AppCompatActivity).startSupportActionMode(
                                actionModeCallback
                            )
                    }

                    !tracker.hasSelection() && actionMode != null -> {
                        actionMode?.finish()
                        actionMode = null
                    }

                    else -> {
                    }
                }
            }
        })

        adapter.tracker = tracker

        binding.floatingActionButton.setOnClickListener {
            val action = SudokuListFragmentDirections.actionSudokuListFragmentToEditFragment(0)
            findNavController().navigate(action)
        }
    }

    private lateinit var adapter: SudokuListAdapter
    private var actionMode: ActionMode? = null
    private lateinit var tracker: SelectionTracker<Long>

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            Log.d(TAG, "onCreateActionMode")

            menu?.add("Delete") // TODO xmlで inflateするほどのことか
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            Log.d(TAG, "onPrepareActionMode")
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            Log.d(TAG, "onActionItemClicked")

            // TODO deleteボタンタップ時の処理
            val ids = tracker.selection.toList()
            //Log.d(TAG, ids.joinToString(" "))
            viewModel.deleteItems(ids)

            // ActionModeを解除したときに RecyclerViewの選択状態も解除
            tracker.clearSelection()
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            Log.d(TAG, "onDestroyActionMode")
            actionMode = null

            // ActionModeを解除したときに RecyclerViewの選択状態も解除
            tracker.clearSelection()
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

    companion object {
        private const val TAG = "SudokuListFragment"
    }
}

// 参考にしたコード
// https://android.gcreate.jp/recycler_view_multiselection_with_action_mode/
// https://github.com/gen0083/SampleRecyclerViewMultipleSelection/blob/master/app/src/main/java/jp/gcreate/samplerecyclerviewmultipleselection/MainActivity.kt

// Two actionbars when starting ActionMode
// https://stackoverflow.com/questions/37629856/two-actionbars-when-starting-actionmode