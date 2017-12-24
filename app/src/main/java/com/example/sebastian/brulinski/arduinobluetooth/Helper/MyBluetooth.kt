package com.example.sebastian.brulinski.arduinobluetooth.Helper

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import com.example.sebastian.brulinski.arduinobluetooth.R
import java.io.IOException
import java.io.OutputStream

class MyBluetooth(private val activity: Activity?, handler: Handler?, discoveryDevicesReceiver: BroadcastReceiver?) {

    var mBluetoothAdapter: BluetoothAdapter? = null
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private var connectThread: ConnectThread? = null
    private var connectHandler = handler

    private val TAG = "MyBluetooth"

    //Discovery devices
    private var devicesReceiver = discoveryDevicesReceiver
    private lateinit var receiverIntentFilters: IntentFilter

    init {
        Log.d(TAG, "MyBluetooth init")
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        /*
        Check whether device support bluetooth
         */
        if (mBluetoothAdapter == null) {
            //Device doesn't support bluetooth
            Toast.makeText(activity?.applicationContext,
                    activity?.getString(R.string.bt_not_supported_message),
                    Toast.LENGTH_SHORT).show()
            activity?.finish()
        }

//        if (!isBtEnabled()) {
//            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            activity.startActivityForResult(enableBluetoothIntent, ENABLE_BT_REQUEST_CODE)
//        } else {
//            getPairedDevices()
//        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            receiverIntentFilters = IntentFilter()
            receiverIntentFilters.addAction(BluetoothDevice.ACTION_FOUND)
            receiverIntentFilters.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        }
    }

    fun getPairedDevices(): Set<BluetoothDevice> {
        mBluetoothAdapter?.cancelDiscovery()
        pairedDevices = mBluetoothAdapter!!.bondedDevices

        return pairedDevices
    }

    fun connectToDevice(btDevice: BluetoothDevice) {
        connectThread = ConnectThread(btDevice, connectHandler!!)
        connectThread?.start()
    }

    fun getBluetoothSocket(): BluetoothSocket? = connectThread?.getSocket()

    fun discoverDevices() {
        mBluetoothAdapter?.startDiscovery()
        activity?.registerReceiver(devicesReceiver, receiverIntentFilters)
    }

    fun cancelDiscovery() {
        mBluetoothAdapter?.cancelDiscovery()
        try{
            activity?.unregisterReceiver(devicesReceiver)
        }catch (e: IllegalArgumentException){

        }
    }

    fun write(toWrite: ByteArray, socketOutputStream: OutputStream) {
        try {
            socketOutputStream.write(
                    toWrite
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // private fun isBtEnabled(): Boolean = mBluetoothAdapter!!.isEnabled


    inner class ConnectThread(device: BluetoothDevice, handler: Handler) : Thread() {

        private var mHandler: Handler = handler
        private var mDevice = device
        private var mBluetoothSocket: BluetoothSocket?
        private val TAG = "ConnectThread"
        //APP UUID
        private val UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        init {
            var tmp: BluetoothSocket? = null

            try {
                tmp = mDevice.createRfcommSocketToServiceRecord(UUID)
            } catch (e: IOException) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mBluetoothSocket = tmp

        }

        override fun run() {
            mBluetoothAdapter?.cancelDiscovery()

            try {
                mBluetoothSocket?.connect()
            } catch (connectionException: IOException) {
                try {
                    mBluetoothSocket?.close()
                } catch (closeException: IOException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return
            }

            //Send connected device name to handler
            val message = Message()
            val bundle = Bundle()
            bundle.putParcelable("device", mDevice)
            message.data = bundle
            mHandler.handleMessage(message)
        }

        fun cancel() {
            try {
                mBluetoothSocket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun getSocket(): BluetoothSocket? = mBluetoothSocket
    }
}