package org.nunocky.sudokusolver.solver

/**
 * Cellを行・列・3x3ブロックの単位で管理する
 */
class Group(val cells: Set<Cell>)