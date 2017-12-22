package com.example.sebastian.brulinski.arduinobluetooth.Observer

import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothStateObserversInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothStateSubjectsInterface
import com.example.sebastian.brulinski.arduinobluetooth.Activities.MainActivity

class BluetoothStateDirector : BluetoothStateSubjectsInterface {

    private val observers = ArrayList<BluetoothStateObserversInterface>()

    override fun registerObserver(observer: BluetoothStateObserversInterface) {
        observers.add(observer)
    }

    override fun unregisterObserver(observer: BluetoothStateObserversInterface) {
        observers.remove(observer)
    }

    override fun notifyAllObservers(state: MainActivity.Companion.BluetoothStates) {
        for (observer in observers) {
                observer.update(state)
        }
    }
}