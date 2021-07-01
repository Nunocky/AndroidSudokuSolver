package org.nunocky.sudokusolver.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SudokuDAO {
    @Insert
    fun insert(entity: SudokuEntity) : Long

    @Delete
    fun delete(entity: SudokuEntity)

    @Update
    fun update(entity: SudokuEntity)

    @Query("select * from sudoku order by createdAt")
    fun findAll(): LiveData<List<SudokuEntity>>
}