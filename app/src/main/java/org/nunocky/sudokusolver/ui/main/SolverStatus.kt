package org.nunocky.sudokusolver.ui.main

/**
 * 解析機の状態
 * TODO これはライブラリに移動すべき
 */
enum class SolverStatus {
    INIT, // 初期状態、データをロードしていない
    READY, // データをロードして解析が可能な状態
    WORKING, // 解析実行中
    SUCCESS, // 解析成功 (終了)
    FAILED, // 解析失敗 (終了)
    INTERRUPTED, // 解析を中断した (終了)
    ERROR // エラーが発生した (終了)
}