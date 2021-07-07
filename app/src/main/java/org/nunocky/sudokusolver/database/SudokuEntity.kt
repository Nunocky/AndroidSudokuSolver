package org.nunocky.sudokusolver.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "sudoku")
data class SudokuEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    var cells: String = "000000000000000000000000000000000000000000000000000000000000000000000000000000000",
    var createdAt: Calendar = Calendar.getInstance(),
    var difficulty: Int? = 0
)

