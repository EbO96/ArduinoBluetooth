package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import com.example.sebastian.brulinski.arduinobluetooth.Activities.MainActivity
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothActionsInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothStateObserversInterface
import com.example.sebastian.brulinski.arduinobluetooth.R
import kotlinx.android.synthetic.main.fragment_terminal.*

class Terminal : Fragment(), BluetoothStateObserversInterface {

    //Tags and flags
    private val TAG = "Terminal"
    private val STATE_TERMINAL_TEXT_KEY = "terminal_text_key"
    private var appendNewLine = true

    //Callbacks
    private lateinit var bluetoothActionsCallback: BluetoothActionsInterface

    private var sendText = StringBuilder()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            layoutInflater.inflate(R.layout.fragment_terminal, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        MainActivity.mBluetoothStateDirector.registerObserver(this)

        //Change texts color at red when no devices connected
        if (!bluetoothActionsCallback.isConnectedToDevice())
            changeTextColors("#FF0000")

        //Edit text ime options
        terminalEditText.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendMessage()
                }
            }
            true
        }

        sendMessageToDeviceImageButton.setOnClickListener {
            sendMessage()
        }

        clearMessageImageButton.setOnClickListener {
            terminalEditText.setText("")
        }

        clearMessageImageButton.setOnLongClickListener {
            terminalTextView.text = null
            sendText.delete(0, sendText.length)
            true
        }

        if(savedInstanceState != null) {
            sendText = savedInstanceState.getSerializable(STATE_TERMINAL_TEXT_KEY) as StringBuilder
            terminalTextView.text = "$sendText"
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putSerializable(STATE_TERMINAL_TEXT_KEY, sendText)
        super.onSaveInstanceState(outState)
    }


    private fun sendMessage() {
        var text = "${terminalEditText.text}"

        if (appendNewLine) text += "\n"

        bluetoothActionsCallback.writeToDevice(text.toByteArray())

        sendText.append(text)
        terminalTextView.text = "$sendText"
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
        activity.runOnUiThread {
            terminalTextView.setTextColor(Color.parseColor(hexColor))
            terminalEditText.setTextColor(Color.parseColor(hexColor))
            terminalEditText.setHintTextColor(Color.parseColor(hexColor))
            sendMessageToDeviceImageButton.setColorFilter(Color.parseColor(hexColor))
            clearMessageImageButton.setColorFilter(Color.parseColor(hexColor))
        }
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
        MainActivity.mBluetoothStateDirector.unregisterObserver(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater?.inflate(R.menu.terminal_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item?.itemId){
            R.id.send_new_line_option -> run {
                item.isChecked = !item.isChecked
                appendNewLine = item.isChecked
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
