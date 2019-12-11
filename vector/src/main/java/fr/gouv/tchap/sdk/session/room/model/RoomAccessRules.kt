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

package fr.gouv.tchap.sdk.session.room.model

import org.matrix.androidsdk.rest.model.Event
import org.matrix.androidsdk.core.JsonUtils

const val EVENT_TYPE_STATE_ROOM_ACCESS_RULES = "im.vector.room.access_rules"

const val STATE_EVENT_CONTENT_KEY_RULE = "rule"

const val RESTRICTED = "restricted"
const val UNRESTRICTED = "unrestricted"
const val DIRECT = "direct"

data class RoomAccessRulesContent(
        @JvmField
        var rule: String? = null
)

fun getRule(event: Event): String? {
    val content = JsonUtils.toClass(event.contentAsJsonObject, RoomAccessRulesContent::class.java)
    return content?.rule
}
