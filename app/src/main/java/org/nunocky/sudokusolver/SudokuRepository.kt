package org.nunocky.sudokusolver

import androidx.lifecycle.LiveData
import org.nunocky.sudokusolver.database.AppDatabase
import org.nunocky.sudokusolver.database.SudokuEntity

class SudokuRepository(database: AppDatabase) {
    private val dao = database.getSudokuDAO()

    fun insert(entity: SudokuEntity) = dao.insert(entity)
    fun delete(entity: SudokuEntity) = dao.delete(entity)
    fun update(entity: SudokuEntity) = dao.update(entity)
    fun findById(id: Long) = dao.findById(id)
    fun findAll(): LiveData<List<SudokuEntity>> = dao.findAll()
}