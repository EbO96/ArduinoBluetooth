package com.example.sebastian.brulinski.arduinobluetooth.Interfaces

import android.view.View
import com.example.sebastian.brulinski.arduinobluetooth.Models.MyBluetoothDevice
import java.text.FieldPosition

interface ViewBindInterface {
    fun bindViews(device: MyBluetoothDevice, position: Int)
}