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
package fr.gouv.tchap.sdk.rest.client;

import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.RestClient;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.rest.DefaultRetrofit2CallbackWrapper;

import fr.gouv.tchap.sdk.rest.api.TchapValidityApi;


public class TchapValidityRestClient extends RestClient<TchapValidityApi> {

    /**
     * {@inheritDoc}
     */
    public TchapValidityRestClient(HomeServerConnectionConfig hsConfig) {
        super(hsConfig, TchapValidityApi.class, URI_API_PREFIX_PATH_UNSTABLE, false, false);
    }

    /**
     * Request a renewal email
     * @param callback the callback
     */
    public void requestRenewalEmail(final ApiCallback<Void> callback) {
        mApi.requestRenewalEmail().enqueue(new DefaultRetrofit2CallbackWrapper<>(callback));
    }
}
