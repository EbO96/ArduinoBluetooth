package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
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
import com.example.sebastian.brulinski.arduinobluetooth.Models.ActionsItem
import com.example.sebastian.brulinski.arduinobluetooth.R
import com.example.sebastian.brulinski.arduinobluetooth.RecyclerAdapters.ActionsAdapter
import com.example.sebastian.brulinski.arduinobluetooth.RecyclerAdapters.DevicesAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_connect_to_bluetooth_devices.*
import org.jetbrains.anko.toast

class ConnectToBluetoothDevices : Fragment(), BluetoothStateObserversInterface {

    //Tags
    private val TAG = "ConnectToDevices"
    //Callbacks
    private lateinit var bluetoothActionsCallback: BluetoothActionsInterface
    private lateinit var setProperFragmentCallback: SetProperFragmentInterface

    //List elements
    private lateinit var devicesAdapter: DevicesAdapter
    private lateinit var actionsAdapter: ActionsAdapter
    private val actionItemArray = ArrayList<ActionsItem>()
    private var connectedDeviceView: View? = null

    private val devicesLayoutManager by lazy {
        LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    }

    private val actionsLayoutManager by lazy {
        LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    }

    private val itemDecorator by lazy {
        DividerItemDecoration(activity, devicesLayoutManager.orientation)
    }

    private val actionsItemDecorator by lazy {
        DividerItemDecoration(activity, devicesLayoutManager.orientation)
    }

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
        MainActivity.mBluetoothStateDirector.registerObserver(this)

        //Set devices recycler
        setDevicesRecycler()
        setActionsRecycler()

        setProperFragmentCallback = activity as SetProperFragmentInterface //Init interface used to changing fragments in container

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

        devicesRecycler.addItemDecoration(itemDecorator)
        devicesRecycler.layoutManager = devicesLayoutManager

        //This listener responses at click at devices list
        val clickListener = View.OnClickListener { view ->
            getDeviceAndConnect(view, null,null)
        }
        //Set devices adapter
        devicesAdapter = DevicesAdapter(bluetoothActionsCallback.getMyPairedBluetoothDevices(), activity, clickListener)
        devicesRecycler.adapter = devicesAdapter
    }

    private fun setActionsRecycler() {

        actionsItemDecorator.setDrawable(ContextCompat.getDrawable(activity, R.drawable.divider))
        actionsRecyclerView.addItemDecoration(actionsItemDecorator)
        actionsRecyclerView.layoutManager = LinearLayoutManager(activity)

        val listItemClickListener = View.OnClickListener { view ->

            val title = view.findViewById<TextView>(R.id.actionTitleTextView)

            when (title.text) {
                getString(R.string.terminal) -> setProperFragmentCallback.setTerminalFragment()
                getString(R.string.vehicle_control) -> setProperFragmentCallback.setVehicleControlFragment()
            }
        }

        actionItemArray.add(ActionsItem(getString(R.string.terminal), getString(R.string.terminal_action_item_description), ContextCompat
                .getDrawable(activity, R.drawable.terminal_icon)))

        actionItemArray.add(ActionsItem(getString(R.string.vehicle_control), getString(R.string.vehicle_action_item_description), ContextCompat
                .getDrawable(activity, R.drawable.vehicle_control_icon)))

        actionsAdapter = ActionsAdapter(actionItemArray, listItemClickListener)
        actionsRecyclerView.adapter = actionsAdapter
    }

    private fun getDeviceAndConnect(view: View?, deviceName: String?, btDevice: BluetoothDevice?) { //Connect to selected device
        val device: BluetoothDevice?

        device = if (view != null || deviceName != null) {
            val deviceNameToFind = deviceName ?: "${view?.findViewById<TextView>(R.id.deviceNameTextView)?.text}"

            findDeviceByName(deviceNameToFind)
        } else btDevice

        if (device != null) {
            bluetoothActionsCallback.resetConnection()
            bluetoothActionsCallback.connectToDevice(device)
            connectedDeviceView = view
        }
    }

    //Get BluetoothDevice object by his name displayed on devices list
    private fun findDeviceByName(deviceName: String): BluetoothDevice? {

        for (myBluetoothDevice in bluetoothActionsCallback.getMyPairedBluetoothDevices()) {

            if (myBluetoothDevice.device != null) {
                val device = myBluetoothDevice.device
                if (device.name == deviceName) return device
            }
        }
        return null
    }

    //This is methods which is trigger when BluetoothDirector send notifications
    override fun update(state: MainActivity.Companion.BluetoothStates) {

        if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_DISCONNECTED) {
            connectedDeviceView?.findViewById<ImageView>(R.id.connectedImageView)?.visibility = View.INVISIBLE
            showDisconnectMessage()
            disconnectFromWeb()
            connectedDeviceView = null
            Log.d(TAG, "disconnected")
        } else if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_CONNECTED) {
            activity.runOnUiThread{
                connectedDeviceView?.findViewById<ImageView>(R.id.connectedImageView)?.visibility = View.VISIBLE
                Log.d(TAG, "connected")
            }
        } else if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_FOUND) {
            devicesAdapter.notifyDataSetChanged()
        } else if (state == MainActivity.Companion.BluetoothStates.STATE_BT_ON) {
            if (devicesRecycler.adapter != null) {
                bluetoothActionsCallback.getMyPairedBluetoothDevices()
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