package org.nunocky.sudokulib

/**
 * Cellを行・列・3x3ブロックの単位で管理する
 */
class Group(val id: Int, val cells: Set<Cell>)