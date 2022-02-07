package org.nunocky.sudokusolver.adapter

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.util.*

class CalenderJsonAdapter : JsonAdapter<Calendar>() {

    @Synchronized
    @Throws(Exception::class)
    override fun fromJson(reader: JsonReader): Calendar? {
        val string = reader.nextString()
        return Calendar.getInstance().apply {
            timeInMillis = string.toLong()
        }
    }

    @Synchronized
    @Throws(Exception::class)
    override fun toJson(writer: JsonWriter, value: Calendar?) {
        writer.value(value?.timeInMillis ?: 0)
    }
}