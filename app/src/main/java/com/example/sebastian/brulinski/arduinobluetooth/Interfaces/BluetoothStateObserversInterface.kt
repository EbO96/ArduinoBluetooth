package com.example.sebastian.brulinski.arduinobluetooth.Interfaces

import com.example.sebastian.brulinski.arduinobluetooth.Activities.MainActivity

interface BluetoothStateObserversInterface {
    fun update(state: MainActivity.Companion.BluetoothStates)
}