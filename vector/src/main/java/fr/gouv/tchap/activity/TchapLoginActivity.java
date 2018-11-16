/*
 * Copyright 2018 New Vector Ltd
 * Copyright 2018 DINSIC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.gouv.tchap.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.design.widget.TextInputEditText;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;
import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.callback.SimpleApiCallback;
import org.matrix.androidsdk.rest.client.LoginRestClient;
import org.matrix.androidsdk.rest.client.ProfileRestClient;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.login.Credentials;
import org.matrix.androidsdk.rest.model.login.LoginFlow;
import org.matrix.androidsdk.rest.model.login.RegistrationFlowResponse;
import org.matrix.androidsdk.rest.model.pid.ThreePid;
import org.matrix.androidsdk.ssl.CertUtil;
import org.matrix.androidsdk.ssl.Fingerprint;
import org.matrix.androidsdk.ssl.UnrecognizedCertificateException;
import org.matrix.androidsdk.util.JsonUtils;
import org.matrix.androidsdk.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.OnClick;
import fr.gouv.tchap.sdk.rest.client.TchapRestClient;
import fr.gouv.tchap.sdk.rest.model.Platform;
import fr.gouv.tchap.util.HomeServerConnectionConfigFactoryKt;
import im.vector.LoginHandler;
import im.vector.Matrix;
import im.vector.R;
import im.vector.RegistrationManager;
import im.vector.UnrecognizedCertHandler;
import im.vector.activity.AccountCreationActivity;
import im.vector.activity.AccountCreationCaptchaActivity;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.FallbackLoginActivity;
import im.vector.activity.MXCActionBarActivity;
import im.vector.activity.SplashActivity;
import im.vector.features.hhs.ResourceLimitDialogHelper;
import im.vector.gcm.GCMHelper;
import im.vector.receiver.VectorRegistrationReceiver;
import im.vector.receiver.VectorUniversalLinkReceiver;
import im.vector.services.EventStreamService;

/**
 * Displays the login screen.
 */
public class TchapLoginActivity extends MXCActionBarActivity implements RegistrationManager.RegistrationListener, RegistrationManager.UsernameValidityListener {
    private static final String LOG_TAG = TchapLoginActivity.class.getSimpleName();

    private static final int ACCOUNT_CREATION_ACTIVITY_REQUEST_CODE = 314;
    private static final int FALLBACK_LOGIN_ACTIVITY_REQUEST_CODE = 315;
    private static final int CAPTCHA_CREATION_ACTIVITY_REQUEST_CODE = 316;

    private final static int REGISTER_POLLING_PERIOD = 10 * 1000;

    private static final int REQUEST_REGISTRATION_COUNTRY = 1245;
    private static final int REQUEST_LOGIN_COUNTRY = 5678;

    // activity modes
    // either the user logs in
    // or creates a new account
    private static final int MODE_UNKNOWN = 0;
    private static final int MODE_LOGIN = 1;
    private static final int MODE_ACCOUNT_CREATION = 2;
    private static final int MODE_ACCOUNT_CREATION_WAIT_FOR_EMAIL = 21;
    private static final int MODE_FORGOT_PASSWORD = 3;
    private static final int MODE_FORGOT_PASSWORD_WAITING_VALIDATION = 4;
    private static final int MODE_FORGOT_PASSWORD_WAITING_VALIDATION_2 = 7;
    //private static final int MODE_ACCOUNT_CREATION_THREE_PID = 5; Tchap: this mode is not used for the moment.
    private static final int MODE_START = 6;

    // saved parameters index

    // login
    private static final String SAVED_LOGIN_EMAIL_ADDRESS = "SAVED_LOGIN_EMAIL_ADDRESS";
    private static final String SAVED_LOGIN_PASSWORD_ADDRESS = "SAVED_LOGIN_PASSWORD_ADDRESS";

    // creation
    private static final String SAVED_CREATION_EMAIL_NAME = "SAVED_CREATION_EMAIL_NAME";
    private static final String SAVED_CREATION_PASSWORD1 = "SAVED_CREATION_PASSWORD1";
    private static final String SAVED_CREATION_PASSWORD2 = "SAVED_CREATION_PASSWORD2";
    private static final String SAVED_CREATION_REGISTRATION_RESPONSE = "SAVED_CREATION_REGISTRATION_RESPONSE";
    private static final String SAVED_CREATION_EMAIL_THREEPID = "SAVED_CREATION_EMAIL_THREEPID";
    private ThreePid mPendingEmailValidation;

    // forgot password
    private static final String SAVED_FORGOT_EMAIL_ADDRESS = "SAVED_FORGOT_EMAIL_ADDRESS";
    private static final String SAVED_FORGOT_PASSWORD1 = "SAVED_FORGOT_PASSWORD1";
    private static final String SAVED_FORGOT_PASSWORD2 = "SAVED_FORGOT_PASSWORD2";

    // mode
    private static final String SAVED_MODE = "SAVED_MODE";

    // servers part
    private static final String SAVED_TCHAP_PLATFORM = "SAVED_TCHAP_PLATFORM";
    private static final String SAVED_CONFIG_EMAIL = "SAVED_CONFIG_EMAIL";

    // activity mode
    private int mMode = MODE_START;

    /* ==========================================================================================
     * UI
     * ========================================================================================== */

    @BindView(R.id.fragment_tchap_first_welcome)
    View screenWelcome;

    @BindView(R.id.fragment_tchap_first_login)
    View screenLogin;

    @BindView(R.id.fragment_tchap_first_register)
    View screenRegister;

    @BindView(R.id.fragment_tchap_first_forgotten_password)
    View screenForgottenPassword;

    @BindView(R.id.fragment_tchap_first_message_button)
    View screenMessageButton;

    @BindView(R.id.fragment_tchap_first_register_wait_for_email)
    View screenRegisterWaitForEmail;

    @BindView(R.id.fragment_tchap_register_wait_for_email_email)
    TextView screenRegisterWaitForEmailEmailTextView;

    @BindView(R.id.fragment_tchap_first_message_button_notice)
    TextView messageNotice;

    @BindView(R.id.fragment_tchap_first_message_button_submit)
    Button messageButton;

    // the login account name
    private EditText mLoginEmailTextView;

    // the login password
    private EditText mLoginPasswordTextView;

    // the creation user name
    private EditText mCreationEmailAddressTextView;

    // the password 1 name
    private EditText mCreationPassword1TextView;

    // the password 2 name
    private EditText mCreationPassword2TextView;

    // forgot my password
    private TextView mPasswordForgottenTxtView;

    // the forgot password email text view
    @BindView(R.id.fragment_tchap_first_forget_password_email)
    TextView mForgotEmailTextView;

    // the password 1 name
    @BindView(R.id.fragment_tchap_first_forget_password_new_password)
    EditText mForgotPassword1TextView;

    // the password 2 name
    @BindView(R.id.fragment_tchap_first_forget_password_new_password_confirm)
    EditText mForgotPassword2TextView;

    // used to display a UI mask on the screen
    private RelativeLayout mLoginMaskView;

    // a text displayed while there is progress
    private TextView mProgressTextView;

    // the layout (there is a layout for each mode)
    private View mMainLayout;

    // HS / identity URL layouts
    //private View mHomeServerUrlsLayout;
    //private CheckBox mUseCustomHomeServersCheckbox;

    // the pending universal link uri (if any)
    private Parcelable mUniversalLinkUri;

    // Account creation - Three pid
    private TextView mThreePidInstructions;
    private EditText mEmailAddress;
    //private View mPhoneNumberLayout;
    //private EditText mPhoneNumber;
    //private Button mSubmitThreePidButton;
    //private Button mSkipThreePidButton;

    // allowed registration response
    private RegistrationFlowResponse mRegistrationResponse;

    // login handler
    private final LoginHandler mLoginHandler = new LoginHandler();

    // next link parameters
    private HashMap<String, String> mEmailValidationExtraParams;

    // the next link parameters were not managed
    private boolean mIsMailValidationPending;

    // use to reset the password when the user click on the email validation
    private HashMap<String, String> mForgotPid = null;

    // network state notification
    private final BroadcastReceiver mNetworkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                if ((networkInfo != null) && networkInfo.isConnected()) {
                    // refresh only once
                    if (mIsWaitingNetworkConnection) {
                        refreshDisplay();
                    } else {
                        removeNetworkStateNotificationListener();
                    }
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "## BroadcastReceiver onReceive failed " + e.getMessage());
            }
        }
    };

    private ResourceLimitDialogHelper mResourceLimitDialogHelper;

    private boolean mIsWaitingNetworkConnection = false;

    /**
     * Tell whether the password has been reseted with success.
     * Used to return on login screen on submit button pressed.
     */
    private boolean mIsPasswordResetted;

    // there is a polling thread to monitor when the email has been validated.
    private Runnable mRegisterPollingRunnable;
    private Handler mHandler;

    //private PhoneNumberHandler mLoginPhoneNumberHandler;
    //private PhoneNumberHandler mRegistrationPhoneNumberHandler;

    private Dialog mCurrentDialog;

    // save the config because trust a certificate is asynchronous.
    private HomeServerConnectionConfig mServerConfig;

    @Override
    protected void onDestroy() {
        /*if (mLoginPhoneNumberHandler != null) {
            mLoginPhoneNumberHandler.release();
        }
        if (mRegistrationPhoneNumberHandler != null) {
            mRegistrationPhoneNumberHandler.release();
        }*/
        if (mCurrentDialog != null) {
            mCurrentDialog.dismiss();
            mCurrentDialog = null;
        }

        cancelEmailPolling();
        RegistrationManager.getInstance().resetSingleton();
        super.onDestroy();
        Log.i(LOG_TAG, "## onDestroy(): IN");
        // ignore any server response when the acitity is destroyed
        mMode = MODE_UNKNOWN;
        mEmailValidationExtraParams = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeNetworkStateNotificationListener();
    }

    /**
     * Used in the mail validation flow.
     * This method is called when the TchapLoginActivity is set to foreground due
     * to a {@link #startActivity(Intent)} where the flags Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_SINGLE_TOP}
     * are set (see: {@link VectorRegistrationReceiver}).
     *
     * @param aIntent new intent
     */
    @Override
    protected void onNewIntent(Intent aIntent) {
        super.onNewIntent(aIntent);
        Log.d(LOG_TAG, "## onNewIntent(): IN ");

        Bundle receivedBundle;

        if (null == aIntent) {
            Log.d(LOG_TAG, "## onNewIntent(): Unexpected value - aIntent=null ");
        } else if (null == (receivedBundle = aIntent.getExtras())) {
            Log.d(LOG_TAG, "## onNewIntent(): Unexpected value - extras are missing");
        } else if (receivedBundle.containsKey(VectorRegistrationReceiver.EXTRA_EMAIL_VALIDATION_PARAMS)) {
            Log.d(LOG_TAG, "## onNewIntent() Login activity started by email verification for registration");

            if (processEmailValidationExtras(receivedBundle)) {
                checkIfMailValidationPending();
            }
        }
    }

    @Override
    public int getLayoutRes() {
        return R.layout.activity_tchap_login;
    }

    @Override
    public void initUiAndData() {
        if (null == getIntent()) {
            Log.d(LOG_TAG, "## onCreate(): IN with no intent");
        } else {
            Log.d(LOG_TAG, "## onCreate(): IN with flags " + Integer.toHexString(getIntent().getFlags()));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(false);
        }

        // warn that the application has started.
        CommonActivityUtils.onApplicationStarted(this);

        Intent intent = getIntent();

        // already registered
        if (hasCredentials()) {
            if ((null != intent) && (intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) == 0) {
                Log.d(LOG_TAG, "## onCreate(): goToSplash because the credentials are already provided.");
                goToSplash();
            } else {
                // detect if the application has already been started
                if (null == EventStreamService.getInstance()) {
                    Log.d(LOG_TAG, "## onCreate(): goToSplash with credentials but there is no event stream service.");
                    goToSplash();
                } else {
                    Log.d(LOG_TAG, "## onCreate(): close the login screen because it is a temporary task");
                }
            }

            finish();
            return;
        }

        configureToolbar();

        // bind UI widgets
        mLoginMaskView = findViewById(R.id.flow_ui_mask_login);

        // login
        mLoginEmailTextView = findViewById(R.id.tchap_first_login_email);
        //EditText loginPhoneNumber = findViewById(R.id.login_phone_number_value);
        //EditText loginPhoneNumberCountryCode = findViewById(R.id.login_phone_number_country);
        //loginPhoneNumberCountryCode.setCompoundDrawablesWithIntrinsicBounds(null, null, CommonActivityUtils.tintDrawable(this, ContextCompat.getDrawable(this, R.drawable.ic_material_expand_more_black), R.attr.settings_icon_tint_color), null);
        mLoginPasswordTextView = findViewById(R.id.tchap_first_login_password);

        // Handle the keyboard action done
        mLoginPasswordTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onClick();
                    onLoginClick();
                    handled = true;
                }
                return handled;
            }
        });

        // account creation
        mCreationEmailAddressTextView = findViewById(R.id.tchap_first_register_email);
        mCreationPassword1TextView = findViewById(R.id.tchap_first_register_password);
        mCreationPassword2TextView = findViewById(R.id.tchap_first_register_password_confirm);

        // Handle the keyboard action done
        mCreationPassword2TextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onClick();
                    onRegisterClick();
                    handled = true;
                }
                return handled;
            }
        });

        // account creation - three pid
        mThreePidInstructions = findViewById(R.id.instructions);
        mEmailAddress = findViewById(R.id.registration_email);
        //mPhoneNumberLayout = findViewById(R.id.registration_phone_number);
        //mPhoneNumber = findViewById(R.id.registration_phone_number_value);
        //EditText phoneNumberCountryCode = findViewById(R.id.registration_phone_number_country);
        //phoneNumberCountryCode.setCompoundDrawablesWithIntrinsicBounds(null, null, CommonActivityUtils.tintDrawable(this, ContextCompat.getDrawable(this, R.drawable.ic_material_expand_more_black), R.attr.settings_icon_tint_color), null);
        //mSubmitThreePidButton = findViewById(R.id.button_submit);
        //mSkipThreePidButton = findViewById(R.id.button_skip);

        // forgot password
        mPasswordForgottenTxtView = findViewById(R.id.tchap_first_login_password_forgotten);

        mProgressTextView = findViewById(R.id.flow_progress_message_textview);

        mMainLayout = findViewById(R.id.main_input_layout);

        // "forgot password?" handler
        mPasswordForgottenTxtView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMode = MODE_FORGOT_PASSWORD;
                refreshDisplay();
            }
        });

        if (isFirstCreation()) {
            mResourceLimitDialogHelper = new ResourceLimitDialogHelper(this, null);
        } else {
            final Bundle savedInstanceState = getSavedInstanceState();
            mResourceLimitDialogHelper = new ResourceLimitDialogHelper(this, savedInstanceState);
            restoreSavedData(savedInstanceState);
        }
        addToRestorables(mResourceLimitDialogHelper);

        refreshDisplay();

        // reset the badge counter
        CommonActivityUtils.updateBadgeCount(this, 0);

        // set the handler used by the register to poll the server response
        mHandler = new Handler(getMainLooper());

        // Check whether the application has been resumed from an universal link
        Bundle receivedBundle = (null != intent) ? getIntent().getExtras() : null;
        if (null != receivedBundle) {
            if (receivedBundle.containsKey(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI)) {
                mUniversalLinkUri = receivedBundle.getParcelable(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI);
                Log.d(LOG_TAG, "## onCreate() Login activity started by universal link");
            } else if (receivedBundle.containsKey(VectorRegistrationReceiver.EXTRA_EMAIL_VALIDATION_PARAMS)) {
                Log.d(LOG_TAG, "## onCreate() Login activity started by email verification for registration");
                if (processEmailValidationExtras(receivedBundle)) {
                    // Reset the pending email validation if any.
                    mPendingEmailValidation = null;

                    // Finalize the email verification.
                    checkIfMailValidationPending();
                }
            }
        }

        // Check whether an email validation was pending when the instance was saved.
        if (null != mPendingEmailValidation) {
            Log.d(LOG_TAG, "## onCreate() An email validation was pending");

            // Sanity check
            if (null != mRegistrationResponse && null != mTchapPlatform && null != mCurrentEmail) {
                // retrieve the name and pwd from store data (we consider here that these inputs have been already checked)
                String password = getSavedInstanceState().getString(SAVED_CREATION_PASSWORD1);

                Log.d(LOG_TAG, "## onCreate() Resume email validation");
                // Resume the email validation polling
                enableLoadingScreen(true);
                RegistrationManager.getInstance().setSupportedRegistrationFlows(mRegistrationResponse);
                RegistrationManager.getInstance().setHsConfig(getHsConfig());
                RegistrationManager.getInstance().setAccountData(null, password);
                RegistrationManager.getInstance().addEmailThreePid(mPendingEmailValidation);
                RegistrationManager.getInstance().attemptRegistration(this, this);
                onWaitingEmailValidation();
            }
        }
    }

    /* ==========================================================================================
     * Menu
     * ========================================================================================== */

    @Override
    public int getMenuRes() {
        switch (mMode) {
            case MODE_ACCOUNT_CREATION:
            case MODE_LOGIN:
                return R.menu.tchap_menu_next;
        }

        return -1;
    }

    @Override
    public boolean onOptionsItemSelected(@Nullable MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_next:
                switch (mMode) {
                    case MODE_ACCOUNT_CREATION:
                        onClick();
                        onRegisterClick();
                        return true;
                    case MODE_LOGIN:
                        onClick();
                        onLoginClick();
                        return true;
                }
            case android.R.id.home:
                switch (mMode) {
                    case MODE_ACCOUNT_CREATION:
                    case MODE_LOGIN:
                        fallbackToStartMode();
                        return true;
                    case MODE_ACCOUNT_CREATION_WAIT_FOR_EMAIL:
                        // Go back to register screen
                        cancelEmailPolling();
                        fallbackToRegistrationMode();
                        return true;
                    case MODE_FORGOT_PASSWORD:
                        fallbackToLoginMode();
                        return true;
                    case MODE_FORGOT_PASSWORD_WAITING_VALIDATION:
                        mForgotPid = null;
                        mMode = MODE_FORGOT_PASSWORD;
                        refreshDisplay();
                        return true;
                    case MODE_FORGOT_PASSWORD_WAITING_VALIDATION_2:
                        // switch back directly to login screen
                        fallbackToLoginMode();
                        return true;
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        switch (mMode) {
            case MODE_ACCOUNT_CREATION:
            case MODE_LOGIN:
                Log.d(LOG_TAG, "## fallback to initial screen");
                fallbackToStartMode();
                break;
            case MODE_ACCOUNT_CREATION_WAIT_FOR_EMAIL:
                // Go back to register screen
                cancelEmailPolling();
                fallbackToRegistrationMode();
                break;
            case MODE_FORGOT_PASSWORD:
                fallbackToLoginMode();
                break;
            case MODE_FORGOT_PASSWORD_WAITING_VALIDATION:
                mForgotPid = null;
                mMode = MODE_FORGOT_PASSWORD;
                refreshDisplay();
                break;
            case MODE_FORGOT_PASSWORD_WAITING_VALIDATION_2:
                // switch back directly to login screen
                fallbackToLoginMode();
                break;
            /*case MODE_ACCOUNT_CREATION_THREE_PID:
                Log.d(LOG_TAG, "## cancel the three pid mode");
                cancelEmailPolling();
                RegistrationManager.getInstance().clearThreePid();
                mEmailAddress.setText("");
                //mRegistrationPhoneNumberHandler.reset();
                fallbackToRegistrationMode();*/
            default:
                super.onBackPressed();
        }
    }

    /**
     * Add a listener to be notified when the device gets connected to a network.
     * This method is mainly used to refresh the login UI upon the network is back.
     * See {@link #removeNetworkStateNotificationListener()}
     */
    private void addNetworkStateNotificationListener() {
        if (null != Matrix.getInstance(getApplicationContext()) && !mIsWaitingNetworkConnection) {
            try {
                registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                mIsWaitingNetworkConnection = true;
            } catch (Exception e) {
                Log.e(LOG_TAG, "## addNetworkStateNotificationListener : " + e.getMessage());
            }
        }
    }

    /**
     * Remove the network listener set in {@link #addNetworkStateNotificationListener()}.
     */
    private void removeNetworkStateNotificationListener() {
        if (null != Matrix.getInstance(getApplicationContext()) && mIsWaitingNetworkConnection) {
            try {
                unregisterReceiver(mNetworkReceiver);
                mIsWaitingNetworkConnection = false;
            } catch (Exception e) {
                Log.e(LOG_TAG, "## removeNetworkStateNotificationListener : " + e.getMessage());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensure we have the last version of GooglePlay services (not for F-Droid version then),
        // or TLS 1.2 could not work, especially on Android < 5.0
        GCMHelper.checkLastVersion(this);
    }

    /**
     * Cancel the current mode to switch to the login one.
     * It should restore the login UI
     */
    private void fallbackToLoginMode() {
        onClick();

        // display the main layout
        mMainLayout.setVisibility(View.VISIBLE);

        // cancel the registration flow
        cancelEmailPolling();
        mEmailValidationExtraParams = null;
        mRegistrationResponse = null;
        showMainLayout();
        enableLoadingScreen(false);

        // Reset the forgot password text views to prevent user from using an unknown password.
        mForgotPassword1TextView.setText(null);
        mForgotPassword2TextView.setText(null);
        mForgotPid = null;

        mMode = MODE_LOGIN;
        refreshDisplay();
    }

    /**
     * Cancel the current mode to switch to the start one.
     * It should restore the start UI
     */
    private void fallbackToStartMode() {
        onClick();

        // display the main layout
        mMainLayout.setVisibility(View.VISIBLE);

        // cancel the registration flow
        cancelEmailPolling();
        mEmailValidationExtraParams = null;
        mRegistrationResponse = null;
        showMainLayout();
        enableLoadingScreen(false);

        mMode = MODE_START;
        refreshDisplay();
    }

    /**
     * Cancel the current mode to switch to the registration one.
     * It should restore the registration UI
     */
    private void fallbackToRegistrationMode() {
        // display the main layout
        mMainLayout.setVisibility(View.VISIBLE);

        showMainLayout();
        enableLoadingScreen(false);

        mMode = MODE_ACCOUNT_CREATION;
        refreshDisplay();
    }

    /**
     * @return true if some credentials have been saved.
     */
    private boolean hasCredentials() {
        try {
            MXSession session = Matrix.getInstance(this).getDefaultSession();
            return ((null != session) && session.isAlive());
        } catch (Exception e) {
            Log.e(LOG_TAG, "## Exception: " + e.getMessage());
        }

        Log.e(LOG_TAG, "## hasCredentials() : invalid credentials");

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // getDefaultSession could trigger an exception if the login data are corrupted
                    CommonActivityUtils.logout(TchapLoginActivity.this);
                } catch (Exception e) {
                    Log.w(LOG_TAG, "## Exception: " + e.getMessage());
                }
            }
        });

        return false;
    }

    /**
     * A session has just been created, display the splash screen.
     */
    private void goToSplash() {
        Log.d(LOG_TAG, "## gotoSplash(): Go to splash.");

        Intent intent = new Intent(this, SplashActivity.class);
        if (null != mUniversalLinkUri) {
            intent.putExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI, mUniversalLinkUri);
        }

        startActivity(intent);
    }

    //==============================================================================================================
    // Forgot password management
    //==============================================================================================================

    /**
     * the user forgot his password
     */
    @OnClick(R.id.fragment_tchap_first_forget_password_submit)
    void onForgotPasswordClick() {
        onClick();

        // parameters
        final String email = mForgotEmailTextView.getText().toString().trim();
        final String password = mForgotPassword1TextView.getText().toString().trim();
        final String passwordCheck = mForgotPassword2TextView.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), getString(R.string.auth_reset_password_missing_email), Toast.LENGTH_SHORT).show();
            return;
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), getString(R.string.auth_reset_password_missing_password), Toast.LENGTH_SHORT).show();
            return;
        } else if (password.length() < 6) {
            Toast.makeText(getApplicationContext(), getString(R.string.auth_invalid_password), Toast.LENGTH_SHORT).show();
            return;
        } else if (!TextUtils.equals(password, passwordCheck)) {
            Toast.makeText(getApplicationContext(), getString(R.string.auth_password_dont_match), Toast.LENGTH_SHORT).show();
            return;
        } else if (!TextUtils.isEmpty(email) && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getApplicationContext(), getString(R.string.auth_invalid_email), Toast.LENGTH_SHORT).show();
            return;
        }

        enableLoadingScreen(true);

        Log.d(LOG_TAG, "## onForgotPasswordClick()");
        discoverTchapPlatform(this, email, new ApiCallback<Platform>() {
            private void onError(String errorMessage) {
                Toast.makeText(TchapLoginActivity.this, (null == errorMessage) ? getString(R.string.auth_invalid_email) : errorMessage, Toast.LENGTH_LONG).show();
                enableLoadingScreen(false);
            }

            @Override
            public void onSuccess(Platform platform) {
                Log.d(LOG_TAG, "## onForgotPasswordClick(): discoverTchapPlatform succeeds");

                // Check whether the returned platform is valid
                if (null == platform.hs || platform.hs.isEmpty()) {
                    // The email owner is not able to create a tchap account,
                    Log.e(LOG_TAG, "## onForgotPasswordClick(): invalid platform");
                    onError(getString(R.string.tchap_register_unauthorized_email));
                    return;
                }

                // Store the platform and the corresponding email
                mTchapPlatform = platform;
                mCurrentEmail = email;

                final HomeServerConnectionConfig hsConfig = getHsConfig();
                ProfileRestClient pRest = new ProfileRestClient(hsConfig);

                pRest.forgetPassword(email, new ApiCallback<ThreePid>() {
                    @Override
                    public void onSuccess(ThreePid thirdPid) {
                        if (mMode == MODE_FORGOT_PASSWORD) {
                            Log.d(LOG_TAG, "## onForgotPasswordClick(): requestEmailValidationToken succeeds");

                            enableLoadingScreen(false);

                            // refresh the messages
                            mMode = MODE_FORGOT_PASSWORD_WAITING_VALIDATION;
                            refreshDisplay();

                            mForgotPid = new HashMap<>();
                            mForgotPid.put("client_secret", thirdPid.clientSecret);
                            mForgotPid.put("id_server", hsConfig.getIdentityServerUri().getHost());
                            mForgotPid.put("sid", thirdPid.sid);
                        }
                    }

                    /**
                     * Display a toast to warn that the operation failed
                     * @param errorMessage the error message.
                     */
                    private void onError(final String errorMessage) {
                        Log.e(LOG_TAG, "## onForgotPasswordClick(): requestEmailValidationToken fails with error " + errorMessage);

                        if (mMode == MODE_FORGOT_PASSWORD) {
                            enableLoadingScreen(false);
                            Toast.makeText(TchapLoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onNetworkError(final Exception e) {
                        if (mMode == MODE_FORGOT_PASSWORD) {
                            UnrecognizedCertificateException unrecCertEx = CertUtil.getCertificateException(e);
                            if (unrecCertEx != null) {
                                final Fingerprint fingerprint = unrecCertEx.getFingerprint();

                                UnrecognizedCertHandler.show(hsConfig, fingerprint, false, new UnrecognizedCertHandler.Callback() {
                                    @Override
                                    public void onAccept() {
                                        onForgotPasswordClick();
                                    }

                                    @Override
                                    public void onIgnore() {
                                        onError(e.getLocalizedMessage());
                                    }

                                    @Override
                                    public void onReject() {
                                        onError(e.getLocalizedMessage());
                                    }
                                });
                            } else {
                                onError(e.getLocalizedMessage());
                            }
                        }
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        onError(e.getLocalizedMessage());
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        if (TextUtils.equals(MatrixError.THREEPID_NOT_FOUND, e.errcode)) {
                            onError(getString(R.string.account_email_not_found_error));
                        } else {
                            onError(e.getLocalizedMessage());
                        }
                    }
                });
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(MatrixError matrixError) {
                onError(matrixError.getLocalizedMessage());
            }

            @Override
            public void onUnexpectedError(Exception e) {
                onError(e.getLocalizedMessage());
            }
        });
    }

    /**
     * The user warns the client that the reset password email has been received
     */
    private void onForgotOnEmailValidated(final HomeServerConnectionConfig hsConfig) {
        if (mIsPasswordResetted) {
            Log.d(LOG_TAG, "onForgotOnEmailValidated : go back to login screen");

            mIsPasswordResetted = false;
            fallbackToLoginMode();
        } else {
            ProfileRestClient profileRestClient = new ProfileRestClient(hsConfig);
            enableLoadingScreen(true);

            Log.d(LOG_TAG, "onForgotOnEmailValidated : try to reset the password");

            profileRestClient.resetPassword(mForgotPassword1TextView.getText().toString().trim(), mForgotPid, new ApiCallback<Void>() {
                @Override
                public void onSuccess(Void info) {
                    if (mMode == MODE_FORGOT_PASSWORD_WAITING_VALIDATION) {
                        Log.d(LOG_TAG, "onForgotOnEmailValidated : the password has been updated");

                        enableLoadingScreen(false);

                        // refresh the messages
                        mMode = MODE_FORGOT_PASSWORD_WAITING_VALIDATION_2;

                        mIsPasswordResetted = true;
                        refreshDisplay();
                    }
                }

                /**
                 * Display a toast to warn that the operation failed
                 *
                 * @param errorMessage the error message.
                 */
                private void onError(String errorMessage, boolean cancel) {
                    if (mMode == MODE_FORGOT_PASSWORD_WAITING_VALIDATION) {
                        Log.d(LOG_TAG, "onForgotOnEmailValidated : failed " + errorMessage);

                        // display the dedicated
                        Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                        enableLoadingScreen(false);

                        if (cancel) {
                            fallbackToLoginMode();
                        }
                    }
                }

                @Override
                public void onNetworkError(Exception e) {
                    onError(e.getLocalizedMessage(), false);
                }

                @Override
                public void onMatrixError(MatrixError e) {
                    if (mMode == MODE_FORGOT_PASSWORD_WAITING_VALIDATION) {
                        if (TextUtils.equals(e.errcode, MatrixError.UNAUTHORIZED)) {
                            Log.d(LOG_TAG, "onForgotOnEmailValidated : failed UNAUTHORIZED");

                            onError(getResources().getString(R.string.auth_reset_password_error_unauthorized), false);
                        } else {
                            onError(e.getLocalizedMessage(), true);
                        }
                    }
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    onError(e.getLocalizedMessage(), true);
                }
            });
        }
    }

    //==============================================================================================================
    // registration management
    //==============================================================================================================

    /**
     * Error case Management
     *
     * @param matrixError the matrix error
     */
    private void onFailureDuringAuthRequest(MatrixError matrixError) {
        enableLoadingScreen(false);

        final String errCode = matrixError.errcode;

        if (MatrixError.RESOURCE_LIMIT_EXCEEDED.equals(errCode)) {
            Log.e(LOG_TAG, "## onFailureDuringAuthRequest(): RESOURCE_LIMIT_EXCEEDED");
            mResourceLimitDialogHelper.displayDialog(matrixError);
        } else {
            final String message;

            if (TextUtils.equals(errCode, MatrixError.FORBIDDEN)) {
                message = getString(R.string.login_error_forbidden);
            } else if (TextUtils.equals(errCode, MatrixError.UNKNOWN_TOKEN)) {
                message = getString(R.string.login_error_unknown_token);
            } else if (TextUtils.equals(errCode, MatrixError.BAD_JSON)) {
                message = getString(R.string.login_error_bad_json);
            } else if (TextUtils.equals(errCode, MatrixError.NOT_JSON)) {
                message = getString(R.string.login_error_not_json);
            } else if (TextUtils.equals(errCode, MatrixError.LIMIT_EXCEEDED)) {
                message = getString(R.string.login_error_limit_exceeded);
            } else if (TextUtils.equals(errCode, MatrixError.USER_IN_USE)) {
                message = getString(R.string.login_error_user_in_use);
            } else if (TextUtils.equals(errCode, MatrixError.LOGIN_EMAIL_URL_NOT_YET)) {
                message = getString(R.string.login_error_login_email_not_yet);
            } else {
                message = matrixError.getLocalizedMessage();
            }

            Log.e(LOG_TAG, "## onFailureDuringAuthRequest(): Msg= \"" + message + "\"");
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Parse the given bundle to check if it contains the email verification extra.
     * If yes, it initializes the TchapLoginActivity to start in registration mode to finalize a registration
     * process that is in progress. This is mainly used when the TchapLoginActivity
     * is triggered from the {@link VectorRegistrationReceiver}.
     *
     * @param aRegistrationBundle bundle to be parsed
     * @return true operation succeed, false otherwise
     */
    private boolean processEmailValidationExtras(Bundle aRegistrationBundle) {
        boolean retCode = false;

        Log.d(LOG_TAG, "## processEmailValidationExtras() IN");

        if (null != aRegistrationBundle) {
            mEmailValidationExtraParams = (HashMap<String, String>) aRegistrationBundle.getSerializable(VectorRegistrationReceiver.EXTRA_EMAIL_VALIDATION_PARAMS);

            if (null != mEmailValidationExtraParams) {
                // login was started in email validation mode
                mIsMailValidationPending = true;
                mMode = MODE_ACCOUNT_CREATION;
                Matrix.getInstance(this).clearSessions(this, true, null);
                retCode = true;
            }
        } else {
            Log.e(LOG_TAG, "## processEmailValidationExtras(): Bundle is missing - aRegistrationBundle=null");
        }
        Log.d(LOG_TAG, "## processEmailValidationExtras() OUT - reCode=" + retCode);
        return retCode;
    }


    /**
     * Perform an email validation for a registration flow. One account has been created where
     * a mail was provided. To validate the email ownership a MX submitToken REST api call must be performed.
     *
     * @param aMapParams map containing the parameters
     */
    private void startEmailOwnershipValidation(HashMap<String, String> aMapParams) {
        Log.d(LOG_TAG, "## startEmailOwnershipValidation(): IN aMapParams=" + aMapParams);

        if (null != aMapParams) {
            // display waiting UI..
            enableLoadingScreen(true);

            // display wait screen with no text (same as iOS) for now..
            hideMainLayoutAndToast("");

            // set register mode
            mMode = MODE_ACCOUNT_CREATION;

            String token = aMapParams.get(VectorRegistrationReceiver.KEY_MAIL_VALIDATION_TOKEN);
            String clientSecret = aMapParams.get(VectorRegistrationReceiver.KEY_MAIL_VALIDATION_CLIENT_SECRET);
            String identityServerSessId = aMapParams.get(VectorRegistrationReceiver.KEY_MAIL_VALIDATION_IDENTITY_SERVER_SESSION_ID);
            String sessionId = aMapParams.get(VectorRegistrationReceiver.KEY_MAIL_VALIDATION_SESSION_ID);
            String homeServer = aMapParams.get(VectorRegistrationReceiver.KEY_MAIL_VALIDATION_HOME_SERVER_URL);
            String identityServer = aMapParams.get(VectorRegistrationReceiver.KEY_MAIL_VALIDATION_IDENTITY_SERVER_URL);

            // When the user tries to update his/her password after forgetting it (tap on the dedicated link)
            // The HS / IS urls are not provided in the email link.
            // This link should be only opened by the webclient (known issue server side)
            // Use the current configuration by default (it might not work on some account if the user uses another HS)
            if (null == homeServer) {
                homeServer = getHomeServerUrl();
            }

            if (null == identityServer) {
                identityServer = getIdentityServerUrl();
            }

            // test if the home server urls are valid
            try {
                Uri.parse(homeServer);
                Uri.parse(identityServer);
            } catch (Exception e) {
                Toast.makeText(TchapLoginActivity.this, getString(R.string.login_error_invalid_home_server), Toast.LENGTH_SHORT).show();
                return;
            }

            submitEmailToken(token, clientSecret, identityServerSessId, sessionId, homeServer, identityServer);
        } else {
            Log.d(LOG_TAG, "## startEmailOwnershipValidation(): skipped");
        }
    }

    /**
     * Used to resume the registration process when it is waiting for the mail validation.
     *
     * @param aClientSecret   client secret
     * @param aSid            identity server session ID
     * @param aIdentityServer identity server url
     * @param aSessionId      session ID
     * @param aHomeServer     home server url
     */
    private void submitEmailToken(final String aToken, final String aClientSecret, final String aSid, final String aSessionId, final String aHomeServer, final String aIdentityServer) {
        final HomeServerConnectionConfig homeServerConfig
                = mServerConfig
                = HomeServerConnectionConfigFactoryKt.createHomeServerConnectionConfig(aHomeServer, aIdentityServer);
        RegistrationManager.getInstance().setHsConfig(homeServerConfig);
        Log.d(LOG_TAG, "## submitEmailToken(): IN");

        if (mMode == MODE_ACCOUNT_CREATION) {
            Log.d(LOG_TAG, "## submitEmailToken(): calling submitEmailTokenValidation()..");
            mLoginHandler.submitEmailTokenValidation(getApplicationContext(), homeServerConfig, aToken, aClientSecret, aSid, new ApiCallback<Boolean>() {
                private void errorHandler(String errorMessage) {
                    Log.d(LOG_TAG, "## submitEmailToken(): errorHandler().");
                    enableLoadingScreen(false);
                    showMainLayout();
                    refreshDisplay();
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onSuccess(Boolean isSuccess) {
                    if (isSuccess) {
                        // if aSessionId is null, it means that this request has been triggered by clicking on a "forgot password" link
                        if (null == aSessionId) {
                            Log.d(TchapLoginActivity.LOG_TAG, "## submitEmailToken(): onSuccess() - the password update is in progress");

                            mMode = MODE_FORGOT_PASSWORD_WAITING_VALIDATION;

                            mForgotPid = new HashMap<>();
                            mForgotPid.put("client_secret", aClientSecret);
                            mForgotPid.put("id_server", homeServerConfig.getIdentityServerUri().getHost());
                            mForgotPid.put("sid", aSid);

                            mIsPasswordResetted = false;
                            onForgotOnEmailValidated(homeServerConfig);
                        } else {
                            // the validation of mail ownership succeed, just resume the registration flow
                            // next step: just register
                            Log.d(TchapLoginActivity.LOG_TAG, "## submitEmailToken(): onSuccess() - registerAfterEmailValidations() started");
                            mMode = MODE_ACCOUNT_CREATION;
                            enableLoadingScreen(true);
                            RegistrationManager.getInstance().registerAfterEmailValidation(TchapLoginActivity.this, aClientSecret, aSid, aIdentityServer, aSessionId, TchapLoginActivity.this);
                        }
                    } else {
                        Log.d(TchapLoginActivity.LOG_TAG, "## submitEmailToken(): onSuccess() - failed (success=false)");
                        errorHandler(getString(R.string.login_error_unable_register_mail_ownership));
                    }
                }

                @Override
                public void onNetworkError(Exception e) {
                    Log.d(TchapLoginActivity.LOG_TAG, "## submitEmailToken(): onNetworkError() Msg=" + e.getLocalizedMessage());
                    errorHandler(getString(R.string.login_error_unable_register) + " : " + e.getLocalizedMessage());
                }

                @Override
                public void onMatrixError(MatrixError e) {
                    Log.d(TchapLoginActivity.LOG_TAG, "## submitEmailToken(): onMatrixError() Msg=" + e.getLocalizedMessage());
                    errorHandler(getString(R.string.login_error_unable_register) + " : " + e.getLocalizedMessage());
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    Log.d(TchapLoginActivity.LOG_TAG, "## submitEmailToken(): onUnexpectedError() Msg=" + e.getLocalizedMessage());
                    errorHandler(getString(R.string.login_error_unable_register) + " : " + e.getLocalizedMessage());
                }
            });
        }
    }

    /**
     * Check if the client supports the registration kind.
     *
     * @param registrationFlowResponse the response
     */
    private void onRegistrationFlow(RegistrationFlowResponse registrationFlowResponse) {
        enableLoadingScreen(false);

        mRegistrationResponse = registrationFlowResponse;

        // Check whether all listed flows in this authentication session are supported
        // We suggest using the fallback page (if any), when at least one flow is not supported.
        if (RegistrationManager.getInstance().hasNonSupportedStage()) {
            String hs = getHomeServerUrl();
            boolean validHomeServer = false;

            try {
                Uri hsUri = Uri.parse(hs);
                validHomeServer = "http".equals(hsUri.getScheme()) || "https".equals(hsUri.getScheme());
            } catch (Exception e) {
                Log.e(LOG_TAG, "## Exception: " + e.getMessage());
            }

            if (!validHomeServer) {
                Toast.makeText(TchapLoginActivity.this, getString(R.string.login_error_invalid_home_server), Toast.LENGTH_SHORT).show();
                return;
            }

            fallbackToLoginMode();

            Intent intent = new Intent(TchapLoginActivity.this, AccountCreationActivity.class);
            intent.putExtra(AccountCreationActivity.EXTRA_HOME_SERVER_ID, hs);
            startActivityForResult(intent, ACCOUNT_CREATION_ACTIVITY_REQUEST_CODE);
        }
    }

    /**
     * Start a mail validation if required.
     */
    private void checkIfMailValidationPending() {
        Log.d(LOG_TAG, "## checkIfMailValidationPending(): mIsMailValidationPending=" + mIsMailValidationPending);

        if (mIsMailValidationPending) {
            mIsMailValidationPending = false;

            // remove the pending polling register if any
            cancelEmailPolling();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (null != mEmailValidationExtraParams) {
                        startEmailOwnershipValidation(mEmailValidationExtraParams);
                    }
                }
            });
        } else {
            Log.d(LOG_TAG, "## checkIfMailValidationPending(): pending mail validation not started");
        }
    }

    /**
     * Check the homeserver flows.
     * i.e checks if this registration page is enough to perform a registration.
     * else switch to a fallback page
     */
    private void checkRegistrationFlows(final SimpleApiCallback<Void> callback) {
        Log.d(LOG_TAG, "## checkRegistrationFlows(): IN");
        // should only check registration flows
        if (mMode != MODE_ACCOUNT_CREATION) {
            return;
        }

        if (null == mRegistrationResponse) {
            try {
                final HomeServerConnectionConfig hsConfig = getHsConfig();

                // sanity check
                if (null != hsConfig) {
                    enableLoadingScreen(true);

                    mLoginHandler.getSupportedRegistrationFlows(TchapLoginActivity.this, hsConfig, new SimpleApiCallback<HomeServerConnectionConfig>() {
                        @Override
                        public void onSuccess(HomeServerConnectionConfig homeserverConnectionConfig) {
                            // should never be called
                        }

                        private void onError(String errorMessage) {
                            // should not check login flows
                            if (mMode == MODE_ACCOUNT_CREATION) {
                                showMainLayout();
                                enableLoadingScreen(false);
                                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onNetworkError(Exception e) {
                            addNetworkStateNotificationListener();
                            if (mMode == MODE_ACCOUNT_CREATION) {
                                Log.e(LOG_TAG, "Network Error: " + e.getMessage(), e);
                                onError(getString(R.string.login_error_registration_network_error) + " : " + e.getLocalizedMessage());
                            }
                        }

                        @Override
                        public void onUnexpectedError(Exception e) {
                            if (mMode == MODE_ACCOUNT_CREATION) {
                                onError(getString(R.string.login_error_unable_register) + " : " + e.getLocalizedMessage());
                            }
                        }

                        @Override
                        public void onMatrixError(MatrixError e) {
                            removeNetworkStateNotificationListener();

                            if (mMode == MODE_ACCOUNT_CREATION) {
                                Log.d(LOG_TAG, "## checkRegistrationFlows(): onMatrixError - Resp=" + e.getLocalizedMessage());
                                RegistrationFlowResponse registrationFlowResponse = null;

                                // when a response is not completed the server returns an error message
                                if (null != e.mStatus) {
                                    if (e.mStatus == 401) {
                                        try {
                                            registrationFlowResponse = JsonUtils.toRegistrationFlowResponse(e.mErrorBodyAsString);
                                        } catch (Exception castExcept) {
                                            Log.e(LOG_TAG, "JsonUtils.toRegistrationFlowResponse " + castExcept.getLocalizedMessage());
                                        }
                                    } else if (e.mStatus == 403) {
                                        // not supported by the server
                                        // For Tchap, it depends on the email. Stay in the registration screen
                                        refreshDisplay();
                                    }
                                }

                                if (null != registrationFlowResponse) {
                                    RegistrationManager.getInstance().setSupportedRegistrationFlows(registrationFlowResponse);
                                    onRegistrationFlow(registrationFlowResponse);
                                    callback.onSuccess(null);
                                } else {
                                    onFailureDuringAuthRequest(e);
                                }

                                // start Login due to a pending email validation
                                checkIfMailValidationPending();
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.login_error_invalid_home_server), Toast.LENGTH_SHORT).show();
                enableLoadingScreen(false);
            }
        } else {
            // The registration flows are already known, pursue the current action.
            callback.onSuccess(null);
        }
    }

    /**
     * Hide the main layout and display a toast.
     *
     * @param text the text helper
     */
    private void hideMainLayoutAndToast(String text) {
        mMainLayout.setVisibility(View.GONE);
        mProgressTextView.setVisibility(View.VISIBLE);
        mProgressTextView.setText(text);
    }

    /**
     * Show the main layout.
     */
    private void showMainLayout() {
        mMainLayout.setVisibility(View.VISIBLE);
        mProgressTextView.setVisibility(View.GONE);
    }

    //==============================================================================================================
    // login management
    //==============================================================================================================

    /**
     * Dismiss the keyboard and save the updated values
     */
    private void onClick() {
        if (null != getCurrentFocus()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * The user clicks on the login button
     */
    @OnClick(R.id.fragment_tchap_first_welcome_login_button)
    void onLoginClick() {
        onClick();

        // the user switches to another mode
        if (mMode != MODE_LOGIN) {
            // end any pending registration UI
            showMainLayout();

            mMode = MODE_LOGIN;
            refreshDisplay();
            return;
        }


        final String emailAddress = mLoginEmailTextView.getText().toString().trim();
        final String password = mLoginPasswordTextView.getText().toString().trim();

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            Toast.makeText(this, getString(R.string.auth_invalid_login_param), Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.auth_invalid_login_param), Toast.LENGTH_SHORT).show();
            return;
        }

        enableLoadingScreen(true);

        Log.d(LOG_TAG, "## onLoginClick()");
        discoverTchapPlatform(this, emailAddress, new ApiCallback<Platform>() {
            private void onError(String errorMessage) {
                Toast.makeText(TchapLoginActivity.this, (null == errorMessage) ? getString(R.string.login_error_unable_login) : errorMessage, Toast.LENGTH_LONG).show();
                enableLoadingScreen(false);
            }

            @Override
            public void onSuccess(Platform platform) {
                // Check whether the returned platform is valid
                if (null == platform.hs || platform.hs.isEmpty()) {
                    // The email owner is not able to create a tchap account,
                    Log.e(LOG_TAG, "## onLoginClick(): invalid platform");
                    onError(getString(R.string.login_error_unable_login));
                    return;
                }

                // Store the platform and the corresponding email to detect changes
                mTchapPlatform = platform;
                mCurrentEmail = emailAddress;

                final HomeServerConnectionConfig hsConfig = getHsConfig();

                mLoginHandler.getSupportedLoginFlows(TchapLoginActivity.this, hsConfig, new SimpleApiCallback<List<LoginFlow>>() {
                    @Override
                    public void onSuccess(List<LoginFlow> flows) {
                        // stop listening to network state
                        removeNetworkStateNotificationListener();

                        enableLoadingScreen(false);
                        boolean isSupported = true;

                        // supported only m.login.password by now
                        for (LoginFlow flow : flows) {
                            isSupported &= TextUtils.equals(LoginRestClient.LOGIN_FLOW_TYPE_PASSWORD, flow.type);
                        }

                        // if not supported, switch to the fallback login
                        if (!isSupported) {
                            Intent intent = new Intent(TchapLoginActivity.this, FallbackLoginActivity.class);
                            intent.putExtra(FallbackLoginActivity.EXTRA_HOME_SERVER_ID, hsConfig.getHomeserverUri().toString());
                            startActivityForResult(intent, FALLBACK_LOGIN_ACTIVITY_REQUEST_CODE);
                        } else {
                            login(hsConfig, emailAddress, null, null, password);
                        }
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        Log.e(LOG_TAG, "Network Error: " + e.getMessage(), e);
                        // listen to network state, to resume processing as soon as the network is back
                        addNetworkStateNotificationListener();
                        onError(e.getLocalizedMessage());
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        onError(e.getLocalizedMessage());
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        onFailureDuringAuthRequest(e);
                    }
                });
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(getString(R.string.login_error_unable_login) + " : " + e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(MatrixError matrixError) {
                onError(getString(R.string.login_error_unable_login) + " : " + matrixError.getLocalizedMessage());
            }

            @Override
            public void onUnexpectedError(Exception e) {
                onError(getString(R.string.login_error_unable_login) + " : " + e.getLocalizedMessage());
            }
        });
    }

    /**
     * Make login request with given params
     *
     * @param hsConfig           the HS config
     * @param username           the username
     * @param phoneNumber        the phone number
     * @param phoneNumberCountry the phone number country code
     * @param password           the user password
     */
    private void login(final HomeServerConnectionConfig hsConfig, final String username, final String phoneNumber,
                       final String phoneNumberCountry, final String password) {
        try {
            mLoginHandler.login(this, hsConfig, username, phoneNumber, phoneNumberCountry, password, new SimpleApiCallback<HomeServerConnectionConfig>(this) {
                @Override
                public void onSuccess(HomeServerConnectionConfig c) {
                    enableLoadingScreen(false);
                    goToSplash();
                    TchapLoginActivity.this.finish();
                }

                @Override
                public void onNetworkError(Exception e) {
                    Log.e(LOG_TAG, "## login(): Network Error: " + e.getMessage());
                    enableLoadingScreen(false);
                    Toast.makeText(getApplicationContext(), getString(R.string.login_error_network_error), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    Log.e(LOG_TAG, "## login(): onUnexpectedError" + e.getMessage());
                    enableLoadingScreen(false);
                    String msg = getString(R.string.login_error_unable_login) + " : " + e.getMessage();
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onMatrixError(MatrixError e) {
                    Log.e(LOG_TAG, "## login(): onMatrixError " + e.getLocalizedMessage());
                    enableLoadingScreen(false);
                    onFailureDuringAuthRequest(e);
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.login_error_invalid_home_server), Toast.LENGTH_SHORT).show();
            enableLoadingScreen(false);
        }
    }

    //==============================================================================================================
    // Instance backup
    //==============================================================================================================

    /**
     * Restore the saved instance data.
     *
     * @param savedInstanceState the instance state
     */
    private void restoreSavedData(Bundle savedInstanceState) {

        Log.d(LOG_TAG, "## restoreSavedData(): IN");
        if (null != savedInstanceState) {
            mLoginEmailTextView.setText(savedInstanceState.getString(SAVED_LOGIN_EMAIL_ADDRESS));
            mLoginPasswordTextView.setText(savedInstanceState.getString(SAVED_LOGIN_PASSWORD_ADDRESS));

            mCreationEmailAddressTextView.setText(savedInstanceState.getString(SAVED_CREATION_EMAIL_NAME));
            mCreationPassword1TextView.setText(savedInstanceState.getString(SAVED_CREATION_PASSWORD1));
            mCreationPassword2TextView.setText(savedInstanceState.getString(SAVED_CREATION_PASSWORD2));

            mForgotEmailTextView.setText(savedInstanceState.getString(SAVED_FORGOT_EMAIL_ADDRESS));
            mForgotPassword1TextView.setText(savedInstanceState.getString(SAVED_FORGOT_PASSWORD1));
            mForgotPassword2TextView.setText(savedInstanceState.getString(SAVED_FORGOT_PASSWORD2));

            mRegistrationResponse = (RegistrationFlowResponse) savedInstanceState.getSerializable(SAVED_CREATION_REGISTRATION_RESPONSE);

            mMode = savedInstanceState.getInt(SAVED_MODE, MODE_LOGIN);

            // check if the application has been opened by click on an url
            if (savedInstanceState.containsKey(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI)) {
                mUniversalLinkUri = savedInstanceState.getParcelable(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI);
            }

            mPendingEmailValidation = (ThreePid) savedInstanceState.getSerializable(SAVED_CREATION_EMAIL_THREEPID);
            mTchapPlatform = (Platform) savedInstanceState.getSerializable(SAVED_TCHAP_PLATFORM);
            mCurrentEmail = savedInstanceState.getString(SAVED_CONFIG_EMAIL);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
        Log.d(LOG_TAG, "## onSaveInstanceState(): IN");

        if (!TextUtils.isEmpty(mLoginEmailTextView.getText().toString().trim())) {
            savedInstanceState.putString(SAVED_LOGIN_EMAIL_ADDRESS, mLoginEmailTextView.getText().toString().trim());
        }

        if (!TextUtils.isEmpty(mLoginPasswordTextView.getText().toString().trim())) {
            savedInstanceState.putString(SAVED_LOGIN_PASSWORD_ADDRESS, mLoginPasswordTextView.getText().toString().trim());
        }

        if (!TextUtils.isEmpty(mCreationEmailAddressTextView.getText().toString().trim())) {
            savedInstanceState.putString(SAVED_CREATION_EMAIL_NAME, mCreationEmailAddressTextView.getText().toString().trim());
        }

        if (!TextUtils.isEmpty(mCreationPassword1TextView.getText().toString().trim())) {
            savedInstanceState.putString(SAVED_CREATION_PASSWORD1, mCreationPassword1TextView.getText().toString().trim());
        }

        if (!TextUtils.isEmpty(mCreationPassword2TextView.getText().toString().trim())) {
            savedInstanceState.putString(SAVED_CREATION_PASSWORD2, mCreationPassword2TextView.getText().toString().trim());
        }

        if (!TextUtils.isEmpty(mForgotEmailTextView.getText().toString().trim())) {
            savedInstanceState.putString(SAVED_FORGOT_EMAIL_ADDRESS, mForgotEmailTextView.getText().toString().trim());
        }

        if (!TextUtils.isEmpty(mForgotPassword1TextView.getText().toString().trim())) {
            savedInstanceState.putString(SAVED_FORGOT_PASSWORD1, mForgotPassword1TextView.getText().toString().trim());
        }

        if (!TextUtils.isEmpty(mForgotPassword2TextView.getText().toString().trim())) {
            savedInstanceState.putString(SAVED_FORGOT_PASSWORD2, mForgotPassword2TextView.getText().toString().trim());
        }

        if (null != mTchapPlatform) {
            savedInstanceState.putSerializable(SAVED_TCHAP_PLATFORM, mTchapPlatform);

            if (null != mCurrentEmail) {
                savedInstanceState.putString(SAVED_CONFIG_EMAIL, mCurrentEmail);
            }
        }

        if (null != mRegistrationResponse) {
            savedInstanceState.putSerializable(SAVED_CREATION_REGISTRATION_RESPONSE, mRegistrationResponse);
        }

        // check if the application has been opened by click on an url
        if (null != mUniversalLinkUri) {
            savedInstanceState.putParcelable(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI, mUniversalLinkUri);
        }

        // check whether an email validation is in progress
        if (null != mRegisterPollingRunnable) {
            // Retrieve the current email three pid
            ThreePid email3pid = RegistrationManager.getInstance().getEmailThreePid();
            if (null != email3pid) {
                savedInstanceState.putSerializable(SAVED_CREATION_EMAIL_THREEPID, email3pid);
            }
        }

        savedInstanceState.putInt(SAVED_MODE, mMode);
    }

    //==============================================================================================================
    // Display management
    //==============================================================================================================

    /**
     * Refresh Toolbar visibility and title, screen
     */
    private void refreshDisplay() {
        switch (mMode) {
            case MODE_START:
                toolbar.setVisibility(View.GONE);
                screenWelcome.setVisibility(View.VISIBLE);
                screenRegister.setVisibility(View.GONE);
                screenRegisterWaitForEmail.setVisibility(View.GONE);
                screenLogin.setVisibility(View.GONE);
                screenForgottenPassword.setVisibility(View.GONE);
                screenMessageButton.setVisibility(View.GONE);
                break;
            case MODE_ACCOUNT_CREATION:
                toolbar.setVisibility(View.VISIBLE);
                toolbar.setTitle(R.string.tchap_register_title);
                screenWelcome.setVisibility(View.GONE);
                screenRegister.setVisibility(View.VISIBLE);
                screenRegisterWaitForEmail.setVisibility(View.GONE);
                screenLogin.setVisibility(View.GONE);
                screenForgottenPassword.setVisibility(View.GONE);
                screenMessageButton.setVisibility(View.GONE);
                break;
            case MODE_ACCOUNT_CREATION_WAIT_FOR_EMAIL:
                toolbar.setVisibility(View.VISIBLE);
                toolbar.setTitle(R.string.tchap_register_title);
                screenWelcome.setVisibility(View.GONE);
                screenRegister.setVisibility(View.GONE);
                screenRegisterWaitForEmail.setVisibility(View.VISIBLE);
                screenLogin.setVisibility(View.GONE);
                screenForgottenPassword.setVisibility(View.GONE);
                screenMessageButton.setVisibility(View.GONE);
                break;
            case MODE_LOGIN:
                toolbar.setVisibility(View.VISIBLE);
                toolbar.setTitle(R.string.tchap_connection_title);
                screenWelcome.setVisibility(View.GONE);
                screenRegister.setVisibility(View.GONE);
                screenRegisterWaitForEmail.setVisibility(View.GONE);
                screenLogin.setVisibility(View.VISIBLE);
                screenForgottenPassword.setVisibility(View.GONE);
                screenMessageButton.setVisibility(View.GONE);
                break;
            case MODE_FORGOT_PASSWORD:
                toolbar.setVisibility(View.VISIBLE);
                toolbar.setTitle(R.string.tchap_connection_title);
                screenWelcome.setVisibility(View.GONE);
                screenRegister.setVisibility(View.GONE);
                screenRegisterWaitForEmail.setVisibility(View.GONE);
                screenLogin.setVisibility(View.GONE);
                screenForgottenPassword.setVisibility(View.VISIBLE);
                screenMessageButton.setVisibility(View.GONE);
                break;
            case MODE_FORGOT_PASSWORD_WAITING_VALIDATION:
            case MODE_FORGOT_PASSWORD_WAITING_VALIDATION_2:
                toolbar.setVisibility(View.VISIBLE);
                toolbar.setTitle(R.string.tchap_connection_title);
                screenWelcome.setVisibility(View.GONE);
                screenRegister.setVisibility(View.GONE);
                screenRegisterWaitForEmail.setVisibility(View.GONE);
                screenLogin.setVisibility(View.GONE);
                screenForgottenPassword.setVisibility(View.GONE);
                screenMessageButton.setVisibility(View.VISIBLE);
                break;
            default:
                // TODO manage other cases
                toolbar.setTitle("");
                break;
        }

        switch (mMode) {
            case MODE_FORGOT_PASSWORD_WAITING_VALIDATION:
                messageNotice.setText(getString(R.string.auth_reset_password_email_validation_message, mCurrentEmail));
                messageButton.setText(R.string.auth_reset_password_next_step_button);
                break;
            case MODE_FORGOT_PASSWORD_WAITING_VALIDATION_2:
                messageNotice.setText(R.string.auth_reset_password_success_message);
                messageButton.setText(R.string.auth_return_to_login);
                break;
            default:
                break;
        }


        supportInvalidateOptionsMenu();

        // Tchap: The following view "threePidLayout" is not used for the moment.
        View threePidLayout = findViewById(R.id.three_pid_layout);
        threePidLayout.setVisibility(View.GONE);
        //threePidLayout.setVisibility((mMode == MODE_ACCOUNT_CREATION_THREE_PID) ? View.VISIBLE : View.GONE);
        //mSubmitThreePidButton.setVisibility(mMode == MODE_ACCOUNT_CREATION_THREE_PID ? View.VISIBLE : View.GONE);
        //mSkipThreePidButton.setVisibility(mMode == MODE_ACCOUNT_CREATION_THREE_PID && RegistrationManager.getInstance().canSkip() ? View.VISIBLE : View.GONE);
    }

    /**
     * Display a loading screen mask over the login screen
     *
     * @param isLoadingScreenVisible true to enable the loading screen, false otherwise
     */
    private void enableLoadingScreen(boolean isLoadingScreenVisible) {
        supportInvalidateOptionsMenu();

        if (null != mLoginMaskView) {
            mLoginMaskView.setVisibility(isLoadingScreenVisible ? View.VISIBLE : View.GONE);
        }
    }

    //==============================================================================================================
    // extracted info
    //==============================================================================================================

    /**
     * Sanitize an URL
     *
     * @param url the url to sanitize
     * @return the sanitized url
     */
    private static String sanitizeUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }

        return url.replaceAll("\\s", "");
    }

    /**
     * @return the homeserver config. null if the url is not valid
     */
    private HomeServerConnectionConfig getHsConfig() {
        try {
            mServerConfig = null;
            mServerConfig = HomeServerConnectionConfigFactoryKt.createHomeServerConnectionConfig(getHomeServerUrl(), getIdentityServerUrl());
        } catch (Exception e) {
            Log.e(LOG_TAG, "getHsConfig fails " + e.getLocalizedMessage());
        }

        return mServerConfig;
    }

    //==============================================================================================================
    // third party activities
    //==============================================================================================================

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "## onActivityResult(): IN - requestCode=" + requestCode + " resultCode=" + resultCode);
        if (resultCode == RESULT_OK && requestCode == REQUEST_REGISTRATION_COUNTRY) {
            /*if (data != null && data.hasExtra(CountryPickerActivity.EXTRA_OUT_COUNTRY_CODE) && mRegistrationPhoneNumberHandler != null) {
                mRegistrationPhoneNumberHandler.setCountryCode(data.getStringExtra(CountryPickerActivity.EXTRA_OUT_COUNTRY_CODE));
            }*/
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_LOGIN_COUNTRY) {
           /* if (data != null && data.hasExtra(CountryPickerActivity.EXTRA_OUT_COUNTRY_CODE) && mLoginPhoneNumberHandler != null) {
                mLoginPhoneNumberHandler.setCountryCode(data.getStringExtra(CountryPickerActivity.EXTRA_OUT_COUNTRY_CODE));
            }*/
        } else if (CAPTCHA_CREATION_ACTIVITY_REQUEST_CODE == requestCode) {
            if (resultCode == RESULT_OK) {
                Log.d(LOG_TAG, "## onActivityResult(): CAPTCHA_CREATION_ACTIVITY_REQUEST_CODE => RESULT_OK");
                String captchaResponse = data.getStringExtra("response");
                RegistrationManager.getInstance().setCaptchaResponse(captchaResponse);
                createAccount();
            } else {
                Log.d(LOG_TAG, "## onActivityResult(): CAPTCHA_CREATION_ACTIVITY_REQUEST_CODE => RESULT_KO");
                // cancel the registration flow
                mRegistrationResponse = null;
                showMainLayout();
                enableLoadingScreen(false);
                refreshDisplay();
            }
        } else if ((ACCOUNT_CREATION_ACTIVITY_REQUEST_CODE == requestCode) || (FALLBACK_LOGIN_ACTIVITY_REQUEST_CODE == requestCode)) {
            if (resultCode == RESULT_OK) {
                Log.d(LOG_TAG, "## onActivityResult(): ACCOUNT_CREATION_ACTIVITY_REQUEST_CODE => RESULT_OK");
                String homeServer = data.getStringExtra("homeServer");
                String userId = data.getStringExtra("userId");
                String accessToken = data.getStringExtra("accessToken");

                // build a credential with the provided items
                Credentials credentials = new Credentials();
                credentials.userId = userId;
                credentials.homeServer = homeServer;
                credentials.accessToken = accessToken;

                final HomeServerConnectionConfig hsConfig = getHsConfig();

                try {
                    hsConfig.setCredentials(credentials);
                } catch (Exception e) {
                    Log.d(LOG_TAG, "hsConfig setCredentials failed " + e.getLocalizedMessage());
                }

                Log.d(LOG_TAG, "Account creation succeeds");

                // let's go...
                MXSession session = Matrix.getInstance(getApplicationContext()).createSession(hsConfig);
                Matrix.getInstance(getApplicationContext()).addSession(session);
                goToSplash();
                finish();
            } else if ((resultCode == RESULT_CANCELED) && (FALLBACK_LOGIN_ACTIVITY_REQUEST_CODE == requestCode)) {
                Log.d(LOG_TAG, "## onActivityResult(): RESULT_CANCELED && FALLBACK_LOGIN_ACTIVITY_REQUEST_CODE");
            }
        }
    }

    /*
     * *********************************************************************************************
     * Account creation - Threepid
     * *********************************************************************************************
     */

    /**
     * Init the view asking for email and/or phone number depending on supported registration flows
     */
    private void initThreePidView() {
        // Make sure to start with a clear state
        RegistrationManager.getInstance().clearThreePid();
        mEmailAddress.setText("");
        //mRegistrationPhoneNumberHandler.reset();
        mEmailAddress.requestFocus();

        mThreePidInstructions.setText(RegistrationManager.getInstance().getThreePidInstructions(this));

        if (RegistrationManager.getInstance().supportStage(LoginRestClient.LOGIN_FLOW_TYPE_EMAIL_IDENTITY)) {
            mEmailAddress.setVisibility(View.VISIBLE);
            if (RegistrationManager.getInstance().isOptional(LoginRestClient.LOGIN_FLOW_TYPE_EMAIL_IDENTITY)) {
                mEmailAddress.setHint(R.string.auth_opt_email_placeholder);
            } else {
                mEmailAddress.setHint(R.string.auth_email_placeholder);
            }
        } else {
            mEmailAddress.setVisibility(View.GONE);
        }

        /*if (RegistrationManager.getInstance().supportStage(LoginRestClient.LOGIN_FLOW_TYPE_MSISDN)) {
            mRegistrationPhoneNumberHandler.setCountryCode(PhoneNumberUtils.getCountryCode(this));
            mPhoneNumberLayout.setVisibility(View.VISIBLE);
            if (RegistrationManager.getInstance().isOptional(LoginRestClient.LOGIN_FLOW_TYPE_MSISDN)) {
                mPhoneNumber.setHint(R.string.auth_opt_phone_number_placeholder);
            } else {
                mPhoneNumber.setHint(R.string.auth_phone_number_placeholder);
            }
        } else {
            mPhoneNumberLayout.setVisibility(View.GONE);
        }*/

        /*if (RegistrationManager.getInstance().canSkip()) {
            mSkipThreePidButton.setVisibility(View.VISIBLE);
            mSkipThreePidButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Make sure no three pid is attached to the process
                    RegistrationManager.getInstance().clearThreePid();
                    createAccount();
                    //mRegistrationPhoneNumberHandler.reset();
                    mEmailAddress.setText("");
                }
            });
        } else {
            mSkipThreePidButton.setVisibility(View.GONE);
        }

        mSubmitThreePidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitThreePids();
            }
        });*/
    }

    /**
     * Submit the three pids
     */
    private void submitThreePids() {
        dismissKeyboard(this);

        // Make sure to start with a clear state in case user already submitted before but canceled
        RegistrationManager.getInstance().clearThreePid();

        // Check that email format is valid and not empty if field is required
        final String email = mEmailAddress.getText().toString();
        if (!TextUtils.isEmpty(email)) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, R.string.auth_invalid_email, Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (RegistrationManager.getInstance().isEmailRequired()) {
            Toast.makeText(this, R.string.auth_missing_email, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that phone number format is valid and not empty if field is required
        /*if (mRegistrationPhoneNumberHandler.getPhoneNumber() != null) {
            if (!mRegistrationPhoneNumberHandler.isPhoneNumberValidForCountry()) {
                Toast.makeText(this, R.string.auth_invalid_phone, Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (RegistrationManager.getInstance().isPhoneNumberRequired()) {
            Toast.makeText(this, R.string.auth_missing_phone, Toast.LENGTH_SHORT).show();
            return;
        }*/

        /*if (!RegistrationManager.getInstance().canSkip() && mRegistrationPhoneNumberHandler.getPhoneNumber() == null && TextUtils.isEmpty(email)) {
            // Both are required and empty
            Toast.makeText(this, R.string.auth_missing_email_or_phone, Toast.LENGTH_SHORT).show();
            return;
        }*/

        if (!TextUtils.isEmpty(email)) {
            // Communicate email to singleton (will be validated later on)
            RegistrationManager.getInstance().addEmailThreePid(new ThreePid(email, ThreePid.MEDIUM_EMAIL));
        }

        /*if (mRegistrationPhoneNumberHandler.getPhoneNumber() != null) {
            // Communicate phone number to singleton + start validation process (always phone first)
            enableLoadingScreen(true);
            RegistrationManager.getInstance().addPhoneNumberThreePid(mRegistrationPhoneNumberHandler.getE164PhoneNumber(), mRegistrationPhoneNumberHandler.getCountryCode(),
                    new RegistrationManager.ThreePidRequestListener() {
                        @Override
                        public void onThreePidRequested(ThreePid pid) {
                            enableLoadingScreen(false);
                            if (!TextUtils.isEmpty(pid.sid)) {
                                onPhoneNumberSidReceived(pid);
                            }
                        }

                        @Override
                        public void onThreePidRequestFailed(@StringRes int errorMessageRes) {
                            TchapLoginActivity.this.onThreePidRequestFailed(getString(errorMessageRes));
                        }
                    });
        } else*/
        {
            createAccount();
        }
    }

    /**
     * Ask user the token received by SMS after phone number validation
     *
     * @param pid phone number pid
     */
    private void onPhoneNumberSidReceived(final ThreePid pid) {
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_phone_number_verification, null);
        mCurrentDialog = new AlertDialog.Builder(TchapLoginActivity.this)
                .setView(dialogLayout)
                .setMessage(R.string.settings_phone_number_verification_instruction)
                .setPositiveButton(R.string.auth_submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing here
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

        // Trick to prevent dialog being closed automatically when positive button is used
        mCurrentDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final TextInputEditText tokenView = dialogLayout.findViewById(R.id.phone_number_code_value);
                        submitPhoneNumber(tokenView.getText().toString(), pid);
                    }
                });
            }
        });

        mCurrentDialog.show();
    }

    /**
     * Submit the phone number token entered by the user
     *
     * @param token code entered by the user
     * @param pid   phone number pid
     */
    private void submitPhoneNumber(final String token, final ThreePid pid) {
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(TchapLoginActivity.this, R.string.auth_invalid_token, Toast.LENGTH_SHORT).show();
        } else {
            RegistrationManager.getInstance().submitValidationToken(token, pid,
                    new RegistrationManager.ThreePidValidationListener() {
                        @Override
                        public void onThreePidValidated(boolean isSuccess) {
                            if (isSuccess) {
                                createAccount();
                            } else {
                                Toast.makeText(TchapLoginActivity.this, R.string.auth_invalid_token, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    /**
     * Start registration process
     */
    private void createAccount() {
        if (mCurrentDialog != null) {
            mCurrentDialog.dismiss();
        }
        enableLoadingScreen(true);
        hideMainLayoutAndToast("");
        RegistrationManager.getInstance().attemptRegistration(this, this);
    }

    /**
     * Cancel the polling for email validation
     */
    private void cancelEmailPolling() {
        if (mHandler != null && mRegisterPollingRunnable != null) {
            mHandler.removeCallbacks(mRegisterPollingRunnable);
        }
    }

    /*
     * *********************************************************************************************
     * Account creation - Listeners
     * *********************************************************************************************
     */

    @Override
    public void onRegistrationSuccess(String warningMessage) {
        cancelEmailPolling();
        enableLoadingScreen(false);
        if (!TextUtils.isEmpty(warningMessage)) {
            mCurrentDialog = new AlertDialog.Builder(TchapLoginActivity.this)
                    .setTitle(R.string.dialog_title_warning)
                    .setMessage(warningMessage)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            goToSplash();
                            finish();
                        }
                    })
                    .show();
        } else {
            goToSplash();
            finish();
        }
    }

    @Override
    public void onRegistrationFailed(String message) {
        cancelEmailPolling();
        mEmailValidationExtraParams = null;
        Log.e(LOG_TAG, "## onRegistrationFailed(): " + message);
        showMainLayout();
        enableLoadingScreen(false);
        refreshDisplay();
        Toast.makeText(this, R.string.login_error_unable_register, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onWaitingEmailValidation() {
        Log.d(LOG_TAG, "## onWaitingEmailValidation()");

        // Prompt the user to check his email
        mMode = MODE_ACCOUNT_CREATION_WAIT_FOR_EMAIL;
        screenRegisterWaitForEmailEmailTextView.setText(mCurrentEmail);
        refreshDisplay();

        // Loop to know whether the email has been checked
        mRegisterPollingRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "## onWaitingEmailValidation() attempt registration");
                RegistrationManager.getInstance().attemptRegistration(TchapLoginActivity.this, TchapLoginActivity.this);
                mHandler.postDelayed(mRegisterPollingRunnable, REGISTER_POLLING_PERIOD);
            }
        };
        mHandler.postDelayed(mRegisterPollingRunnable, REGISTER_POLLING_PERIOD);
    }

    @Override
    public void onWaitingCaptcha() {
        cancelEmailPolling();
        final String publicKey = RegistrationManager.getInstance().getCaptchaPublicKey();
        if (!TextUtils.isEmpty(publicKey)) {
            Log.d(LOG_TAG, "## onWaitingCaptcha");
            Intent intent = new Intent(TchapLoginActivity.this, AccountCreationCaptchaActivity.class);
            intent.putExtra(AccountCreationCaptchaActivity.EXTRA_HOME_SERVER_URL, getHomeServerUrl());
            intent.putExtra(AccountCreationCaptchaActivity.EXTRA_SITE_KEY, publicKey);
            startActivityForResult(intent, CAPTCHA_CREATION_ACTIVITY_REQUEST_CODE);
        } else {
            Log.d(LOG_TAG, "## onWaitingCaptcha(): captcha flow cannot be done");
            Toast.makeText(this, getString(R.string.login_error_unable_register), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onThreePidRequestFailed(String message) {
        Log.d(LOG_TAG, "## onThreePidRequestFailed():" + message);
        enableLoadingScreen(false);
        showMainLayout();
        refreshDisplay();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUsernameAvailabilityChecked(boolean isAvailable) {
        // Tchap do not used checkUsernameAvailability()
        // This callback is never called

        /*enableLoadingScreen(false);
        if (!isAvailable) {
            showMainLayout();
            Toast.makeText(this, R.string.auth_username_in_use, Toast.LENGTH_LONG).show();
        } else {
            if (RegistrationManager.getInstance().canAddThreePid()) {
                // Show next screen with email/phone number
                showMainLayout();
                mMode = MODE_ACCOUNT_CREATION_THREE_PID;
                initThreePidView();
                refreshDisplay();
            } else {
                // Start registration
                createAccount();
            }
        }*/
    }

    @Override
    public void onResourceLimitExceeded(MatrixError e) {
        enableLoadingScreen(false);
        mResourceLimitDialogHelper.displayDialog(e);
    }


    /*
     * *********************************************************************************************
     * DINSIC management
     * *********************************************************************************************
     */
    // DINSIC specific
    private Platform mTchapPlatform;
    private String mCurrentEmail;

    /**
     * @return the home server Url according to current tchap platform.
     */
    private String getHomeServerUrl() {
        return (null != mTchapPlatform && null != mTchapPlatform.hs) ? getString(R.string.server_url_prefix) + mTchapPlatform.hs : null;
    }

    /**
     * @return the identity server URL according to current tchap platform.
     */
    private String getIdentityServerUrl() {
        return (null != mTchapPlatform && null != mTchapPlatform.hs) ? getString(R.string.server_url_prefix) + mTchapPlatform.hs : null;
    }

    /**
     * Get the Tchap platform configuration (HS/IS) for the provided email address.
     *
     * @param activity     current activity
     * @param emailAddress the email address to consider
     * @param callback     the asynchronous callback
     */
    public static void discoverTchapPlatform(Activity activity, final String emailAddress, final ApiCallback<Platform> callback) {
        Log.d(LOG_TAG, "## discoverTchapPlatform [" + emailAddress + "]");
        // Prepare the list of the known ISes in order to run over the list until to get an answer.
        List<String> idServerUrls = new ArrayList<>();

        // Consider first the current identity server if any.
        String currentIdServerUrl = null;
        MXSession currentSession = Matrix.getInstance(activity).getDefaultSession();
        if (null != currentSession) {
            currentIdServerUrl = currentSession.getHomeServerConfig().getIdentityServerUri().toString();
            idServerUrls.add(currentIdServerUrl);
        }

        // Add randomly the preferred known ISes
        List<String> currentHosts = new ArrayList<>(Arrays.asList(activity.getResources().getStringArray(R.array.preferred_identity_server_names)));
        while (!currentHosts.isEmpty()) {
            int index = (new Random()).nextInt(currentHosts.size());
            String host = currentHosts.remove(index);

            String idServerUrl = activity.getString(R.string.server_url_prefix) + host;
            if (null == currentIdServerUrl || !idServerUrl.equals(currentIdServerUrl)) {
                idServerUrls.add(idServerUrl);
            }
        }

        // Add randomly the other known ISes
        currentHosts = new ArrayList<>(Arrays.asList(activity.getResources().getStringArray(R.array.identity_server_names)));
        while (!currentHosts.isEmpty()) {
            int index = (new Random()).nextInt(currentHosts.size());
            String host = currentHosts.remove(index);

            String idServerUrl = activity.getString(R.string.server_url_prefix) + host;
            if (null == currentIdServerUrl || !idServerUrl.equals(currentIdServerUrl)) {
                idServerUrls.add(idServerUrl);
            }
        }

        discoverTchapPlatform(emailAddress, idServerUrls, callback);
    }

    /**
     * Run over all the provided hosts by removing them one by one until we get the Tchap platform for the provided email address.
     *
     * @param emailAddress       the email address to consider
     * @param identityServerUrls the list of the available identity server urls
     * @param callback           the asynchronous callback
     */
    private static void discoverTchapPlatform(final String emailAddress, final List<String> identityServerUrls, final ApiCallback<Platform> callback) {
        if (identityServerUrls.isEmpty()) {
            callback.onMatrixError(new MatrixError(MatrixError.UNKNOWN, "No host"));
        }

        // Retrieve the first identity server url by removing it from the list.
        String selectedUrl = identityServerUrls.remove(0);
        TchapRestClient tchapRestClient = new TchapRestClient(HomeServerConnectionConfigFactoryKt.createHomeServerConnectionConfig(null, selectedUrl));
        tchapRestClient.info(emailAddress, ThreePid.MEDIUM_EMAIL, new ApiCallback<Platform>() {
            @Override
            public void onSuccess(Platform platform) {
                Log.d(LOG_TAG, "## discoverTchapPlatform succeeded (" + platform.hs + ", " + platform.invited + ")");
                callback.onSuccess(platform);
            }

            @Override
            public void onNetworkError(Exception e) {
                Log.e(LOG_TAG, "## discoverTchapPlatform failed " + e.getMessage());
                if (identityServerUrls.isEmpty()) {
                    // We checked all the known hosts, return the error
                    callback.onNetworkError(e);
                } else {
                    // Try again
                    discoverTchapPlatform(emailAddress, identityServerUrls, callback);
                }
            }

            @Override
            public void onMatrixError(MatrixError matrixError) {
                Log.e(LOG_TAG, "## discoverTchapPlatform failed " + matrixError.getMessage());
                if (identityServerUrls.isEmpty()) {
                    // We checked all the known hosts, return the error
                    callback.onMatrixError(matrixError);
                } else {
                    // Try again
                    discoverTchapPlatform(emailAddress, identityServerUrls, callback);
                }
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e(LOG_TAG, "## discoverTchapPlatform failed " + e.getMessage());
                if (identityServerUrls.isEmpty()) {
                    // We checked all the known hosts, return the error
                    callback.onUnexpectedError(e);
                } else {
                    // Try again
                    discoverTchapPlatform(emailAddress, identityServerUrls, callback);
                }
            }
        });
    }

    /**
     * The user clicks on the register button.
     */
    @OnClick(R.id.fragment_tchap_first_welcome_register_button)
    void onRegisterClick() {
        Log.d(LOG_TAG, "## onRegisterClick(): IN");
        onClick();

        // the user switches to another mode
        if (mMode != MODE_ACCOUNT_CREATION) {
            mMode = MODE_ACCOUNT_CREATION;
            refreshDisplay();
            return;
        }

        // Check the provided email
        final String emailAddress = mCreationEmailAddressTextView.getText().toString().trim();
        if (TextUtils.isEmpty(emailAddress) || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            Toast.makeText(this, R.string.auth_invalid_email, Toast.LENGTH_SHORT).show();
            return;
        }

        // Handle parameters
        final String password = mCreationPassword1TextView.getText().toString().trim();
        String passwordCheck = mCreationPassword2TextView.getText().toString().trim();

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), getString(R.string.auth_missing_password), Toast.LENGTH_SHORT).show();
            return;
        } else if (password.length() < 6) {
            Toast.makeText(getApplicationContext(), getString(R.string.auth_invalid_password), Toast.LENGTH_SHORT).show();
            return;
        } else if (!TextUtils.equals(password, passwordCheck)) {
            Toast.makeText(getApplicationContext(), getString(R.string.auth_password_dont_match), Toast.LENGTH_SHORT).show();
            return;
        }

        enableLoadingScreen(true);

        // Retrieve the platform for the selected email
        discoverTchapPlatform(this, emailAddress, new ApiCallback<Platform>() {
            private void onError(String errorMessage) {
                enableLoadingScreen(false);
                // Notify the user.
                Toast.makeText(TchapLoginActivity.this, (null == errorMessage) ? getString(R.string.auth_invalid_email) : errorMessage, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSuccess(Platform platform) {
                // Check whether the returned platform is valid
                if (null == platform.hs || platform.hs.isEmpty()) {
                    // The email owner is not able to create a tchap account.
                    Log.e(LOG_TAG, "## onRegisterClick(): unauthorized email");
                    onError(getString(R.string.tchap_register_unauthorized_email));
                    return;
                }

                // Store the platform and the corresponding email
                mTchapPlatform = platform;
                mCurrentEmail = emailAddress;

                RegistrationManager.getInstance().setHsConfig(getHsConfig());
                // The username is forced by the Tchap server, we don't send it anymore.
                RegistrationManager.getInstance().setAccountData(null, password);

                RegistrationManager.getInstance().clearThreePid();
                RegistrationManager.getInstance().addEmailThreePid(new ThreePid(mCurrentEmail, ThreePid.MEDIUM_EMAIL));
                mIsMailValidationPending = true;

                checkRegistrationFlows(new SimpleApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void info) {
                        RegistrationManager.getInstance().attemptRegistration(TchapLoginActivity.this, TchapLoginActivity.this);
                    }
                });
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(MatrixError matrixError) {
                onError(matrixError.getLocalizedMessage());
            }

            @Override
            public void onUnexpectedError(Exception e) {
                onError(e.getLocalizedMessage());
            }
        });
    }

    /**
     * Tells if current user is external
     *
     * @param
     * @return true if external
     */
    public static boolean isUserExternal(MXSession session) {
        String myHost = session.getHomeServerConfig().getHomeserverUri().getHost();
        return myHost.contains(".e.");
    }

    /* ==========================================================================================
     * UI Events
     * ========================================================================================== */

    @OnClick(R.id.fragment_tchap_register_wait_for_email_back)
    void onEmailNotReceived() {
        cancelEmailPolling();
        fallbackToRegistrationMode();
    }

    @OnClick(R.id.fragment_tchap_first_message_button_submit)
    void messageSubmit() {
        onForgotOnEmailValidated(getHsConfig());
    }
}
