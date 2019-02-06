/*
 * Copyright 2019 New Vector Ltd
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

package fr.gouv.tchap.model

import org.json.JSONException
import org.json.JSONObject
import org.matrix.androidsdk.HomeServerConnectionConfig

/**
 * Class used to store the matrix home server(s) configuration for a Tchap account:
 *  - hsConfig: the main homeserver config (this corresponds to the protected hs when the account has
 *  access to the protected infra).
 *  - email: the email address used to create the account.
 *  - hasProtectedAccess: tell whether the account has access to the protected infra.
 *  - shadowHSConfig: the config of the optional secondary homeserver (this corresponds to the agent hs, if any).
 *  - version: the configuration version (see the currentVersion define in TargetConfiguration)
 *     - DEFAULT_VERSION: the Tchap config has been created without knowing if the corresponding
 *     account has access to the protected infra.
 *     - use any other version values to handle Tchap config migration.
 */
data class TchapConnectionConfig(
        val hsConfig: HomeServerConnectionConfig,
        val email: String? = null,
        val hasProtectedAccess: Boolean = false,
        val shadowHSConfig: HomeServerConnectionConfig? = null,
        val version: Int = DEFAULT_VERSION) {

    @Throws (JSONException::class)
    fun toJson() = hsConfig.toJson().apply {
        put(EMAIL_ADDRESS_KEY, email)
        put(HAS_PROTECTED_ACCESS_KEY, hasProtectedAccess)
        if (shadowHSConfig != null) {
            put(SHADOW_HS_CONFIG_KEY, shadowHSConfig.toJson())
        }
        put(VERSION_KEY, version)
    }

    // Take into account the provided HS config by returning a new instance in which the config
    // of the homeserver related to the same user id has been replaced.
    fun replaceHSConfig(updatedHSConfig: HomeServerConnectionConfig): TchapConnectionConfig?{
        if (hsConfig.credentials?.userId.equals(updatedHSConfig.credentials?.userId)) {
            return TchapConnectionConfig(updatedHSConfig, email, hasProtectedAccess, shadowHSConfig, version)
        } else if (shadowHSConfig?.credentials?.userId.equals(updatedHSConfig.credentials?.userId)) {
            return TchapConnectionConfig(hsConfig, email, hasProtectedAccess, updatedHSConfig, version)
        }
        return null
    }

    companion object {
        private const val DEFAULT_VERSION = 0
        private const val EMAIL_ADDRESS_KEY = "EMAIL_ADDRESS"
        private const val HAS_PROTECTED_ACCESS_KEY = "HAS_PROTECTED_ACCESS"
        private const val SHADOW_HS_CONFIG_KEY = "SHADOW_HS_CONFIG"
        private const val VERSION_KEY = "VERSION"

        fun fromJson(json: JSONObject): TchapConnectionConfig {
            val config = HomeServerConnectionConfig.fromJson(json)

            // Check whether the provided JSON Object corresponds to an account stored before the migration.
            if (json.has(EMAIL_ADDRESS_KEY) && json.has(HAS_PROTECTED_ACCESS_KEY) && json.has(VERSION_KEY)) {
                val email = json.getString(EMAIL_ADDRESS_KEY)
                val hasProtectedAccess = json.has(HAS_PROTECTED_ACCESS_KEY)
                val version = json.getInt(VERSION_KEY)

                if (json.has(SHADOW_HS_CONFIG_KEY)) {
                    val shadowConfig = HomeServerConnectionConfig.fromJson(json.getJSONObject(SHADOW_HS_CONFIG_KEY))
                    return TchapConnectionConfig(config, email, hasProtectedAccess, shadowConfig, version)
                } else {
                    return TchapConnectionConfig(config, email, hasProtectedAccess, null, version)
                }
            } else {
                // A Tchap config is created with this existing config and the default values for email, hasProtectedAccess, shadowHSConfig and version.
                return TchapConnectionConfig(config)
            }
        }
    }
}