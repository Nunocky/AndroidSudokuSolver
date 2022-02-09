package org.nunocky.sudokusolver.ui.main

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StableIdKeyProvider
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.nunocky.sudokusolver.FILTER_ANIMATION_DURATION
import org.nunocky.sudokusolver.R
import org.nunocky.sudokusolver.adapter.OnItemClickListener
import org.nunocky.sudokusolver.adapter.SudokuEntityDetailsLookup
import org.nunocky.sudokusolver.adapter.SudokuListAdapter
import org.nunocky.sudokusolver.animation.FilterViewHeightAnimation
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.databinding.FragmentSudokuListBinding


/**
 * 登録した問題一覧
 */
@AndroidEntryPoint
class SudokuListFragment : Fragment() {
    private lateinit var binding: FragmentSudokuListBinding

    //    private val viewModel: SudokuListViewModel by viewModels()
    private val viewModel: SudokuListViewModel by activityViewModels()

    private lateinit var adapter: SudokuListAdapter
    private var actionMode: ActionMode? = null
    private lateinit var tracker: SelectionTracker<Long>

//    private var filterViewHeight = 0
//    private lateinit var expandAnimation: Animation
//    private lateinit var collapseAnimation: Animation

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

        adapter = SudokuListAdapter(viewLifecycleOwner, viewModel)
        binding.recyclerView.adapter = adapter

        adapter.listener = object : OnItemClickListener {
            override fun onItemClicked(view: View, entity: SudokuEntity) {
                // 指定の問題を解くための画面遷移
                val action =
                    SudokuListFragmentDirections.actionSudokuListFragmentToSolverFragment(entity.id)
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

                // TODO ロングタップ時の処理 アニメーションでフィルタがたたまれたときに他のアイテムもロングタップ状態になるのに対応する
                //      多分この辺
//                if (2 < tracker.selection.size()) {
//                    tracker.selection.forEach { id ->
//                        if (tracker.selection.first() != id) {
//                            tracker.deselect(id)
//                        }
//                    }
//                }

                when {
                    tracker.hasSelection() && actionMode == null -> {
                        actionMode =
                            (requireActivity() as AppCompatActivity).startSupportActionMode(
                                actionModeCallback
                            )

                        actionMode?.title = resources.getString(R.string.deleteItem)
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

        addFilterViewObserver()
    }

    private val filterViewObserver = ViewTreeObserver.OnWindowFocusChangeListener {
        if (it) {
//            binding.filterList.root.also { v ->
//                filterViewHeight = v.height
//
//                collapseAnimation =
//                    FilterViewHeightAnimation(v, -filterViewHeight, filterViewHeight).apply {
//                        setAnimationListener(object : Animation.AnimationListener {
//                            override fun onAnimationStart(animation: Animation?) {
//                            }
//
//                            override fun onAnimationEnd(animation: Animation?) {
//                                binding.filterList.root.visibility = View.GONE
//                            }
//
//                            override fun onAnimationRepeat(animation: Animation?) {
//                            }
//                        })
//                    }
//                collapseAnimation.duration = FILTER_ANIMATION_DURATION
//
//                expandAnimation = FilterViewHeightAnimation(v, filterViewHeight, 0).apply {
//                    setAnimationListener(object : Animation.AnimationListener {
//                        override fun onAnimationStart(animation: Animation?) {
//                            binding.filterList.root.visibility = View.VISIBLE
//                        }
//
//                        override fun onAnimationEnd(animation: Animation?) {
//                        }
//
//                        override fun onAnimationRepeat(animation: Animation?) {
//                        }
//                    })
//                }
//                expandAnimation.duration = FILTER_ANIMATION_DURATION
//            }
        } else {
            removeFilterViewObserver()
        }
    }

    private fun addFilterViewObserver() {
        view?.viewTreeObserver?.addOnWindowFocusChangeListener(filterViewObserver)
    }

    private fun removeFilterViewObserver() {
        view?.viewTreeObserver?.removeOnWindowFocusChangeListener(filterViewObserver)
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Log.d(TAG, "onCreateActionMode")
            mode.menuInflater.inflate(R.menu.menu_item_select, menu)
            viewModel.isActionMode.value = true
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
//            binding.filterList.root.clearAnimation()
//            binding.filterList.root.startAnimation(collapseAnimation)
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            if (item.itemId == R.id.action_delete) {
                val ids = tracker.selection.toList()
                viewModel.deleteItems(ids)

                // Snackbar表示。復元機能も
                val snackBar = Snackbar.make(binding.root, "deleted", Snackbar.LENGTH_SHORT)
                    .setAction(
                        "restore"
                    ) {
                        viewModel.restoreDeletedItems()
                    }

                snackBar.show()
            }

            // ActionModeを解除したときに RecyclerViewの選択状態も解除
            tracker.clearSelection()
            return false
        }

        // アクションモード解除
        override fun onDestroyActionMode(mode: ActionMode?) {
            // フィルタ選択ビューを再表示
//            binding.filterList.root.clearAnimation()
//            binding.filterList.root.startAnimation(expandAnimation)

            actionMode = null

            // RecyclerViewの選択状態を解除
            tracker.clearSelection()
            viewModel.isActionMode.value = false
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
            R.id.action_filter -> {
                val action =
                    SudokuListFragmentDirections.actionSudokuListFragmentToFilterDialogFragment()
                findNavController().navigate(action)
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