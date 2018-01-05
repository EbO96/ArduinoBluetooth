package com.example.sebastian.brulinski.arduinobluetooth.Activities

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.*
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.example.sebastian.brulinski.arduinobluetooth.Fragments.ConnectToBluetoothDevices
import com.example.sebastian.brulinski.arduinobluetooth.Fragments.Terminal
import com.example.sebastian.brulinski.arduinobluetooth.Fragments.VehicleControlFragment
import com.example.sebastian.brulinski.arduinobluetooth.Helper.MyBluetooth
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.ApplyVehicleWidgetsSettings
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothActionsInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.SetProperFragmentInterface
import com.example.sebastian.brulinski.arduinobluetooth.Models.MyBluetoothDevice
import com.example.sebastian.brulinski.arduinobluetooth.Observer.BluetoothStateDirector
import com.example.sebastian.brulinski.arduinobluetooth.R
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import showConnectingToDeviceAlert

class MainActivity : AppCompatActivity(), SetProperFragmentInterface, BluetoothActionsInterface, ApplyVehicleWidgetsSettings {

    //Debug and fragments tags
    private val TAG = "MainActivity" //Log tag
    private val connectedDeviceKey = "connected_device"

    private val TERMINAL_TAG = "TERMINAL" //fragment tag
    private val VEHICLE_CONTROL_TAG = "VEHICLE_CONTROL" //fragment tag
    private val CONNECT_FRAGMENT_TAG = "CONNECT_FRAGMENT" //fragment tag

    //Request codes and permission codes
    private val LOCATION_PERMISSION_ID = 1001
    private val ENABLE_BT_REQUEST_CODE = 1
    private var permissionCheck: Int? = null

    //Fragments
    private val fragmentManager by lazy { supportFragmentManager }
    private val connectToDevice by lazy { ConnectToBluetoothDevices() }
    private val terminal by lazy { Terminal() }
    private val vehicleControl by lazy { VehicleControlFragment() }
    private var currentFragment: Fragment? = null

    //Fragment dialogs
    private var connectToDeviceDialog: AlertDialog? = null

    //Bluetooth
    private lateinit var myBluetooth: MyBluetooth
    private var foundDevices = ArrayList<BluetoothDevice>()
    private var devices = ArrayList<MyBluetoothDevice>()
    private var isConnected = false
    private var isBluetoothOn = false
    private lateinit var bluetoothStateReceiver: BroadcastReceiver
    private lateinit var disconnectReceiver: BroadcastReceiver
    private var currentConnectedDevice: BluetoothDevice? = null //We save current connected device name to this value

    companion object {
        val mBluetoothStateDirector by lazy { BluetoothStateDirector() }

        //Bluetooth state flags
        enum class BluetoothStates {
            STATE_BT_OFF,
            STATE_BT_ON,
            STATE_DEVICE_CONNECTED,
            STATE_DEVICE_DISCONNECTED,
            STATE_DEVICE_FOUND
        }
    }

    //Handlers
    private val connectHandler by lazy {
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                Looper.prepare()

                //Show Snackbar with information about which device has been connected
                val DEVICE = "device"
                val msgData = msg?.data
                val device = msgData?.getParcelable<BluetoothDevice>(DEVICE)
                Snackbar.make(findViewById(R.id.mainFragmentsContainer), "${getString(R.string.connected_to_message)}: ${device!!.name}", Snackbar.LENGTH_SHORT).show()
                connectToDeviceDialog?.dismiss()
                connectToDeviceDialog = null
                isConnected = true
                mBluetoothStateDirector.notifyAllObservers(BluetoothStates.STATE_DEVICE_CONNECTED)
            }
        }
    }

    private val readHandler by lazy {
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                Looper.prepare()
            }
        }
    }

    //Receiver used to handle found devices
    private val devicesReceiver by lazy {
        object : BroadcastReceiver() {

            override fun onReceive(p0: Context?, p1: Intent?) {
                val action = p1!!.action
                when (action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val extraDevice = p1.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if (extraDevice != null) {
                            foundDevices.add(extraDevice)
                            devices.checkIfAlreadyDeviceExistAndAddToList(extraDevice, MyBluetoothDevice.Companion.DeviceType.FOUND)
                            mBluetoothStateDirector.notifyAllObservers(BluetoothStates.STATE_DEVICE_FOUND)
                        }
                    }
                    BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                        val extraDevice = p1.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    }
                }
            }
        }
    }

    //Add paired or found device to list
    private fun ArrayList<MyBluetoothDevice>.checkIfAlreadyDeviceExistAndAddToList(device: BluetoothDevice, type: MyBluetoothDevice.Companion.DeviceType) {

        val labelText = if (type == MyBluetoothDevice.Companion.DeviceType.PAIRED) getString(R.string.paired)
        else getString(R.string.found)

        var addLabelFlag = true

        this
                .filter { it.label == labelText }
                .forEach { addLabelFlag = false }

        this
                .filter { it.device == device }
                .forEach { return }

        //Add label to list (Paired or Found)
        if (addLabelFlag)
            this.add(MyBluetoothDevice(null, false, MyBluetoothDevice.Companion.DeviceType.LABEL, labelText))

        //Add device
        this.add(MyBluetoothDevice(device, false, type, null))
    }

    /**
     * START
     */
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.app_toolbar))

        supportFragmentManager.addOnBackStackChangedListener {
            val fragment = supportFragmentManager.findFragmentById(mainFragmentsContainer.id)

            setProperTitleAtToolbar(fragment)
        }

        //Get bluetooth instance
        myBluetooth = MyBluetooth(this, connectHandler, devicesReceiver)

        //Check location permission
        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//Check permission whether phone running on Marshmallow or above
            /*
        If app hasn't location permissions, show message and finish app
         */
            if (permissionCheck != PackageManager.PERMISSION_GRANTED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                toast("Grand location permissions, because bluetooth devices can share fine location")
                finish()
            }
        }

        /**
         * Add Broadcast receivers for Actions like CONNECTED, DISCONNECTED AND BT STATE CHANGES
         */
        bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action

                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.extras.getInt(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            isBluetoothOn = false
                            turnOnBluetooth()
                            mBluetoothStateDirector.notifyAllObservers(BluetoothStates.STATE_BT_OFF)
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {

                        }
                        BluetoothAdapter.STATE_TURNING_ON -> {

                        }

                        BluetoothAdapter.STATE_ON -> {
                            isBluetoothOn = true
                            mBluetoothStateDirector.notifyAllObservers(BluetoothStates.STATE_BT_ON)
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
                }
                connectToDeviceDialog?.dismiss()
                connectToDeviceDialog = null
                currentConnectedDevice = myBluetooth.getBluetoothSocket()?.remoteDevice
            }
        }


        val disconnectedConnectedIntentFilter = IntentFilter()

        disconnectedConnectedIntentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)

        registerReceiver(disconnectReceiver, disconnectedConnectedIntentFilter)

        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        //Set fragment
        if (savedInstanceState == null) {
            setConnectToDeviceFragment()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        //Get current connected device name
        outState?.putParcelable(connectedDeviceKey, currentConnectedDevice)
        Log.d(TAG, "saved it $currentConnectedDevice")
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        //Get connected device name
        if (savedInstanceState != null) {
            currentConnectedDevice = savedInstanceState.getParcelable(connectedDeviceKey)
            Log.d(TAG, "restored it $currentConnectedDevice")

            //Connect to previous connected device
            Handler().postDelayed({
                if (currentConnectedDevice != null) {
                    connectToDevice(currentConnectedDevice!!)
                }
            }, 300)
        }

        super.onRestoreInstanceState(savedInstanceState)
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

    private fun hideToolbar(condition: () -> Boolean) {

        if (condition())
            supportActionBar?.hide()
        else supportActionBar?.show()
    }

    private fun setProperTitleAtToolbar(currentVisibleFragment: Fragment) {

        when (currentVisibleFragment) {

            is ConnectToBluetoothDevices -> {
                supportActionBar?.title = getString(R.string.connect_fragmnent_title)
            }
            is Terminal -> {
                supportActionBar?.title = getString(R.string.terminal)
            }
            is VehicleControlFragment -> {
                supportActionBar?.title = getString(R.string.vehicle_control)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val f = supportFragmentManager.findFragmentById(R.id.mainFragmentsContainer)

        setProperTitleAtToolbar(f)

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            turnOnBluetooth() //Turn on bluetooth when disabled
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
                toast("This app need enabled bluetooth")
        }
    }

    private fun setConnectToDeviceFragment() {
        val transaction = fragmentManager.beginTransaction()
        currentFragment = connectToDevice
        transaction.add(R.id.mainFragmentsContainer, currentFragment, CONNECT_FRAGMENT_TAG)
        transaction.commit()
    }

    override fun setTerminalFragment() {

        val transaction = fragmentManager.beginTransaction()
        currentFragment = terminal
        transaction.add(R.id.mainFragmentsContainer, currentFragment)
        transaction.addToBackStack(TERMINAL_TAG)
        transaction.commit()
    }

    override fun setVehicleControlFragment() {

        val transaction = fragmentManager.beginTransaction()
        currentFragment = vehicleControl
        transaction.add(R.id.mainFragmentsContainer, vehicleControl)
        transaction.addToBackStack(VEHICLE_CONTROL_TAG)
        transaction.commit()
    }

    private fun turnOnBluetooth() {
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBluetoothIntent, ENABLE_BT_REQUEST_CODE)
    }

    /*
    Methods to manage bluetooth actions
     */
    override fun writeToDevice(toWrite: ByteArray) {

        if (!isConnected)
            toast(R.string.message_no_sent)
        try {
            myBluetooth.write(toWrite, myBluetooth.getBluetoothSocket()!!.outputStream)
        } catch (e: KotlinNullPointerException) {
            toast(R.string.cant_send_message)
        }
    }

    override fun readFromDevice() {
        if (isConnected) {
            myBluetooth.read(readHandler, myBluetooth.getBluetoothSocket()!!.inputStream)
        }
    }

    override fun isConnectedToDevice(): Boolean = isConnected

    override fun isBluetoothOn(): Boolean = isBluetoothOn

    override fun connectToDevice(device: BluetoothDevice) {
        myBluetooth.connectToDevice(device)
        connectToDeviceDialog = showConnectingToDeviceAlert(this, getString(R.string.connecting_to), null, device.name,
                R.layout.connecting_to_device_dialog)

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

    override fun getMyPairedBluetoothDevices(): ArrayList<MyBluetoothDevice> {

        for (device in getPairedDevices()) {
            devices.checkIfAlreadyDeviceExistAndAddToList(device, MyBluetoothDevice.Companion.DeviceType.PAIRED)
        }

        return devices
    }

    override fun applyVehicleWidgetSettings() {
        val fragment = supportFragmentManager.findFragmentById(mainFragmentsContainer.id)

        if(fragment is VehicleControlFragment)
            fragment.applyAccelerometerData()
    }
}
