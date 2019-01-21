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

    fun getRoomWithId(roomId: String): TchapRoom? {
        mainSession.dataHandler.getRoom(roomId, false)?.let {
            return TchapRoom(it, mainSession, config.hasProtectedAccess)
        }

        shadowSession?.dataHandler?.getRoom(roomId, false)?.let {
            return TchapRoom(it, shadowSession, false)
        }

        return null
    }
}

