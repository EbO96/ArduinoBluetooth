package com.example.sebastian.brulinski.arduinobluetooth.Interfaces

import com.example.sebastian.brulinski.arduinobluetooth.Activities.MainActivity

interface BluetoothStateSubjectsInterface {
    fun registerObserver(observer: BluetoothStateObserversInterface)
    fun unregisterObserver(observer: BluetoothStateObserversInterface)
    fun notifyAllObservers(state: MainActivity.Companion.BluetoothStates)
}