package org.nunocky.sudokusolver

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.hilt.android.HiltAndroidApp
import org.nunocky.sudokusolver.database.AppDatabase
import org.nunocky.sudokusolver.database.MIGRATION_1_2

@HiltAndroidApp
class MyApplication : Application() {

//    val appDatabase: AppDatabase by lazy {
//        Room.databaseBuilder(this@MyApplication, AppDatabase::class.java, "appDatabase")
//            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
//            .fallbackToDestructiveMigration()
//            .addMigrations(MIGRATION_1_2)
//            .build()
//    }
}