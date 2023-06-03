package org.nunocky.sudokulib

enum class DIFFICULTY(val value: Int) {
    IMPOSSIBLE(0),
    UNDEF(1),
    EASY(2),
    MEDIUM(3),
    HARD(4),
    EXTREME(5),
}

//fun DIFFICULTY.toInt() : Int {
//    return when (this) {
//        DIFFICULTY.IMPOSSIBLE -> 0
//        DIFFICULTY.UNDEF -> 1
//        DIFFICULTY.EASY -> 2
//        DIFFICULTY.MEDIUM -> 3
//        DIFFICULTY.HARD -> 4
//        else -> 5
//    }
//}
//
//fun Int.toDIFFICULTY() : DIFFICULTY{
//    return when(this) {
//        0 -> DIFFICULTY.IMPOSSIBLE
//        1 -> DIFFICULTY.UNDEF
//        2 -> DIFFICULTY.EASY
//        3 -> DIFFICULTY.MEDIUM
//        4 -> DIFFICULTY.HARD
//        else -> DIFFICULTY.EXTREME
//    }
//}