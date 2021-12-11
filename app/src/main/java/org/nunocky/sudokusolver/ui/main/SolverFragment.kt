package org.nunocky.sudokusolver.ui.main

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    private val args: SolverFragmentArgs by navArgs()
    private lateinit var binding: FragmentSolverBinding
    private val viewModel: SolverViewModel by viewModels()

    @Inject
    lateinit var preference: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val navController = findNavController()
        val currentBackStackEntry = navController.currentBackStackEntry!!
        val savedStateHandle = currentBackStackEntry.savedStateHandle

        savedStateHandle.getLiveData<Boolean>(EditFragment.KEY_SAVED)
            .observe(currentBackStackEntry, { success ->
                if (!success) {
                    val startDestination = navController.graph.startDestination
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(startDestination, true)
                        .build()
                    navController.navigate(startDestination, null, navOptions)
                } else {
                    // TODO SAVE時の entityIdを受け取る方法
                    reset()
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

        viewModel.entityId.observe(viewLifecycleOwner) { entityId ->
            if (entityId == 0L) {
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

        viewModel.solverMethod.observe(viewLifecycleOwner) {
            reset()
        }

        viewModel.solverReady.observe(viewLifecycleOwner) {
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
    }

    override fun onResume() {
        super.onResume()
        // 速度を sharedPreferenceから復元
         viewModel.stepSpeed.value= preference.stepSpeed
         viewModel.solverMethod.value = preference.solverMethod

//        viewModel.stepSpeed.value =
//            requireActivity().getPreferences(Context.MODE_PRIVATE).getInt("stepSpeed", 0)
//
//        viewModel.solverMethod.value =
//            requireActivity().getPreferences(Context.MODE_PRIVATE).getInt("solverMethod", 1)
    }

    override fun onPause() {
        super.onPause()
        // 速度を保存
        preference.stepSpeed = viewModel.stepSpeed.value ?: 0
        preference.solverMethod = viewModel.solverMethod.value ?: 0
//        requireActivity().getPreferences(Context.MODE_PRIVATE).edit {
//            putInt("stepSpeed", viewModel.stepSpeed.value ?: 0)
//            putInt("solverMethod", viewModel.solverMethod.value ?: 0)
//            commit()
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_solver, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_edit) {
            val action = SolverFragmentDirections.actionGlobalEditFragment(args.entityId)
            findNavController().navigate(action)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun stopSolve() = lifecycleScope.launch {
        viewModel.stopSolver()
    }

    private fun reset() = lifecycleScope.launch(Dispatchers.IO) {
     // TODO 遷移時にここが3回呼ばれている ... solverMethodが3回変更になっている
        loadSudoku()
        viewModel.resetSolver()
    }

    private suspend fun loadSudoku() {
        viewModel.loadSudoku(viewModel.entityId.value!!)
        // TODO セルの色を初期化する
        binding.sudokuBoard.cellViews.forEachIndexed { n, cellView ->
            cellView.apply {
                fixedNum = viewModel.solver.cells[n].value
                if (fixedNum != 0) {
                    setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.fixedCell))
                    candidates = IntArray(0)
                    showCandidates = false
                } else {
                    candidates = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9).toIntArray()
                }
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
                delay((viewModel.stepSpeed.value ?: 1) * 100L)
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
                when (viewModel.solverMethod.value ?: 1) {
                    0, 1 -> {
                        viewModel.updateDifficulty(args.entityId, difficulty)
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

