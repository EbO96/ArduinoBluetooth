package com.example.sebastian.brulinski.arduinobluetooth.Activities

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.*
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.example.sebastian.brulinski.arduinobluetooth.Fragments.ConnectToDevice
import com.example.sebastian.brulinski.arduinobluetooth.Fragments.Terminal
import com.example.sebastian.brulinski.arduinobluetooth.Fragments.VehicleControlFragment
import com.example.sebastian.brulinski.arduinobluetooth.Helper.MyBluetooth
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothActionsInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.SetProperFragmentInterface
import com.example.sebastian.brulinski.arduinobluetooth.Models.MyBluetoothDevice
import com.example.sebastian.brulinski.arduinobluetooth.Observer.BluetoothStateDirector
import com.example.sebastian.brulinski.arduinobluetooth.R

class MainActivity : AppCompatActivity(), SetProperFragmentInterface, BluetoothActionsInterface {

    private val TAG = "MainActivity" //Log tag
    private val TERMINAL_TAG = "TERMINAL" //Log tag
    private val VEHICLE_CONTROL_TAG = "VEHICLE_CONTROL" //Log tag
    private val LOCATION_PERMISSION_ID = 1001
    private val ENABLE_BT_REQUEST_CODE = 1
    private var permissionCheck: Int? = null
    private val fragmentManager = supportFragmentManager
    private lateinit var bluetoothStateReceiver: BroadcastReceiver
    private lateinit var disconnectReceiver: BroadcastReceiver

    companion object {
        val mBluetoothStateDirector = BluetoothStateDirector()

        //Bluetooth state flags
        enum class BluetoothStates {
            STATE_BT_OFF,
            STATE_BT_ON,
            STATE_DEVICE_CONNECTED,
            STATE_DEVICE_DISCONNECTED,
            STATE_DEVICE_FOUND
        }
    }

    //Fragments
    private val connectToDevice = ConnectToDevice()
    private val terminal = Terminal()
    private val vehicleControl = VehicleControlFragment()

    //Bluetooth
    private lateinit var myBluetooth: MyBluetooth //TODO added this as 2
    private var foundDevices = ArrayList<BluetoothDevice>() //TODO 3
    private var devices = ArrayList<MyBluetoothDevice>() //TODO 4
    private var isConnected = false

    //Handlers //TODO 5
    private val connectHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message?) {
            Looper.prepare()

            //keys
            val DEVICE = "device"
            val msgData = msg?.data

            val device = msgData?.getParcelable<BluetoothDevice>(DEVICE)

            Snackbar.make(findViewById(R.id.main_container), "${getString(R.string.connected_to_message)}: ${device!!.name}", Snackbar.LENGTH_LONG).show()
        }
    }

    //Broadcast Receivers
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
                        devices.checkIfAlreadyDeviceExist(extraDevice, MyBluetoothDevice.Companion.DeviceType.FOUND)
                        mBluetoothStateDirector.notifyAllObservers(BluetoothStates.STATE_DEVICE_FOUND)
                    }
                }
                BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                    val extraDevice = p1.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    Log.d(TAG, "pair request from ${extraDevice.name}")
                }
            }
        }
    }

    private fun ArrayList<MyBluetoothDevice>.checkIfAlreadyDeviceExist(device: BluetoothDevice, type: MyBluetoothDevice.Companion.DeviceType) {

        val labelText = if (type == MyBluetoothDevice.Companion.DeviceType.PAIRED) getString(R.string.paired)
        else getString(R.string.found)

        var addLabelFlag = true

        this
                .filter { it.label == labelText }
                .forEach { addLabelFlag = false }

        this
                .filter { it.device == device }
                .forEach { return }

        if (addLabelFlag)
            this.add(MyBluetoothDevice(null, false, MyBluetoothDevice.Companion.DeviceType.LABEL, labelText))

        Log.d(TAG, device.name)
        this.add(MyBluetoothDevice(device, false, type, null))
    }

    private var currentFragment: Fragment? = null

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.setShowHideAnimationEnabled(true)

        //TODO added this as 1
        myBluetooth = MyBluetooth(this, connectHandler, devicesReceiver)

        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//Check permission whether phone running on Marshmallow or above
            /*
        If app hasn't location permissions, show message and finish app
         */
            if (permissionCheck != PackageManager.PERMISSION_GRANTED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Log.d(TAG, "permissions not granted")
                Toast.makeText(this, "Grand location permissions, because bluetooth devices can share fine location",
                        Toast.LENGTH_LONG).show()
                finish()
            }
        }

        bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action

                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.extras.getInt(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            turnOnBluetooth()
                            mBluetoothStateDirector.notifyAllObservers(BluetoothStates.STATE_BT_OFF)
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            //Toast.makeText(this@MainActivity, "state turning off", Toast.LENGTH_SHORT).show()

                        }
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            //Toast.makeText(this@MainActivity, "state turning on", Toast.LENGTH_SHORT).show()

                        }

                        BluetoothAdapter.STATE_ON -> {

                        }
                    }
                }
            }
        }

        disconnectReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action

                when (action) {
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        isConnected = false
                        mBluetoothStateDirector.notifyAllObservers(BluetoothStates.STATE_DEVICE_DISCONNECTED)
                        resetConnection()
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        isConnected = true
                        mBluetoothStateDirector.notifyAllObservers(BluetoothStates.STATE_DEVICE_CONNECTED)
                    }
                }
            }
        }

        registerReceiver(disconnectReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))
        registerReceiver(disconnectReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED))

        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        if (savedInstanceState == null) {
            setConnectToDeviceFragment()
        }
    }

    override fun resetConnection() {

        myBluetooth.cancelDiscovery()
        val socket = myBluetooth.getBluetoothSocket()
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

    override fun onStart() {
        super.onStart()
        /*
        Check locations permission
         */
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "requesting permissions")
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        LOCATION_PERMISSION_ID)
                return
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            turnOnBluetooth()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
        unregisterReceiver(disconnectReceiver)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_ID && grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == ENABLE_BT_REQUEST_CODE) run {
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled)
                Toast.makeText(this, "This app need enabled bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setConnectToDeviceFragment() {
        val transaction = fragmentManager.beginTransaction()
        currentFragment = connectToDevice
        mBluetoothStateDirector.registerObserver(connectToDevice)
        transaction.add(R.id.main_container, currentFragment)
        transaction.commit()
    }

    override fun setTerminalFragment() {
        supportActionBar?.hide()
        val transaction = fragmentManager.beginTransaction()
        currentFragment = terminal
        mBluetoothStateDirector.registerObserver(terminal)
        transaction.add(R.id.main_container, currentFragment)
        transaction.addToBackStack(TERMINAL_TAG)
        transaction.commit()
    }

    override fun setVehicleControlFragment() {

        supportActionBar?.hide()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val transaction = fragmentManager.beginTransaction()
        currentFragment = vehicleControl
        mBluetoothStateDirector.registerObserver(vehicleControl)
        transaction.add(R.id.main_container, vehicleControl)
        transaction.addToBackStack(VEHICLE_CONTROL_TAG)
        transaction.commit()
    }

    private fun turnOnBluetooth() {
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBluetoothIntent, ENABLE_BT_REQUEST_CODE)
    }

    override fun writeToDevice(toWrite: ByteArray) {

        if (!isConnected)
            Toast.makeText(applicationContext, getString(R.string.message_no_sent), Toast.LENGTH_SHORT).show()

        try {
            myBluetooth.write(toWrite, myBluetooth.getBluetoothSocket()!!.outputStream)
        } catch (e: KotlinNullPointerException) {
            Toast.makeText(applicationContext, getString(R.string.cant_send_message), Toast.LENGTH_SHORT).show()
        }
    }

    override fun isConnectedToDevice(): Boolean = isConnected

    override fun readFromDevice() {
    }

    override fun connectToDevice(device: BluetoothDevice) {
        myBluetooth.connectToDevice(device)
    }

    override fun disconnectFromDevice() {
    }

    override fun getPairedDevices(): Set<BluetoothDevice> = myBluetooth.getPairedDevices()

    override fun startDiscoveringDevices() {
        myBluetooth.discoverDevices()
    }

    override fun stopDiscoveringDevices() {
        myBluetooth.cancelDiscovery()
    }

    override fun getConnectedDevice(): BluetoothDevice? {
        return null
    }

    override fun getMyBluetoothDevices(): ArrayList<MyBluetoothDevice> {

        for (device in getPairedDevices()) {
            devices.checkIfAlreadyDeviceExist(device, MyBluetoothDevice.Companion.DeviceType.PAIRED)
        }

        return devices
    }
}
