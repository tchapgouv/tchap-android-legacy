/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

package im.vector;

import android.content.Context;
import android.text.TextUtils;

import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.callback.SuccessCallback;
import org.matrix.androidsdk.rest.client.LoginRestClient;
import org.matrix.androidsdk.rest.client.ThirdPidRestClient;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.login.Credentials;
import org.matrix.androidsdk.rest.model.login.LoginFlow;
import org.matrix.androidsdk.rest.model.login.RegistrationParams;
import org.matrix.androidsdk.rest.model.pid.ThreePid;
import org.matrix.androidsdk.util.Log;

import java.util.List;

import fr.gouv.tchap.model.TchapConnectionConfig;
import fr.gouv.tchap.model.TchapSession;
import im.vector.settings.VectorLocale;

public class LoginHandler {
    private static final String LOG_TAG = LoginHandler.class.getSimpleName();

    /**
     * The account authentication succeeds, store here the dedicated Tchap session and create it.
     *
     * @param matrixInstance  the current Matrix instance
     * @param tchapConfig     the Tchap homeserver(s) config
     */
    private void onRegistrationDone(Matrix matrixInstance,
                                    TchapConnectionConfig tchapConfig) {
        // Sanity check: check whether the tchap session does not already exist.
        String userId = tchapConfig.getHsConfig().getCredentials().userId;
        TchapSession existingSession = matrixInstance.getTchapSession(userId);

        if (existingSession == null) {
            matrixInstance.addTchapConnectionConfig(tchapConfig);
            matrixInstance.createTchapSession(tchapConfig);
        }
    }

    /**
     * Try to login.
     * The Matrix session(s) are created if the operation succeeds.
     *
     * @param context            The context.
     * @param tchapConfig        The Tchap homeserver(s) config.
     * @param username           The username.
     * @param phoneNumber        The phone number.
     * @param phoneNumberCountry The phone number country code.
     * @param password           The password;
     * @param callback           The callback.
     */
    public void login(final Context context,
                      final TchapConnectionConfig tchapConfig,
                      final String username,
                      final String phoneNumber,
                      final String phoneNumberCountry,
                      final String password,
                      final ApiCallback<String> callback) {
        // Consider the main HS first.
        final HomeServerConnectionConfig hsConfig = tchapConfig.getHsConfig();
        callLogin(context, hsConfig, username, phoneNumber, phoneNumberCountry, password, new UnrecognizedCertApiCallback<Credentials>(hsConfig, callback) {
            @Override
            public void onSuccess(Credentials credentials) {
                // sanity check - GA issue
                if (TextUtils.isEmpty(credentials.userId)) {
                    callback.onMatrixError(new MatrixError(MatrixError.FORBIDDEN, "No user id"));
                    return;
                }

                hsConfig.setCredentials(credentials);

                // Check whether a shadow HS is available in the Tchap configuration
                final HomeServerConnectionConfig shadowHS = tchapConfig.getShadowHSConfig();
                if (shadowHS != null) {
                    shadowLogin(context, shadowHS, username, phoneNumber, phoneNumberCountry, password, new SuccessCallback<String>() {
                        @Override
                        public void onSuccess(String warningMessage) {
                            onRegistrationDone(Matrix.getInstance(context.getApplicationContext()), tchapConfig);
                            callback.onSuccess(warningMessage);
                        }
                    });

                } else {
                    onRegistrationDone(Matrix.getInstance(context.getApplicationContext()), tchapConfig);
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onAcceptedCert() {
                login(context, tchapConfig, username, phoneNumber, phoneNumberCountry, password, callback);
            }
        });
    }

    /**
     * Handle the login stage on the shadow HS.
     * This method returns a warning message in case of failure, otherwise it returns null.
     *
     * @param context            the context.
     * @param shadowHSConfig     The shadow homeserver config.
     * @param username           The username.
     * @param phoneNumber        The phone number.
     * @param phoneNumberCountry The phone number country code.
     * @param password           The password;
     * @param callback           The callback.
     */
    private void shadowLogin(final Context context,
                             final HomeServerConnectionConfig shadowHSConfig,
                             final String username,
                             final String phoneNumber,
                             final String phoneNumberCountry,
                             final String password,
                             final SuccessCallback<String> callback) {
        callLogin(context, shadowHSConfig, username, phoneNumber, phoneNumberCountry, password, new UnrecognizedCertApiCallback<Credentials>(shadowHSConfig) {
            private void onError(String errorMessage) {
                Log.e(LOG_TAG, "Login to the shadow HS failed " + errorMessage);
                callback.onSuccess(context.getString(R.string.tchap_auth_agent_failure_warning_msg));
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getMessage());
            }

            @Override
            public void onMatrixError(MatrixError e) {
                onError(e.getMessage());
            }

            @Override
            public void onUnexpectedError(Exception e) {
                onError(e.getMessage());
            }

            @Override
            public void onSuccess(Credentials credentials) {
                // sanity check - GA issue
                if (TextUtils.isEmpty(credentials.userId)) {
                    onError("No user id");
                    return;
                }

                shadowHSConfig.setCredentials(credentials);
                callback.onSuccess(null);
            }

            @Override
            public void onAcceptedCert() {
                shadowLogin(context, shadowHSConfig, username, phoneNumber, phoneNumberCountry, password, callback);
            }
        });
    }

    /**
     * Log the user using the given params after identifying if the login is a 3pid, a username or a phone number
     *
     * @param hsConfig
     * @param username
     * @param phoneNumber
     * @param phoneNumberCountry
     * @param password
     * @param callback
     */
    private void callLogin(final Context ctx,
                           final HomeServerConnectionConfig hsConfig,
                           final String username,
                           final String phoneNumber,
                           final String phoneNumberCountry,
                           final String password,
                           final ApiCallback<Credentials> callback) {
        LoginRestClient client = new LoginRestClient(hsConfig);
        String deviceName = ctx.getString(R.string.login_mobile_device);

        if (!TextUtils.isEmpty(username)) {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
                // Login with 3pid
                client.loginWith3Pid(ThreePid.MEDIUM_EMAIL,
                        username.toLowerCase(VectorLocale.INSTANCE.getApplicationLocale()), password, deviceName, null, callback);
            } else {
                // Login with user
                client.loginWithUser(username, password, deviceName, null, callback);
            }
        } else if (!TextUtils.isEmpty(phoneNumber) && !TextUtils.isEmpty(phoneNumberCountry)) {
            client.loginWithPhoneNumber(phoneNumber, phoneNumberCountry, password, deviceName, null, callback);
        }
    }

    /**
     * Retrieve the supported registration flows of a home server.
     *
     * @param context  the application context.
     * @param hsConfig the home server config.
     * @param callback the supported flows list callback.
     */
    public void getSupportedRegistrationFlows(final Context context,
                                              final HomeServerConnectionConfig hsConfig,
                                              final ApiCallback<Void> callback) {
        final RegistrationParams params = new RegistrationParams();
        LoginRestClient client = new LoginRestClient(hsConfig);

        // avoid dispatching the device name
        params.initial_device_display_name = context.getString(R.string.login_mobile_device);

        client.register(params, new UnrecognizedCertApiCallback<Credentials>(hsConfig, callback) {
            @Override
            public void onSuccess(Credentials credentials) {
                // Should never happen, the request must fail by calling onMatrixError().
                callback.onMatrixError(new MatrixError(MatrixError.NOT_SUPPORTED, "Unexpected registration success"));
            }

            @Override
            public void onAcceptedCert() {
                getSupportedRegistrationFlows(context, hsConfig, callback);
            }
        });
    }

    /**
     * Perform the validation of a mail ownership.
     *
     * @param context           Android App context
     * @param aHomeServerConfig server configuration
     * @param aToken            the token
     * @param aClientSecret     the client secret
     * @param aSid              the server identity session id
     * @param aRespCallback     asynchronous callback response
     */
    public void submitEmailTokenValidation(final Context context,
                                           final HomeServerConnectionConfig aHomeServerConfig,
                                           final String aToken,
                                           final String aClientSecret,
                                           final String aSid,
                                           final ApiCallback<Boolean> aRespCallback) {
        final ThreePid pid = new ThreePid(null, ThreePid.MEDIUM_EMAIL);
        ThirdPidRestClient restClient = new ThirdPidRestClient(aHomeServerConfig);

        pid.submitValidationToken(restClient, aToken, aClientSecret, aSid, new UnrecognizedCertApiCallback<Boolean>(aHomeServerConfig, aRespCallback) {
            @Override
            public void onSuccess(Boolean info) {
                aRespCallback.onSuccess(info);
            }

            @Override
            public void onAcceptedCert() {
                submitEmailTokenValidation(context, aHomeServerConfig, aToken, aClientSecret, aSid, aRespCallback);
            }
        });
    }
}
