package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.example.sebastian.brulinski.arduinobluetooth.Activities.MainActivity
import com.example.sebastian.brulinski.arduinobluetooth.Fragments.BottomSheet.VehicleWidgetsSettingsBottomSheet
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothActionsInterface
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.BluetoothStateObserversInterface
import com.example.sebastian.brulinski.arduinobluetooth.R
import kotlinx.android.synthetic.main.fragment_vehicle_control.*
import org.jetbrains.anko.toast
import showChangeButtonConfigDialog
import java.util.*

class VehicleControlFragment : Fragment(), BluetoothStateObserversInterface, SensorEventListener {

    private val TAG = "VehicleControlFragment"

    //Accelerometer
    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensor: Sensor
    private val accelerometerManager by lazy {
        ManageAccelerometerData()
    }
    private var lastMove: String? = null

    //ButtonActions
    private val actionForward by lazy { WidgetConfig() }
    private val actionBack by lazy { WidgetConfig() }
    private val actionLeft by lazy { WidgetConfig() }
    private val actionRight by lazy { WidgetConfig() }
    private val speedSeekBar by lazy { WidgetConfig() }
    private val mWidgetsValuesFromAccelerometerAndWidgets by lazy {
        WidgetsValuesFromAccelerometerAndWidgets()
    }

    //Seekbar flags
    private var sendWhenMoved = false

    //Shared Preferences
    private val preferencesButtonsFileName = "buttons_config"
    private val preferencesWidgetsAndAccelerometerFileName = VehicleWidgetsSettingsBottomSheet.preferencesFileName
    private lateinit var sharedPrefButtons: SharedPreferences
    private lateinit var prefButtonsEditor: SharedPreferences.Editor

    private val sharedPrefWidgetsAndAccelerometer: SharedPreferences by lazy {
        activity.getSharedPreferences(preferencesWidgetsAndAccelerometerFileName, Context.MODE_PRIVATE)
    }

    private val preferencesWidgetsAndAccelerometerEditor: SharedPreferences.Editor by lazy {
        sharedPrefWidgetsAndAccelerometer.edit()
    }

    //Callbacks
    private lateinit var bluetoothActionsCallback: BluetoothActionsInterface

    //Touch listeners
    private val forwardTouchListener by lazy {
        View.OnTouchListener { _, motionEvent ->
            if (!accelerometerModeSwitch.isChecked) {
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    bluetoothActionsCallback.writeToDevice(actionForward.release().toByteArray())

                } else if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    bluetoothActionsCallback.writeToDevice(actionForward.press().toByteArray())
                }
            }
            true
        }
    }

    //Touch listeners
    private val backTouchListener by lazy {
        View.OnTouchListener { _, motionEvent ->
            if (!accelerometerModeSwitch.isChecked) {
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    bluetoothActionsCallback.writeToDevice(actionBack.release().toByteArray())

                } else if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    bluetoothActionsCallback.writeToDevice(actionBack.press().toByteArray())
                }
            }
            true
        }
    }

    //Touch listeners
    private val leftTouchListener by lazy {
        View.OnTouchListener { _, motionEvent ->
            if (!accelerometerModeSwitch.isChecked) {
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    bluetoothActionsCallback.writeToDevice(actionLeft.release().toByteArray())

                } else if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    bluetoothActionsCallback.writeToDevice(actionLeft.press().toByteArray())
                }
            }
            true
        }
    }

    //Touch listeners
    private val rightTouchListener by lazy {
        View.OnTouchListener { _, motionEvent ->
            if (!accelerometerModeSwitch.isChecked) {
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    bluetoothActionsCallback.writeToDevice(actionRight.release().toByteArray())

                } else if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    bluetoothActionsCallback.writeToDevice(actionRight.press().toByteArray())
                }
            }
            true
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
        SEND_WHEN_MOVED,
        SEEK_ID
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
    private val SEEK_ID = "${Move.SEEKBAR}-${Action.SEEK_ID}"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Init shared preferences
        sharedPrefButtons = activity.getSharedPreferences(preferencesButtonsFileName, Context.MODE_PRIVATE)
        prefButtonsEditor = sharedPrefButtons.edit()

        val actions = ArrayList<String>()

        //Get widgets config from shared preferences or set default values
        actions.add(sharedPrefButtons.getString(FPKEY, "f"))
        actions.add(sharedPrefButtons.getString(FRKEY, "s"))
        actions.add(sharedPrefButtons.getBoolean(FHKEY, false).toString())
        actions.add(sharedPrefButtons.getString(BPKEY, "b"))
        actions.add(sharedPrefButtons.getString(BRKEY, "s"))
        actions.add(sharedPrefButtons.getBoolean(BHKEY, false).toString())
        actions.add(sharedPrefButtons.getString(LPKEY, "l"))
        actions.add(sharedPrefButtons.getString(LRKEY, "s"))
        actions.add(sharedPrefButtons.getBoolean(LHKEY, false).toString())
        actions.add(sharedPrefButtons.getString(RPKEY, "r"))
        actions.add(sharedPrefButtons.getString(RRKEY, "s"))
        actions.add(sharedPrefButtons.getBoolean(RHKEY, false).toString())
        actions.add(sharedPrefButtons.getInt(SEEK_MAX_KEY, 255).toString())
        actions.add(sharedPrefButtons.getInt(SEEK_MIN_KEY, 0).toString())
        actions.add(sharedPrefButtons.getBoolean(SEEK_HAS_KEY, false).toString())
        actions.add(sharedPrefButtons.getString(SEEK_ID, "."))
        actions.add(sharedPrefButtons.getBoolean(SEEK_WHEN_SEND, false).toString())

        applyAccelerometerData()

        //Set this config at widgets
        setActions(actions)
    }

    private fun setActions(actionsArray: ArrayList<String>) {

        actionForward.setAndSave(Move.FORWARD, Action.PRESS, actionsArray[0], null, null, actionsArray[2].toBoolean())
        actionForward.setAndSave(Move.FORWARD, Action.RELEASE, actionsArray[1], null, null, actionsArray[2].toBoolean())

        actionBack.setAndSave(Move.BACK, Action.PRESS, actionsArray[3], null, null, actionsArray[5].toBoolean())
        actionBack.setAndSave(Move.BACK, Action.RELEASE, actionsArray[4], null, null, actionsArray[5].toBoolean())

        actionLeft.setAndSave(Move.LEFT, Action.PRESS, actionsArray[6], null, null, actionsArray[8].toBoolean())
        actionLeft.setAndSave(Move.LEFT, Action.RELEASE, actionsArray[7], null, null, actionsArray[8].toBoolean())

        actionRight.setAndSave(Move.RIGHT, Action.PRESS, actionsArray[9], null, null, actionsArray[11].toBoolean())
        actionRight.setAndSave(Move.RIGHT, Action.RELEASE, actionsArray[10], null, null, actionsArray[11].toBoolean())

        speedSeekBar.setAndSave(Move.SEEKBAR, Action.MAX, actionsArray[12], null, null, actionsArray[16].toBoolean())
        speedSeekBar.setAndSave(Move.SEEKBAR, Action.MIN, actionsArray[13], null, null, actionsArray[16].toBoolean())
        speedSeekBar.setAndSave(Move.SEEKBAR, Action.SEND_WHEN_MOVED, "", actionsArray[14].toBoolean(), null, actionsArray[16].toBoolean())
        speedSeekBar.setAndSave(Move.SEEKBAR, Action.SEEK_ID, "", null, actionsArray[15], actionsArray[16].toBoolean())
    }


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            layoutInflater.inflate(R.layout.fragment_vehicle_control, container, false)


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vehicleSpeedSeekBar.setSeekBarMax(speedSeekBar.press(), speedSeekBar.release())
        sendWhenMoved = speedSeekBar.sendWhenItMoves()
        sendWhenItMovesSwitch.isChecked = sendWhenMoved
        setControlMode()

        editModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                setControlMode()
            } else {
                setClickActions()
                addTouchListenersToButtons(null, null, null, null)
            }
        }

        sendWhenItMovesSwitch.setOnCheckedChangeListener { _, isChecked ->
            sendWhenMoved = isChecked
            speedSeekBar.setAndSave(Move.SEEKBAR, Action.SEND_WHEN_MOVED, "", sendWhenMoved, null, speedSeekBar.hasNewLine())
        }

        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)


        accelerometerModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mSensorManager.registerListener(this@VehicleControlFragment, mSensor,
                        SensorManager.SENSOR_DELAY_NORMAL)
            } else mSensorManager.unregisterListener(this)
        }

        SettingsImageButton.setOnClickListener {
            VehicleWidgetsSettingsBottomSheet().show(activity.supportFragmentManager, "SETTINGS_BOTTOM_SHEET")
        }
    }

    //Accelerometer listeners
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        val mySensor = sensorEvent?.sensor

        if (mySensor?.type == Sensor.TYPE_ACCELEROMETER && bluetoothActionsCallback.isConnectedToDevice()) {
            val x = sensorEvent.values[0]
            val y = sensorEvent.values[1]

            //Calculate PWM based on values from accelerometer
            val pwmX = Math.abs(x).calculatePWM({ it in 4.0F..9.0F })
            val pwmY = Math.abs(y).calculatePWM({ it in 4.0F..9.0F })

            //Log.d(TAG, "PWM X = $pwmX\nPWM Y = $pwmY")

            //Check direction and send command to vehicle
            accelerometerManager.sendToVehicle(pwmX, pwmY,
                    {
                        accelerometerManager.checkMove(Direction.X, x, y, { accelerometerManager.checkStopAction(x, y) })
                    }
                    ,
                    {
                        accelerometerManager.checkMove(Direction.Y, y, x, { accelerometerManager.checkStopAction(x, y) })
                    })

        }
    }

    private fun Float.calculatePWM(condition: (Float) -> Boolean): Int {
        //Transform values in range 4..9 to values in range 0..255
        val result = (((this - 4.0F) * 255.0F) / 4.0F).toInt()
        return if (condition(this) && result <= 255) {
            return result
        } else if (result > 255) 255
        else 0
    }

    private enum class Direction {
        X, Y
    }

    //Class to manage accelerometer data
    private inner class ManageAccelerometerData {

        private fun Float.checkStopXorY(): Boolean = this in -3..3

        fun checkStopAction(x: Float, y: Float): Boolean = x.checkStopXorY() && y.checkStopXorY()//When X and Y is between -3 and 3

        fun checkMove(direction: Direction, value1: Float, value2: Float, checkStopFunction: () -> Boolean): String? {

            if (!checkStopFunction()) {
                return when {
                    value1 < -5 && value2.checkStopXorY() -> when (direction) {
                        Direction.X -> {
                            mWidgetsValuesFromAccelerometerAndWidgets.forwardAction
                        }
                        Direction.Y -> {
                            mWidgetsValuesFromAccelerometerAndWidgets.leftAction
                        }
                    }
                    value1 > 5 && value2.checkStopXorY() -> when (direction) {
                        Direction.X -> {
                            mWidgetsValuesFromAccelerometerAndWidgets.backAction
                        }
                        Direction.Y -> {
                            mWidgetsValuesFromAccelerometerAndWidgets.rightAction
                        }
                    }
                    else -> {
                        null
                    }
                }
            }
            return "s" //TODO impelment STOP action in settings
        }

        fun sendToVehicle(pwmX: Int, pwmY: Int, checkForX: () -> String?, checkForY: () -> String?) {
            val forX = checkForX()
            val forY = checkForY()

            var valueToSent: String? = null
            var pwmValue: Int? = null

            if (forX == forY && forX != null) { //When x and y value is STOP then send only once
                valueToSent = forX
                pwmValue = pwmX
            } else { //Send X or Y values
                //Send x data
                if (forX != null) {
                    valueToSent = forX
                    pwmValue = pwmX
                } else if (forY != null) {
                    valueToSent = forY
                    pwmValue = pwmY
                }
            }

            //Send only when value is different of previous value
            if (valueToSent != null) {
                if (lastMove != valueToSent) {
                    if (mWidgetsValuesFromAccelerometerAndWidgets.sendPWM)
                        valueToSent += "$pwmValue"
                    if (mWidgetsValuesFromAccelerometerAndWidgets.appendNewLine)
                        valueToSent += "\n"
                    bluetoothActionsCallback.writeToDevice(valueToSent.toByteArray())
                }
                lastMove = valueToSent
            }
        }
    }


    private fun Float.sendMoveDirectionToVehicle(toWrite: String, condition: (Float) -> Boolean) {
        if (condition(this) && bluetoothActionsCallback.isConnectedToDevice()) {
            bluetoothActionsCallback.writeToDevice(toWrite.toByteArray())
            return
        }
    }

    /*
    Widgets responses
     */
    private fun addTouchListenersToButtons(forwardTouchListener: View.OnTouchListener?, backTouchListener: View.OnTouchListener?,
                                           leftTouchListener: View.OnTouchListener?, rightTouchListener: View.OnTouchListener?) {

        moveForwardImageButton.setOnTouchListener(forwardTouchListener)

        moveBackImageButton.setOnTouchListener(backTouchListener)

        turnLeftImageButton.setOnTouchListener(leftTouchListener)

        turnRightImageButton.setOnTouchListener(rightTouchListener)
    }

    private fun setControlMode() { //Enable all widgets and set responses for they

        moveForwardImageButton.setOnLongClickListener(null)
        moveBackImageButton.setOnLongClickListener(null)
        turnLeftImageButton.setOnLongClickListener(null)
        turnRightImageButton.setOnLongClickListener(null)

        turnRightImageButton.setOnClickListener(null)
        turnRightImageButton.setOnClickListener(null)
        turnRightImageButton.setOnClickListener(null)
        turnRightImageButton.setOnClickListener(null)

        vehicleSpeedSeekBar.setOnTouchListener(null)

        vehicleSpeedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, p1: Int, p2: Boolean) {
                if (sendWhenMoved && bluetoothActionsCallback.isConnectedToDevice()) {
                    calculateProgressAndSend(seekBar.progress)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (!sendWhenMoved && bluetoothActionsCallback.isConnectedToDevice()) {
                    calculateProgressAndSend(seekBar.progress)
                }
            }
        })

        addTouchListenersToButtons(forwardTouchListener, backTouchListener, leftTouchListener, rightTouchListener)
    }

    //Calculate speed from seekbar and send to device
    private fun calculateProgressAndSend(progress: Int) {
        var myProgress = progress
        myProgress += speedSeekBar.release().toInt()

        val toWrite = "${speedSeekBar.speedSeekBarId()}$myProgress".addNewLine { speedSeekBar.hasNewLine() }

        bluetoothActionsCallback.writeToDevice(toWrite)
    }

    private fun String.addNewLine(condition: () -> Boolean): ByteArray {
        return if (condition()) "$this\n".toByteArray()
        else this.toByteArray()
    }

    //Response for widget click in edit mode or control mode
    private fun setClickActions() {

        moveForwardImageButton.setOnLongClickListener {
            moveForwardImageButton.showDialog(action = Move.FORWARD, press = actionForward.press(), release = actionForward.release(), seekId = speedSeekBar.speedSeekBarId(), hasNewLine = actionForward.hasNewLine())
        }

        moveBackImageButton.setOnLongClickListener {
            moveForwardImageButton.showDialog(action = Move.BACK, press = actionBack.press(), release = actionBack.release(), seekId = speedSeekBar.speedSeekBarId(), hasNewLine = actionBack.hasNewLine())
        }

        turnLeftImageButton.setOnLongClickListener {
            moveForwardImageButton.showDialog(action = Move.LEFT, press = actionLeft.press(), release = actionLeft.release(), seekId = speedSeekBar.speedSeekBarId(), hasNewLine = actionLeft.hasNewLine())
        }

        turnRightImageButton.setOnLongClickListener {
            moveForwardImageButton.showDialog(action = Move.RIGHT, press = actionRight.press(), release = actionRight.release(), seekId = speedSeekBar.speedSeekBarId(), hasNewLine = actionRight.hasNewLine())
        }

        moveForwardImageButton.setOnClickListener {
            activity.toast("${Move.FORWARD}\nPress: ${actionForward.press().trim()}\nRelease: ${actionForward.release().trim()}\nNew line: ${actionForward.hasNewLine()}")
        }

        moveBackImageButton.setOnClickListener {
            activity.toast("${Move.BACK}\nPress: ${actionBack.press().trim()}\nRelease: ${actionBack.release().trim()}\nNew line: ${actionBack.hasNewLine()}")
        }

        turnLeftImageButton.setOnClickListener {
            activity.toast("${Move.LEFT}\nPress : ${actionLeft.press().trim()}\nRelease: ${actionLeft.release().trim()}\nNew line: ${actionLeft.hasNewLine()}")
        }

        turnRightImageButton.setOnClickListener {
            activity.toast("${Move.RIGHT}\nPress: ${actionRight.press().trim()}\nRelease: ${actionRight.release().trim()}\nNew line: ${actionRight.hasNewLine()}")
        }

        vehicleSpeedSeekBar.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                vehicleSpeedSeekBar.showDialog(Move.SEEKBAR, speedSeekBar.press(), speedSeekBar.release(), speedSeekBar.speedSeekBarId(), speedSeekBar.hasNewLine())

            }
            true
        }

        vehicleSpeedSeekBar.setOnSeekBarChangeListener(null)
    }

    //Show this dialog when we want to edit some responses at widgets
    private fun View.showDialog(action: Move?, press: String, release: String, seekId: String?, hasNewLine: Boolean): Boolean {

        showChangeButtonConfigDialog(activity, getString(R.string.change_button_config), action.toString(), true, getString(android.R.string.ok),
                getString(android.R.string.cancel), press, release, seekId, hasNewLine, this is SeekBar,

                { actionPress, actionRelease, appendNewLine, seekBarId ->
                    Log.d(TAG, seekBarId)

                    actionsForWidgets(action!!, actionPress, actionRelease, seekBarId, appendNewLine)

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
    private fun actionsForWidgets(action: Move, actionPress: String, actionRelease: String, seekId: String?, appendNewLine: Boolean) {
        when (action) {
            Move.FORWARD -> {
                actionForward.setAndSave(Move.FORWARD, Action.PRESS, actionPress, null, null, appendNewLine)
                actionForward.setAndSave(Move.FORWARD, Action.RELEASE, actionRelease, null, null, appendNewLine)
            }
            Move.BACK -> {
                actionBack.setAndSave(Move.BACK, Action.PRESS, actionPress, null, null, appendNewLine)
                actionBack.setAndSave(Move.BACK, Action.RELEASE, actionRelease, null, null, appendNewLine)
            }
            Move.LEFT -> {
                actionLeft.setAndSave(Move.LEFT, Action.PRESS, actionPress, null, null, appendNewLine)
                actionLeft.setAndSave(Move.LEFT, Action.RELEASE, actionRelease, null, null, appendNewLine)
            }
            Move.RIGHT -> {
                actionRight.setAndSave(Move.RIGHT, Action.PRESS, actionPress, null, null, appendNewLine)
                actionRight.setAndSave(Move.RIGHT, Action.RELEASE, actionRelease, null, null, appendNewLine)
            }
            Move.SEEKBAR -> {
                speedSeekBar.setAndSave(Move.SEEKBAR, Action.MAX, actionPress, null, null, appendNewLine)
                speedSeekBar.setAndSave(Move.SEEKBAR, Action.MIN, actionRelease, null, null, appendNewLine)
                speedSeekBar.setAndSave(Move.SEEKBAR, Action.SEND_WHEN_MOVED, actionRelease, sendWhenMoved, null, appendNewLine)
                speedSeekBar.setAndSave(Move.SEEKBAR, Action.SEEK_ID, actionRelease, null, seekId, appendNewLine)
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
        mSensorManager.unregisterListener(this)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        (activity as MainActivity).supportActionBar?.show()
        MainActivity.mBluetoothStateDirector.unregisterObserver(this)
    }


    override fun onResume() {
        super.onResume()
        if (accelerometerModeSwitch.isChecked)
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    fun applyAccelerometerData() {
        //For forward accelerometer move
        mWidgetsValuesFromAccelerometerAndWidgets.forwardAction =
                sharedPrefWidgetsAndAccelerometer.getString("${VehicleWidgetsSettingsBottomSheet
                        .Companion
                        .MySharedPreferencesKeys
                        .FORWARD_ACTION}"
                        , "f")
        //For back accelerometer move
        mWidgetsValuesFromAccelerometerAndWidgets.backAction =
                sharedPrefWidgetsAndAccelerometer.getString("${VehicleWidgetsSettingsBottomSheet
                        .Companion
                        .MySharedPreferencesKeys
                        .BACK_ACTION}"
                        , "b")
        //For left accelerometer move
        mWidgetsValuesFromAccelerometerAndWidgets.leftAction =
                sharedPrefWidgetsAndAccelerometer.getString("${VehicleWidgetsSettingsBottomSheet
                        .Companion
                        .MySharedPreferencesKeys
                        .LEFT_ACTION}"
                        , "l")
        //For right accelerometer move
        mWidgetsValuesFromAccelerometerAndWidgets.rightAction =
                sharedPrefWidgetsAndAccelerometer.getString("${VehicleWidgetsSettingsBottomSheet
                        .Companion
                        .MySharedPreferencesKeys
                        .RIGHT_ACTION}"
                        , "r")
        //For send pwm cation for accelerometer move
        mWidgetsValuesFromAccelerometerAndWidgets.sendPWM =
                sharedPrefWidgetsAndAccelerometer.getBoolean("${VehicleWidgetsSettingsBottomSheet
                        .Companion
                        .MySharedPreferencesKeys
                        .SEND_PWM}"
                        , true)
        //For append new line to accelerometer data to send
        mWidgetsValuesFromAccelerometerAndWidgets.appendNewLine =
                sharedPrefWidgetsAndAccelerometer.getBoolean("${VehicleWidgetsSettingsBottomSheet
                        .Companion
                        .MySharedPreferencesKeys
                        .APPEND_NEW_LINE}"
                        , true)
    }

    //Object of this class represents single widget as button or seekbar
    private inner class WidgetConfig(private var pressAction: String = "1", private var releaseAction: String = "2",
                                     private var appendNewLine: Boolean = true, private var whenSend: Boolean? = false,
                                     private var seekBarId: String? = ".") {

        fun setAndSave(move: Move, action: Action, text: String, whenSend: Boolean?, seekId: String?, containNewLine: Boolean) {
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
                Action.SEEK_ID -> this.seekBarId = seekId
                VehicleControlFragment.Action.HAS_NEW_LINE -> TODO()
            }

            appendNewLine = containNewLine

            if (move == Move.SEEKBAR && action != Action.SEND_WHEN_MOVED && action != Action.SEEK_ID) {
                prefButtonsEditor.putInt("$move-$action", toSave.toInt())
            } else if (action == Action.SEND_WHEN_MOVED && whenSend != null)
                prefButtonsEditor.putBoolean("$move-$action", whenSend)
            else if (action == Action.SEEK_ID)
                prefButtonsEditor.putString("$move-$action", seekId)
            else prefButtonsEditor.putString("$move-$action", toSave)

            prefButtonsEditor.putBoolean("$move-${Action.HAS_NEW_LINE}", containNewLine)
            prefButtonsEditor.commit()
        }

        fun press(): String = pressAction
        fun release(): String = releaseAction
        fun hasNewLine(): Boolean = appendNewLine
        fun sendWhenItMoves(): Boolean = whenSend!!
        fun speedSeekBarId(): String? = seekBarId!!
    }

    private class WidgetsValuesFromAccelerometerAndWidgets(var forwardAction: String = "",
                                                           var backAction: String = "",
                                                           var leftAction: String = "",
                                                           var rightAction: String = "",
                                                           var sendPWM: Boolean = true,
                                                           var appendNewLine: Boolean = true)
}