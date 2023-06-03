package org.nunocky.sudokusolver

import android.app.Application
import android.content.Context
import org.nunocky.sudokulib.METHOD
import org.nunocky.sudokulib.toInt
import org.nunocky.sudokulib.toMETHOD
import javax.inject.Inject
import javax.inject.Singleton

// TODO Jetpackの新しいやつに書き換える
// https://proandroiddev.com/is-jetpack-datastore-a-replacement-for-sharedpreferences-efe92d02fcb3
@Singleton
class Preference @Inject constructor(
    application: Application
) {
    private val sharedPreference = application.getSharedPreferences("app", Context.MODE_PRIVATE)

    var stepSpeed: Int
        get() =
            sharedPreference.getInt("stepSpeed", 0)
        set(value) {
            sharedPreference.edit()
                .putInt("stepSpeed", value)
                .apply()
        }

    var solverMethod: METHOD
        get() =
            (sharedPreference.getInt("solverMethod", 1)).toMETHOD()
        set(value) {
            sharedPreference.edit()
                .putInt("solverMethod", value.toInt())
                .apply()
        }
}