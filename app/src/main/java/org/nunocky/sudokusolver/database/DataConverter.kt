package org.nunocky.sudokusolver.database

import androidx.room.TypeConverter
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
}