package com.example.sebastian.brulinski.arduinobluetooth.AsyncTasks

import android.os.AsyncTask


open class MyBluetoothJobExecutor() : AsyncTask<Void, Void, String>() {
    override fun doInBackground(vararg p0: Void?): String {
        return "Bluetooth background long running task started"
    }
}