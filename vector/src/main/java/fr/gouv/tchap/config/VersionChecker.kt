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
import fr.gouv.tchap.sdk.rest.client.TchapConfigRestClient
import fr.gouv.tchap.sdk.rest.model.TchapClientConfig
import im.vector.BuildConfig
import im.vector.R
import org.jetbrains.anko.defaultSharedPreferences
import org.matrix.androidsdk.core.Log
import org.matrix.androidsdk.core.callback.ApiCallback
import org.matrix.androidsdk.core.callback.SuccessCallback
import org.matrix.androidsdk.core.model.MatrixError
import java.util.concurrent.TimeUnit


object VersionChecker {

    private const val LAST_VERSION_CHECK_TS_KEY = "LAST_VERSION_CHECK_TS_KEY"
    private const val LAST_VERSION_INFO_DISPLAYED_VERSION_CODE = "LAST_VERSION_INFO_DISPLAYED_CODE"
    private const val LOG_TAG = "VersionChecker"

    private val MAX_DELAY_BETWEEN_TWO_REQUEST_MILLIS = TimeUnit.DAYS.toMillis(1)

    private val restClient = TchapConfigRestClient()

    private lateinit var lastVersionCheckResult: VersionCheckResult

    fun checkVersion(context: Context, callback: SuccessCallback<VersionCheckResult>) {
        val lastCheck = context.defaultSharedPreferences.getLong(LAST_VERSION_CHECK_TS_KEY, 0)

        lastVersionCheckResult = VersionCheckResultStore.read(context)

        if (lastVersionCheckResult is VersionCheckResult.Unknown
                || System.currentTimeMillis() > lastCheck + MAX_DELAY_BETWEEN_TWO_REQUEST_MILLIS) {
            restClient.getClientConfig(object : ApiCallback<TchapClientConfig> {
                override fun onSuccess(info: TchapClientConfig) {
                    context.defaultSharedPreferences.edit {
                        putLong(LAST_VERSION_CHECK_TS_KEY, System.currentTimeMillis())
                    }

                    lastVersionCheckResult = info.toVersionCheckResult(context)
                    VersionCheckResultStore.write(context, lastVersionCheckResult)

                    callback.onSuccess(lastVersionCheckResult)
                }

                override fun onUnexpectedError(e: Exception) {
                    Log.e(LOG_TAG, "error", e)
                    callback.onSuccess(lastVersionCheckResult)
                }

                override fun onMatrixError(e: MatrixError) {
                    Log.e(LOG_TAG, "error $e")
                    callback.onSuccess(lastVersionCheckResult)
                }

                override fun onNetworkError(e: Exception) {
                    Log.e(LOG_TAG, "error", e)
                    callback.onSuccess(lastVersionCheckResult)
                }
            })
        } else {
            callback.onSuccess(lastVersionCheckResult)
        }
    }

    fun onUpgradeScreenDisplayed(context: Context, forVersionCode: Int) {
        context.defaultSharedPreferences.edit {
            putInt(LAST_VERSION_INFO_DISPLAYED_VERSION_CODE, forVersionCode)
        }
    }

    private fun TchapClientConfig.toVersionCheckResult(context: Context): VersionCheckResult {
        val criticalMinVersion = minimumClientVersion?.criticalMinVersion
        val mandatoryMinVersion = minimumClientVersion?.mandatoryMinVersion
        val infoMinVersion = minimumClientVersion?.infoMinVersion

        return when {
            criticalMinVersion?.minVersionCode ?: 0 > BuildConfig.VERSION_CODE  ->
                // A critical update is available
                VersionCheckResult.ShowUpgradeScreen(
                        message = criticalMinVersion?.message?.get(context.getString(R.string.resources_language)).takeIf { it.isNullOrEmpty().not() }
                                ?: criticalMinVersion?.message?.get("default").takeIf { it.isNullOrEmpty().not() }
                                ?: context.getString(R.string.an_update_is_available_critical),
                        forVersionCode = criticalMinVersion?.minVersionCode ?: 0,
                        canOpenApp = false
                )
            mandatoryMinVersion?.minVersionCode ?: 0 > BuildConfig.VERSION_CODE ->
                // A mandatory update is available
                VersionCheckResult.ShowUpgradeScreen(
                        message = mandatoryMinVersion?.message?.get(context.getString(R.string.resources_language)).takeIf { it.isNullOrEmpty().not() }
                                ?: mandatoryMinVersion?.message?.get("default").takeIf { it.isNullOrEmpty().not() }
                                ?: context.getString(R.string.an_update_is_available_mandatory),
                        forVersionCode = mandatoryMinVersion?.minVersionCode ?: 0,
                        canOpenApp = mandatoryMinVersion?.allowOpeningApp == true
                )
            infoMinVersion?.minVersionCode ?: 0 > BuildConfig.VERSION_CODE      ->
                // An update is available
                if (infoMinVersion?.displayOnlyOnce == true
                        && context.defaultSharedPreferences.getInt(LAST_VERSION_INFO_DISPLAYED_VERSION_CODE, 0) == infoMinVersion.minVersionCode) {
                    // Already displayed
                    VersionCheckResult.Ok
                } else {
                    VersionCheckResult.ShowUpgradeScreen(
                            message = infoMinVersion?.message?.get(context.getString(R.string.resources_language)).takeIf { it.isNullOrEmpty().not() }
                                    ?: infoMinVersion?.message?.get("default").takeIf { it.isNullOrEmpty().not() }
                                    ?: context.getString(R.string.an_update_is_available_info),
                            forVersionCode = infoMinVersion?.minVersionCode ?: 0,
                            canOpenApp = infoMinVersion?.allowOpeningApp ?: true
                    )
                }
            else                                                                ->
                // No update available
                VersionCheckResult.Ok
        }
    }
}
