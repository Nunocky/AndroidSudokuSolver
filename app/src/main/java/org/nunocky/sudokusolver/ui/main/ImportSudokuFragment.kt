package org.nunocky.sudokusolver.ui.main

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nunocky.sudokusolver.MyApplication
import org.nunocky.sudokusolver.database.SudokuEntity
import org.nunocky.sudokusolver.database.SudokuRepository
import org.nunocky.sudokusolver.databinding.FragmentImportSudokuBinding

class ImportSudokuViewModel(
    application: Application,
    private val repository: SudokuRepository
) : AndroidViewModel(application) {
    class Factory(private val application: Application, private val repository: SudokuRepository) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ImportSudokuViewModel(application, repository) as T
        }
    }

    suspend fun execImport(uri: Uri) = withContext(Dispatchers.IO) {
        val app = getApplication() as MyApplication
        app.contentResolver.openInputStream(uri).use { oStream ->
            oStream?.bufferedReader()?.use { reader ->

                val list = ArrayList<SudokuEntity>()

                var line: String? = ""
                do {
                    line = reader.readLine()?.trim()
                    if (line?.length == 81) {
                        list.add(
                            SudokuEntity(
                                id = 0,
                                cells = line,
                                difficulty = 1

                            )
                        )
                    }
                } while (line?.isNotEmpty() == true)

                repository.insert(list)
            }
        }
    }
}

class ImportSudokuFragment : Fragment() {
    private lateinit var binding: FragmentImportSudokuBinding

    private val viewModel: ImportSudokuViewModel by viewModels {
        val app = (requireActivity().application as MyApplication)
        val appDatabase = app.appDatabase
        ImportSudokuViewModel.Factory(requireActivity().application, SudokuRepository(appDatabase))
    }

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
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.toolbar)

        binding.btnExecute.setOnClickListener {

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, "sudoku_backup.txt")
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