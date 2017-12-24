package com.example.sebastian.brulinski.arduinobluetooth.Services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BluetoothConnectionService: Service() {

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}