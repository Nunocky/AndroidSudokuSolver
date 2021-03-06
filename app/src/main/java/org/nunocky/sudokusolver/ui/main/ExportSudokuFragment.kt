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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.databinding.FragmentExportSudokuBinding

@AndroidEntryPoint
class ExportSudokuFragment : Fragment() {
    private lateinit var binding: FragmentExportSudokuBinding
    private var fmt = 0 // 出力フォーマット (0:text, 1:json)

    private val viewModel: ExportSudokuViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExportSudokuBinding.inflate(inflater, container, false)
        //binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnExecute.setOnClickListener {
            fmt = 0

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, "sudoku_backup.txt")
            }
            startForResult.launch(intent)
        }

        binding.btnExportJson.setOnClickListener {
            fmt = 1
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "sudoku_backup.json")
            }
            startForResult.launch(intent)
        }
    }

    // startActivityForResultがDeprecatedになった対応方法
    //   https://buildersbox.corp-sansan.com/entry/2020/05/27/110000
    @ExperimentalCoroutinesApi
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            if (result?.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data

                uri?.let {
                    lifecycleScope.launch {

                        if (fmt == 0) {
                            viewModel.execExport(it)
                        } else {
                            viewModel.execExportJson(it)
                        }
                        Toast.makeText(requireActivity(), "exported", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
}


