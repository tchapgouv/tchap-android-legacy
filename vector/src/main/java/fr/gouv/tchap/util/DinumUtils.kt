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

import android.content.Context
import fr.gouv.tchap.sdk.session.room.model.*
import im.vector.BuildConfig
import im.vector.R
import org.matrix.androidsdk.MXSession
import org.matrix.androidsdk.core.Log
import org.matrix.androidsdk.core.callback.ApiCallback
import org.matrix.androidsdk.data.Room
import org.matrix.androidsdk.data.store.IMXStore
import java.util.*
import java.util.concurrent.TimeUnit

private const val LOG_TAG = "DinumUtils"

//=============================================================================================
// Target
//=============================================================================================

fun isSecure(): Boolean {
    return BuildConfig.FLAVOR_target == "protecteed"
}

//=============================================================================================
// Rooms
//=============================================================================================

fun getJoinedRooms(session: MXSession): List<Room> {
    // copy from HomeRoomsViewModel
    return session.dataHandler.store?.rooms?.filter {
        val isJoined = it.isJoined
        val tombstoneContent = it.state.roomTombstoneContent
        val redirectRoom = if (tombstoneContent?.replacementRoom != null) {
            session.dataHandler.getRoom(tombstoneContent.replacementRoom)
        } else {
            null
        }
        val isVersioned = redirectRoom?.isJoined
                ?: false
        isJoined && !isVersioned && !it.isConferenceUserRoom
    } .orEmpty()
}

//=============================================================================================
// Room messages retention
//=============================================================================================

enum class RetentionConstants(val value: Int) {
    undefined(UNDEFINED_RETENTION_VALUE),
    oneDay(1),
    oneWeek(7),
    oneMonth(30),
    sixMonths(180),
    oneYear(365)
}

/**
 * Get the current room retention period in days.
 *
 * @param room the room.
 * @return the room retention period in days, or UNDEFINED_RETENTION_VALUE if none.
 */
fun getRoomRetention(room: Room): Int {
    // Select the latest state event if any
    return room.state.getStateEvents(setOf(EVENT_TYPE_STATE_ROOM_RETENTION))
            .apply { sortBy { it.originServerTs } }
            .lastOrNull()
            ?.let { getMaxLifetime(it) }
            ?.also { Log.d(LOG_TAG, "## getRoomRetention(): the period ${it}ms is defined") }
            ?.let { lifetime -> convertMsToDays(lifetime).coerceIn(1..365) }
            ?: UNDEFINED_RETENTION_VALUE
}

fun setRoomRetention(session: MXSession, room: Room, periodInDays: Int, callback: ApiCallback<Void>) {
    session.roomsApiClient.sendStateEvent(
            room.roomId,
            EVENT_TYPE_STATE_ROOM_RETENTION,
            "",
            mapOf(
                    STATE_EVENT_CONTENT_MAX_LIFETIME to convertDaysToMs(periodInDays),
                    STATE_EVENT_CONTENT_EXPIRE_ON_CLIENTS to true
            )
            , callback)
}

fun getRetentionLabel(context: Context, retentionValue: Int): String {
    return when (retentionValue) {
        RetentionConstants.undefined.value -> context.getString(R.string.tchap_room_settings_retention_infinite)
        RetentionConstants.oneYear.value -> context.getString(R.string.tchap_room_settings_retention_1_year)
        RetentionConstants.sixMonths.value -> context.getString(R.string.tchap_room_settings_retention_6_months)
        RetentionConstants.oneMonth.value -> context.getString(R.string.tchap_room_settings_retention_1_month)
        RetentionConstants.oneWeek.value -> context.getString(R.string.tchap_room_settings_retention_1_week)
        else -> context.resources.getQuantityString(R.plurals.tchap_room_settings_retention_in_days, retentionValue, retentionValue)
    }
}

/**
 * Clean the storage of a session by removing the expired contents.
 *
 * @param session the current session
 */
fun clearSessionExpiredContents(session: MXSession) {
    session.dataHandler.store
            .takeIf { it?.isReady?: false }
            ?.let { store ->
                store.rooms
                        .filter { !it.isInvited }
                        .map { room -> clearExpiredRoomContentsFromStore(store, room) }
                        .any { it }
                        .let { doCommit -> if (doCommit) store.commit() }
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
    return session.dataHandler.store
            .takeIf { it?.isReady?: false }
            ?.let { store ->
                clearExpiredRoomContentsFromStore(store, room)
                        .also { hasStoreChanged -> if (hasStoreChanged) store.commit() }
            }
            ?: false
}

private fun clearExpiredRoomContentsFromStore(store: IMXStore, room: Room): Boolean {
    var shouldCommitStore = false
    val retentionInDays = getRoomRetention(room)

    if (retentionInDays != UNDEFINED_RETENTION_VALUE) {
        val limitEventTs = System.currentTimeMillis() - convertDaysToMs(retentionInDays)

        // This is a bit more optimized than using a filter, even if the algorithm is not very nice to read
        store.getRoomMessages(room.roomId)
                .orEmpty()
                .firstOrNull { event ->
                    when {
                        event.stateKey != null                   ->
                            // Ignore state event
                            false
                        event.getOriginServerTs() < limitEventTs -> {
                            store.deleteEvent(event)
                            shouldCommitStore = true
                            // Go on
                            false
                        }
                        else                                     ->
                            // Break the loop, we've reached the first non-state event in the timeline which is not expired
                            true
                    }
                }
    }
    
    return shouldCommitStore
}

//=============================================================================================
// Room alias
//=============================================================================================

/**
 * Create a room alias name with a prefix.
 *
 * @param prefix
 * @return the suggested alias name.
 */
fun createRoomAliasName(prefix: String): String {
    return prefix.trim()
            .replace("[^a-zA-Z0-9]".toRegex(), "") + getRandomString()
}

/**
 * Create a room alias with a prefix.
 *
 * @param session the user's session
 * @param prefix
 * @return the suggested alias.
 */
fun createRoomAlias(session: MXSession, prefix: String): String {
    return "#" + createRoomAliasName(prefix) + ":" + DinsicUtils.getHomeServerNameFromMXIdentifier(session.myUserId)
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
fun convertDaysToMs(daysNb: Int) = TimeUnit.DAYS.toMillis(daysNb.toLong())

/**
 * Convert a duration (in ms) to a number of days.
 *
 * @param durationMs
 * @return the number of days.
 */
fun convertMsToDays(durationMs: Long) = TimeUnit.MILLISECONDS.toDays(durationMs).toInt()

/**
 * Generate a random room alias of 7 characters to avoid empty room alias.
 */
fun getRandomString(): String {
    val RANDOMCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
    val stringBuilder = StringBuilder()
    val rnd = Random()
    while (stringBuilder.length < 11) { // length of the random string.
        val index = (rnd.nextFloat() * RANDOMCHARS.length).toInt()
        stringBuilder.append(RANDOMCHARS[index])
    }
    return stringBuilder.toString()
}
