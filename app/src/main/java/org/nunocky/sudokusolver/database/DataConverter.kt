package org.nunocky.sudokusolver.database

import androidx.room.TypeConverter
import org.nunocky.sudokulib.DIFFICULTY
import org.nunocky.sudokulib.toDIFFICULTY
import org.nunocky.sudokulib.toInt
import java.util.*

class DataConverter {
    @TypeConverter
    fun calenderToLong(calendar: Calendar): Long {
        return calendar.timeInMillis
    }

    @TypeConverter
    fun longToCalendar(value: Long): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = value
        }
    }

    @TypeConverter
    fun difficultyToInt(difficulty: DIFFICULTY) : Int = difficulty.toInt()

    @TypeConverter
    fun intToDifficulty(value : Int) : DIFFICULTY = value.toDIFFICULTY()
}