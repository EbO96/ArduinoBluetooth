package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.sebastian.brulinski.arduinobluetooth.Activities.MainActivity
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothActionsInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothStateObserversInterface
import com.example.sebastian.brulinski.arduinobluetooth.R
import com.example.sebastian.brulinski.arduinobluetooth.databinding.FragmentVehicleControlBinding
import showChangeButtonConfigDialog

class VehicleControlFragment : Fragment(), BluetoothStateObserversInterface {

    private lateinit var binding: FragmentVehicleControlBinding
    private val TAG = "VehicleControlFragment"

    //ButtonActions
    private val actionForward = ButtonConfig()
    private val actionBack = ButtonConfig()
    private val actionLeft = ButtonConfig()
    private val actionRight = ButtonConfig()

    //Shared Preferences
    private val preferencesFileName = "buttons_config"

    private lateinit var sharedPref: SharedPreferences

    private lateinit var prefEditor: SharedPreferences.Editor

    //Callbacks
    private lateinit var bluetoothActionsCallback: BluetoothActionsInterface

    enum class Move {
        FORWARD,
        BACK,
        LEFT,
        RIGHT
    }

    enum class Action {
        PRESS,
        RELEASE,
        HAS_NEW_LINE
    }

    private val FPKEY = "${Move.FORWARD}-${Action.PRESS}"
    private val FRKEY = "${Move.FORWARD}-${Action.RELEASE}"
    private val FHKEY = "${Move.FORWARD}-${Action.HAS_NEW_LINE}"
    private val BPKEY = "${Move.BACK}-${Action.PRESS}"
    private val BRKEY = "${Move.BACK}-${Action.RELEASE}"
    private val BHKEY = "${Move.BACK}-${Action.HAS_NEW_LINE}"
    private val LPKEY = "${Move.LEFT}-${Action.PRESS}"
    private val LRKEY = "${Move.LEFT}-${Action.RELEASE}"
    private val LHKEY = "${Move.LEFT}-${Action.HAS_NEW_LINE}"
    private val RPKEY = "${Move.RIGHT}-${Action.PRESS}"
    private val RRKEY = "${Move.RIGHT}-${Action.RELEASE}"
    private val RHKEY = "${Move.RIGHT}-${Action.HAS_NEW_LINE}"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPref = activity.getSharedPreferences(preferencesFileName, Context.MODE_PRIVATE)
        prefEditor = sharedPref.edit()

        val actions = ArrayList<String>()

        actions.add(sharedPref.getString(FPKEY, "f"))
        actions.add(sharedPref.getString(FRKEY, "s"))
        actions.add(sharedPref.getBoolean(FHKEY, false).toString())
        actions.add(sharedPref.getString(BPKEY, "b"))
        actions.add(sharedPref.getString(BRKEY, "s"))
        actions.add(sharedPref.getBoolean(BHKEY, false).toString())
        actions.add(sharedPref.getString(LPKEY, "l"))
        actions.add(sharedPref.getString(LRKEY, "s"))
        actions.add(sharedPref.getBoolean(LHKEY, false).toString())
        actions.add(sharedPref.getString(RPKEY, "r"))
        actions.add(sharedPref.getString(RRKEY, "s"))
        actions.add(sharedPref.getBoolean(RHKEY, false).toString())

        setActions(actions)

    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_vehicle_control, container, false)

        binding.moveForward.setOnClickListener {
            bluetoothActionsCallback.writeToDevice(actionForward.press().toByteArray())
        }

        binding.moveBack.setOnClickListener {
            bluetoothActionsCallback.writeToDevice(actionBack.press().toByteArray())
        }

        binding.turnLeft.setOnClickListener {
            bluetoothActionsCallback.writeToDevice(actionLeft.press().toByteArray())
        }

        binding.turnRight.setOnClickListener {
            bluetoothActionsCallback.writeToDevice(actionRight.press().toByteArray())
        }

        setLongPressesActions()
        return binding.root
    }

    private fun setActions(actionsArray: ArrayList<String>) {

        actionForward.setAndSave(Move.FORWARD, Action.PRESS, actionsArray[0], actionsArray[2].toBoolean())
        actionForward.setAndSave(Move.FORWARD, Action.RELEASE, actionsArray[1], actionsArray[2].toBoolean())

        actionBack.setAndSave(Move.BACK, Action.PRESS, actionsArray[3], actionsArray[5].toBoolean())
        actionBack.setAndSave(Move.BACK, Action.RELEASE, actionsArray[4], actionsArray[5].toBoolean())

        actionLeft.setAndSave(Move.LEFT, Action.PRESS, actionsArray[6], actionsArray[8].toBoolean())
        actionLeft.setAndSave(Move.LEFT, Action.RELEASE, actionsArray[7], actionsArray[8].toBoolean())

        actionRight.setAndSave(Move.RIGHT, Action.PRESS, actionsArray[9], actionsArray[11].toBoolean())
        actionRight.setAndSave(Move.RIGHT, Action.RELEASE, actionsArray[10], actionsArray[11].toBoolean())
    }

    private fun setLongPressesActions() {
        binding.moveForward.setOnLongClickListener {
            showDialog(action = Move.FORWARD, press = actionForward.press(), release = actionForward.release(), hasNewLine = actionForward.hasNewLine())
        }

        binding.moveBack.setOnLongClickListener {
            showDialog(action = Move.BACK, press = actionBack.press(), release = actionBack.release(), hasNewLine = actionBack.hasNewLine())
        }

        binding.turnLeft.setOnLongClickListener {
            showDialog(action = Move.LEFT, press = actionLeft.press(), release = actionLeft.release(), hasNewLine = actionLeft.hasNewLine())
        }

        binding.turnRight.setOnLongClickListener {
            showDialog(action = Move.RIGHT, press = actionRight.press(), release = actionRight.release(), hasNewLine = actionRight.hasNewLine())
        }
    }


    private fun showDialog(action: Move, press: String, release: String, hasNewLine: Boolean): Boolean {
        showChangeButtonConfigDialog(activity, getString(R.string.change_button_config), null, true, getString(android.R.string.ok),
                getString(android.R.string.cancel), press, release, hasNewLine,

                { actionPress, actionRelease, appendNewLine ->

                    when (action) {
                        Move.FORWARD -> {
                            actionForward.setAndSave(Move.FORWARD, Action.PRESS, actionPress, appendNewLine)
                            actionForward.setAndSave(Move.FORWARD, Action.RELEASE, actionRelease, appendNewLine)
                        }
                        Move.BACK -> {
                            actionBack.setAndSave(Move.BACK, Action.PRESS, actionPress, appendNewLine)
                            actionBack.setAndSave(Move.BACK, Action.RELEASE, actionRelease, appendNewLine)
                        }
                        Move.LEFT -> {
                            actionLeft.setAndSave(Move.LEFT, Action.PRESS, actionPress, appendNewLine)
                            actionLeft.setAndSave(Move.LEFT, Action.RELEASE, actionRelease, appendNewLine)
                        }
                        Move.RIGHT -> {
                            actionRight.setAndSave(Move.RIGHT, Action.PRESS, actionPress, appendNewLine)
                            actionRight.setAndSave(Move.RIGHT, Action.RELEASE, actionRelease, appendNewLine)
                        }
                    }

                },
                {

                })

        return true
    }

    override fun update(state: MainActivity.Companion.BluetoothStates) {

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
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        (activity as MainActivity).supportActionBar?.show()
        MainActivity.mBluetoothStateDirector.unregisterObserver(this)
    }


    private inner class ButtonConfig(private var pressAction: String = "1", private var releaseAction: String = "2",
                                     private var appendNewLine: Boolean = true) {

        fun setAndSave(move: Move, action: Action, text: String, containNewLine: Boolean) {

//            text.replace("\n", "")
//
//            if(containNewLine)

            var toSave = text + "\n"

            if(!containNewLine) toSave = toSave.replace("\n", "").trim()

            when (action) {
                Action.PRESS -> this.pressAction = toSave
                Action.RELEASE -> this.releaseAction = toSave
                else -> {

                }
            }
            appendNewLine = containNewLine

            prefEditor.putString("$move-$action", toSave)
            prefEditor.putBoolean("$move-${Action.HAS_NEW_LINE}", containNewLine)
            prefEditor.commit()
        }

        fun press(): String = pressAction
        fun release(): String = releaseAction
        fun hasNewLine(): Boolean = appendNewLine
    }
}
