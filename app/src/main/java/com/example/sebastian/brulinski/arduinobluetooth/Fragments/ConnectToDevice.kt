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
import com.example.sebastian.brulinski.arduinobluetooth.Models.MyBluetoothDevice
import com.example.sebastian.brulinski.arduinobluetooth.R
import com.example.sebastian.brulinski.arduinobluetooth.RecyclerAdapters.DevicesAdapter
import com.example.sebastian.brulinski.arduinobluetooth.databinding.FragmentConnectToDeviceBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import showLoginDialog
import java.util.*

class ConnectToDevice : Fragment(), BluetoothStateObserversInterface {

    private lateinit var binding: FragmentConnectToDeviceBinding

    //Callbacks
    private lateinit var bluetoothActionsCallback: BluetoothActionsInterface

    //List elements
    private lateinit var devicesAdapter: DevicesAdapter

    private var connectedDeviceView: View? = null

    private val TAG = "ConnectToDevice"
    private lateinit var connectHandler: Handler

    private lateinit var setProperFragmentCallback: SetProperFragmentInterface

    //Menu item
    private var logOutMenuItem: MenuItem? = null

    //Database
    private val webCommandsReference = FirebaseDatabase.getInstance().reference.child("message")
    private lateinit var webCommandsEventListener: ValueEventListener

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_connect_to_device, container, false)

        setHasOptionsMenu(true)

        // Set devices recycler
        setDevicesRecycler()

        setProperFragmentCallback = activity as SetProperFragmentInterface

        binding.terminalButton.setOnClickListener {
            setProperFragmentCallback.setTerminalFragment()
        }

        binding.customAction3.text = "${getString(R.string.custom_action)} 3"

        binding.controlFromWebSwitch.setOnCheckedChangeListener { compoundButton, checked ->
            if (checked) {
                if (checkAlreadyLoggedIn()) {

                } else {
                    showLoginDialog(activity, { email, password, dialog ->

                        loginOrCreateAccount(email, password, dialog)
                    })
                }
            }
            if (checkAlreadyLoggedIn()) {
                if(bluetoothActionsCallback.isConnectedToDevice()){
                    if (checked) {
                        binding.linkTextView.visibility = View.VISIBLE
                        webCommandsEventListener = webCommandsReference.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot?) {
                                //TODO write
                                bluetoothActionsCallback.writeToDevice("${snapshot?.value}\n".toByteArray())
                            }

                            override fun onCancelled(p0: DatabaseError?) {

                            }
                        })
                    } else {
                        binding.linkTextView.visibility = View.INVISIBLE
                        webCommandsReference.removeEventListener(webCommandsEventListener)
                    }
                }else {
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
    }

    private fun setDevicesRecycler() {
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        val itemDecorator = DividerItemDecoration(activity, layoutManager.orientation)
        binding.devicesRecycler.addItemDecoration(itemDecorator)
        binding.devicesRecycler.layoutManager = layoutManager

        //Device item click
        val clickListener = View.OnClickListener { view ->
            getDeviceAndConnect(view, null)
        }

        devicesAdapter = DevicesAdapter(bluetoothActionsCallback.getMyBluetoothDevices(), activity, clickListener)
        binding.devicesRecycler.adapter = devicesAdapter
    }

    private fun getDeviceAndConnect(view: View?, btDevice: BluetoothDevice?) {
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

    private fun findDeviceByName(deviceName: String): BluetoothDevice? {

        for (myBluetoothDevice in bluetoothActionsCallback.getMyBluetoothDevices()) {

            if (myBluetoothDevice.device != null) {
                val device = myBluetoothDevice.device
                if (device.name == deviceName) return device
            }
        }
        return null
    }


    override fun update(state: MainActivity.Companion.BluetoothStates) {

        if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_DISCONNECTED && this.isAdded) {
            connectedDeviceView?.findViewById<ImageView>(R.id.connected_image_view)?.visibility = View.INVISIBLE
            showDisconnectFromDeviceMessage()
            disconnectFromWeb()
            connectedDeviceView = null
        } else if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_CONNECTED && this.isAdded) {
            activity.runOnUiThread {
                connectedDeviceView?.findViewById<ImageView>(R.id.connected_image_view)?.visibility = View.VISIBLE
            }
        }else if(state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_FOUND){
            devicesAdapter.notifyDataSetChanged()
        }else if(state == MainActivity.Companion.BluetoothStates.STATE_BT_ON){
            setDevicesRecycler()
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

    private fun showDisconnectFromDeviceMessage() {
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
