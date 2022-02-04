package org.nunocky.sudokusolver.ui.main

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StableIdKeyProvider
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.nunocky.sudokusolver.R
import org.nunocky.sudokusolver.adapter.OnItemClickListener
import org.nunocky.sudokusolver.adapter.SudokuEntityDetailsLookup
import org.nunocky.sudokusolver.adapter.SudokuListAdapter
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.databinding.FragmentSudokuListBinding


/**
 * 登録した問題一覧
 */
@AndroidEntryPoint
class SudokuListFragment : Fragment() {
    private lateinit var binding: FragmentSudokuListBinding
    private lateinit var adapter: SudokuListAdapter
    private var actionMode: ActionMode? = null
    private lateinit var tracker: SelectionTracker<Long>

    private val viewModel: SudokuListViewModel by viewModels()

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

        viewModel.filter.observe(requireActivity()) {
            requireActivity().let { activity ->
                activity.getPreferences(Context.MODE_PRIVATE).edit {
                    putBoolean("filterImpossible", viewModel.filterImpossible.value ?: true)
                    putBoolean("filterUnTested", viewModel.filterUnTested.value ?: true)
                    putBoolean("filterEasy", viewModel.filterEasy.value ?: true)
                    putBoolean("filterMedium", viewModel.filterMedium.value ?: true)
                    putBoolean("filterHard", viewModel.filterHard.value ?: true)
                    putBoolean("filterExtreme", viewModel.filterExtreme.value ?: true)
                    commit()
                }
            }
        }

        binding.recyclerView.layoutManager =
            GridLayoutManager(requireActivity(), 2, RecyclerView.VERTICAL, false)

//        adapter = SudokuListAdapter(emptyList())
        adapter = SudokuListAdapter()
        binding.recyclerView.adapter = adapter

        adapter.listener = object : OnItemClickListener {
            override fun onItemClicked(view: View, entity: SudokuEntity) {
                // 指定の問題を解くための画面遷移
                val action = SudokuListFragmentDirections.actionSudokuListFragmentToSolverFragment(entity.id)
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

                        actionMode?.title = resources.getString(R.string.delete)
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

        // 問題を新規作成するための画面遷移
        binding.floatingActionButton.setOnClickListener {
            val action =
                SudokuListFragmentDirections.actionSudokuListFragmentToSolverFragment(0L)
            findNavController().navigate(action)
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Log.d(TAG, "onCreateActionMode")
            mode.menuInflater.inflate(R.menu.menu_item_select, menu)
            return true
        }

        // TODO 修正する
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Log.d(TAG, "onPrepareActionMode")
//            binding.filterList.root.visibility = View.GONE
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            //Log.d(TAG, "onActionItemClicked")
            if (item.itemId == R.id.action_delete) {
                val ids = tracker.selection.toList()
                //Log.d(TAG, ids.joinToString(" "))
                viewModel.deleteItems(ids)

                // Snackbar表示。復元機能も
                Snackbar.make(binding.root, "deleted", Snackbar.LENGTH_SHORT)
                    .setAction(
                        "restore"
                    ) {
                        viewModel.restoreDeletedItems()
                    }
                    .show()
            }

            // ActionModeを解除したときに RecyclerViewの選択状態も解除
            tracker.clearSelection()
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            //Log.d(TAG, "onDestroyActionMode")
            // TODO 修正する
//            binding.filterList.root.visibility = View.VISIBLE
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
        when (item.itemId) {
            R.id.action_about -> {
                findNavController().navigate(R.id.aboutFragment)
                return true
            }
            R.id.action_export -> {
                findNavController().navigate(R.id.exportSudokuFragment)
                return true
            }
            R.id.action_import -> {
                findNavController().navigate(R.id.importSudokuFragment)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        requireActivity().let { activity ->
            val prefs = activity.getPreferences(Context.MODE_PRIVATE)
            viewModel.filterImpossible.value = prefs.getBoolean("filterImpossible", true)
            viewModel.filterUnTested.value = prefs.getBoolean("filterUnTested", true)
            viewModel.filterEasy.value = prefs.getBoolean("filterEasy", true)
            viewModel.filterMedium.value = prefs.getBoolean("filterMedium", true)
            viewModel.filterHard.value = prefs.getBoolean("filterHard", true)
            viewModel.filterExtreme.value = prefs.getBoolean("filterExtreme", true)
        }
    }
}

// 参考にしたコード
// https://android.gcreate.jp/recycler_view_multiselection_with_action_mode/
// https://github.com/gen0083/SampleRecyclerViewMultipleSelection/blob/master/app/src/main/java/jp/gcreate/samplerecyclerviewmultipleselection/MainActivity.kt

// Two actionbars when starting ActionMode
// https://stackoverflow.com/questions/37629856/two-actionbars-when-starting-actionmode