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
package fr.gouv.tchap.sdk.rest.client;

import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.RestClient;
import org.matrix.androidsdk.core.JsonUtils;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.rest.callback.RestAdapterCallback;

import fr.gouv.tchap.sdk.rest.api.TchapApi;
import fr.gouv.tchap.sdk.rest.model.Platform;


public class TchapRestClient extends RestClient<TchapApi> {

    /**
     * {@inheritDoc}
     */
    public TchapRestClient(HomeServerConnectionConfig hsConfig) {
        super(hsConfig, TchapApi.class, URI_API_PREFIX_IDENTITY, JsonUtils.getGson(false), true);
    }

    /**
     * Retrieve the Tchap platform from a 3rd party id.
     * @param address 3rd party id
     * @param medium the media.
     * @param callback the callback
     */
    public void info(String address, String medium, final ApiCallback<Platform> callback) {
        mApi.info(address, medium).enqueue(new RestAdapterCallback<>("platformInfo", null, callback, null));
    }
}
