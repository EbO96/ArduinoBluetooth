package com.example.sebastian.brulinski.arduinobluetooth.Interfaces

import android.bluetooth.BluetoothDevice
import android.view.View
import com.example.sebastian.brulinski.arduinobluetooth.Models.MyBluetoothDevice

interface BluetoothActionsInterface {
    fun writeToDevice(toWrite: ByteArray)
    fun readFromDevice()
    fun resetConnection()
    fun connectToDevice(device: BluetoothDevice)
    fun disconnectFromDevice()
    fun getPairedDevices(): Set<BluetoothDevice>
    fun startDiscoveringDevices()
    fun stopDiscoveringDevices()
    fun getConnectedDevice(): BluetoothDevice?
    fun getMyBluetoothDevices(): ArrayList<MyBluetoothDevice>
    fun isConnectedToDevice(): Boolean
    fun isBluetoothOn(): Boolean
}