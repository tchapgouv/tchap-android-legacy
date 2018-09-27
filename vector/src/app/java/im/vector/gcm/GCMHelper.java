/*
 * Copyright 2014 OpenMarket Ltd
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
package im.vector.gcm;

import android.content.Context;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.google.firebase.iid.FirebaseInstanceId;

import org.matrix.androidsdk.util.Log;

public class GCMHelper {
    private static final String LOG_TAG = GCMHelper.class.getSimpleName();

    /**
     * Retrieves the FCM registration token.
     */
    public static String getRegistrationToken() {
        final String registrationToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(LOG_TAG, "## getRegistrationToken(): " + registrationToken);
        return registrationToken;
    }

    /**
     * Clear the registration token.
     */
    public static void clearRegistrationToken() {
        try {
            FirebaseInstanceId.getInstance().deleteInstanceId();
        } catch (Exception e) {
            Log.e(LOG_TAG, "##clearRegistrationToken() failed " + e.getMessage(), e);
        }
    }

    /**
     * Check that we have the last version of Google Play Services
     *
     * @param context
     */
    public static void checkLastVersion(Context context) {
        try {
            ProviderInstaller.installIfNeeded(context);
        } catch (GooglePlayServicesRepairableException e) {
            // Prompt the user to install/update/enable Google Play services.
            GoogleApiAvailability.getInstance().showErrorNotification(context, e.getConnectionStatusCode());
        } catch (GooglePlayServicesNotAvailableException e) {
            // Indicates a non-recoverable error: let the user know.
            Log.e(LOG_TAG, "GooglePlayServicesNotAvailableException", e);
        }
    }
}
