/*
 * Copyright 2020 New Vector Ltd
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
package fr.gouv.tchap.sdk.rest.client

import fr.gouv.tchap.sdk.rest.api.TchapUserInfoApi
import fr.gouv.tchap.sdk.rest.model.UserStatusInfo
import org.matrix.androidsdk.HomeServerConnectionConfig
import org.matrix.androidsdk.RestClient
import org.matrix.androidsdk.core.callback.ApiCallback
import org.matrix.androidsdk.core.json.GsonProvider
import org.matrix.androidsdk.rest.callback.RestAdapterCallback


class TchapUserInfoRestClient(hsConfig: HomeServerConnectionConfig) : RestClient<TchapUserInfoApi>(hsConfig,
        TchapUserInfoApi::class.java,
        RestClient.URI_API_PREFIX_PATH_R0,
        GsonProvider.provideKotlinGson(),
        false) {

    /**
     * Get the expiration and deactivation information about a given user.
     * @param userId the user identifier
     * @param callback the callback
     */
    fun getUserStatusInfo(userId: String, callback: ApiCallback<UserStatusInfo>) {
        mApi.getUserStatusInfo(userId)
                .enqueue(RestAdapterCallback<UserStatusInfo>("getUserStatusInfo", null, callback, null))
    }
}
