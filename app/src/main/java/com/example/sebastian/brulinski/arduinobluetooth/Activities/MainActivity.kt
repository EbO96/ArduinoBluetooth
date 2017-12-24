package com.example.sebastian.brulinski.arduinobluetooth.Activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.example.sebastian.brulinski.arduinobluetooth.AsyncTasks.MyBluetoothJobExecutor
import com.example.sebastian.brulinski.arduinobluetooth.Fragments.ConnectToDevice
import com.example.sebastian.brulinski.arduinobluetooth.Fragments.Terminal
import com.example.sebastian.brulinski.arduinobluetooth.Fragments.VehicleControlFragment
import com.example.sebastian.brulinski.arduinobluetooth.Helper.MyBluetooth
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.ConnectToDeviceInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.SetProperFragmentInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.TerminalInterface
import com.example.sebastian.brulinski.arduinobluetooth.Observer.BluetoothStateDirector
import com.example.sebastian.brulinski.arduinobluetooth.R

class MainActivity : AppCompatActivity(), SetProperFragmentInterface, TerminalInterface {

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
            STATE_DEVICE_DISCONNECTED
        }
    }

    //Fragments
    private val connectToDevice = ConnectToDevice()
    private val terminal = Terminal()

    private var currentFragment: Fragment? = null

    private lateinit var connectToDeviceFragmentCallback: ConnectToDeviceInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectToDeviceFragmentCallback = connectToDevice

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
                            connectToDeviceFragmentCallback.checkDevicesAdapter()
                            connectToDeviceFragmentCallback.checkDevicesAdapter()
                        }
                    }
                }
            }
        }

        disconnectReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                mBluetoothStateDirector.notifyAllObservers(BluetoothStates.STATE_DEVICE_DISCONNECTED)
            }
        }

        registerReceiver(disconnectReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))

        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        if (savedInstanceState == null) {
            setConnectToDeviceFragment()
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
        val transacion = fragmentManager.beginTransaction()
        val f = Terminal()
        currentFragment = f
        mBluetoothStateDirector.registerObserver(f)
        val b = Bundle()
        b.putParcelable("device", connectToDeviceFragmentCallback.getConnectedDevice())
        currentFragment?.arguments = b
        transacion.add(R.id.main_container, f)
        transacion.addToBackStack(TERMINAL_TAG)
        transacion.commit()
    }

    override fun setVehicleControlFragment() {
        val transaction = fragmentManager.beginTransaction()
        val f = VehicleControlFragment()
        currentFragment = f
        mBluetoothStateDirector.registerObserver(f)
//        val b = Bundle()
//        currentFragment?.arguments = b
        transaction.add(R.id.main_container, f)
        transaction.addToBackStack(VEHICLE_CONTROL_TAG)
        transaction.commit()
    }

    private fun turnOnBluetooth() {
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBluetoothIntent, ENABLE_BT_REQUEST_CODE)
    }

    override fun getMyBluetooth(): MyBluetooth? {
        return connectToDeviceFragmentCallback.getMyBluetooth()
    }

    override fun getConnectedDeviceSocket(): BluetoothSocket? = connectToDeviceFragmentCallback.getDeviceSocket()
}
