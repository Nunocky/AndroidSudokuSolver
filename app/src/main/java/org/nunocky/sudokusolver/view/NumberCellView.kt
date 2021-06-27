package org.nunocky.sudokusolver.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class NumberCellView : View {
    var fixedNum = 0
        set(value) {
            field = value
            invalidate()
        }

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        val frameLayout = FrameLayout(context)

    }

}