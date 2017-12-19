package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.ConnectToDeviceInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.SetProperFragmentInterface
import com.example.sebastian.brulinski.arduinobluetooth.MainActivity
import com.example.sebastian.brulinski.arduinobluetooth.MyBluetooth
import com.example.sebastian.brulinski.arduinobluetooth.R
import com.example.sebastian.brulinski.arduinobluetooth.databinding.FragmentConnectToDeviceBinding

class ConnectToDevice : Fragment(), ConnectToDeviceInterface {

    private lateinit var binding: FragmentConnectToDeviceBinding

    //List elements
    lateinit var arrayAdapter: ArrayAdapter<String>
    private var devices = ArrayList<String>()
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private lateinit var myBluetooth: MyBluetooth

    private val TAG = "ConnectToDevice"
    private lateinit var connectHandler: Handler

    private lateinit var setProperFragmentCallback: SetProperFragmentInterface



    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_connect_to_device, container, false)
        /**
         * Set devices ListView
         */
        arrayAdapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, devices)

        binding.devicesListView.adapter = arrayAdapter

        handleDevicesListClick()

        connectHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                Looper.prepare()

                //keys
                val DEVICE_NAME = "device_name"
                val msgData = msg?.data

                val deviceName = msgData?.getString(DEVICE_NAME)
                Snackbar.make(binding.root, "${getString(R.string.connected_to_message)}: $deviceName", Snackbar.LENGTH_LONG).show()
            }
        }

        myBluetooth = MyBluetooth(activity, connectHandler)

        setPairedDevicesAtList()

        setProperFragmentCallback = activity as SetProperFragmentInterface

        binding.terminalButton.setOnClickListener {
            setProperFragmentCallback.setTerminalFragment()
        }

        return binding.root
    }



    private fun updateDevicesList(element: String) {
        devices.add(element)
        arrayAdapter.notifyDataSetChanged()
    }

    fun setPairedDevicesAtList() {
        pairedDevices = myBluetooth.getPairedDevices()

        for (device in pairedDevices) {
            devices.add("${device.name} : ${device.address}")
        }
        arrayAdapter.notifyDataSetChanged()
    }

    private fun handleDevicesListClick() {
        binding.devicesListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            myBluetooth.connectToDevice(pairedDevices.elementAt(position))
        }
    }

    override fun checkDevicesAdapter() {
        if (arrayAdapter.isEmpty)
            setPairedDevicesAtList()
    }

}
