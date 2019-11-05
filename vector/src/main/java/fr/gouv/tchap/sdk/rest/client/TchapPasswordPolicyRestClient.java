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
import org.matrix.androidsdk.core.JsonUtils;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.rest.callback.RestAdapterCallback;

import fr.gouv.tchap.sdk.rest.api.TchapPasswordPolicyApi;
import fr.gouv.tchap.sdk.rest.model.PasswordPolicy;

public class TchapPasswordPolicyRestClient extends RestClient<TchapPasswordPolicyApi> {

    /**
     * {@inheritDoc}
     */
    public TchapPasswordPolicyRestClient(HomeServerConnectionConfig hsConfig) {
        super(hsConfig, TchapPasswordPolicyApi.class, URI_API_PREFIX_PATH_UNSTABLE, JsonUtils.getGson(false), false);
    }

    /**
     * Request the server's policy
     * @param callback the callback
     */
    public void getPasswordPolicy(final ApiCallback<PasswordPolicy> callback) {
        mApi.passwordPolicy().enqueue(new RestAdapterCallback<>("getPasswordPolicy", null, callback, null));
    }
}
