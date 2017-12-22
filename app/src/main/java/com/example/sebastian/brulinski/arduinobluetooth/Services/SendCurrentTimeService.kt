package com.example.sebastian.brulinski.arduinobluetooth.Services

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import com.example.sebastian.brulinski.arduinobluetooth.Helper.MyBluetooth
import com.example.sebastian.brulinski.arduinobluetooth.Helper.getCurrentDateAndTime
import java.util.*

class SendCurrentTimeService: Service() {

    //Bluetooth
    private lateinit var myBluetooth: MyBluetooth

    private val myHandler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message?) {
            val timer = Timer()
            val runAsynchrounous = object : TimerTask(){
                override fun run() {
                    myBluetooth.write(getCurrentDateAndTime().toByteArray(), myBluetooth.getBluetoothSocket()!!.outputStream)
                }
            }
            timer.schedule(runAsynchrounous, 0, 3000)
        }
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        myBluetooth = MyBluetooth(this, myHandler, null)
        myBluetooth.connectToDevice(intent!!.getParcelableExtra("device"))

        return START_STICKY
    }

    override fun onDestroy() {
        myBluetooth.getBluetoothSocket()?.inputStream?.close()
        myBluetooth.getBluetoothSocket()?.outputStream?.close()
        myBluetooth.getBluetoothSocket()?.close()
        super.onDestroy()
    }
}