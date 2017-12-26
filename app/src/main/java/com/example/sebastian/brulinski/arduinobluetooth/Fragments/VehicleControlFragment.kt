package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
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
    private val actionForward = WidgetConfig()
    private val actionBack = WidgetConfig()
    private val actionLeft = WidgetConfig()
    private val actionRight = WidgetConfig()
    private val speedSeekBar = WidgetConfig()

    //Seekbar flags
    private var sendWhenMoved = false

    //Shared Preferences
    private val preferencesFileName = "buttons_config"
    private lateinit var sharedPref: SharedPreferences
    private lateinit var prefEditor: SharedPreferences.Editor

    //Callbacks
    private lateinit var bluetoothActionsCallback: BluetoothActionsInterface

    //Touch listeners
    private val forwardTouchListener = object : View.OnTouchListener {
        override fun onTouch(p0: View?, motionEvent: MotionEvent): Boolean {
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                bluetoothActionsCallback.writeToDevice(actionForward.release().toByteArray())

            } else if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                bluetoothActionsCallback.writeToDevice(actionForward.press().toByteArray())
            }
            return true
        }
    }

    //Touch listeners
    private val backTouchListener = object : View.OnTouchListener {
        override fun onTouch(p0: View?, motionEvent: MotionEvent): Boolean {
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                bluetoothActionsCallback.writeToDevice(actionBack.release().toByteArray())

            } else if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                bluetoothActionsCallback.writeToDevice(actionBack.press().toByteArray())
            }
            return true
        }
    }

    //Touch listeners
    private val leftTouchListener = object : View.OnTouchListener {
        override fun onTouch(p0: View?, motionEvent: MotionEvent): Boolean {
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                bluetoothActionsCallback.writeToDevice(actionLeft.release().toByteArray())

            } else if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                bluetoothActionsCallback.writeToDevice(actionRight.press().toByteArray())
            }
            return true
        }
    }

    //Touch listeners
    private val rightTouchListener = object : View.OnTouchListener {
        override fun onTouch(p0: View?, motionEvent: MotionEvent): Boolean {
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                bluetoothActionsCallback.writeToDevice(actionRight.release().toByteArray())

            } else if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                bluetoothActionsCallback.writeToDevice(actionRight.press().toByteArray())
            }
            return true
        }
    }

    //Enum values
    enum class Move {
        FORWARD,
        BACK,
        LEFT,
        RIGHT,
        SEEKBAR
    }

    enum class Action {
        PRESS,
        RELEASE,
        HAS_NEW_LINE,
        MIN,
        MAX,
        SEND_WHEN_MOVED
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
    private val SEEK_MAX_KEY = "${Move.SEEKBAR}-${Action.MAX}"
    private val SEEK_MIN_KEY = "${Move.SEEKBAR}-${Action.MIN}"
    private val SEEK_HAS_KEY = "${Move.SEEKBAR}-${Action.HAS_NEW_LINE}"
    private val SEEK_WHEN_SEND = "${Move.SEEKBAR}-${Action.SEND_WHEN_MOVED}"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Init shared preferences
        sharedPref = activity.getSharedPreferences(preferencesFileName, Context.MODE_PRIVATE)
        prefEditor = sharedPref.edit()

        val actions = ArrayList<String>()

        //Get widgets config from shared preferences or set default values
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
        actions.add(sharedPref.getInt(SEEK_MAX_KEY, 255).toString())
        actions.add(sharedPref.getInt(SEEK_MIN_KEY, 0).toString())
        actions.add(sharedPref.getBoolean(SEEK_HAS_KEY, false).toString())
        actions.add(sharedPref.getBoolean(SEEK_WHEN_SEND, false).toString())

        //Set this config at widgets
        setActions(actions)
    }

    private fun setActions(actionsArray: ArrayList<String>) {

        actionForward.setAndSave(Move.FORWARD, Action.PRESS, actionsArray[0], null, actionsArray[2].toBoolean())
        actionForward.setAndSave(Move.FORWARD, Action.RELEASE, actionsArray[1], null, actionsArray[2].toBoolean())

        actionBack.setAndSave(Move.BACK, Action.PRESS, actionsArray[3], null, actionsArray[5].toBoolean())
        actionBack.setAndSave(Move.BACK, Action.RELEASE, actionsArray[4], null, actionsArray[5].toBoolean())

        actionLeft.setAndSave(Move.LEFT, Action.PRESS, actionsArray[6], null, actionsArray[8].toBoolean())
        actionLeft.setAndSave(Move.LEFT, Action.RELEASE, actionsArray[7], null, actionsArray[8].toBoolean())

        actionRight.setAndSave(Move.RIGHT, Action.PRESS, actionsArray[9], null, actionsArray[11].toBoolean())
        actionRight.setAndSave(Move.RIGHT, Action.RELEASE, actionsArray[10], null, actionsArray[11].toBoolean())

        speedSeekBar.setAndSave(Move.SEEKBAR, Action.MAX, actionsArray[12], null, actionsArray[15].toBoolean())
        speedSeekBar.setAndSave(Move.SEEKBAR, Action.MIN, actionsArray[13], null, actionsArray[15].toBoolean())
        speedSeekBar.setAndSave(Move.SEEKBAR, Action.SEND_WHEN_MOVED, "", actionsArray[14].toBoolean(), actionsArray[15].toBoolean())
    }


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_vehicle_control, container, false)

        binding.speedSeekBar.setSeekBarMax(speedSeekBar.press(), speedSeekBar.release())
        sendWhenMoved = speedSeekBar.sendWhenItMoves()
        binding.sendWhenMovedSwitch.isChecked = sendWhenMoved
        setControlMode()

        binding.editModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                setControlMode()
            } else {
                setClickActions()
                addTouchListenersToButtons(null, null, null, null)
            }
        }

        binding.sendWhenMovedSwitch.setOnCheckedChangeListener { _, isChecked ->
            sendWhenMoved = isChecked
            speedSeekBar.setAndSave(Move.SEEKBAR, Action.SEND_WHEN_MOVED, "", sendWhenMoved, speedSeekBar.hasNewLine())
        }

        speedSeekBar

        return binding.root
    }

    /*
    Widgets responses
     */
    private fun addTouchListenersToButtons(forwardTouchListener: View.OnTouchListener?, backTouchListener: View.OnTouchListener?,
                                           leftTouchListener: View.OnTouchListener?, rightTouchListener: View.OnTouchListener?) {

        binding.moveForward.setOnTouchListener(forwardTouchListener)

        binding.moveBack.setOnTouchListener(backTouchListener)

        binding.turnLeft.setOnTouchListener(leftTouchListener)

        binding.turnRight.setOnTouchListener(rightTouchListener)
    }

    private fun setControlMode() { //Enable all widgets and set responses for they

        binding.moveForward.setOnLongClickListener(null)
        binding.moveBack.setOnLongClickListener(null)
        binding.turnLeft.setOnLongClickListener(null)
        binding.turnRight.setOnLongClickListener(null)

        binding.turnRight.setOnClickListener(null)
        binding.turnRight.setOnClickListener(null)
        binding.turnRight.setOnClickListener(null)
        binding.turnRight.setOnClickListener(null)

        binding.speedSeekBar.setOnTouchListener(null)

        binding.speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, p1: Int, p2: Boolean) {
                if (sendWhenMoved && bluetoothActionsCallback.isConnectedToDevice()) {
                    var progress = seekBar.progress
                    progress += speedSeekBar.release().toInt()

                    bluetoothActionsCallback.writeToDevice(progress.toString().toByteArray())
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (!sendWhenMoved && bluetoothActionsCallback.isConnectedToDevice()) {
                    var progress = seekBar.progress
                    progress += speedSeekBar.release().toInt()

                    bluetoothActionsCallback.writeToDevice(progress.toString().toByteArray())
                }
            }
        })

        addTouchListenersToButtons(forwardTouchListener, backTouchListener, leftTouchListener, rightTouchListener)
    }

    //Response for widget click in edit mode or control mode
    private fun setClickActions() {

        binding.moveForward.setOnLongClickListener {
            binding.moveForward.showDialog(action = Move.FORWARD, press = actionForward.press(), release = actionForward.release(), hasNewLine = actionForward.hasNewLine())
        }

        binding.moveBack.setOnLongClickListener {
            binding.moveForward.showDialog(action = Move.BACK, press = actionBack.press(), release = actionBack.release(), hasNewLine = actionBack.hasNewLine())
        }

        binding.turnLeft.setOnLongClickListener {
            binding.moveForward.showDialog(action = Move.LEFT, press = actionLeft.press(), release = actionLeft.release(), hasNewLine = actionLeft.hasNewLine())
        }

        binding.turnRight.setOnLongClickListener {
            binding.moveForward.showDialog(action = Move.RIGHT, press = actionRight.press(), release = actionRight.release(), hasNewLine = actionRight.hasNewLine())
        }

        binding.moveForward.setOnClickListener {
            Toast.makeText(activity,
                    "${Move.FORWARD}\nPress: ${actionForward.press()}\nRelease: ${actionForward.release()}\nNew line: ${actionForward.hasNewLine()}"
                    , Toast.LENGTH_SHORT).show()
        }

        binding.moveBack.setOnClickListener {
            Toast.makeText(activity,
                    "${Move.BACK}\nPress: ${actionBack.press()}\nRelease: ${actionBack.release()}\nNew line: ${actionBack.hasNewLine()}"
                    , Toast.LENGTH_SHORT).show()
        }

        binding.turnLeft.setOnClickListener {
            Toast.makeText(activity,
                    "${Move.LEFT}\nPress : ${actionLeft.press()}\nRelease: ${actionLeft.release()}\nNew line: ${actionLeft.hasNewLine()}"
                    , Toast.LENGTH_SHORT).show()
        }

        binding.turnRight.setOnClickListener {
            Toast.makeText(activity,
                    "${Move.RIGHT}\nPress: ${actionRight.press()}\nRelease: ${actionRight.release()}\nNew line: ${actionRight.hasNewLine()}"
                    , Toast.LENGTH_SHORT).show()
        }

        binding.speedSeekBar.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                binding.speedSeekBar.showDialog(Move.SEEKBAR, speedSeekBar.press(), speedSeekBar.release(), speedSeekBar.hasNewLine())

            }
            true
        }

        binding.speedSeekBar.setOnSeekBarChangeListener(null)
    }

    //Show this dialog when we want to edit some responses at widgets
    private fun View.showDialog(action: Move?, press: String, release: String, hasNewLine: Boolean): Boolean {

        showChangeButtonConfigDialog(activity, getString(R.string.change_button_config), action.toString(), true, getString(android.R.string.ok),
                getString(android.R.string.cancel), press, release, hasNewLine, this is SeekBar,

                { actionPress, actionRelease, appendNewLine ->

                    actionsForWidgets(action!!, actionPress, actionRelease, appendNewLine)

                    (this as? SeekBar)?.setSeekBarMax(actionPress, actionRelease)
                },
                {

                })

        return true
    }

    //Set seek bar max value
    private fun SeekBar.setSeekBarMax(actionPress: String, actionRelease: String) {
        val deltaMaxMin = actionPress.toInt() - actionRelease.toInt()
        this.max = if (deltaMaxMin > 0) deltaMaxMin
        else actionPress.toInt()
    }

    //Set actions at widgets and save then in shared preferences
    private fun actionsForWidgets(action: Move, actionPress: String, actionRelease: String, appendNewLine: Boolean) {
        when (action) {
            Move.FORWARD -> {
                actionForward.setAndSave(Move.FORWARD, Action.PRESS, actionPress, null, appendNewLine)
                actionForward.setAndSave(Move.FORWARD, Action.RELEASE, actionRelease, null, appendNewLine)
            }
            Move.BACK -> {
                actionBack.setAndSave(Move.BACK, Action.PRESS, actionPress, null, appendNewLine)
                actionBack.setAndSave(Move.BACK, Action.RELEASE, actionRelease, null, appendNewLine)
            }
            Move.LEFT -> {
                actionLeft.setAndSave(Move.LEFT, Action.PRESS, actionPress, null, appendNewLine)
                actionLeft.setAndSave(Move.LEFT, Action.RELEASE, actionRelease, null, appendNewLine)
            }
            Move.RIGHT -> {
                actionRight.setAndSave(Move.RIGHT, Action.PRESS, actionPress, null, appendNewLine)
                actionRight.setAndSave(Move.RIGHT, Action.RELEASE, actionRelease, null, appendNewLine)
            }
            Move.SEEKBAR -> {
                speedSeekBar.setAndSave(Move.SEEKBAR, Action.MAX, actionPress, null, appendNewLine)
                speedSeekBar.setAndSave(Move.SEEKBAR, Action.MIN, actionRelease, null, appendNewLine)
                speedSeekBar.setAndSave(Move.SEEKBAR, Action.SEND_WHEN_MOVED, actionRelease, sendWhenMoved, appendNewLine)
            }
        }
    }

    //Responses for BluetoothDirector notifications
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


    //Object of this class represents single widget as button or seekbar
    private inner class WidgetConfig(private var pressAction: String = "1", private var releaseAction: String = "2",
                                     private var appendNewLine: Boolean = true, private var whenSend: Boolean? = false) {

        fun setAndSave(move: Move, action: Action, text: String, whenSend: Boolean?, containNewLine: Boolean) {
            var toSave = ""

            if (whenSend == null) {
                toSave = text + "\n"
                if (!containNewLine || move == Move.SEEKBAR) toSave = toSave.replace("\n", "").trim()
            }

            when (action) {
                Action.PRESS -> this.pressAction = toSave
                Action.RELEASE -> this.releaseAction = toSave
                Action.MAX -> this.pressAction = toSave
                Action.MIN -> this.releaseAction = toSave
                Action.SEND_WHEN_MOVED -> this.whenSend = whenSend
                VehicleControlFragment.Action.HAS_NEW_LINE -> TODO()
            }

            appendNewLine = containNewLine

            if (move == Move.SEEKBAR && action != Action.SEND_WHEN_MOVED) {
                prefEditor.putInt("$move-$action", toSave.toInt())
            } else if (action == Action.SEND_WHEN_MOVED && whenSend != null)
                prefEditor.putBoolean("$move-$action", whenSend)
            else prefEditor.putString("$move-$action", toSave)

            prefEditor.putBoolean("$move-${Action.HAS_NEW_LINE}", containNewLine)
            prefEditor.commit()
        }

        fun press(): String = pressAction
        fun release(): String = releaseAction
        fun hasNewLine(): Boolean = appendNewLine
        fun sendWhenItMoves(): Boolean = whenSend!!
    }
}
