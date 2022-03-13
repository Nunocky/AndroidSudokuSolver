package org.nunocky.sudokusolver.ui.main

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * @param rotation clockwise degree (0, 90, 180, 270)
 */
fun Bitmap.rotate(rotation: Int): Bitmap {
    val matrix = Matrix()

    when (rotation) {
        90, 270 -> {
            matrix.postRotate(rotation.toFloat())
        }
        180 -> {
            matrix.postRotate(180f)
        }
        else -> {
        }
    }

    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, false)
}

//@IntDef(0, 90, 180, 270)
//@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
//annotation class RotationDegreesValue

fun Bitmap.cropCenter(w: Int, h: Int): Bitmap {
    return Bitmap.createBitmap(this, (width - w) / 2, (height - h) / 2, w, h)
}
