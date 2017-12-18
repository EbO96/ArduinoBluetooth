package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import com.example.sebastian.brulinski.arduinobluetooth.MyBluetooth
import com.example.sebastian.brulinski.arduinobluetooth.R

class ConnectToDevice : Fragment() {

    //UI elements
    private lateinit var devicesListView: ListView
    //List elements
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private var devices = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // devices = arguments.getStringArrayList("devices")
    }

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

        setPairedDevicesAtList()
        return view
    }

    private fun updateDevicesList(element: String) {
        devices.add(element)
        arrayAdapter.notifyDataSetChanged()
    }

    private fun setPairedDevicesAtList() {
        val p = MyBluetooth(activity).getPairedDevices()

        for (device in p) {
            devices.add("${device.name} : ${device.address}")
        }
        arrayAdapter.notifyDataSetChanged()
    }

    private fun handleDevicesListClick() {
        devicesListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->

        }
    }
}
