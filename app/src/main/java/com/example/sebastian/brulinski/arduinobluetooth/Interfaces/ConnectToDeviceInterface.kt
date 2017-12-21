package com.example.sebastian.brulinski.arduinobluetooth.Interfaces

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.example.sebastian.brulinski.arduinobluetooth.Helper.MyBluetooth

interface ConnectToDeviceInterface {
    fun checkDevicesAdapter()
    fun getMyBluetooth(): MyBluetooth?
    fun getConnectedDevice(): BluetoothDevice?
    fun getDeviceSocket(): BluetoothSocket?
}