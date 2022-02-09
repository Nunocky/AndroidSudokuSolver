package org.nunocky.sudokusolver.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.nunocky.sudokusolver.databinding.DialogFilterBinding
import org.nunocky.sudokusolver.ui.main.SudokuListViewModel

@AndroidEntryPoint
class FilterDialogFragment : DialogFragment() {
    private lateinit var binding: DialogFilterBinding
    val viewModel: SudokuListViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFilterBinding.inflate(layoutInflater, null, false)
        binding.viewModel = viewModel
//        binding.lifecycleOwner = viewLifecycleOwner

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle("Filter")
            .setPositiveButton("Close") { d, v ->
                d.dismiss()
            }
            .create()
    }
}