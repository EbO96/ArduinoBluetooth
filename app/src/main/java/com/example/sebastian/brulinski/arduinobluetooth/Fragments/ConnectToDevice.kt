package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.databinding.DataBindingUtil
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
import android.widget.Toast
import com.example.sebastian.brulinski.arduinobluetooth.Activities.MainActivity
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothActionsInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothStateObserversInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.SetProperFragmentInterface
import com.example.sebastian.brulinski.arduinobluetooth.R
import com.example.sebastian.brulinski.arduinobluetooth.RecyclerAdapters.DevicesAdapter
import com.example.sebastian.brulinski.arduinobluetooth.databinding.FragmentConnectToDeviceBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import showLoginDialog

class ConnectToDevice : Fragment(), BluetoothStateObserversInterface {

    //Tags
    private val TAG = "ConnectToDevice"
    //Binding
    private lateinit var binding: FragmentConnectToDeviceBinding

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
                              savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_connect_to_device, container, false)

        setHasOptionsMenu(true)//This fragment has own options menu

        //Set devices recycler
        setDevicesRecycler()

        setProperFragmentCallback = activity as SetProperFragmentInterface //Init interface used to changing fragments in container

        binding.terminalButton.setOnClickListener {
            setProperFragmentCallback.setTerminalFragment()
        }

        //On/Off control from Website
        binding.controlFromWebSwitch.setOnCheckedChangeListener { compoundButton, checked ->
            if (checked) {
                if (checkAlreadyLoggedIn()) {

                } else {
                    showLoginDialog(activity, { email, password, dialog ->
                        //Account is required
                        loginOrCreateAccount(email, password, dialog)
                    })
                }
            }

            if (checkAlreadyLoggedIn()) {
                if (bluetoothActionsCallback.isConnectedToDevice()) {
                    if (checked) {
                        binding.linkTextView.visibility = View.VISIBLE
                        webCommandsEventListener = webCommandsReference.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot?) {
                                //When user are logged then we can send to device all incoming messages from web
                                bluetoothActionsCallback.writeToDevice("${snapshot?.value}\n".toByteArray())
                            }

                            override fun onCancelled(p0: DatabaseError?) {

                            }
                        })
                    } else {
                        binding.linkTextView.visibility = View.INVISIBLE
                        webCommandsReference.removeEventListener(webCommandsEventListener)
                    }
                } else {
                    Toast.makeText(activity, getString(R.string.first_connect_to_device_msg), Toast.LENGTH_SHORT).show()
                    Handler().postDelayed({
                        disconnectFromWeb()
                    }, 500)
                }
            } else {
                Handler().postDelayed({
                    disconnectFromWeb()
                }, 500)
            }
        }

        /**
         *Set proper fragment
         */
        binding.vehicleControlButton.setOnClickListener {
            setProperFragmentCallback.setVehicleControlFragment()
        }

        binding.cancelDiscoveringButton.setOnClickListener {
            bluetoothActionsCallback.stopDiscoveringDevices()
            binding.discoverDevicesLayout.visibility = View.GONE
        }

        return binding.root
    }

    private fun loginOrCreateAccount(email: String, password: String, dialog: AlertDialog) {
        val mAuth = FirebaseAuth.getInstance()

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(activity, "${getString(R.string.account_created_successfully)}: $email", Toast.LENGTH_SHORT).show()
                checkAlreadyLoggedIn()
            } else {
                if (task.exception?.message == "The email address is already in use by another account.")
                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { taskLogin ->
                        if (taskLogin.isSuccessful) {
                            Toast.makeText(activity, getString(R.string.logged_successfully), Toast.LENGTH_SHORT).show()
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
        binding.devicesRecycler.addItemDecoration(itemDecorator)
        binding.devicesRecycler.layoutManager = layoutManager

        //This listener responses at click at devices list
        val clickListener = View.OnClickListener { view ->
            getDeviceAndConnect(view, null)
        }
        //Set devices adapter
        devicesAdapter = DevicesAdapter(bluetoothActionsCallback.getMyBluetoothDevices(), activity, clickListener)
        binding.devicesRecycler.adapter = devicesAdapter
    }

    private fun getDeviceAndConnect(view: View?, btDevice: BluetoothDevice?) { //Connect to selected device
        val device: BluetoothDevice?

        device = if (view != null) {
            val foundDevice = findDeviceByName("${view.findViewById<TextView>(R.id.device_name).text}")
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
            connectedDeviceView?.findViewById<ImageView>(R.id.connected_image_view)?.visibility = View.INVISIBLE
            showDisconnectMessage()
            disconnectFromWeb()
            connectedDeviceView = null
        } else if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_CONNECTED && this.isAdded) {
            activity.runOnUiThread {
                connectedDeviceView?.findViewById<ImageView>(R.id.connected_image_view)?.visibility = View.VISIBLE
            }
        } else if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_FOUND) {
            devicesAdapter.notifyDataSetChanged()
        } else if (state == MainActivity.Companion.BluetoothStates.STATE_BT_ON) {
            if (binding.devicesRecycler.adapter != null) {
                bluetoothActionsCallback.getMyBluetoothDevices()
                devicesAdapter.notifyDataSetChanged()
            }
            else setDevicesRecycler()
        }
    }

    private fun disconnectFromWeb() {
        try {
            webCommandsReference.removeEventListener(webCommandsEventListener)
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
        binding.controlFromWebSwitch.isChecked = false
        binding.linkTextView.visibility = View.INVISIBLE
    }

    private fun showDisconnectMessage() {
        val deviceName = connectedDeviceView?.findViewById<TextView>(R.id.device_name)?.text.toString()
        Snackbar.make(binding.root, "${getString(R.string.disconnected_from_message)}: $deviceName", Snackbar.LENGTH_LONG).show()
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
                val view = binding.discoverDevicesLayout
                if (view.visibility != View.VISIBLE) {
                    bluetoothActionsCallback.startDiscoveringDevices()
                    view.visibility = View.VISIBLE
                }
            }
            R.id.logout -> {
                FirebaseAuth.getInstance().signOut()
                binding.controlFromWebSwitch.isChecked = false
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
