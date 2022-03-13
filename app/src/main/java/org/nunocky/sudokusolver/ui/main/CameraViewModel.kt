package org.nunocky.sudokusolver.ui.main

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.SudokuImageProcessor
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val app: Application
) : ViewModel() {

    //    private val _result = MutableStateFlow<Pair<Bitmap?, String?>>(null to null)
    private val _result =
        MutableStateFlow<Result<Pair<Bitmap?, String?>>?>(Result.success(null to ""))
    val result = _result.asLiveData()

    fun process(bmp: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            // 数独ボードの正規化
            val sudokuImageProcessor = SudokuImageProcessor(app)

            sudokuImageProcessor.process(bmp)
                .onSuccess {
                    _result.value = Result.success(it)
                }
                .onFailure {
                    _result.value = Result.failure(it)
                }

//                .onSuccess {
//                    val board = it.first
//                    val v = it.second
//
//                }
//                .onFailure {
//
//                }

        }
    }

}