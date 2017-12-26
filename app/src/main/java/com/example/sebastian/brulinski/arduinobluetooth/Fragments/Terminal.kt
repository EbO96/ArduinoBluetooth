package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.content.Context
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.example.sebastian.brulinski.arduinobluetooth.Activities.MainActivity
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothActionsInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothStateObserversInterface
import com.example.sebastian.brulinski.arduinobluetooth.R
import com.example.sebastian.brulinski.arduinobluetooth.databinding.FragmentTerminalBinding

class Terminal : Fragment(), BluetoothStateObserversInterface {

    //Tags and flags
    private val TAG = "Terminal"
    private var appendNewLine = true

    //Callbacks
    private lateinit var bluetoothActionsCallback: BluetoothActionsInterface

    private lateinit var binding: FragmentTerminalBinding//Binding
    private val sendText = StringBuilder()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_terminal, container, false)
        setHasOptionsMenu(true)

        //Change texts color at red when no devices connected
        if (!bluetoothActionsCallback.isConnectedToDevice())
            changeTextColors("#FF0000")

        //Edit text ime options
        binding.terminalEditText.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendMessage()
                }
            }
            true
        }

        binding.sendMessageToDevice.setOnClickListener {
            sendMessage()
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

    private fun sendMessage() {
        var text = "${binding.terminalEditText.text}"

        if (appendNewLine) text += "\n"

        bluetoothActionsCallback.writeToDevice(text.toByteArray())

        sendText.append(text)
        binding.terminalTextTextView.text = "$sendText"
    }

    //Responses at notifications from BluetoothDirector
    override fun update(state: MainActivity.Companion.BluetoothStates) {

        if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_DISCONNECTED) {
            changeTextColors("#FF0000")
        }
        if (state == MainActivity.Companion.BluetoothStates.STATE_DEVICE_CONNECTED) {
            changeTextColors("#00FF00")
        }
    }

    private fun changeTextColors(hexColor: String) {
        binding.terminalTextTextView.setTextColor(Color.parseColor(hexColor))
        binding.terminalEditText.setTextColor(Color.parseColor(hexColor))
        binding.terminalEditText.setHintTextColor(Color.parseColor(hexColor))
        binding.sendMessageToDevice.setColorFilter(Color.parseColor(hexColor))
        binding.clearMessage.setColorFilter(Color.parseColor(hexColor))
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
        (activity as MainActivity).supportActionBar?.show()
        MainActivity.mBluetoothStateDirector.unregisterObserver(this)
    }
}
