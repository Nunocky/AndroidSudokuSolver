package org.nunocky.sudokusolver

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import org.nunocky.sudokusolver.database.AppDatabase

class MyApplication : Application() {

    val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(this@MyApplication, AppDatabase::class.java, "appDatabase")
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .fallbackToDestructiveMigration()
            .build()
    }

    override fun onCreate() {
        super.onCreate()
    }
}