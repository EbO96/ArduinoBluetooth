package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.sebastian.brulinski.arduinobluetooth.Activities.MainActivity
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothStateObserversInterface
import com.example.sebastian.brulinski.arduinobluetooth.R
import com.example.sebastian.brulinski.arduinobluetooth.databinding.FragmentVehicleControlBinding

class VehicleControlFragment : Fragment(), BluetoothStateObserversInterface {

    private lateinit var binding: FragmentVehicleControlBinding
    private val TAG = "VehicleControlFragment"


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_vehicle_control, container, false)

        return binding.root
    }


    override fun update(state: MainActivity.Companion.BluetoothStates) {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        (activity as MainActivity).supportActionBar?.show()
        MainActivity.mBluetoothStateDirector.unregisterObserver(this)
    }

}
