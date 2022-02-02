package org.nunocky.sudokusolver.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.databinding.FragmentImportSudokuBinding

@AndroidEntryPoint
class ImportSudokuFragment : Fragment() {
    private lateinit var binding: FragmentImportSudokuBinding

    private val viewModel: ImportSudokuViewModel by viewModels()
//    {
//        val app = (requireActivity().application as MyApplication)
//        val appDatabase = app.appDatabase
//        ImportSudokuViewModel.Factory(requireActivity().application, SudokuRepository(appDatabase))
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImportSudokuBinding.inflate(inflater, container, false)
        //binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnExecute.setOnClickListener {

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startForResult.launch(intent)
        }
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            if (result?.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data

                uri?.let {
                    lifecycleScope.launch {
                        viewModel.execImport(it)
                        Toast.makeText(requireActivity(), "imported", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

}