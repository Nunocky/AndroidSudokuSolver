package org.nunocky.sudokulib

/**
 * 数字のマス目のデータ
 * value : 0は未確定、 1~9が入っていたら確定
 */
class Cell(
    val id: Int = 0
) {
    var value: Int = 0
        set(value) {
            if (value < 0 || 9 < value) {
                throw IllegalArgumentException()
            }
            if (value != 0) {
                _candidates.clear()
            }
            field = value
        }

    private var _candidates: MutableSet<Int> = mutableSetOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
    val candidates: Set<Int> = _candidates

    /**
     * 候補から指定した値を削除する
     */
    fun removeCandidate(value: Int) {
        _candidates.remove(value)
    }

    /**
     * 候補をセットする
     */
    fun setCandidates(values: Set<Int>) {
        _candidates.clear()
        _candidates.addAll(values)
    }

    val isFixed: Boolean
        get() = (0 < value)

    override fun toString(): String {
        return "$value"
    }
}