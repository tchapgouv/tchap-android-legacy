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

import android.content.Context
import androidx.core.content.edit
import org.jetbrains.anko.defaultSharedPreferences

object VersionCheckResultStore {

    private const val VERSION_CHECK_RESULT_STORE_TYPE = "VERSION_CHECK_RESULT_STORE_TYPE"
    private const val VERSION_CHECK_RESULT_STORE_MESSAGE = "VERSION_CHECK_RESULT_STORE_MESSAGE"
    private const val VERSION_CHECK_RESULT_STORE_FOR_VERSION_CODE = "VERSION_CHECK_RESULT_STORE_FOR_VERSION_CODE"
    private const val VERSION_CHECK_RESULT_STORE_DISPLAY_ONLY_ONCE = "VERSION_CHECK_RESULT_STORE_DISPLAY_ONLY_ONCE"
    private const val VERSION_CHECK_RESULT_STORE_CAN_OPEN_APP = "VERSION_CHECK_RESULT_STORE_CAN_OPEN_APP"

    fun read(context: Context): VersionCheckResult {
        return when (context.defaultSharedPreferences.getInt(VERSION_CHECK_RESULT_STORE_TYPE, 0)) {
            1    -> VersionCheckResult.Ok
            2    -> VersionCheckResult.ShowUpgradeScreen(
                    message = context.defaultSharedPreferences.getString(VERSION_CHECK_RESULT_STORE_MESSAGE, "") ?: "",
                    forVersionCode = context.defaultSharedPreferences.getInt(VERSION_CHECK_RESULT_STORE_FOR_VERSION_CODE, 0),
                    displayOnlyOnce = context.defaultSharedPreferences.getBoolean(VERSION_CHECK_RESULT_STORE_DISPLAY_ONLY_ONCE, false),
                    canOpenApp = context.defaultSharedPreferences.getBoolean(VERSION_CHECK_RESULT_STORE_CAN_OPEN_APP, false))
            else -> VersionCheckResult.Unknown
        }
    }

    fun write(context: Context, versionCheckResult: VersionCheckResult) {
        context.defaultSharedPreferences.edit {
            when (versionCheckResult) {
                is VersionCheckResult.Ok                -> {
                    putInt(VERSION_CHECK_RESULT_STORE_TYPE, 1)
                    remove(VERSION_CHECK_RESULT_STORE_MESSAGE)
                    remove(VERSION_CHECK_RESULT_STORE_FOR_VERSION_CODE)
                    remove(VERSION_CHECK_RESULT_STORE_DISPLAY_ONLY_ONCE)
                    remove(VERSION_CHECK_RESULT_STORE_CAN_OPEN_APP)
                }
                is VersionCheckResult.ShowUpgradeScreen -> {
                    putInt(VERSION_CHECK_RESULT_STORE_TYPE, 2)
                    putString(VERSION_CHECK_RESULT_STORE_MESSAGE, versionCheckResult.message)
                    putInt(VERSION_CHECK_RESULT_STORE_FOR_VERSION_CODE, versionCheckResult.forVersionCode)
                    putBoolean(VERSION_CHECK_RESULT_STORE_DISPLAY_ONLY_ONCE, versionCheckResult.displayOnlyOnce)
                    putBoolean(VERSION_CHECK_RESULT_STORE_CAN_OPEN_APP, versionCheckResult.canOpenApp)
                }
                else                                    -> {
                    putInt(VERSION_CHECK_RESULT_STORE_TYPE, 0)
                    remove(VERSION_CHECK_RESULT_STORE_MESSAGE)
                    remove(VERSION_CHECK_RESULT_STORE_FOR_VERSION_CODE)
                    remove(VERSION_CHECK_RESULT_STORE_DISPLAY_ONLY_ONCE)
                    remove(VERSION_CHECK_RESULT_STORE_CAN_OPEN_APP)
                }
            }
        }
    }

}