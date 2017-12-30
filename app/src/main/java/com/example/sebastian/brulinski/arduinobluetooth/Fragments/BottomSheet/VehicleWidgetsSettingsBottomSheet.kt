package com.example.sebastian.brulinski.arduinobluetooth.Fragments.BottomSheet

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.ApplyVehicleWidgetsSettings
import com.example.sebastian.brulinski.arduinobluetooth.R
import android.view.ViewGroup.LayoutParams.FILL_PARENT



class VehicleWidgetsSettingsBottomSheet : BottomSheetDialogFragment() {

    //Callbacks
    private lateinit var mApplySettingsCallback: ApplyVehicleWidgetsSettings

    //TAG
    companion object {
        val preferencesFileName = "VehicleWidgetSettings"

        enum class MySharedPreferencesKeys {
            FORWARD_ACTION,
            BACK_ACTION,
            LEFT_ACTION,
            RIGHT_ACTION,
            SEND_PWM,
            APPEND_NEW_LINE
        }
    }

    val TAG = "VehicleSettingsBottomSheet"

    private val sharedPreferences: SharedPreferences by lazy {
        activity.getSharedPreferences(preferencesFileName, Context.MODE_PRIVATE)
    }

    private val preferencesEditor: SharedPreferences.Editor by lazy {
        sharedPreferences.edit()
    }

    //Widgets
    private lateinit var forwardActionEditText: EditText
    private lateinit var backActionEditText: EditText
    private lateinit var leftActionEditText: EditText
    private lateinit var rightActionEditText: EditText
    private lateinit var sendPwmCheckBox: CheckBox
    private lateinit var appendNewLineCheckBox: CheckBox

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater?.inflate(R.layout.vehicle_widgets_settings, container, false)

        forwardActionEditText = view!!.findViewById(R.id.forwardValueEditText)
        backActionEditText = view.findViewById(R.id.backValueEditText)
        leftActionEditText = view.findViewById(R.id.leftValueEditText)
        rightActionEditText = view.findViewById(R.id.rightValueEditText)

        sendPwmCheckBox = view.findViewById(R.id.sendPWMValuesCheckBox)
        appendNewLineCheckBox = view.findViewById(R.id.appendNewLineForAccelerometerCheckBox)


        return view
    }

    private fun getValuesFromFieldsAndSave() {
        WidgetsValues.forwardAction = "${forwardActionEditText.text}"
        WidgetsValues.backAction = "${backActionEditText.text}"
        WidgetsValues.leftAction = "${leftActionEditText.text}"
        WidgetsValues.rightAction = "${rightActionEditText.text}"

        WidgetsValues.sendPWM = sendPwmCheckBox.isChecked
        WidgetsValues.appendNewLine = appendNewLineCheckBox.isChecked

        preferencesEditor.putString("${MySharedPreferencesKeys.FORWARD_ACTION}", WidgetsValues.forwardAction)
        preferencesEditor.putString("${MySharedPreferencesKeys.BACK_ACTION}", WidgetsValues.backAction)
        preferencesEditor.putString("${MySharedPreferencesKeys.LEFT_ACTION}", WidgetsValues.leftAction)
        preferencesEditor.putString("${MySharedPreferencesKeys.RIGHT_ACTION}", WidgetsValues.rightAction)
        preferencesEditor.putBoolean("${MySharedPreferencesKeys.SEND_PWM}", WidgetsValues.sendPWM)
        preferencesEditor.putBoolean("${MySharedPreferencesKeys.APPEND_NEW_LINE}", WidgetsValues.appendNewLine)

        preferencesEditor.apply()

        mApplySettingsCallback.applyVehicleWidgetSettings()
    }

    override fun onDestroy() {
        super.onDestroy()
        getValuesFromFieldsAndSave()

    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            mApplySettingsCallback = context as ApplyVehicleWidgetsSettings //Main activity as context
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    private object WidgetsValues {
        var forwardAction: String = ""
        var backAction: String = ""
        var leftAction: String = ""
        var rightAction: String = ""
        var sendPWM: Boolean = true
        var appendNewLine: Boolean = true
    }
}