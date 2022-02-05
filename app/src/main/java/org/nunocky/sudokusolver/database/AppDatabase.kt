package org.nunocky.sudokusolver.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SudokuEntity::class], version = 3)
@TypeConverters(DataConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getSudokuDAO(): SudokuDAO
}

// 難易度の追加
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE sudoku ADD COLUMN difficulty INTEGER")
    }
}

// サムネイル画像パスの追加
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE sudoku ADD COLUMN thumbnail TEXT")
    }
}

