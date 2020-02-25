/*
 * Copyright 2020 New Vector Ltd
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

import android.net.Uri
import org.matrix.androidsdk.core.MXPatterns

private val PERMALINK_SUPPORTED_PATHS = listOf("/")
private const val PERMALINK_FRAGMENT_ROOM = "/#/room/"

/**
 * Creates a permalink for a room id or a room alias
 *
 * @param linkPrefix the prefix for the Tchap permalink
 * @param roomIdOrAlias the id of the room
 * @return the permalink
 */
fun createPermalink(linkPrefix: String, roomIdOrAlias: String): String {
    return linkPrefix + PERMALINK_FRAGMENT_ROOM + escape(roomIdOrAlias)
}

/**
 * Creates a permalink for an event.
 *
 * @param linkPrefix the prefix for the Tchap permalink
 * @param roomId  the id of the room
 * @param eventId the id of the event
 * @return the permalink
 */
fun createPermalink(linkPrefix: String, roomId: String, eventId: String): String {
    return linkPrefix + PERMALINK_FRAGMENT_ROOM + escape(roomId) + "/" + escape(eventId)
}

private fun escape(id: String): String {
    return id.replace("/".toRegex(), "%2F").replace("\\+".toRegex(), "%2B")
}

/**
 * Check whether the permalink refers to a room alias, and return this alias
 * if any.
 *
 * For example, in case of: "https://tchap.gouv.fr/#/room/#test0JR878N:agent.dinum.tchap.gouv.fr",
 * this will return "#test0JR878N:agent.dinum.tchap.gouv.fr"
 *
 * @param supportedHosts
 * @param url
 * @return the room alias (if any), null if none
 */
fun getRoomAliasFromPermalink(supportedHosts: Array<String>, url: String): String? {
    return Uri.parse(url).takeIf { supportedHosts.contains(it.host)
            && PERMALINK_SUPPORTED_PATHS.contains(it.path) }
            ?.encodedFragment
            ?.split("/", limit = 3)
            ?.takeIf { it.size > 2
                    && it[1] == "room" }
            ?.let { it[2] }
            ?.takeIf { MXPatterns.isRoomAlias(it) }
}

/**
 * Check whether the permalink refers to a user, and return the user id if any.
 *
 * For example, in case of:
 * "https://tchap.gouv.fr/#/user/@jean-philippe.martin-modernisation.fr:matrix.test.org",
 * this will return "@jean-philippe.martin-modernisation.fr:matrix.test.org"
 *
 * @param supportedHosts
 * @param url
 * @return the user id (if any), null if none
 */
fun getUserIdFromPermalink(supportedHosts: Array<String>, url: String): String? {
    return Uri.parse(url).takeIf { supportedHosts.contains(it.host)
            && PERMALINK_SUPPORTED_PATHS.contains(it.path) }
            ?.encodedFragment
            ?.split("/", limit = 3)
            ?.takeIf { it.size > 2
                    && it[1] == "user" }
            ?.let { it[2] }
            ?.takeIf { MXPatterns.isUserId(it) }
}
