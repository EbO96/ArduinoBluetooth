package com.example.sebastian.brulinski.arduinobluetooth.Interfaces

import android.bluetooth.BluetoothSocket
import com.example.sebastian.brulinski.arduinobluetooth.Helper.MyBluetooth

interface TerminalInterface {
    fun getMyBluetooth(): MyBluetooth?
    //fun getMyBluetooth(): BluetoothConnectionService?

    fun getConnectedDeviceSocket(): BluetoothSocket?
}