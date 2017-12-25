import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import com.example.sebastian.brulinski.arduinobluetooth.R

fun <T> showAlert(context: Context, title: String?, message: String?, cancelable: Boolean,
                  posButton: String, negButton: String, layout: View?, clickedPos: () -> T, clickedNeg: () -> T) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setCancelable(cancelable)
    builder.setView(layout)

    builder.setPositiveButton(posButton, DialogInterface.OnClickListener { _, _ ->
        clickedPos()
    })

    builder.setNegativeButton(negButton, DialogInterface.OnClickListener { _, _ ->
        clickedNeg()
    })

    builder.create().show()
}

fun <T> showChangeButtonConfigDialog(activity: Activity, title: String?, message: String?, cancelable: Boolean, posButton: String, negButton: String,
                                     press: String, release: String, hasNewLine: Boolean, isSeekBar: Boolean,
                                     clickedPos: (actionPress: String, actionRelease: String, appendNewLine: Boolean) -> T, clickedNeg: () -> T) {

    val mLayout = activity.layoutInflater.inflate(R.layout.change_button_config_dialog_layout, null)

    val builder = AlertDialog.Builder(activity)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setCancelable(cancelable)
    builder.setView(mLayout)

    val pressAction = mLayout.findViewById<EditText>(R.id.press_action_edit_text)
    val releaseAction = mLayout.findViewById<EditText>(R.id.release_action_edit_text)
    val appendNewLine = mLayout.findViewById<CheckBox>(R.id.append_new_line_check_box)

    val pressInputLayout = mLayout.findViewById<TextInputLayout>(R.id.press_in_layout)
    val releaseInputLayout = mLayout.findViewById<TextInputLayout>(R.id.release_in_layout)

    if(isSeekBar){
        pressInputLayout.hint = activity.getString(R.string.maximum_value)
        releaseInputLayout.hint = activity.getString(R.string.minimum_value)
        pressAction.inputType = InputType.TYPE_CLASS_NUMBER
        releaseAction.inputType = InputType.TYPE_CLASS_NUMBER
    }

    pressAction.setText(press)
    releaseAction.setText(release)
    appendNewLine.isChecked = hasNewLine

    builder.setPositiveButton(posButton, { _, _ ->
        clickedPos("${pressAction.text}", "${releaseAction.text}", appendNewLine.isChecked)
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

    val email = layout.findViewById<EditText>(R.id.email_edit_text)
    val password = layout.findViewById<EditText>(R.id.password_edit_text)
    val loginOrRegister = layout.findViewById<Button>(R.id.login_register_button)
    val authProgressBar = layout.findViewById<ProgressBar>(R.id.auth_progress_bar)
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