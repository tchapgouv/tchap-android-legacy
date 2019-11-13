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

package fr.gouv.tchap.config

sealed class VersionCheckResult {
    /**
     * Config file has not been or cannot be retrieved
     */
    object Unknown : VersionCheckResult()

    /**
     * The application is up to date, or no need to display the upgrade message
     */
    object Ok : VersionCheckResult()

    /**
     * the application is not up to date
     */
    data class ShowUpgradeScreen(
            val message: String,
            val forVersionCode: Int,
            val displayOnlyOnce: Boolean,
            val canOpenApp: Boolean) : VersionCheckResult()
}