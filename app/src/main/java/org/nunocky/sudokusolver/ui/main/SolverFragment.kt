package org.nunocky.sudokusolver.ui.main

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nunocky.sudokulib.Cell
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

    private var solverJob: Job = Job().apply { cancel() }

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
            startSolve()
        }

        binding.btnReset.setOnClickListener {
            reset()
        }

        binding.btnStop.setOnClickListener {
            stopSolve()
        }

        lifecycleScope.launch {

            viewModel.solverStatusFlow.collect { status ->
                //.observe(viewLifecycleOwner) { status ->
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
                                it[difficulty]
                            }

                        // 難易度をデータベースに反映する (総当り方法だけのときは行わない)
                        when (viewModel.solverMethod.value) {
                            0, 1 -> {
                                viewModel.updateDifficulty(difficulty)
                            }
                        }

                        // TODO フォーマット付き文字列リソースにする
                        val message =
                            requireActivity().resources.getString(R.string.solver_success) + " ($difficultyStr)"
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

                    else -> {
                        throw RuntimeException("unknown status")
                    }
                }

                requireActivity().invalidateOptionsMenu()
            }
        }

        viewModel.stepSpeed.observe(viewLifecycleOwner) {
            preference.stepSpeed = viewModel.stepSpeed.value!!
        }

        viewModel.solverMethod.observe(viewLifecycleOwner) {
            preference.solverMethod = viewModel.solverMethod.value!!
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
            //isEnabled = (viewModel.canReset.value == true)
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

    private fun startSolve() {
        if (solverJob.isActive) {
            return
        }

        solverJob = viewModel.startSolve(Dispatchers.IO) { cells ->
            drawSudokuBoard(cells)
        }
    }

    private fun stopSolve() {
        solverJob.cancel()
        viewModel.stopSolver()
    }

    private fun reset() {
        viewModel.entityId.value?.let {
            loadSudoku(it)
        }
    }

    /**
     * 指定 idの数独をロードし画面に反映する
     * @param id SudokuEntityの id
     */
    private fun loadSudoku(id: Long) {
        if (id == 0L) {
            return
        }

        // TODO ViewModelに移動
        // 非同期で viewModel.loadSudokuを行い、それが終わったらセルの設定をおこなう。
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.elapsedTime.postValue(0L)
            viewModel.stepsFlow.value = 0
            viewModel.loadSudoku(id)

            withContext(Dispatchers.Main) {
                syncBoard()
            }
        }

        binding.sudokuBoard.updated = false
    }

    private fun drawSudokuBoard(cells: List<Cell>) {
        binding.sudokuBoard.cellViews.forEachIndexed { n, cellView ->
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

