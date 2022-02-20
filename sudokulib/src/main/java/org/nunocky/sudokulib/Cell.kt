package org.nunocky.sudokulib

/**
 * 数字のマス目のデータ
 * value : 0は未確定、 1~9が入っていたら確定
 */
class Cell {
    var id: Int = 0
    var value: Int = 0
        set(value) {
            if (value < 0 || 9 < value) {
                throw IllegalArgumentException()
            }
            if (value != 0) {
                candidates = mutableSetOf()
            }
            field = value
//            parent?.notifyDataChanged()
        }

    var candidates: MutableSet<Int> = mutableSetOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
    var groups: Set<Group> = emptySet()
    var parent: SudokuSolver? = null

    val isFixed: Boolean
        get() = (0 < value)

    override fun toString(): String {
        return "$value"
    }
}