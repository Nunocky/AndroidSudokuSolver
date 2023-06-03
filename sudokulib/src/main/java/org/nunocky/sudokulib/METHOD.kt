package org.nunocky.sudokulib

enum class METHOD {
    ONLY_STANDARD,
    ONLY_DFS,
    STANDARD_AND_DFS,
}

fun METHOD.toInt(): Int {
    return when (this) {
        METHOD.ONLY_STANDARD -> 0
        METHOD.ONLY_DFS -> 1
        else -> 2
    }
}

fun Int.toMETHOD(): METHOD {
    return when (this) {
        0 -> METHOD.ONLY_STANDARD
        1 -> METHOD.ONLY_DFS
        else -> METHOD.STANDARD_AND_DFS
    }
}