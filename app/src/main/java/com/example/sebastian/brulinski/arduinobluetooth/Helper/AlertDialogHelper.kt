package com.example.sebastian.brulinski.arduinobluetooth.Helper

import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AlertDialog


class AlertDialogHelper {
    companion object {
        fun <T> showAlert(context: Context, title: String?, message: String?, cancelable: Boolean,
                          posButton: String, negButton: String, clickedPos: () -> T, clickedNeg: () -> T) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setCancelable(cancelable)

            builder.setPositiveButton(posButton, DialogInterface.OnClickListener { _, _ ->
                clickedPos()
            })

            builder.setNegativeButton(negButton, DialogInterface.OnClickListener { _, _ ->
                clickedNeg()
            })

            builder.create().show()
        }
    }
}