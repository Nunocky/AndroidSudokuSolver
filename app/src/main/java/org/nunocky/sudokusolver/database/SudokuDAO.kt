package org.nunocky.sudokusolver.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SudokuDAO {
    @Insert
    fun insert(entity: SudokuEntity): Long

    @Delete
    fun delete(entity: SudokuEntity)

    @Query("delete from sudoku where id in (:ids)")
    fun deleteByIds(ids : List<Long>)

    @Update
    fun update(entity: SudokuEntity)

    @Query("select * from sudoku where id=:id")
    fun findById(id: Long): SudokuEntity?

    @Query("select * from sudoku order by createdAt")
    fun findAll(): LiveData<List<SudokuEntity>>

    @Query("select * from sudoku where difficulty in (:difficulties)")
    fun findByDifficulties(difficulties: List<Int>): LiveData<List<SudokuEntity>>
}