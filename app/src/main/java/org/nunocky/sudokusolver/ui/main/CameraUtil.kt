package org.nunocky.sudokusolver.ui.main

import android.hardware.Camera
import android.hardware.camera2.CameraMetadata

/**
 * - 0
 * - 90 画面上部に対して時計回りに 90度
 * - 180
 * - 270 画面上部に対して反時計回りに 90度
 */
fun getCameraOrientation(): Int {
    val info = Camera.CameraInfo()
    Camera.getCameraInfo(CameraMetadata.LENS_FACING_BACK, info)
    return info.orientation
}
