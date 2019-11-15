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

package fr.gouv.tchap.version

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.edit
import fr.gouv.tchap.sdk.rest.client.TchapConfigRestClient
import fr.gouv.tchap.sdk.rest.model.MinVersion
import fr.gouv.tchap.sdk.rest.model.TchapClientConfig
import im.vector.BuildConfig
import im.vector.R
import org.jetbrains.anko.defaultSharedPreferences
import org.matrix.androidsdk.core.Log
import org.matrix.androidsdk.core.callback.ApiCallback
import org.matrix.androidsdk.core.callback.SuccessCallback
import org.matrix.androidsdk.core.model.MatrixError
import java.util.*

object VersionChecker {

    private const val LAST_VERSION_CHECK_TS_KEY = "LAST_VERSION_CHECK_TS_KEY"
    private const val LAST_VERSION_INFO_DISPLAYED_VERSION_CODE = "LAST_VERSION_INFO_DISPLAYED_CODE"
    private const val LOG_TAG = "VersionChecker"

    private const val MIN_DELAY_BETWEEN_TWO_REQUEST_MILLIS = 1 * 86_400_000 // TimeUnit.DAYS.toMillis(1)

    private val restClient = TchapConfigRestClient()

    private lateinit var lastVersionCheckResult: VersionCheckResult

    fun checkVersion(context: Context, callback: SuccessCallback<VersionCheckResult>) {
        val lastDayCheck = context.defaultSharedPreferences.getLong(LAST_VERSION_CHECK_TS_KEY, 0)
        val currentDay = getCurrentDayMillis()

        lastVersionCheckResult = VersionCheckResultStore.read(context)

        if (lastVersionCheckResult is VersionCheckResult.Unknown
                || currentDay >= lastDayCheck + MIN_DELAY_BETWEEN_TWO_REQUEST_MILLIS) {
            restClient.getClientConfig(object : ApiCallback<TchapClientConfig> {
                override fun onSuccess(info: TchapClientConfig) {
                    context.defaultSharedPreferences.edit {
                        putLong(LAST_VERSION_CHECK_TS_KEY, currentDay)
                    }

                    lastVersionCheckResult = info.toVersionCheckResult(context)
                    VersionCheckResultStore.write(context, lastVersionCheckResult)

                    answer(context, callback)
                }

                override fun onUnexpectedError(e: Exception) {
                    Log.e(LOG_TAG, "error", e)
                    answer(context, callback)
                }

                override fun onMatrixError(e: MatrixError) {
                    Log.e(LOG_TAG, "error $e")
                    answer(context, callback)
                }

                override fun onNetworkError(e: Exception) {
                    Log.e(LOG_TAG, "error", e)
                    answer(context, callback)
                }
            })
        } else {
            answer(context, callback)
        }
    }

    private fun answer(context: Context, callback: SuccessCallback<VersionCheckResult>) {
        when (val final = lastVersionCheckResult) {
            is VersionCheckResult.Ok,
            is VersionCheckResult.Unknown           -> callback.onSuccess(final)
            is VersionCheckResult.ShowUpgradeScreen -> {
                // Check if already displayed
                if (final.displayOnlyOnce
                        && context.defaultSharedPreferences.getInt(LAST_VERSION_INFO_DISPLAYED_VERSION_CODE, 0) == final.forVersionCode) {
                    // Already displayed
                    callback.onSuccess(VersionCheckResult.Ok)
                } else {
                    callback.onSuccess(final)
                }
            }
        }
    }

    /**
     * Return the timestamp of the current day at 5 AM
     */
    private fun getCurrentDayMillis(): Long {
        return Calendar.getInstance()
                .apply {
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    set(Calendar.HOUR_OF_DAY, 5)
                }
                .timeInMillis
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
                criticalMinVersion?.toShowUpgradeScreen(context, R.string.an_update_is_available_critical)
            mandatoryMinVersion?.minVersionCode ?: 0 > BuildConfig.VERSION_CODE ->
                // A mandatory update is available
                mandatoryMinVersion?.toShowUpgradeScreen(context, R.string.an_update_is_available_mandatory)
            infoMinVersion?.minVersionCode ?: 0 > BuildConfig.VERSION_CODE      ->
                infoMinVersion?.toShowUpgradeScreen(context, R.string.an_update_is_available_info)
            else                                                                ->
                // No update available
                VersionCheckResult.Ok
        } ?: VersionCheckResult.Unknown
    }

    private fun MinVersion.toShowUpgradeScreen(context: Context, @StringRes defaultMessageResId: Int): VersionCheckResult.ShowUpgradeScreen {
        return VersionCheckResult.ShowUpgradeScreen(
                message = message?.get(context.getString(R.string.resources_language)).takeIf { it.isNullOrEmpty().not() }
                        ?: message?.get("default").takeIf { it.isNullOrEmpty().not() }
                        ?: context.getString(defaultMessageResId),
                forVersionCode = minVersionCode ?: 0,
                displayOnlyOnce = displayOnlyOnce == true,
                canOpenApp = allowOpeningApp == true
        )
    }
}
