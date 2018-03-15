/* 
 * Copyright 2018 DINSIC
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
import org.matrix.androidsdk.rest.callback.ApiCallback;

import fr.gouv.tchap.sdk.rest.api.TchapApi;
import fr.gouv.tchap.sdk.rest.model.Platform;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class TchapRestClient extends RestClient<TchapApi> {

    /**
     * {@inheritDoc}
     */
    public TchapRestClient(HomeServerConnectionConfig hsConfig) {
        super(hsConfig, TchapApi.class, URI_API_PREFIX_IDENTITY, false, true);
    }

    /**
     * Retrieve the Tchap platform from a 3rd party id.
     * @param address 3rd party id
     * @param medium the media.
     * @param callback the callback
     */
    public void info(String address, String medium, final ApiCallback<Platform> callback) {
        try {
            mApi.info(address, medium, new Callback<Platform>() {
                @Override
                public void success(Platform platform, Response response) {
                    callback.onSuccess(platform);
                }

                @Override
                public void failure(RetrofitError error) {
                    callback.onUnexpectedError(error);
                }
            });
        }  catch (Throwable t) {
            callback.onUnexpectedError(new Exception(t));
        }
    }
}
