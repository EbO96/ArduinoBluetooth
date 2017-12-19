package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.bluetooth.BluetoothDevice
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
import android.widget.ListView
import android.widget.Toast
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.ConnectToDeviceInterface
import com.example.sebastian.brulinski.arduinobluetooth.MyBluetooth
import com.example.sebastian.brulinski.arduinobluetooth.R

class ConnectToDevice : Fragment(), ConnectToDeviceInterface {

    //UI elements
    private lateinit var devicesListView: ListView
    //List elements
    lateinit var arrayAdapter: ArrayAdapter<String>
    private var devices = ArrayList<String>()
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private lateinit var myBluetooth: MyBluetooth

    private val TAG = "ConnectToDevice"

    private lateinit var connectHandler: Handler

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater!!.inflate(R.layout.fragment_connect_to_device, container, false)
        /**
         * Set devices ListView
         */
        devicesListView = view!!.findViewById(R.id.devices_list_view)
        arrayAdapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, devices)

        devicesListView.adapter = arrayAdapter

        handleDevicesListClick()

        connectHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                Looper.prepare()

                //keys
                val DEVICE_NAME = "device_name"
                val msgData = msg?.data

                val deviceName = msgData?.getString(DEVICE_NAME)
                Snackbar.make(view, "${getString(R.string.connected_to_message)}: $deviceName", Snackbar.LENGTH_LONG).show()
            }
        }

        myBluetooth = MyBluetooth(activity, connectHandler)

        setPairedDevicesAtList()

        return view
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
        devicesListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            myBluetooth.connectToDevice(pairedDevices.elementAt(position))
        }
    }

    override fun checkDevicesAdapter() {
        if (arrayAdapter.isEmpty)
            setPairedDevicesAtList()
    }

}
