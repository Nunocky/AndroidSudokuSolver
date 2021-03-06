package org.nunocky.sudokusolver.adapter

import com.squareup.moshi.JsonAdapter
import se.ansman.kotshi.KotshiJsonAdapterFactory

@KotshiJsonAdapterFactory
abstract class SudokuJsonAdapterFactory : JsonAdapter.Factory {
    companion object {
        val INSTANCE: SudokuJsonAdapterFactory = KotshiSudokuJsonAdapterFactory
    }
}
