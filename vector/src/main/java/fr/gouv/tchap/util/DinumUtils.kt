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

package fr.gouv.tchap.util

import fr.gouv.tchap.sdk.session.room.model.*
import im.vector.BuildConfig
import org.matrix.androidsdk.MXSession
import org.matrix.androidsdk.core.Log
import org.matrix.androidsdk.core.callback.ApiCallback
import org.matrix.androidsdk.data.Room
import org.matrix.androidsdk.data.store.IMXStore
import java.util.*
import java.util.concurrent.TimeUnit

private val LOG_TAG = "DinumUtils"

//=============================================================================================
// Target
//=============================================================================================

fun isSecure(): Boolean {
    return BuildConfig.FLAVOR_target == "protecteed"
}

//=============================================================================================
// Room messages retention
//=============================================================================================

/**
 * Get the current room retention period in days.
 *
 * @param room the room.
 * @return the room retention period
 */
fun getRoomRetention(room: Room): Int {
    // Select the latest state event if any
    return room.state.getStateEvents(HashSet(Arrays.asList(EVENT_TYPE_STATE_ROOM_RETENTION)))
            .apply {
                sortBy { it.originServerTs }
            }
            .lastOrNull()
            ?.let {
                getMaxLifetime(it)
            }
            ?.let { lifetime ->
                Log.d(LOG_TAG, "## getRoomRetention(): the period " + lifetime + "ms is defined")
                convertMsToDays(lifetime).coerceIn(1..365)
            }
            ?: DEFAULT_RETENTION_VALUE_IN_DAYS
}

fun setRoomRetention(session: MXSession, room: Room, periodInDays: Int, callback: ApiCallback<Void>) {
    val content = HashMap<String, Any>()
    content[STATE_EVENT_CONTENT_MAX_LIFETIME] = convertDaysToMs(periodInDays)
    content[STATE_EVENT_CONTENT_EXPIRE_ON_CLIENTS] = true

    session.roomsApiClient.sendStateEvent(room.roomId, EVENT_TYPE_STATE_ROOM_RETENTION, "", content, callback)
}

/**
 * Clean the storage of a session by removing the expired contents.
 *
 * @param session the current session
 */
fun clearSessionExpiredContents(session: MXSession) {
    session.dataHandler.store
            .takeIf { it.isReady }
            ?.let { store ->
                val doCommit = store.rooms
                        .filter { !it.isInvited }
                        .map { room -> clearExpiredRoomContentsFromStore(store, room) }
                        .any { it }
                if (doCommit) {
                    store.commit()
                }
            }
}

/**
 * Clean the storage of a room by removing the expired contents.
 *
 * @param session the current session
 * @param room    the room
 * @return true if the store has been updated.
 */
fun clearExpiredRoomContents(session: MXSession, room: Room): Boolean {
    var hasStoreChanged = false
    session.dataHandler.store
            .takeIf { it.isReady }
            ?.let { store ->
                hasStoreChanged = clearExpiredRoomContentsFromStore(store, room)
                if (hasStoreChanged) {
                    store.commit()
                }
            }
    return hasStoreChanged
}

private fun clearExpiredRoomContentsFromStore(store: IMXStore, room: Room): Boolean {
    var shouldCommitStore = false
    val retentionDurationMs = TimeUnit.DAYS.toMillis(getRoomRetention(room).toLong())

    val events = store.getRoomMessages(room.roomId)
    if (null != events) {
        for (event in events) {
            if (event.stateKey == null) {
                val eventLifetime = System.currentTimeMillis() - event.getOriginServerTs()
                if (eventLifetime > retentionDurationMs) {
                    store.deleteEvent(event)
                    shouldCommitStore = true
                } else {
                    break
                }
            }
        }
    }

    return shouldCommitStore
}

//=============================================================================================
// Others
//=============================================================================================

/**
 * Convert a number of days to a duration in ms.
 *
 * @param daysNb number of days.
 * @return the duration in ms.
 */
fun convertDaysToMs(daysNb: Int): Long {
    return TimeUnit.DAYS.toMillis(daysNb.toLong())
}

/**
 * Convert a duration (in ms) to a number of days.
 *
 * @param durationMs
 * @return the number of days.
 */
fun convertMsToDays(durationMs: Long): Int {
    return TimeUnit.MILLISECONDS.toDays(durationMs).toInt()
}
