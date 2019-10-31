package fr.gouv.tchap.activity

import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import butterknife.BindView
import butterknife.OnClick
import com.google.android.material.textfield.TextInputEditText
import im.vector.Matrix
import im.vector.R
import im.vector.activity.CommonActivityUtils
import im.vector.activity.VectorAppCompatActivity
import im.vector.ui.themes.ActivityOtherThemes
import org.matrix.androidsdk.MXSession
import org.matrix.androidsdk.core.callback.SimpleApiCallback

class TchapUnsupportedAndroidVersionActivity : VectorAppCompatActivity() {

    /* ==========================================================================================
     * UI
     * ========================================================================================== */

    @BindView(R.id.tv_message)
    lateinit var message: TextView

    @BindView(R.id.sign_out_button)
    lateinit var signOutButton: Button

    @BindView(R.id.export_button)
    lateinit var exportButton: Button

    /* ==========================================================================================
     * DATA
     * ========================================================================================== */

    private var session: MXSession? = null

    /* ==========================================================================================
     * Life cycle
     * ========================================================================================== */

    override fun getOtherThemes() = ActivityOtherThemes.NoActionBar

    override fun getLayoutRes() = R.layout.activity_unsupported_android_version

    override fun initUiAndData() {
        super.initUiAndData()

        waitingView = findViewById(R.id.waiting_view)

        // Get the session
        session = Matrix.getInstance(this).defaultSession

        if (session == null) {
            message.text = getString(R.string.tchap_unsupported_android_version_default_msg)
            signOutButton.isVisible = false
            exportButton.isVisible = false
        }
    }

    /* ==========================================================================================
     * UI Event
     * ========================================================================================== */

    @OnClick(R.id.sign_out_button)
    internal fun signOut() {
        showWaitingView()
        CommonActivityUtils.logout(this)
    }

    @OnClick(R.id.export_button)
    internal fun exportKeysAndSignOut() {
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_export_e2e_keys, null)
        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.encryption_export_room_keys)
                .setView(dialogLayout)

        val passPhrase1EditText = dialogLayout.findViewById<TextInputEditText>(R.id.dialog_e2e_keys_passphrase_edit_text)
        val passPhrase2EditText = dialogLayout.findViewById<TextInputEditText>(R.id.dialog_e2e_keys_confirm_passphrase_edit_text)
        val exportButton = dialogLayout.findViewById<Button>(R.id.dialog_e2e_keys_export_button)
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                exportButton.isEnabled = !TextUtils.isEmpty(passPhrase1EditText.text) && TextUtils.equals(passPhrase1EditText.text, passPhrase2EditText.text)
            }

            override fun afterTextChanged(s: Editable) {

            }
        }

        passPhrase1EditText.addTextChangedListener(textWatcher)
        passPhrase2EditText.addTextChangedListener(textWatcher)

        exportButton.isEnabled = false

        val exportDialog = builder.show()

        exportButton.setOnClickListener {
            showWaitingView()

            CommonActivityUtils.exportKeys(session, passPhrase1EditText.text.toString(), object : SimpleApiCallback<String>(this) {

                override fun onSuccess(filename: String) {
                    hideWaitingView()

                    AlertDialog.Builder(this@TchapUnsupportedAndroidVersionActivity)
                            .setMessage(getString(R.string.encryption_export_saved_as, filename))
                            .setPositiveButton(R.string.action_sign_out) { dialog, id ->
                                showWaitingView()
                                CommonActivityUtils.logout(this@TchapUnsupportedAndroidVersionActivity)
                            }
                            .show()
                }
            })

            exportDialog.dismiss()
        }
    }
}