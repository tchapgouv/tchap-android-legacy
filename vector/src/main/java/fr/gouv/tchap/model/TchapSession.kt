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
package fr.gouv.tchap.model

import android.content.Context
import org.matrix.androidsdk.MXSession
import org.matrix.androidsdk.core.callback.ApiCallback
import org.matrix.androidsdk.core.model.MatrixError

data class TchapSession(
        val config: TchapConnectionConfig,
        val mainSession: MXSession,
        val shadowSession: MXSession? = null) {

    fun getSessions(): List<MXSession> {
        return listOf(mainSession, shadowSession).mapNotNull { it }
    }

    fun clear(context: Context) {
        mainSession.clear(context)
        shadowSession?.clear(context)
    }

    fun isAlive(): Boolean {
        var res = mainSession.isAlive
        shadowSession?.let {
            res = res and it.isAlive
        }
        return res
    }

    fun isReady(): Boolean {
        var res = mainSession.dataHandler.store.isReady
        shadowSession?.let {
            res = res and it.dataHandler.store.isReady
        }
        return res
    }

    fun getRoom(roomId: String): TchapRoom? {
        mainSession.dataHandler.getRoom(roomId, false)?.let {
            return TchapRoom(it, mainSession, config.hasProtectedAccess)
        }

        shadowSession?.let {
            it.dataHandler.getRoom(roomId, false)?.let {
                return TchapRoom(it, shadowSession)
            }
        }

        return null
    }

    fun getSummary(roomId: String): TchapRoomSummary? {
        mainSession.dataHandler.store.getSummary(roomId)?.let {
            return TchapRoomSummary(it, mainSession, config.hasProtectedAccess)
        }

        shadowSession?.let {
            it.dataHandler.store.getSummary(roomId)?.let {
                return TchapRoomSummary(it, shadowSession)
            }
        }

        return null
    }

    fun roomIdByAlias(roomAlias: String, callback: ApiCallback<String>) {
        mainSession.dataHandler.roomIdByAlias(roomAlias, object : ApiCallback<String> {
            override fun onSuccess(info: String) {
                callback.onSuccess(info)
            }

            override fun onNetworkError(e: Exception) {
                callback.onNetworkError(e)
            }

            override fun onMatrixError(e: MatrixError) {
                shadowSession?.let {
                    it.dataHandler.roomIdByAlias(roomAlias, callback)
                    return
                }
                callback.onMatrixError(e)
            }

            override fun onUnexpectedError(e: Exception) {
                shadowSession?.let {
                    it.dataHandler.roomIdByAlias(roomAlias, callback)
                    return
                }
                callback.onUnexpectedError(e)
            }
        })
    }
}

