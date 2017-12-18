package com.example.sebastian.brulinski.arduinobluetooth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.example.sebastian.brulinski.arduinobluetooth.Fragments.ConnectToDevice

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity" //Log tag
    private val LOCATION_PERMISSION_ID = 1001
    private var permissionCheck: Int? = null
    private val fragmentManager = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
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

        setConnectToDeviceFragment()
    }

    override fun onStart() {
        super.onStart()
        /*
        Check locations permission
         */
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "requesting permissions")
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    LOCATION_PERMISSION_ID)
            return
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_ID && grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }
    }

    private fun setConnectToDeviceFragment(){
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.main_container, ConnectToDevice())
        transaction.commit()
    }
}
