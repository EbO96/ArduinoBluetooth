package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.sebastian.brulinski.arduinobluetooth.Helper.MyBluetooth
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothStateObserversInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.ConnectToDeviceInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.SetProperFragmentInterface
import com.example.sebastian.brulinski.arduinobluetooth.MainActivity
import com.example.sebastian.brulinski.arduinobluetooth.Models.MyBluetoothDevice
import com.example.sebastian.brulinski.arduinobluetooth.R
import com.example.sebastian.brulinski.arduinobluetooth.RecyclerAdapters.DevicesAdapter
import com.example.sebastian.brulinski.arduinobluetooth.databinding.FragmentConnectToDeviceBinding
import java.util.*

class ConnectToDevice : Fragment(), ConnectToDeviceInterface, BluetoothStateObserversInterface {

    private lateinit var binding: FragmentConnectToDeviceBinding

    //List elements
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private var foundDevices = ArrayList<BluetoothDevice>()
    private var devices = ArrayList<MyBluetoothDevice>()
    private var myBluetooth: MyBluetooth? = null
    private lateinit var devicesAdapter: DevicesAdapter
    private var connectedDeviceView: View? = null

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
        connectHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                Looper.prepare()

                //keys
                val DEVICE = "device"
                val msgData = msg?.data

                val device = msgData?.getParcelable<BluetoothDevice>(DEVICE)
                currentConnectedDevice = device

                Snackbar.make(binding.root, "${getString(R.string.connected_to_message)}: ${device!!.name}", Snackbar.LENGTH_LONG).show()

                activity.runOnUiThread {
                    connectedDeviceView?.findViewById<ImageView>(R.id.connected_image_view)?.visibility = View.VISIBLE
                }
            }
        }

        //Found devices receiver
        //Get found devices
        val devicesReceiver = object : BroadcastReceiver() {

            override fun onReceive(p0: Context?, p1: Intent?) {
                val action = p1!!.action
                when (action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val extraDevice = p1.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if (extraDevice != null) {
                            foundDevices.add(extraDevice)
                            devices.checkIfAlreadyFoundDeviceExist(extraDevice)
                        }
                    }
                    BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                        val extraDevice = p1.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        Log.d(TAG, "pair request from ${extraDevice.name}")
                    }
                }
            }
        }

        //Initialize class which is used to arrange bluetooth connection
        myBluetooth = MyBluetooth(activity, connectHandler, devicesReceiver)
        /**
         * Set devices recycler
         */
        setDevicesRecycler()
        setPairedDevicesAtList() //Fill list by paired bluetooth devices

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
            myBluetooth?.cancelDiscovery()
        }

        return binding.root
    }

    private fun ArrayList<MyBluetoothDevice>.checkIfAlreadyFoundDeviceExist(device: BluetoothDevice) {
        var addLabelFlag = true

        this
                .filter { it.label == getString(R.string.found) }
                .forEach { addLabelFlag = false }

        if (addLabelFlag)
            this.add(MyBluetoothDevice(null, false, MyBluetoothDevice.Companion.DeviceType.LABEL, getString(R.string.found)))

        this
                .filter { it.device == device }
                .forEach { return }

        this.add(MyBluetoothDevice(device, false, MyBluetoothDevice.Companion.DeviceType.FOUND, null))
        devicesAdapter.notifyItemInserted(this.size - 1)

    }

    private fun setDevicesRecycler() {
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        val itemDecorator = DividerItemDecoration(activity, layoutManager.orientation)
        binding.devicesRecycler.addItemDecoration(itemDecorator)
        binding.devicesRecycler.layoutManager = layoutManager

        //Device item click
        val clickListener = View.OnClickListener { view ->

            val device = findDeviceByName("${view.findViewById<TextView>(R.id.device_name).text}")

            if (device != null) {
                Log.d(TAG, device.address.toString())
                resetConnection()
                myBluetooth?.connectToDevice(device)
                connectedDeviceView = view
            }
        }

        devicesAdapter = DevicesAdapter(devices, activity, clickListener)
        binding.devicesRecycler.adapter = devicesAdapter
    }

    private fun findDeviceByName(deviceName: String): BluetoothDevice? {
        for (myBluetoothDevice in devices) {
            if (myBluetoothDevice.device != null) {
                val device = myBluetoothDevice.device
                if (device.name == deviceName) return device
            }
        }
        return null
    }

    private fun resetConnection() {
        if (connectedDeviceView != null) {
            connectedDeviceView?.findViewById<ImageView>(R.id.connected_image_view)?.visibility = View.INVISIBLE
            val deviceName = connectedDeviceView?.findViewById<TextView>(R.id.device_name)?.text.toString()
            Snackbar.make(binding.root, "${getString(R.string.disconnected_from_message)}: $deviceName", Snackbar.LENGTH_LONG).show()
        }

        connectedDeviceView = null
        val socket = getMyBluetooth()?.getBluetoothSocket()
        val inputStream = socket?.inputStream
        val outputStream = socket?.outputStream

        if (inputStream != null) {
            try {
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (outputStream != null) {
            try {
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (socket != null) {
            try {
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun update(state: MainActivity.Companion.BluetoothStates) {
        if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_DISCONNECTED) {
            connectedDeviceView?.findViewById<ImageView>(R.id.connected_image_view)?.visibility = View.INVISIBLE
            showDisconnectFromDeviceMessage()
        }
    }

    private fun showDisconnectFromDeviceMessage() {
        val deviceName = connectedDeviceView?.findViewById<TextView>(R.id.device_name)?.text.toString()
        Snackbar.make(binding.root, "${getString(R.string.disconnected_from_message)}: $deviceName", Snackbar.LENGTH_LONG).show()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putInt("discovery_layout_state", discovingDevicesLayoutVisibilityState)
        super.onSaveInstanceState(outState)
    }

    override fun getMyBluetooth(): MyBluetooth? = myBluetooth

    override fun getConnectedDevice(): BluetoothDevice? = currentConnectedDevice

    override fun getDeviceSocket(): BluetoothSocket? = getMyBluetooth()?.getBluetoothSocket()

    private fun setPairedDevicesAtList() {
        devices.add(MyBluetoothDevice(null, false,
                MyBluetoothDevice.Companion.DeviceType.LABEL, getString(R.string.paired)))

        if (myBluetooth != null) {
            pairedDevices = myBluetooth!!.getPairedDevices()
            var connectedFlag: Boolean
            val socket = getMyBluetooth()?.getBluetoothSocket()

            for (device in pairedDevices) {

                connectedFlag = if (socket != null) socket.isConnected && socket.remoteDevice == device
                else false

                devices.add(MyBluetoothDevice(device, connectedFlag, MyBluetoothDevice.Companion.DeviceType.PAIRED, null))
            }
            devicesAdapter.notifyDataSetChanged()
        }

    }


    override fun checkDevicesAdapter() {
        //TODO
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
                    myBluetooth?.discoverDevices()
                    deleteFoundDevices()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteFoundDevices() {
        var indexOfFoundLabel = -1

        for (x in 0 until devices.size) {
            if (devices[x].label == getString(R.string.found)) indexOfFoundLabel = x
        }

        val indexes = ArrayList<Int>()

        if (indexOfFoundLabel != -1) {
            indexes += indexOfFoundLabel until devices.size
        }

        Collections.sort(indexes, Collections.reverseOrder())
        for (index in indexes) {
            devices.removeAt(index)
            devicesAdapter.notifyItemRemoved(index)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        resetConnection()
        Toast.makeText(activity, "${getString(R.string.disconnected_from_message)}: ${currentConnectedDevice?.name}", Toast.LENGTH_SHORT).show()
        MainActivity.mBluetoothStateDirector.unregisterObserver(this)
    }

}
