package com.example.sebastian.brulinski.arduinobluetooth.Models

import android.bluetooth.BluetoothDevice

class MyBluetoothDevice(val device: BluetoothDevice?, var connected: Boolean,  val type: DeviceType, val label: String?) {

    companion object {
        enum class DeviceType {
            FOUND, PAIRED, LABEL
        }
    }
}