package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.example.sebastian.brulinski.arduinobluetooth.Activities.MainActivity
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothActionsInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothStateObserversInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.SetProperFragmentInterface
import com.example.sebastian.brulinski.arduinobluetooth.R
import com.example.sebastian.brulinski.arduinobluetooth.RecyclerAdapters.DevicesAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_connect_to_bluetooth_devices.*
import org.jetbrains.anko.toast
import showLoginDialog

class ConnectToBluetoothDevices : Fragment(), BluetoothStateObserversInterface {

    //Tags
    private val TAG = "ConnectToDevice"
    //Callbacks
    private lateinit var bluetoothActionsCallback: BluetoothActionsInterface
    private lateinit var setProperFragmentCallback: SetProperFragmentInterface

    //List elements
    private lateinit var devicesAdapter: DevicesAdapter
    private var connectedDeviceView: View? = null

    //Menu item
    private var logOutMenuItem: MenuItem? = null

    //Database
    private val webCommandsReference = FirebaseDatabase.getInstance().reference.child("message")
    private lateinit var webCommandsEventListener: ValueEventListener


    //START
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            layoutInflater.inflate(R.layout.fragment_connect_to_bluetooth_devices, container, false)


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)//This fragment has own options menu

        //Set devices recycler
        setDevicesRecycler()

        setProperFragmentCallback = activity as SetProperFragmentInterface //Init interface used to changing fragments in container

        terminalButton.setOnClickListener {
            setProperFragmentCallback.setTerminalFragment()
        }

        /**
         *Set proper fragment
         */
        vehicleControlButton.setOnClickListener {
            setProperFragmentCallback.setVehicleControlFragment()
        }

        cancelDiscoveringButton.setOnClickListener {
            bluetoothActionsCallback.stopDiscoveringDevices()
            discoverDevicesRootLayout.visibility = View.GONE
        }

    }

    private fun loginOrCreateAccount(email: String, password: String, dialog: AlertDialog) {
        val mAuth = FirebaseAuth.getInstance()
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                activity.toast("${getString(R.string.account_created_successfully)}: $email")
                checkAlreadyLoggedIn()
            } else {
                if (task.exception?.message == "The email address is already in use by another account.")
                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { taskLogin ->
                        if (taskLogin.isSuccessful) {
                            activity.toast(R.string.logged_successfully)
                            checkAlreadyLoggedIn()
                        }
                    }
            }
            dialog.dismiss()
        }
    }

    private fun checkAlreadyLoggedIn(): Boolean {
        val flag = FirebaseAuth.getInstance().currentUser != null
        logOutMenuItem?.isVisible = flag
        return flag
    }

    private fun setDevicesRecycler() { //Init recycler where all found and paired devices are displayed
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        val itemDecorator = DividerItemDecoration(activity, layoutManager.orientation)
        devicesRecycler.addItemDecoration(itemDecorator)
        devicesRecycler.layoutManager = layoutManager

        //This listener responses at click at devices list
        val clickListener = View.OnClickListener { view ->
            getDeviceAndConnect(view, null)
        }
        //Set devices adapter
        devicesAdapter = DevicesAdapter(bluetoothActionsCallback.getMyBluetoothDevices(), activity, clickListener)
        devicesRecycler.adapter = devicesAdapter
    }

    private fun getDeviceAndConnect(view: View?, btDevice: BluetoothDevice?) { //Connect to selected device
        val device: BluetoothDevice?

        device = if (view != null) {
            val foundDevice = findDeviceByName("${view.findViewById<TextView>(R.id.deviceNameTextView).text}")
            foundDevice

        } else btDevice

        if (device != null) {
            bluetoothActionsCallback.resetConnection()
            bluetoothActionsCallback.connectToDevice(device)
            connectedDeviceView = view
        }
    }

    //Get BluetoothDevice object by his name displayed on devices list
    private fun findDeviceByName(deviceName: String): BluetoothDevice? {

        for (myBluetoothDevice in bluetoothActionsCallback.getMyBluetoothDevices()) {

            if (myBluetoothDevice.device != null) {
                val device = myBluetoothDevice.device
                if (device.name == deviceName) return device
            }
        }
        return null
    }

    //This is methods which is trigger when BluetoothDirector send notifications
    override fun update(state: MainActivity.Companion.BluetoothStates) {

        if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_DISCONNECTED && this.isAdded) {
            connectedDeviceView?.findViewById<ImageView>(R.id.connectedImageView)?.visibility = View.INVISIBLE
            showDisconnectMessage()
            disconnectFromWeb()
            connectedDeviceView = null
        } else if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_CONNECTED && this.isAdded) {
            activity.runOnUiThread {
                connectedDeviceView?.findViewById<ImageView>(R.id.connectedImageView)?.visibility = View.VISIBLE
            }
        } else if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_FOUND) {
            devicesAdapter.notifyDataSetChanged()
        } else if (state == MainActivity.Companion.BluetoothStates.STATE_BT_ON) {
            if (devicesRecycler.adapter != null) {
                bluetoothActionsCallback.getMyBluetoothDevices()
                devicesAdapter.notifyDataSetChanged()
            } else setDevicesRecycler()
        }
    }

    private fun disconnectFromWeb() {
        try {
            webCommandsReference.removeEventListener(webCommandsEventListener)
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
    }

    private fun showDisconnectMessage() {
        val deviceName = connectedDeviceView?.findViewById<TextView>(R.id.deviceNameTextView)?.text.toString()
        Snackbar.make(ConnectToDeviceRootView, "${getString(R.string.disconnected_from_message)}: $deviceName", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.connect_to_device_menu, menu)
        logOutMenuItem = menu?.findItem(R.id.logout)
        checkAlreadyLoggedIn()
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.discover_devices -> {
                val view = discoverDevicesRootLayout
                if (view.visibility != View.VISIBLE) {
                    bluetoothActionsCallback.startDiscoveringDevices()
                    view.visibility = View.VISIBLE
                }
            }
            R.id.logout -> {
                FirebaseAuth.getInstance().signOut()
                checkAlreadyLoggedIn()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        try {
            bluetoothActionsCallback = context as BluetoothActionsInterface

        } catch (e: ClassCastException) {
            Log.e(TAG, "$context must implement BluetoothActionsInterface", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bluetoothActionsCallback.resetConnection()
        MainActivity.mBluetoothStateDirector.unregisterObserver(this)
    }
}