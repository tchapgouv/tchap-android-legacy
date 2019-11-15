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
