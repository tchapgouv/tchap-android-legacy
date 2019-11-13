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
package fr.gouv.tchap.sdk.rest.client

import android.net.Uri
import fr.gouv.tchap.config.CONFIGURATION_VARIANT
import fr.gouv.tchap.sdk.rest.api.TchapConfigApi
import fr.gouv.tchap.sdk.rest.model.TchapClientConfig
import org.matrix.androidsdk.HomeServerConnectionConfig
import org.matrix.androidsdk.RestClient
import org.matrix.androidsdk.core.JsonUtils
import org.matrix.androidsdk.core.callback.ApiCallback
import org.matrix.androidsdk.rest.callback.RestAdapterCallback


class TchapConfigRestClient : RestClient<TchapConfigApi>(HomeServerConnectionConfig.Builder()
        .withHomeServerUri(Uri.parse("https://www.tchap.gouv.fr"))
        .build(),
        TchapConfigApi::class.java,
        "",
        JsonUtils.getKotlinGson(),
        false) {

    /**
     * Retrieve the Tchap config.
     */
    fun getClientConfig(callback: ApiCallback<TchapClientConfig>) {
        mApi.getTchapClientConfig(CONFIGURATION_VARIANT)
                .enqueue(RestAdapterCallback<TchapClientConfig>("getClientConfig", null, callback, null))
    }
}
