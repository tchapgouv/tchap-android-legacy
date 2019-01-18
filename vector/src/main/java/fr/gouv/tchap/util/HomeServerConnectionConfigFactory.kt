/*
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

package fr.gouv.tchap.util

import android.net.Uri
import android.os.Build
import android.support.annotation.VisibleForTesting
import fr.gouv.tchap.config.CERTIFICATE_FINGERPRINT_LIST
import fr.gouv.tchap.config.ENABLE_CERTIFICATE_PINNING
import fr.gouv.tchap.config.ENABLE_PROTECTED_ACCESS
import fr.gouv.tchap.config.currentVersion
import fr.gouv.tchap.model.TchapConnectionConfig
import fr.gouv.tchap.sdk.rest.model.Platform
import org.matrix.androidsdk.HomeServerConnectionConfig
import org.matrix.androidsdk.ssl.Fingerprint
import java.lang.Long.parseLong

/**
 * Create a TchapConnectionConfig from a Tchap platform description.
 * @return a Tchap config with all the required parameters for the Tchap application.
 */
fun createTchapConnectionConfig(email: String, tchapPlatform: Platform, serverURLPrefix: String): TchapConnectionConfig? {
    val config = createHomeServerConnectionConfig(tchapPlatform, serverURLPrefix)
    val hasProtectedAccess  = hasProtectedAccess(tchapPlatform)
    val shadowConfig = createShadowHomeServerConnectionConfig(tchapPlatform, serverURLPrefix)
    val version = TchapConnectionConfig.currentVersion()

    if (config != null) {
        return TchapConnectionConfig(config, email, hasProtectedAccess, shadowConfig, version)
    }
    return null;
}

/**
 * Create a TchapConnectionConfig based on a Home server url, and a potential Identity server url.
 * @return a Tchap config with all the required parameters for the Tchap application.
 */
fun createTchapConnectionConfig(homeServerUrl: String, identityServerUrl: String?): TchapConnectionConfig? {
    val config = createHomeServerConnectionConfig(homeServerUrl, identityServerUrl)

    if (config != null) {
        return TchapConnectionConfig(config)
    }
    return null;
}

/**
 * Create a HomeServerConnectionConfig with all the required parameters for the Tchap application
 */
fun createHomeServerConnectionConfig(homeServerUrl: String, identityServerUrl: String?): HomeServerConnectionConfig {
    return HomeServerConnectionConfig.Builder()
            .withHomeServerUri(Uri.parse(homeServerUrl))
            .apply {

                identityServerUrl?.let {
                    withIdentityServerUri(Uri.parse(identityServerUrl))
                }

                for (certificateFingerprint in CERTIFICATE_FINGERPRINT_LIST) {
                    addAllowedFingerPrint(Fingerprint(Fingerprint.HashType.SHA256, getBytesFromString(certificateFingerprint)))
                }
            }
            .withTlsLimitations(true, Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
            .withPin(ENABLE_CERTIFICATE_PINNING)
            .build()
}

/**
 * Create the HomeServerConnectionConfig with all the required parameters for the main home server defined in Tchap platform.
 */
fun createHomeServerConnectionConfig(tchapPlatform: Platform, serverURLPrefix: String): HomeServerConnectionConfig? {
    val homeServerUrl = getHomeServerUrl(tchapPlatform, serverURLPrefix)
    if (homeServerUrl != null) {
        val identityServerUrl = getIdentityServerUrl(tchapPlatform, serverURLPrefix)
        return createHomeServerConnectionConfig(homeServerUrl, identityServerUrl)
    }
    return null
}

/**
 * Create the HomeServerConnectionConfig with all the required parameters for the potential shadow server.
 */
fun createShadowHomeServerConnectionConfig(tchapPlatform: Platform, serverURLPrefix: String): HomeServerConnectionConfig? {
    val homeServerUrl = getShadowHomeServerUrl(tchapPlatform, serverURLPrefix)
    if (homeServerUrl != null) {
        val identityServerUrl = getShadowIdentityServerUrl(tchapPlatform, serverURLPrefix)
        return createHomeServerConnectionConfig(homeServerUrl, identityServerUrl)
    }
    return null
}

/**
 * @return the main home server url from a Tchap platform description.
 */
fun getHomeServerUrl(tchapPlatform: Platform, serverURLPrefix: String): String? {
    var hs = tchapPlatform.hs

    // Fallback to the shadow hs (if any) when the protected access is not supported by the client.
    if (!ENABLE_PROTECTED_ACCESS && tchapPlatform.shadowHs != null) {
        hs = tchapPlatform.shadowHs
    }

    return if (hs != null && !hs.isEmpty()) serverURLPrefix + hs else null
}

/**
 * @return the main identity server url from a Tchap platform description.
 */
fun getIdentityServerUrl(tchapPlatform: Platform, serverURLPrefix: String): String? = getHomeServerUrl(tchapPlatform, serverURLPrefix)

/**
 * @return the url of the potential shadow home server from a Tchap platform description.
 */
private fun getShadowHomeServerUrl(tchapPlatform: Platform, serverURLPrefix: String): String? {
    // Check whether the protected access is supported by the client.
    if (ENABLE_PROTECTED_ACCESS) {
        val hs = tchapPlatform.shadowHs
        return if (hs != null && !hs.isEmpty()) serverURLPrefix + hs else null
    }
    return null
}

/**
 * @return the url of the potential shadow identity server from a Tchap platform description.
 */
private fun getShadowIdentityServerUrl(tchapPlatform: Platform, serverURLPrefix: String): String? = getShadowHomeServerUrl(tchapPlatform, serverURLPrefix)

/**
 * @return true when the platform description corresponds to an account with a protected access
 */
private fun hasProtectedAccess(tchapPlatform: Platform): Boolean {
    // Check whether the protected access is supported by the client.
    if (ENABLE_PROTECTED_ACCESS) {
        // The platform description corresponds to an account with a protected access
        // when the "shadow_hs" key is defined (even with an empty value).
        return tchapPlatform.shadowHs != null
    }
    return false
}

@VisibleForTesting
fun getBytesFromString(certificateFingerprint: String): ByteArray {
    // Note: will be reassigned with correct size later
    var bytes = ByteArray(0)

    var idx = 0

    certificateFingerprint.replace(":", "")
            .chunked(2)
            .also {
                bytes = ByteArray(it.size)
            }
            .map {
                bytes[idx++] = parseLong(it, 16).toByte()
            }

    return bytes
}