package org.nunocky.sudokusolver.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EditViewModel : ViewModel() {
    var currentValue = MutableLiveData(0)
}