package com.example.sebastian.brulinski.arduinobluetooth.Services

import android.app.Activity
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import android.widget.Toast
import com.example.sebastian.brulinski.arduinobluetooth.R
import java.io.IOException
import java.io.OutputStream

open class BluetoothConnectionService(handler: Handler?, discoveryDevicesReceiver: BroadcastReceiver?): Service() {

    var mBluetoothAdapter: BluetoothAdapter? = null
    private var connectThread: ConnectThread? = null
    private var connectHandler = handler

    private val TAG = "MyBluetooth"

    //Discovery devices
    private var devicesReceiver = discoveryDevicesReceiver
    private lateinit var receiverIntentFilters: IntentFilter
    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MyBluetooth init")
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        /*
        Check whether device support bluetooth
         */
        if (mBluetoothAdapter == null) {
            //Device doesn't support bluetooth
            Toast.makeText(applicationContext,
                    this.getString(R.string.bt_not_supported_message),
                    Toast.LENGTH_SHORT).show()
            onDestroy()
        }

        receiverIntentFilters = IntentFilter()
        receiverIntentFilters.addAction(BluetoothDevice.ACTION_FOUND)
        receiverIntentFilters.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        connectThread?.cancel()
    }

    fun getPairedDevices(): Set<BluetoothDevice> {
        mBluetoothAdapter?.cancelDiscovery()
        return mBluetoothAdapter!!.bondedDevices
    }

    fun connectToDevice(btDevice: BluetoothDevice) {
        connectThread = ConnectThread(btDevice, connectHandler!!)
        connectThread?.start()
    }

    fun getBluetoothSocket(): BluetoothSocket? = connectThread?.getSocket()

    fun discoverDevices() {
        mBluetoothAdapter?.startDiscovery()
        registerReceiver(devicesReceiver, receiverIntentFilters)
    }

    fun cancelDiscovery() {
        mBluetoothAdapter?.cancelDiscovery()
        this.unregisterReceiver(devicesReceiver)
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