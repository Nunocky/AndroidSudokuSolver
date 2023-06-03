package org.nunocky.sudokusolver.ui.main

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.nunocky.sudokulib.Cell
import org.nunocky.sudokulib.METHOD
import org.nunocky.sudokulib.toInt
import org.nunocky.sudokusolver.NavigationMainDirections
import org.nunocky.sudokusolver.Preference
import org.nunocky.sudokusolver.R
import org.nunocky.sudokusolver.databinding.FragmentSolverBinding
import javax.inject.Inject

/**
 * 問題を解く
 *
 *
 */
@AndroidEntryPoint
class SolverFragment : Fragment() {
    private lateinit var binding: FragmentSolverBinding
    private val args: SolverFragmentArgs by navArgs()
    private val viewModel: SolverViewModel by viewModels()

    @Inject
    lateinit var preference: Preference

    private val navController by lazy { findNavController() }

    private lateinit var currentBackStackEntry: NavBackStackEntry
    private lateinit var savedStateHandle: SavedStateHandle

//    private var solverJob: Job = Job().apply { cancel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        currentBackStackEntry = navController.currentBackStackEntry!!
        savedStateHandle = currentBackStackEntry.savedStateHandle

        savedStateHandle.getLiveData<Boolean>(EditFragment.KEY_SAVED)
            .observe(currentBackStackEntry) { success ->
                val entityId = savedStateHandle.get<Long>("entityId")
                if (success) {
                    viewModel.entityId.value = entityId
                } else {
                    // 編集画面で保存しなかった -> リスト画面に戻る。ただし id!=0ならとどまる
                    if (entityId == 0L) {
                        navController.popBackStack()
                    } else {
                        viewModel.entityId.value = entityId
                    }
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSolverBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.entityId.observe(viewLifecycleOwner) { id ->
            if (id != null && id != 0L) {
                loadSudoku(id)
            } else {

                val action = NavigationMainDirections.actionGlobalEditFragment(
                    title = resources.getString(R.string.newItem),
                    entityId = 0L
                )
                navController.navigate(action)
            }
        }

        binding.btnStart.setOnClickListener {
            viewModel.startSolve(Dispatchers.IO) { cells ->
                drawSudokuBoard(cells)
            }
        }

        binding.btnReset.setOnClickListener {
            viewModel.entityId.value?.let {
                loadSudoku(it)
            }
        }

        binding.btnStop.setOnClickListener {
            viewModel.stopSolver()
        }

        lifecycleScope.launch {
            viewModel.solverStatus
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { status ->
                    when (status) {
                        SolverStatus.INIT -> {}

                        SolverStatus.READY -> {
                            syncBoard()
                        }

                        SolverStatus.WORKING -> {}

                        SolverStatus.SUCCESS -> {
                            val difficulty = viewModel.solver.difficulty

                            val difficultyStr =
                                requireActivity().resources.getStringArray(R.array.difficulty).let {
                                    it[difficulty.toInt()]
                                }

                            // 難易度をデータベースに反映する (総当り方法だけのときは行わない)
                            when (viewModel.solverMethod.value) {
                                METHOD.ONLY_STANDARD, METHOD.STANDARD_AND_DFS -> {
                                    viewModel.updateDifficulty(difficulty)
                                }
                            }

                            val message =
                                requireActivity().resources.getString(
                                    R.string.solver_success,
                                    difficultyStr
                                )
                            showSnackbar(true, message)
                        }

                        SolverStatus.FAILED -> {
                            showSnackbar(false, "FAILED")
                        }

                        SolverStatus.INTERRUPTED -> {
                            showSnackbar(false, "INTERRUPTED")
                        }

                        SolverStatus.ERROR -> {
                            showSnackbar(false, "ERROR")
                        }
                    }

                    requireActivity().invalidateOptionsMenu()
                }
        }

        viewModel.stepSpeed.observe(viewLifecycleOwner) {
            preference.stepSpeed = viewModel.stepSpeed.value!!
        }

        viewModel.solverMethodIndex.observe(viewLifecycleOwner) {
            preference.solverMethodIndex = it
        }

        viewModel.canReset.observe(viewLifecycleOwner) {
            requireActivity().invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_solver, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_edit).apply {
            isVisible = (viewModel.canReset.value == true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_edit) {
            val entityId = savedStateHandle.get<Long>("entityId") ?: args.entityId

            val action = SolverFragmentDirections.actionGlobalEditFragment(
                title = resources.getString(R.string.editItem),
                entityId = entityId
            )
            navController.navigate(action)
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * [SolverViewModel]の情報を UIに反映する
     */
    private fun syncBoard() {
        // 盤面を初期化
        // TODO セルの色を初期化する
        binding.sudokuBoard.cellViews.forEachIndexed { n, cellView ->
            cellView.apply {
                fixedNum = viewModel.solver.cells[n].value
                if (fixedNum != 0) {
                    setBackgroundColor(
                        ContextCompat.getColor(
                            requireActivity(),
                            R.color.fixedCell
                        )
                    )
                    candidates = IntArray(0)
                    showCandidates = false
                } else {
                    candidates = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9).toIntArray()
                }
            }
        }
        binding.sudokuBoard.updated = false
    }

    /**
     * 指定 idの数独をロードし画面に反映する
     * @param id SudokuEntityの id
     */
    private fun loadSudoku(id: Long) {
        if (id == 0L) {
            return
        }

        // 非同期で viewModel.loadSudokuを行い、それが終わったらセルの設定をおこなう。
        viewModel.loadSudoku(id, Dispatchers.IO) {
            syncBoard()
            binding.sudokuBoard.updated = false
        }
    }

    private fun drawSudokuBoard(cells: List<Cell>) {
        binding.sudokuBoard.cellViews.forEachIndexed { n, cellView ->
            cellView.updated = (cellView.fixedNum != cells[n].value)
            cellView.fixedNum = cells[n].value
            cellView.candidates = cells[n].candidates.toIntArray()
        }
    }

    private fun showSnackbar(success: Boolean, message: String) {
        val bgColor = if (success)
            ContextCompat.getColor(requireContext(), R.color.solverSuccess)
        else
            ContextCompat.getColor(requireContext(), R.color.solverFail)

        val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        snackBar.view.setBackgroundColor(bgColor)
        snackBar.show()
    }
}

