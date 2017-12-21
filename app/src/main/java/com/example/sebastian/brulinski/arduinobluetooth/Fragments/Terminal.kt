package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.example.sebastian.brulinski.arduinobluetooth.Helper.AlertDialogHelper
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothStateObserversInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.TerminalInterface
import com.example.sebastian.brulinski.arduinobluetooth.MainActivity
import com.example.sebastian.brulinski.arduinobluetooth.R
import com.example.sebastian.brulinski.arduinobluetooth.databinding.FragmentTerminalBinding
import java.io.OutputStream

class Terminal : Fragment(), BluetoothStateObserversInterface {

    private lateinit var binding: FragmentTerminalBinding
    private val sendText = StringBuilder()

    private lateinit var terminalCallback: TerminalInterface
    private var mDevice: BluetoothDevice? = null
    private var connectedDeviceSocket: BluetoothSocket? = null
    private lateinit var socketOutputStream: OutputStream

    //Flags
    private var appendNewLine = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null)
            mDevice = arguments.getParcelable("device")
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putParcelable("device", mDevice)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_terminal, container, false)
        setHasOptionsMenu(true)
        terminalCallback = activity as TerminalInterface

        connectedDeviceSocket = terminalCallback.getConnectedDeviceSocket()
        if (connectedDeviceSocket != null && connectedDeviceSocket!!.isConnected) {
            socketOutputStream = connectedDeviceSocket!!.outputStream

        } else {
            Toast.makeText(activity, "First connect to device", Toast.LENGTH_SHORT).show()
            binding.terminalEditText.isEnabled = false
            Handler().postDelayed({
                if (this.isAdded)
                    activity.supportFragmentManager.popBackStack()

            }, 2800)
        }

        if (savedInstanceState != null)
            mDevice = savedInstanceState.getParcelable("device")

        //Edit text ime options
        binding.terminalEditText.setOnEditorActionListener { textView, actionId, keyEvent ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendToDevice("${binding.terminalEditText.text}")
                }
            }
            true
        }

        binding.sendMessageToDevice.setOnClickListener {
            sendToDevice("${binding.terminalEditText.text}")
        }

        binding.clearMessage.setOnClickListener {
            binding.terminalEditText.setText("")
        }

        binding.clearMessage.setOnLongClickListener {
            binding.terminalTextTextView.text = null
            sendText.delete(0, sendText.length)
            true
        }

        return binding.root
    }

    override fun update(state: MainActivity.Companion.BluetoothStates) {
        if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_DISCONNECTED)
            AlertDialogHelper.showAlert(activity, "Connection with ${mDevice!!.name} lost",
                    getString(R.string.connection_lost_message), false,
                    getString(R.string.connect), getString(R.string.close),
                    {

            },
                    {
                activity.supportFragmentManager.popBackStack()
            })
    }

    private fun sendToDevice(text: String) {
        if (!text.isEmpty() && connectedDeviceSocket != null && connectedDeviceSocket!!.isConnected) {
            sendText.appendln(text)
            binding.terminalTextTextView.text = "$sendText\n"

            val toSend = if (appendNewLine) "$text\n".toByteArray()
            else text.toByteArray()

            terminalCallback.getMyBluetooth()!!.write(
                    toSend,
                    socketOutputStream
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.terminal_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        MainActivity.mBluetoothStateDirector.unregisterObserver(this)
    }
}