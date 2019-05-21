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
package fr.gouv.tchap.sdk.rest.api;

import org.matrix.androidsdk.rest.model.BulkLookupResponse;
import org.matrix.androidsdk.rest.model.pid.PidResponse;

import fr.gouv.tchap.sdk.rest.model.TchapBulkLookupParams;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface TchapThirdPidApi {

    /**
     * Proxies a 3PID lookup request to an identity server.
     * This route is used by clients in order to allow homeservers to deny 3PID lookups requests
     * to some or all of their users. It requires authentication using an access token.
     *
     * @param address the address.
     * @param medium  the medium.
     * @param idServer The hostname of the identity server to communicate with. May optionally include a port.
     */
    @GET("account/3pid/lookup")
    Call<PidResponse> lookup(@Query("address") String address,
                             @Query("medium") String medium,
                             @Query("id_server") String idServer);

    /**
     * Proxies a bunch of 3PIDs request to an identity server.
     *
     * @param body the body request
     */
    @POST("account/3pid/bulk_lookup")
    Call<BulkLookupResponse> bulkLookup(@Body TchapBulkLookupParams body);
}
