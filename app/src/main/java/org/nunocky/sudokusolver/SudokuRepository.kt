package org.nunocky.sudokusolver

import androidx.lifecycle.LiveData
import org.nunocky.sudokusolver.database.AppDatabase
import org.nunocky.sudokusolver.database.SudokuEntity

class SudokuRepository(database: AppDatabase) {
    data class Filter(
        val checkImpossible: Boolean,
        val checkUntested: Boolean,
        val checkEasy: Boolean,
        val checkMedium: Boolean,
        val checkHard: Boolean,
        val checkExtreme: Boolean
    ) {
        fun toIntArray(): List<Int> {
            return ArrayList<Int>().apply {
                if (checkImpossible) {
                    add(0)
                }
                if (checkUntested) {
                    add(1)
                }
                if (checkEasy) {
                    add(2)
                }
                if (checkMedium) {
                    add(3)
                }
                if (checkHard) {
                    add(4)
                }
                if (checkExtreme) {
                    add(5)
                }
            }
        }
    }

    private val dao = database.getSudokuDAO()

    fun insert(entity: SudokuEntity) = dao.insert(entity)
    fun delete(entity: SudokuEntity) = dao.delete(entity)
    fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)
    fun update(entity: SudokuEntity) = dao.update(entity)
    fun findById(id: Long) = dao.findById(id)
    fun findAll(): LiveData<List<SudokuEntity>> = dao.findAll()
    fun findByDifficulties(difficulties: List<Int>) = dao.findByDifficulties(difficulties)
}