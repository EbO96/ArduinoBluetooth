package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.content.pm.ActivityInfo
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.sebastian.brulinski.arduinobluetooth.Activities.MainActivity
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothStateObserversInterface

import com.example.sebastian.brulinski.arduinobluetooth.R
import com.example.sebastian.brulinski.arduinobluetooth.databinding.FragmentVehicleControlBinding

class VehicleControlFragment : Fragment(), BluetoothStateObserversInterface {

    private lateinit var binding: FragmentVehicleControlBinding

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_vehicle_control, container, false)

        return binding.root
    }

    override fun update(state: MainActivity.Companion.BluetoothStates) {

    }

    override fun onDestroyView() {
        super.onDestroyView()

        MainActivity.mBluetoothStateDirector.unregisterObserver(this)
    }
}
