package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.ConnectToDeviceInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.SetProperFragmentInterface
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

    private var currentConnectedDevice: BluetoothDevice? = null

    private var discovingDevicesLayoutVisibilityState = View.GONE

    private val TAG = "ConnectToDevice"
    private lateinit var connectHandler: Handler

    private lateinit var setProperFragmentCallback: SetProperFragmentInterface


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        Log.d(TAG, "created view")
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_connect_to_device, container, false)
        setHasOptionsMenu(true)
        /**
         * Set devices ListView
         */
        arrayAdapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, devices)

        binding.devicesListView.adapter = arrayAdapter
        ViewCompat.setNestedScrollingEnabled(binding.devicesListView, true)

        handleDevicesListClick()

        connectHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                Looper.prepare()

                //keys
                val DEVICE = "device"
                val msgData = msg?.data

                val device = msgData?.getParcelable<BluetoothDevice>(DEVICE)
                currentConnectedDevice = device

                Snackbar.make(binding.root, "${getString(R.string.connected_to_message)}: ${device!!.name}", Snackbar.LENGTH_LONG).show()
            }
        }

        myBluetooth = MyBluetooth(activity, connectHandler)

        setPairedDevicesAtList()

        setProperFragmentCallback = activity as SetProperFragmentInterface

        binding.terminalButton.setOnClickListener {
            setProperFragmentCallback.setTerminalFragment()
        }

        binding.customAction1.text = "${getString(R.string.custom_action)} 1"
        binding.customAction2.text = "${getString(R.string.custom_action)} 2"
        binding.customAction3.text = "${getString(R.string.custom_action)} 3"

        if (savedInstanceState != null)
            discovingDevicesLayoutVisibilityState = savedInstanceState.getInt("discovery_layout_state")
        binding.discoverDevicesLayout.visibility = discovingDevicesLayoutVisibilityState

        binding.cancelDiscoveringButton.setOnClickListener {
            discovingDevicesLayoutVisibilityState = View.GONE
            binding.discoverDevicesLayout.visibility = discovingDevicesLayoutVisibilityState
        }

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putInt("discovery_layout_state", discovingDevicesLayoutVisibilityState)
        super.onSaveInstanceState(outState)
    }

    override fun getMyBluetooth(): MyBluetooth? = myBluetooth

    override fun getConnectedDevice(): BluetoothDevice? = currentConnectedDevice

    override fun getDeviceSocket(): BluetoothSocket?  = myBluetooth.getBluetoothSocket()

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

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.connect_to_device_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.discover_devices -> {
                val view = binding.discoverDevicesLayout
                if (view.visibility != View.VISIBLE) {
                    discovingDevicesLayoutVisibilityState = View.VISIBLE
                    view.visibility = discovingDevicesLayoutVisibilityState
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
