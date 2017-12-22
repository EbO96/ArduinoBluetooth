import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ViewAnimator
import com.example.sebastian.brulinski.arduinobluetooth.R

fun <T> showAlert(context: Context, title: String?, message: String?, cancelable: Boolean,
                  posButton: String, negButton: String, clickedPos: () -> T, clickedNeg: () -> T) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setCancelable(cancelable)

    builder.setPositiveButton(posButton, DialogInterface.OnClickListener { _, _ ->
        clickedPos()
    })

    builder.setNegativeButton(negButton, DialogInterface.OnClickListener { _, _ ->
        clickedNeg()
    })

    builder.create().show()
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