import android.app.ActionBar
import android.app.Activity
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.example.sebastian.brulinski.arduinobluetooth.R

fun showConnectingToDeviceAlert(activity: Activity, title: String?, message: String?, deviceName: String,
                                    layout: Int): AlertDialog {

    val mLayout = activity.layoutInflater.inflate(layout, null)
    mLayout.findViewById<TextView>(R.id.targetDeviceName).text = deviceName

    val builder = AlertDialog.Builder(activity)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setCancelable(true)
    builder.setView(mLayout)

    val dialog = builder.create()
    dialog.show()

    return dialog
}

fun <T> showChangeButtonConfigDialog(activity: Activity, title: String?, message: String?, cancelable: Boolean, posButton: String, negButton: String,
                                     press: String, release: String, seekId: String?, hasNewLine: Boolean, isSeekBar: Boolean,
                                     clickedPos: (actionPress: String, actionRelease: String, appendNewLine: Boolean, seekBarId: String?) -> T,
                                     clickedNeg: () -> T) {

    val mLayout = activity.layoutInflater.inflate(R.layout.change_button_config_dialog_layout, null)

    val builder = AlertDialog.Builder(activity)
    builder.setTitle(null)
    builder.setMessage(null)
    builder.setCancelable(cancelable)
    builder.setView(mLayout)

    val pressAction = mLayout.findViewById<EditText>(R.id.pressActionEditText)
    val releaseAction = mLayout.findViewById<EditText>(R.id.releaseActionEditText)
    val appendNewLine = mLayout.findViewById<CheckBox>(R.id.appendNewLineCheckBox)
    val seekBarId = mLayout.findViewById<EditText>(R.id.seekbarIdEditText)

    val pressInputLayout = mLayout.findViewById<TextInputLayout>(R.id.pressActionInputLayout)
    val releaseInputLayout = mLayout.findViewById<TextInputLayout>(R.id.releaseActionInputLayout)
    val seekBarIdInputLayout = mLayout.findViewById<TextInputLayout>(R.id.seekbarIdInputLayout)

    if (isSeekBar) {
        pressInputLayout.hint = activity.getString(R.string.maximum_value)
        releaseInputLayout.hint = activity.getString(R.string.minimum_value)
        pressAction.inputType = InputType.TYPE_CLASS_NUMBER
        releaseAction.inputType = InputType.TYPE_CLASS_NUMBER
        seekBarIdInputLayout.visibility = View.VISIBLE
        seekBarId.setText(seekId)
    }

    pressAction.setText(press)
    releaseAction.setText(release)
    appendNewLine.isChecked = hasNewLine

    builder.setPositiveButton(posButton, { _, _ ->
        clickedPos("${pressAction.text}", "${releaseAction.text}", appendNewLine.isChecked, "${seekBarId.text}")
    })

    builder.setNegativeButton(negButton, { _, _ ->
        clickedNeg()
    })

    val alertDialog = builder.create()
    alertDialog.window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)

    alertDialog.show()
}

fun <T> showLoginDialog(activity: Activity, buttonClick: (email: String, password: String, dialog: AlertDialog) -> T) {

    val layout = activity.layoutInflater.inflate(R.layout.login_fragment, null)

    val email = layout.findViewById<EditText>(R.id.emailEditText)
    val password = layout.findViewById<EditText>(R.id.passwordEditText)
    val loginOrRegister = layout.findViewById<Button>(R.id.loginOrRegisterButton)
    val authProgressBar = layout.findViewById<ProgressBar>(R.id.authProgressBar)
    authProgressBar.visibility = View.GONE

    val dialog = AlertDialog.Builder(activity)
            .setView(layout)
            .create()

    dialog.window.setLayout(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT)

    dialog.show()

    loginOrRegister.setOnClickListener {
        authProgressBar.visibility = View.VISIBLE
        buttonClick("${email.text}", "${password.text}", dialog)
    }
}