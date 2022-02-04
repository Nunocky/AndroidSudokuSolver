package org.nunocky.sudokusolver.ui.main

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.nunocky.sudokulib.Cell
import org.nunocky.sudokulib.SudokuSolver
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val currentBackStackEntry = navController.currentBackStackEntry!!
        val savedStateHandle = currentBackStackEntry.savedStateHandle

        savedStateHandle.getLiveData<Boolean>(EditFragment.KEY_SAVED)
            .observe(currentBackStackEntry, { success ->

                // TODO この辺の処理に問題 savedStateHandleの使い方?
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
            })
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

                // TODO エディタから戻ってきたとき、 id=0でここを通りまたエディタに行ってしまう
                //  誰が viewModel.entityIdを操作しているのか?
                //      -> savedStateHandleの仕業らしい
                val action = NavigationMainDirections.actionGlobalEditFragment(entityId = 0L)
                findNavController().navigate(action)
            }
        }

        binding.btnStart.setOnClickListener {
            viewModel.startSolver(callback)
        }

        binding.btnReset.setOnClickListener {
            reset()
        }

        binding.btnStop.setOnClickListener {
            stopSolve()
        }

        // UIのリスナーで実装し直す
//        viewModel.solverMethod.observe(viewLifecycleOwner) {
//            reset()
//        }

        viewModel.solverStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                SolverViewModel.Status.READY -> {
                    syncBoard()
                }
                SolverViewModel.Status.WORKING -> {}
                SolverViewModel.Status.SUCCESS -> {}
                SolverViewModel.Status.FAILED -> {}
                SolverViewModel.Status.INTERRUPTED -> {}
                SolverViewModel.Status.ERROR -> {}
                else -> {}
            }
        }
    }

    override fun onPause() {
        super.onPause()
        preference.stepSpeed = viewModel.stepSpeed.value!!
        preference.solverMethod = viewModel.solverMethod.value!!
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_solver, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_edit) {
            val action = SolverFragmentDirections.actionGlobalEditFragment(args.entityId)
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

    private fun stopSolve() = lifecycleScope.launch {
        viewModel.stopSolver()
    }

    private fun reset() {
        // TODO 遷移時にここが3回呼ばれている ... solverMethodが3回変更されている
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

        // 非同期で viewModel.loadSudokuを行い、それが終わったらセルの設定をおこなう。
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.loadSudoku(id)

            withContext(Dispatchers.Main) {
                syncBoard()
            }
        }

        binding.sudokuBoard.updated = false
    }

    private val callback = object : SudokuSolver.ProgressCallback {
        override fun onProgress(cells: List<Cell>) {
            binding.sudokuBoard.updated = false
            binding.sudokuBoard.cellViews.forEachIndexed { n, cellView ->
                cellView.fixedNum = cells[n].value
                cellView.candidates = cells[n].candidates.toIntArray()
            }

            runBlocking {
                // TODO ノーウェイト / ウェイトあり くらいの区分で良さそう
                delay(viewModel.stepSpeed.value!! * 100L)
            }
        }

        override fun onComplete(success: Boolean) {

            val difficulty = viewModel.solver.difficulty

            val difficultyStr =
                requireActivity().resources.getStringArray(R.array.difficulty).let {
                    it[difficulty]
                }

            val message = if (success)
                requireActivity().resources.getString(R.string.solver_success) + " ($difficultyStr)"
            else
                requireActivity().resources.getString(R.string.solver_fail)

            // 成功したときは難易度をデータベースに反映する
            if (success) {
                when (viewModel.solverMethod.value) {
                    0, 1 -> {
                        viewModel.updateDifficulty(difficulty)
                    }
                }
            }

            val bgColor = if (success)
                ContextCompat.getColor(requireContext(), R.color.solverSuccess)
            else
                ContextCompat.getColor(requireContext(), R.color.solverFail)

            val snackBar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            snackBar.view.setBackgroundColor(bgColor)
            snackBar.show()
        }
//
//        override fun onInterrupted() {
//            val snackBar = Snackbar.make(binding.root, "interrupted", Snackbar.LENGTH_SHORT)
//            val bgColor = ContextCompat.getColor(requireContext(), R.color.solverFail)
//
//            snackBar.view.setBackgroundColor(bgColor)
//            snackBar.show()
//        }
//
//        override fun onSolverError() {
//            val snackBar = Snackbar.make(binding.root, "solver error", Snackbar.LENGTH_SHORT)
//            val bgColor = ContextCompat.getColor(requireContext(), R.color.solverFail)
//
//            snackBar.view.setBackgroundColor(bgColor)
//            snackBar.show()
//        }
    }
}

