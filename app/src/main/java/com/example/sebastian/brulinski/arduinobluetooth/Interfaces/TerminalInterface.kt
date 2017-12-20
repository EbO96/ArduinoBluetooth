package com.example.sebastian.brulinski.arduinobluetooth.Interfaces

import android.bluetooth.BluetoothSocket
import com.example.sebastian.brulinski.arduinobluetooth.MyBluetooth

interface TerminalInterface {
    fun getMyBluetooth(): MyBluetooth?
    fun getConnectedDeviceSocket(): BluetoothSocket?
}