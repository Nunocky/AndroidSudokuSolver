package org.nunocky.sudokusolver.database

import androidx.databinding.adapters.Converters
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.nunocky.sudokulib.DIFFICULTY
import se.ansman.kotshi.JsonSerializable
import java.util.*

@JsonSerializable
@Entity(tableName = "sudoku")
data class SudokuEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    var cells: String = "0".repeat(81),
    var createdAt: Calendar = Calendar.getInstance(),
    var difficulty: DIFFICULTY = DIFFICULTY.UNDEF,
    var thumbnail: String? = "",

    @Ignore
    var isChecked: Boolean = false
)
