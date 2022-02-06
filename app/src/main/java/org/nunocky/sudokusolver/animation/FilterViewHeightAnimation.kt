package org.nunocky.sudokusolver.animation

import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation

// 【Android】アニメーション付き開閉Viewの作り方
// https://qiita.com/farman0629/items/ed86059845551449a359

/**
 * view expand / collapse animation
 */
class FilterViewHeightAnimation(
    private val view: View,
    private var addHeight: Int,
    private val startHeight: Int
) : Animation() {
    override
    fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
        val newHeight = (startHeight + addHeight * interpolatedTime).toInt()
        view.layoutParams.height = newHeight

        if(newHeight == 0) {
            view.visibility = View.GONE
        } else {
            view.visibility = View.VISIBLE
        }

        view.requestLayout()
    }

    override
    fun willChangeBounds(): Boolean = true
}