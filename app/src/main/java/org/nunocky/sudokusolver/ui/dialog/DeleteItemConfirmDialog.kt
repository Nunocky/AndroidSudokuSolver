package org.nunocky.sudokusolver.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class DeleteItemConfirmDialog(private val continuation: Continuation<Boolean>) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireActivity())
            .setTitle("Delete")
            .setMessage("delete this item")
            .setPositiveButton("OK", listener)
            .setNegativeButton("Cancel", listener)
            .create()
    }

    private val listener =
        DialogInterface.OnClickListener { _,
                                          which ->
            continuation.resume(which == DialogInterface.BUTTON_POSITIVE)
        }
}