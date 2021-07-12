package org.nunocky.sudokusolver.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import se.ansman.kotshi.JsonSerializable
import java.util.*

@JsonSerializable
@Entity(tableName = "sudoku")
data class SudokuEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    var cells: String = "0".repeat(81),
    var createdAt: Calendar = Calendar.getInstance(),
    var difficulty: Int? = 1 // "UNTESTED"
)

