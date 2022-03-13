package org.nunocky.sudokusolver.ui.main

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.nunocky.sudokusolver.SudokuImageProcessor
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val app: Application
) : ViewModel() {

    private val _result =
        MutableStateFlow<Result<Pair<Bitmap?, String?>>?>(Result.success(null to ""))
    val result = _result.asLiveData()

    fun process(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var srcBitmap = BitmapFactory.decodeFile(path)
            srcBitmap = srcBitmap.rotate(90)
            val length = min(srcBitmap.width, srcBitmap.height)
            srcBitmap = srcBitmap.cropCenter(length, length)

            // 数独ボードの正規化
            val sudokuImageProcessor = SudokuImageProcessor(app)

            sudokuImageProcessor.process(srcBitmap)
                .onSuccess {
                    _result.value = Result.success(it)
                }
                .onFailure {
                    _result.value = Result.failure(it)
                }
        }
    }

}