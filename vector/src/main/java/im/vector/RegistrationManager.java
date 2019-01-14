/*
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
import android.support.annotation.StringRes;
import android.text.TextUtils;

import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.client.LoginRestClient;
import org.matrix.androidsdk.rest.client.ProfileRestClient;
import org.matrix.androidsdk.rest.client.ThirdPidRestClient;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.login.Credentials;
import org.matrix.androidsdk.rest.model.login.LoginFlow;
import org.matrix.androidsdk.rest.model.login.RegistrationFlowResponse;
import org.matrix.androidsdk.rest.model.login.RegistrationParams;
import org.matrix.androidsdk.rest.model.pid.ThreePid;
import org.matrix.androidsdk.ssl.CertUtil;
import org.matrix.androidsdk.ssl.Fingerprint;
import org.matrix.androidsdk.ssl.UnrecognizedCertificateException;
import org.matrix.androidsdk.util.JsonUtils;
import org.matrix.androidsdk.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import im.vector.util.UrlUtilKt;

public class RegistrationManager {
    private static final String LOG_TAG = RegistrationManager.class.getSimpleName();

    private static volatile RegistrationManager sInstance;

    private static final String ERROR_MISSING_STAGE = "ERROR_MISSING_STAGE";
    private static final String ERROR_EMPTY_USER_ID = "ERROR_EMPTY_USER_ID";

    // JSON keys used for registration request
    private static final String JSON_KEY_CLIENT_SECRET = "client_secret";
    private static final String JSON_KEY_ID_SERVER = "id_server";
    private static final String JSON_KEY_SID = "sid";
    private static final String JSON_KEY_TYPE = "type";
    private static final String JSON_KEY_THREEPID_CREDS = "threepid_creds";
    private static final String JSON_KEY_SESSION = "session";
    private static final String JSON_KEY_CAPTCHA_RESPONSE = "response";
    private static final String JSON_KEY_PUBLIC_KEY = "public_key";

    // Config
    private HomeServerConnectionConfig mHsConfig;
    private LoginRestClient mLoginRestClient;
    private ThirdPidRestClient mThirdPidRestClient;
    private ProfileRestClient mProfileRestClient;

    // Flows
    private RegistrationFlowResponse mRegistrationResponse;

    // Current registration params
    private String mUsername;
    private String mPassword;
    private ThreePid mEmail;
    private ThreePid mPhoneNumber;
    private String mCaptchaResponse;

    // True when the user entered both email and phone but only phone will be used for account registration
    private boolean mShowThreePidWarning;

    /*
     * *********************************************************************************************
     * Singleton
     * *********************************************************************************************
     */

    public static RegistrationManager getInstance() {
        if (sInstance == null) {
            sInstance = new RegistrationManager();
        }
        return sInstance;
    }

    private RegistrationManager() {
    }

    /*
     * *********************************************************************************************
     * Public methods
     * *********************************************************************************************
     */

    /**
     * Reset singleton values to allow a new registration
     */
    public void resetSingleton() {
        mHsConfig = null;
        mLoginRestClient = null;
        mThirdPidRestClient = null;
        mProfileRestClient = null;
        mRegistrationResponse = null;

        mUsername = null;
        mPassword = null;
        mEmail = null;
        mPhoneNumber = null;
        mCaptchaResponse = null;

        mShowThreePidWarning = false;
    }

    /**
     * Set the home server config
     *
     * @param hsConfig
     */
    public void setHsConfig(final HomeServerConnectionConfig hsConfig) {
        mHsConfig = hsConfig;
        mLoginRestClient = null;
        mThirdPidRestClient = null;
        mProfileRestClient = null;
    }

    /**
     * Set username and password (registration params)
     *
     * @param username
     * @param password
     */
    public void setAccountData(final String username, final String password) {
        mUsername = username;
        mPassword = password;
    }

    /**
     * Set the captcha response (registration param)
     *
     * @param captchaResponse
     */
    public void setCaptchaResponse(final String captchaResponse) {
        mCaptchaResponse = captchaResponse;
    }

    /**
     * Set the supported flow stages for the current home server)
     *
     * @param registrationFlowResponse
     */
    public void setSupportedRegistrationFlows(final RegistrationFlowResponse registrationFlowResponse) {
        if (registrationFlowResponse != null) {
            mRegistrationResponse = registrationFlowResponse;
        }
    }

    /**
     * Make the registration request with params depending on singleton values
     *
     * @param context
     * @param listener
     */
    public void attemptRegistration(final Context context, final RegistrationListener listener) {
        final String registrationType;
        if (mRegistrationResponse != null && !TextUtils.isEmpty(mRegistrationResponse.session)) {
            Map<String, Object> authParams;
            if (mPhoneNumber != null && !isCompleted(LoginRestClient.LOGIN_FLOW_TYPE_MSISDN) && !TextUtils.isEmpty(mPhoneNumber.sid)) {
                registrationType = LoginRestClient.LOGIN_FLOW_TYPE_MSISDN;
                authParams = getThreePidAuthParams(mPhoneNumber.clientSecret, mHsConfig.getIdentityServerUri().getHost(),
                        mPhoneNumber.sid, LoginRestClient.LOGIN_FLOW_TYPE_MSISDN, mRegistrationResponse.session);
            } else if (mEmail != null && !isCompleted(LoginRestClient.LOGIN_FLOW_TYPE_EMAIL_IDENTITY)) {
                if (TextUtils.isEmpty(mEmail.sid)) {
                    // Email token needs to be requested before doing validation
                    Log.d(LOG_TAG, "attemptRegistration: request email validation");
                    requestValidationToken(mEmail, new ThreePidRequestListener() {
                        @Override
                        public void onThreePidRequested(ThreePid pid) {
                            if (!TextUtils.isEmpty(pid.sid)) {
                                // The session id for the email validation has just been received.
                                // We trigger here a new registration request without delay to attach the current username
                                // and the pwd to the registration session.
                                attemptRegistration(context, listener);

                                // Notify the listener to wait for the email validation
                                listener.onWaitingEmailValidation();
                            }
                        }

                        @Override
                        public void onThreePidRequestFailed(@StringRes int errorMessageRes) {
                            listener.onThreePidRequestFailed(context.getString(errorMessageRes));
                        }
                    });
                    return;
                } else {
                    registrationType = LoginRestClient.LOGIN_FLOW_TYPE_EMAIL_IDENTITY;
                    authParams = getThreePidAuthParams(mEmail.clientSecret, mHsConfig.getIdentityServerUri().getHost(),
                            mEmail.sid, LoginRestClient.LOGIN_FLOW_TYPE_EMAIL_IDENTITY, mRegistrationResponse.session);
                }
            } else if (!TextUtils.isEmpty(mCaptchaResponse) && !isCompleted(LoginRestClient.LOGIN_FLOW_TYPE_RECAPTCHA)) {
                registrationType = LoginRestClient.LOGIN_FLOW_TYPE_RECAPTCHA;
                authParams = getCaptchaAuthParams(mCaptchaResponse);
            } else {
                // others
                registrationType = "";
                authParams = new HashMap<>();
            }

            if (TextUtils.equals(registrationType, LoginRestClient.LOGIN_FLOW_TYPE_MSISDN)
                    && mEmail != null && !isCaptchaRequired()) {
                // Email will not be processed
                mShowThreePidWarning = true;
                mEmail = null;
            }

            final RegistrationParams params = new RegistrationParams();
            if (!registrationType.equals(LoginRestClient.LOGIN_FLOW_TYPE_RECAPTCHA)) {
                if (mUsername != null) {
                    params.username = mUsername;
                }
                if (mPassword != null) {
                    params.password = mPassword;
                }
                params.bind_email = mEmail != null;
                params.bind_msisdn = mPhoneNumber != null;
            }

            if (authParams != null && !authParams.isEmpty()) {
                params.auth = authParams;
            }

            register(context, params, new InternalRegistrationListener() {
                @Override
                public void onRegistrationSuccess() {
                    if (mShowThreePidWarning) {
                        // An email was entered but was not attached to account
                        listener.onRegistrationSuccess(context.getString(R.string.auth_threepid_warning_message));
                    } else {
                        listener.onRegistrationSuccess(null);
                    }
                }

                @Override
                public void onRegistrationFailed(String message) {
                    if (TextUtils.equals(ERROR_MISSING_STAGE, message)
                            && (mPhoneNumber == null || isCompleted(LoginRestClient.LOGIN_FLOW_TYPE_MSISDN))) {
                        if (mEmail != null && !isCompleted(LoginRestClient.LOGIN_FLOW_TYPE_EMAIL_IDENTITY)) {
                            attemptRegistration(context, listener);
                        } else {
                            // At this point, only captcha can be the missing stage
                            listener.onWaitingCaptcha();
                        }
                    } else {
                        listener.onRegistrationFailed(message);
                    }
                }

                @Override
                public void onResourceLimitExceeded(MatrixError e) {
                    listener.onResourceLimitExceeded(e);
                }
            });
        } else {
            // TODO Report this fix in Riot
            listener.onRegistrationFailed("mRegistrationResponse is null or session is null");
        }
    }

    /**
     * Register step after a mail validation.
     * In the registration flow after an email was validated {@see #startEmailOwnershipValidation},
     * this register request must be performed to reach the next registration step.
     *
     * @param context
     * @param aClientSecret   client secret
     * @param aSid            identity server session ID
     * @param aIdentityServer identity server url
     * @param aSessionId      session ID
     * @param listener
     */
    public void registerAfterEmailValidation(final Context context, final String aClientSecret, final String aSid,
                                             final String aIdentityServer, final String aSessionId,
                                             final RegistrationListener listener) {
        Log.d(LOG_TAG, "registerAfterEmailValidation");
        // set session
        if (null != mRegistrationResponse) {
            mRegistrationResponse.session = aSessionId;
        }

        RegistrationParams registrationParams = new RegistrationParams();
        registrationParams.auth = getThreePidAuthParams(aClientSecret, UrlUtilKt.removeUrlScheme(aIdentityServer),
                aSid, LoginRestClient.LOGIN_FLOW_TYPE_EMAIL_IDENTITY, aSessionId);

        // Note: username, password and bind_email must not be set in registrationParams
        mUsername = null;
        mPassword = null;
        clearThreePid();

        register(context, registrationParams, new InternalRegistrationListener() {
            @Override
            public void onRegistrationSuccess() {
                listener.onRegistrationSuccess(null);
            }

            @Override
            public void onRegistrationFailed(String message) {
                if (TextUtils.equals(ERROR_MISSING_STAGE, message)) {
                    // At this point, only captcha can be the missing stage
                    listener.onWaitingCaptcha();
                } else {
                    listener.onRegistrationFailed(message);
                }
            }

            @Override
            public void onResourceLimitExceeded(MatrixError e) {
                listener.onResourceLimitExceeded(e);
            }
        });
    }

    /**
     * Check if the given stage has been completed
     *
     * @param stage
     * @return true if completed
     */
    private boolean isCompleted(final String stage) {
        return mRegistrationResponse != null && mRegistrationResponse.completed != null && mRegistrationResponse.completed.contains(stage);
    }

    /**
     * @return true if captcha is mandatory for registration and not completed yet
     */
    private boolean isCaptchaRequired() {
        // No Captcha in Tchap for the moment
        return false;
    }

    /**
     * Submit the token for the given three pid
     *
     * @param token
     * @param pid
     * @param listener
     */
    public void submitValidationToken(final String token, final ThreePid pid, final ThreePidValidationListener listener) {
        if (getThirdPidRestClient() != null) {
            pid.submitValidationToken(getThirdPidRestClient(), token, pid.clientSecret, pid.sid, new ApiCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean isSuccess) {
                    listener.onThreePidValidated(isSuccess);
                }

                @Override
                public void onNetworkError(Exception e) {
                    listener.onThreePidValidated(false);
                }

                @Override
                public void onMatrixError(MatrixError e) {
                    listener.onThreePidValidated(false);
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    listener.onThreePidValidated(false);
                }
            });
        }
    }

    /**
     * Get the public key for captcha registration
     *
     * @return public key
     */
    public String getCaptchaPublicKey() {
        String publicKey = null;
        if (mRegistrationResponse != null && mRegistrationResponse.params != null) {
            Object recaptchaParamsAsVoid = mRegistrationResponse.params.get(LoginRestClient.LOGIN_FLOW_TYPE_RECAPTCHA);
            if (null != recaptchaParamsAsVoid) {
                try {
                    Map<String, String> recaptchaParams = (Map<String, String>) recaptchaParamsAsVoid;
                    publicKey = recaptchaParams.get(JSON_KEY_PUBLIC_KEY);

                } catch (Exception e) {
                    Log.e(LOG_TAG, "getCaptchaPublicKey: " + e.getLocalizedMessage(), e);
                }
            }
        }
        return publicKey;
    }

    /**
     * Add email three pid to singleton values
     * It will be processed later on
     *
     * @param emailThreePid
     */
    public void addEmailThreePid(final ThreePid emailThreePid) {
        mEmail = emailThreePid;
    }

    /**
     * Get the current email three pid (if any).
     *
     * @return the corresponding three pid
     */
    public ThreePid getEmailThreePid() {
        return mEmail;
    }

    /**
     * Add phone number to the registration process by requesting token first
     *
     * @param phoneNumber
     * @param countryCode
     * @param listener
     */
    public void addPhoneNumberThreePid(final String phoneNumber, final String countryCode, final ThreePidRequestListener listener) {
        final ThreePid pid = new ThreePid(phoneNumber, countryCode, ThreePid.MEDIUM_MSISDN);
        requestValidationToken(pid, listener);
    }

    /**
     * Clear three pids from singleton values
     */
    public void clearThreePid() {
        mEmail = null;
        mPhoneNumber = null;
        mShowThreePidWarning = false;
    }

    /*
     * *********************************************************************************************
     * Private methods
     * *********************************************************************************************
     */

    /**
     * Get a login rest client
     *
     * @return login rest client
     */
    private LoginRestClient getLoginRestClient() {
        if (mLoginRestClient == null && mHsConfig != null) {
            mLoginRestClient = new LoginRestClient(mHsConfig);
        }
        return mLoginRestClient;
    }

    /**
     * Get a third pid rest client
     *
     * @return third pid rest client
     */
    private ThirdPidRestClient getThirdPidRestClient() {
        if (mThirdPidRestClient == null && mHsConfig != null) {
            mThirdPidRestClient = new ThirdPidRestClient(mHsConfig);
        }
        return mThirdPidRestClient;
    }

    /**
     * Get a profile rest client
     *
     * @return third pid rest client
     */
    private ProfileRestClient getProfileRestClient() {
        if (mProfileRestClient == null && mHsConfig != null) {
            mProfileRestClient = new ProfileRestClient(mHsConfig);
        }
        return mProfileRestClient;
    }

    /**
     * Set the flow stages for the current home server
     *
     * @param registrationFlowResponse
     */
    private void setRegistrationFlowResponse(final RegistrationFlowResponse registrationFlowResponse) {
        if (registrationFlowResponse != null) {
            mRegistrationResponse = registrationFlowResponse;
        }
    }

    /**
     * Format three pid params for registration request
     *
     * @param clientSecret
     * @param host
     * @param sid          received by requestToken request
     * @param medium       type of three pid
     * @param sessionId    session id
     * @return map of params
     */
    private Map<String, Object> getThreePidAuthParams(final String clientSecret, final String host,
                                                      final String sid, final String medium, final String sessionId) {
        Map<String, Object> authParams = new HashMap<>();
        Map<String, String> pidsCredentialsAuth = new HashMap<>();
        pidsCredentialsAuth.put(JSON_KEY_CLIENT_SECRET, clientSecret);
        pidsCredentialsAuth.put(JSON_KEY_ID_SERVER, host);
        pidsCredentialsAuth.put(JSON_KEY_SID, sid);
        authParams.put(JSON_KEY_TYPE, medium);
        authParams.put(JSON_KEY_THREEPID_CREDS, pidsCredentialsAuth);
        authParams.put(JSON_KEY_SESSION, sessionId);
        return authParams;
    }

    /**
     * Format captcha params for registration request
     *
     * @param captchaResponse
     * @return
     */
    private Map<String, Object> getCaptchaAuthParams(final String captchaResponse) {
        Map<String, Object> authParams = new HashMap<>();
        authParams.put(JSON_KEY_TYPE, LoginRestClient.LOGIN_FLOW_TYPE_RECAPTCHA);
        authParams.put(JSON_KEY_CAPTCHA_RESPONSE, captchaResponse);
        authParams.put(JSON_KEY_SESSION, mRegistrationResponse.session);
        return authParams;
    }

    /**
     * Request a validation token for the given three pid
     *
     * @param pid
     * @param listener
     */
    private void requestValidationToken(final ThreePid pid, final ThreePidRequestListener listener) {
        if (getThirdPidRestClient() != null) {
            switch (pid.medium) {
                case ThreePid.MEDIUM_EMAIL:
                    String nextLinkBase = mHsConfig.getHomeserverUri().toString();
                    String nextLink = nextLinkBase + "/#/register?client_secret="+ pid.clientSecret;
                    nextLink += "&hs_url=" + mHsConfig.getHomeserverUri().toString();
                    nextLink += "&is_url=" + mHsConfig.getIdentityServerUri().toString();
                    nextLink += "&session_id=" + mRegistrationResponse.session;
                    pid.requestEmailValidationToken(getProfileRestClient(), nextLink, true, new ApiCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            listener.onThreePidRequested(pid);
                        }

                        @Override
                        public void onNetworkError(final Exception e) {
                            warnAfterCertificateError(e, pid, listener);
                        }

                        @Override
                        public void onUnexpectedError(Exception e) {
                            listener.onThreePidRequested(pid);
                        }

                        @Override
                        public void onMatrixError(MatrixError e) {
                            if (TextUtils.equals(MatrixError.THREEPID_IN_USE, e.errcode)) {
                                listener.onThreePidRequestFailed(R.string.account_email_already_used_error);
                            } else {
                                listener.onThreePidRequested(pid);
                            }
                        }
                    });
                    break;
                case ThreePid.MEDIUM_MSISDN:
                    pid.requestPhoneNumberValidationToken(getProfileRestClient(), true, new ApiCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mPhoneNumber = pid;
                            listener.onThreePidRequested(pid);
                        }

                        @Override
                        public void onNetworkError(final Exception e) {
                            warnAfterCertificateError(e, pid, listener);
                        }

                        @Override
                        public void onUnexpectedError(Exception e) {
                            listener.onThreePidRequested(pid);
                        }

                        @Override
                        public void onMatrixError(MatrixError e) {
                            if (TextUtils.equals(MatrixError.THREEPID_IN_USE, e.errcode)) {
                                listener.onThreePidRequestFailed(R.string.account_phone_number_already_used_error);
                            } else {
                                listener.onThreePidRequested(pid);
                            }
                        }
                    });
                    break;
            }
        }
    }

    /**
     * Display warning dialog in case of certificate error
     *
     * @param e        the exception
     * @param pid
     * @param listener
     */
    private void warnAfterCertificateError(final Exception e, final ThreePid pid, final ThreePidRequestListener listener) {
        UnrecognizedCertificateException unrecCertEx = CertUtil.getCertificateException(e);
        if (unrecCertEx != null) {
            final Fingerprint fingerprint = unrecCertEx.getFingerprint();

            UnrecognizedCertHandler.show(mHsConfig, fingerprint, false, new UnrecognizedCertHandler.Callback() {
                @Override
                public void onAccept() {
                    requestValidationToken(pid, listener);
                }

                @Override
                public void onIgnore() {
                    listener.onThreePidRequested(pid);
                }

                @Override
                public void onReject() {
                    listener.onThreePidRequested(pid);
                }
            });
        } else {
            listener.onThreePidRequested(pid);
        }
    }

    /**
     * Send a registration request with the given parameters
     *
     * @param context
     * @param params   registration params
     * @param listener
     */
    private void register(final Context context, final RegistrationParams params, final InternalRegistrationListener listener) {
        if (getLoginRestClient() != null) {
            params.initial_device_display_name = context.getString(R.string.login_mobile_device);
            mLoginRestClient.register(params, new UnrecognizedCertApiCallback<Credentials>(mHsConfig) {
                @Override
                public void onSuccess(Credentials credentials) {
                    if (TextUtils.isEmpty(credentials.userId)) {
                        listener.onRegistrationFailed(ERROR_EMPTY_USER_ID);
                    } else {
                        // Initiate login process
                        Collection<MXSession> sessions = Matrix.getMXSessions(context);
                        boolean isDuplicated = false;

                        for (MXSession existingSession : sessions) {
                            Credentials cred = existingSession.getCredentials();
                            isDuplicated |= TextUtils.equals(credentials.userId, cred.userId) && TextUtils.equals(credentials.homeServer, cred.homeServer);
                        }

                        if (null == mHsConfig) {
                            listener.onRegistrationFailed("null mHsConfig");
                        } else {
                            if (!isDuplicated) {
                                mHsConfig.setCredentials(credentials);
                                MXSession session = Matrix.getInstance(context).createSession(mHsConfig);
                                Matrix.getInstance(context).addSession(session);
                            }

                            listener.onRegistrationSuccess();
                        }
                    }
                }

                @Override
                public void onAcceptedCert() {
                    register(context, params, listener);
                }

                @Override
                public void onTLSOrNetworkError(final Exception e) {
                    listener.onRegistrationFailed(e.getLocalizedMessage());
                }

                @Override
                public void onMatrixError(MatrixError e) {
                    if (TextUtils.equals(e.errcode, MatrixError.USER_IN_USE)) {
                        // user name is already taken, the registration process stops here (new user name should be provided)
                        // ex: {"errcode":"M_USER_IN_USE","error":"User ID already taken."}
                        Log.d(LOG_TAG, "User name is used");
                        listener.onRegistrationFailed(MatrixError.USER_IN_USE);
                    } else if (TextUtils.equals(e.errcode, MatrixError.UNAUTHORIZED)) {
                        // happens while polling email validation, do nothing
                    } else if (null != e.mStatus && e.mStatus == 401) {
                        try {
                            RegistrationFlowResponse registrationFlowResponse = JsonUtils.toRegistrationFlowResponse(e.mErrorBodyAsString);
                            setRegistrationFlowResponse(registrationFlowResponse);
                        } catch (Exception castExcept) {
                            Log.e(LOG_TAG, "JsonUtils.toRegistrationFlowResponse " + castExcept.getLocalizedMessage(), castExcept);
                        }
                        listener.onRegistrationFailed(ERROR_MISSING_STAGE);
                    } else if (TextUtils.equals(e.errcode, MatrixError.RESOURCE_LIMIT_EXCEEDED)) {
                        listener.onResourceLimitExceeded(e);
                    } else {
                        listener.onRegistrationFailed("");
                    }
                }
            });
        }
    }

    /*
     * *********************************************************************************************
     * Private listeners
     * *********************************************************************************************
     */

    private interface InternalRegistrationListener {
        void onRegistrationSuccess();

        void onRegistrationFailed(String message);

        void onResourceLimitExceeded(MatrixError e);
    }

    /*
     * *********************************************************************************************
     * Public listeners
     * *********************************************************************************************
     */

    public interface ThreePidRequestListener {
        void onThreePidRequested(ThreePid pid);

        void onThreePidRequestFailed(@StringRes int errorMessageRes);
    }

    public interface ThreePidValidationListener {
        void onThreePidValidated(boolean isSuccess);
    }

    public interface RegistrationListener {
        void onRegistrationSuccess(String warningMessage);

        void onRegistrationFailed(String message);

        void onWaitingEmailValidation();

        void onWaitingCaptcha();

        void onThreePidRequestFailed(String message);

        void onResourceLimitExceeded(MatrixError e);
    }
}