package org.nunocky.sudokusolver.database

import androidx.room.*
import java.util.*

@Entity(tableName = "sudoku")
data class SudokuEntity(
    @PrimaryKey(autoGenerate = true) var id: Long,
    var cells: String,
    var createdAt: Calendar
)

