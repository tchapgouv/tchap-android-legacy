/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
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

package im.vector.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.widget.Toast;

import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.Log;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.callback.SimpleApiCallback;
import org.matrix.androidsdk.core.model.HttpError;
import org.matrix.androidsdk.core.model.MatrixError;
import org.matrix.androidsdk.core.model.HttpException;

import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.gouv.tchap.activity.TchapLoginActivity;
import fr.gouv.tchap.sdk.rest.client.TchapValidityRestClient;
import fr.gouv.tchap.util.HomeServerConnectionConfigFactoryKt;
import im.vector.LoginHandler;
import im.vector.Matrix;
import im.vector.R;
import im.vector.receiver.LoginConfig;
import im.vector.receiver.VectorUniversalLinkReceiver;

/**
 * Dummy activity used to dispatch the vector URL links.
 */
@SuppressLint("LongLogTag")
public class VectorUniversalLinkActivity extends VectorAppCompatActivity {
    private static final String LOG_TAG = VectorUniversalLinkActivity.class.getSimpleName();

    private final VectorUniversalLinkReceiver mUniversalLinkReceiver = new VectorUniversalLinkReceiver();

    //private static final String SUPPORTED_PATH_CONFIG = "/config/config";
    // Account validity query
    private static final String ACCOUNT_VALIDITY_RENEW_PATH_SUFFIX = "/account_validity/renew";
    private static final String ACCOUNT_VALIDITY_RENEW_TOKEN = "token";

    @Override
    public int getLayoutRes() {
        // display a spinner while binding the email
        return R.layout.activity_vector_universal_link_activity;
    }

    @Override
    public void initUiAndData() {
        configureToolbar();

        // Register the universal link receiver to the local broadcast manager.
        IntentFilter intentFilter = new IntentFilter(VectorUniversalLinkReceiver.BROADCAST_ACTION_UNIVERSAL_LINK);
        intentFilter.addDataScheme("http");
        intentFilter.addDataScheme("https");
        LocalBroadcastManager.getInstance(this).registerReceiver(mUniversalLinkReceiver, intentFilter);

        String intentAction = null;

        try {
            Uri intentUri = getIntent().getData();
            String dataPath = intentUri.getPath();

            if (dataPath.endsWith(EMAIL_VALIDATION_PATH_SUFFIX)) {
                // We consider here an email validation
                final Map<String, String> mailRegParams = parseMailRegistrationLink(intentUri);

                // Assume it is a new account creation when there is a next link, or when no session is already available.
                MXSession session = Matrix.getInstance(this).getDefaultSession();

                if (mailRegParams.containsKey(KEY_MAIL_VALIDATION_NEXT_LINK) || (null == session)) {
                    if (session == null) {
                        // build Login intent
                        Intent intent = new Intent(VectorUniversalLinkActivity.this, TchapLoginActivity.class);
                        intent.putExtra(EXTRA_EMAIL_VALIDATION_PARAMS, (HashMap) mailRegParams);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                        finish();
                    } else {
                        Log.d(LOG_TAG, "## logout the current sessions, before finalizing an account creation based on an email validation");

                        // This logout is asynchronous, pursue the action in the callback to have the LoginActivity in a "no credentials state".
                        CommonActivityUtils.logout(VectorUniversalLinkActivity.this,
                                Matrix.getMXSessions(VectorUniversalLinkActivity.this),
                                true,
                                new SimpleApiCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void info) {
                                        Log.d(LOG_TAG, "## logout succeeded");

                                        // build Login intent
                                        Intent intent = new Intent(VectorUniversalLinkActivity.this, TchapLoginActivity.class);
                                        intent.putExtra(EXTRA_EMAIL_VALIDATION_PARAMS, (HashMap) mailRegParams);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);

                                        finish();
                                    }
                                });
                    }
                } else {
                    emailBinding(intentUri, mailRegParams);
                }
//            } else if (SUPPORTED_PATH_CONFIG.equals(dataPath)) {
//                MXSession mSession = Matrix.getInstance(this).getDefaultSession();
//
//                if (null == mSession) {
//                    // user is not yet logged in, this is the nominal case
//                    startLoginActivity();
//                    return;
//                } else {
//                    displayAlreadyLoginPopup();
//                    return;
//                }
            } else if (dataPath.endsWith(ACCOUNT_VALIDITY_RENEW_PATH_SUFFIX)) {
                renewAccountValidity(intentUri);
            } else {
                intentAction = VectorUniversalLinkReceiver.BROADCAST_ACTION_UNIVERSAL_LINK;
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "## Exception - Msg=" + ex.getMessage(), ex);
        }

        if (intentAction != null) {
            // since android O
            // set the class to avoid having "Background execution not allowed"
            Intent myBroadcastIntent = new Intent(this, VectorUniversalLinkReceiver.class);
            myBroadcastIntent.setAction(intentAction);
            myBroadcastIntent.setData(getIntent().getData());
            LocalBroadcastManager.getInstance(this).sendBroadcast(myBroadcastIntent);
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUniversalLinkReceiver);
    }

//    /**
//     * Start the login screen with identity server and home server pre-filled
//     */
//    private void startLoginActivity() {
//        Intent intent = new Intent(this, LoginActivity.class);
//        intent.putExtra(LoginActivity.EXTRA_CONFIG, LoginConfig.Companion.parse(getIntent().getData()));
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//        finish();
//    }
//
//    /**
//     * Propose to disconnect from a previous HS, when clicking on an auto config link
//     */
//    private void displayAlreadyLoginPopup() {
//        new AlertDialog.Builder(this)
//                .setTitle(R.string.dialog_title_warning)
//                .setMessage(R.string.error_user_already_logged_in)
//                .setCancelable(false)
//                .setPositiveButton(R.string.logout, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        CommonActivityUtils.logout(VectorUniversalLinkActivity.this,
//                                Matrix.getMXSessions(VectorUniversalLinkActivity.this),
//                                true,
//                                new SimpleApiCallback<Void>() {
//                                    @Override
//                                    public void onSuccess(Void info) {
//                                        Log.d(LOG_TAG, "## displayAlreadyLoginPopup(): logout succeeded");
//                                        startLoginActivity();
//                                    }
//                                });
//                    }
//                })
//                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        finish();
//                    }
//                })
//                .show();
//    }

    /**
     * Email binding management
     *
     * @param uri        the uri.
     * @param aMapParams the parsed params
     */
    private void emailBinding(Uri uri, Map<String, String> aMapParams) {
        Log.d(LOG_TAG, "## emailBinding()");

        String ISUrl = uri.getScheme() + "://" + uri.getHost();

        final HomeServerConnectionConfig homeServerConfig = HomeServerConnectionConfigFactoryKt.createHomeServerConnectionConfig(ISUrl, ISUrl);

        String token = aMapParams.get(KEY_MAIL_VALIDATION_TOKEN);
        String clientSecret = aMapParams.get(KEY_MAIL_VALIDATION_CLIENT_SECRET);
        String identityServerSessId = aMapParams.get(KEY_MAIL_VALIDATION_IDENTITY_SERVER_SESSION_ID);

        final LoginHandler loginHandler = new LoginHandler();

        loginHandler.submitEmailTokenValidation(getApplicationContext(), homeServerConfig, token, clientSecret, identityServerSessId,
                new ApiCallback<Boolean>() {

                    private void errorHandler(final String errorMessage) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                                bringAppToForeground();
                            }
                        });
                    }

                    @Override
                    public void onSuccess(Boolean isSuccess) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(LOG_TAG, "## emailBinding(): succeeds.");
                                bringAppToForeground();
                            }
                        });
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        Log.d(LOG_TAG, "## emailBinding(): onNetworkError() Msg=" + e.getLocalizedMessage());
                        errorHandler(getString(R.string.login_error_unable_register) + " : " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        Log.d(LOG_TAG, "## emailBinding(): onMatrixError() Msg=" + e.getLocalizedMessage());
                        errorHandler(getString(R.string.login_error_unable_register) + " : " + e.getLocalizedMessage());
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        Log.d(LOG_TAG, "## emailBinding(): onUnexpectedError() Msg=" + e.getLocalizedMessage());
                        errorHandler(getString(R.string.login_error_unable_register) + " : " + e.getLocalizedMessage());
                    }
                });
    }

    /**
     * Renew the account validity
     *
     * @param uri  the uri
     */
    private void renewAccountValidity(Uri uri) {
        Log.i(LOG_TAG, "## renewAccountValidity()");

        String serverUrl = uri.getScheme() + "://" + uri.getHost();

        final HomeServerConnectionConfig homeServerConfig = HomeServerConnectionConfigFactoryKt.createHomeServerConnectionConfig(serverUrl, serverUrl);

        String token = null;
        Set<String> names = uri.getQueryParameterNames();
        for (String name : names) {
            if (ACCOUNT_VALIDITY_RENEW_TOKEN.equals(name)) {
                String value = uri.getQueryParameter(name);
                try {
                    token = URLDecoder.decode(value, "UTF-8");
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## renewAccountValidity(): Exception - parse query params Msg=" + e.getLocalizedMessage(), e);
                }
                break;
            }
        }

        TchapValidityRestClient validityRestClient = new TchapValidityRestClient(homeServerConfig);
        validityRestClient.renewAccountValidity(token, new ApiCallback<Void>() {

            @Override
            public void onSuccess(Void info) {
                Log.i(LOG_TAG, "## renewAccountValidity() succeeded");
                onResult(getString(R.string.tchap_renew_account_validity_success_msg), true);
            }

            @Override
            public void onNetworkError(Exception exception) {
                onError(exception.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(MatrixError error) {
                onError(error.getLocalizedMessage());
            }

            @Override
            public void onUnexpectedError(Exception exception) {
                String message = exception.getLocalizedMessage();
                // Check whether the provided token is invalid
                if (exception instanceof HttpException) {
                    HttpError error = ((HttpException) exception).getHttpError();
                    if (error.getHttpCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                        message = getString(R.string.tchap_renew_account_validity_invalid_token_msg);
                    }
                }
                onError(message);
            }

            private void onError(final String message) {
                Log.e(LOG_TAG, "## renewAccountValidity() failed: " + message);
                if (TextUtils.isEmpty(message)) {
                    onResult(getString(R.string.tchap_error_message_default), false);
                } else {
                    onResult(message, false);
                }
            }

            private void onResult(final String message, final Boolean isSuccess) {
                new AlertDialog.Builder(VectorUniversalLinkActivity.this)
                        .setMessage(message)
                        .setPositiveButton(R.string.ok, null)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                if (isSuccess) {
                                    Matrix.getInstance(VectorUniversalLinkActivity.this).onRenewAccountValidity();
                                }
                                bringAppToForeground();
                            }
                        })
                        .show();
            }
        });
    }

    private void bringAppToForeground() {
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasklist = am.getRunningTasks(100);

        if (!tasklist.isEmpty()) {
            int nSize = tasklist.size();
            for (int i = 0; i < nSize; i++) {
                final ActivityManager.RunningTaskInfo taskinfo = tasklist.get(i);
                if (taskinfo.topActivity.getPackageName().equals(getApplicationContext().getPackageName())) {
                    Log.d(LOG_TAG, "Bring the app in foreground.");
                    am.moveTaskToFront(taskinfo.id, 0);
                }
            }
        }

        finish();
    }

    /* ==========================================================================================
     * Registration link
     * ========================================================================================== */

    // Intent Extras
    public static final String EXTRA_EMAIL_VALIDATION_PARAMS = "EXTRA_EMAIL_VALIDATION_PARAMS";

    // The suffix of the email validation path
    private static final String EMAIL_VALIDATION_PATH_SUFFIX = "/validate/email/submitToken";

    // mail validation url query parameters
    // Examples:
    // mail validation url = https://vector.im/_matrix/identity/api/v1/validate/email/submitToken?token=815159
    //                               &client_secret=8033cc24-0312-4c65-a9cd-bb70cea44828&sid=3643&nextLink=...
    // nextLink = https://vector.im/develop/#/register?client_secret=8033cc24-ĸ-4c65-a9cd-bb70cea44828&hs_url=https://matrix.org
    //                               &is_url=https://vector.im&session_id=gRVxdjiMTAfHIRUMtiDvaNMa&sid=3643
    public static final String KEY_MAIL_VALIDATION_TOKEN = "token";
    public static final String KEY_MAIL_VALIDATION_CLIENT_SECRET = "client_secret";
    public static final String KEY_MAIL_VALIDATION_IDENTITY_SERVER_SESSION_ID = "sid";
    public static final String KEY_MAIL_VALIDATION_NEXT_LINK = "nextLink";
    public static final String KEY_MAIL_VALIDATION_HOME_SERVER_URL = "hs_url";
    public static final String KEY_MAIL_VALIDATION_IDENTITY_SERVER_URL = "is_url";
    public static final String KEY_MAIL_VALIDATION_SESSION_ID = "session_id";

    /**
     * Parse the URL sent in the email validation.
     * This flow is part of the registration process {@see <a href="http://matrix.org/speculator/spec/HEAD/identity_service.html">Identy spec server</a>}:
     * https://vector.im/_matrix/identity/api/v1/validate/email/submitToken
     * ?token=172230
     * &client_secret=3a164877-1f6a-4aa3-a056-0dc20ebe6392
     * &sid=3672
     * &nextLink=https%3A//vector.im/develop/%23/register%3Fclient_secret%3D3a164877-1f6a-4aa3-a056-0dc20ebe6392%26hs_url
     * %3Dhttps%3A//matrix.org%26is_url%3Dhttps%3A//vector.im%26session_id%3DipLKXEvRArNFZkDVpIZvqJMa%26sid%3D3672
     *
     * @param uri the uri to parse
     * @return the parameters extracted from the the URI.
     */
    private Map<String, String> parseMailRegistrationLink(Uri uri) {
        Map<String, String> mapParams = new HashMap<>();

        try {
            // sanity check
            if ((null == uri) || TextUtils.isEmpty(uri.getPath())) {
                Log.e(LOG_TAG, "## parseMailRegistrationLink : null");
            } else {
                String uriFragment, host = uri.getHost();
                Log.i(LOG_TAG, "## parseMailRegistrationLink(): host=" + host);

                // remove the server part
                uriFragment = uri.getFragment();
                String lastFrag = uri.getLastPathSegment();
                String specPart = uri.getSchemeSpecificPart();
                Log.i(LOG_TAG, "## parseMailRegistrationLink(): uriFragment=" + uriFragment);
                Log.i(LOG_TAG, "## parseMailRegistrationLink(): getLastPathSegment()=" + lastFrag);
                Log.i(LOG_TAG, "## parseMailRegistrationLink(): getSchemeSpecificPart()=" + specPart);

                Uri nextLinkUri = null;
                Set<String> names = uri.getQueryParameterNames();
                for (String name : names) {
                    String value = uri.getQueryParameter(name);

                    if (KEY_MAIL_VALIDATION_NEXT_LINK.equals(name)) {
                        // remove "#" to allow query params parsing
                        nextLinkUri = Uri.parse(value.replace("#/", ""));
                    }

                    try {
                        value = URLDecoder.decode(value, "UTF-8");
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "## parseMailRegistrationLink(): Exception - parse query params Msg=" + e.getLocalizedMessage(), e);
                    }
                    mapParams.put(name, value);
                }

                // parse next link URI
                if (null != nextLinkUri) {

                    String nextLinkHomeServer = nextLinkUri.getQueryParameter(KEY_MAIL_VALIDATION_HOME_SERVER_URL);
                    mapParams.put(KEY_MAIL_VALIDATION_HOME_SERVER_URL, nextLinkHomeServer);

                    String nextLinkIdentityServer = nextLinkUri.getQueryParameter(KEY_MAIL_VALIDATION_IDENTITY_SERVER_URL);
                    mapParams.put(KEY_MAIL_VALIDATION_IDENTITY_SERVER_URL, nextLinkIdentityServer);

                    String nextLinkSessionId = nextLinkUri.getQueryParameter(KEY_MAIL_VALIDATION_SESSION_ID);
                    mapParams.put(KEY_MAIL_VALIDATION_SESSION_ID, nextLinkSessionId);
                }

                Log.i(LOG_TAG, "## parseMailRegistrationLink(): map query=" + mapParams.toString());
            }
        } catch (Exception e) {
            mapParams = null;
            Log.e(LOG_TAG, "## parseMailRegistrationLink(): Exception - Msg=" + e.getLocalizedMessage(), e);
        }

        return mapParams;
    }
}
