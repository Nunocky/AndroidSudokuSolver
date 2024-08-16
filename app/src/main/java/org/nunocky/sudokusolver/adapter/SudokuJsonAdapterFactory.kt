package org.nunocky.sudokusolver.adapter

import com.squareup.moshi.JsonAdapter
import se.ansman.kotshi.KotshiJsonAdapterFactory

@KotshiJsonAdapterFactory
object SudokuJsonAdapterFactory : JsonAdapter.Factory by KotshiSudokuJsonAdapterFactory
