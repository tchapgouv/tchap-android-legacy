package fr.gouv.tchap.sdk.session.room.model

import com.google.gson.annotations.SerializedName
import fr.gouv.tchap.util.DinsicUtils
import org.matrix.androidsdk.rest.model.Event
import org.matrix.androidsdk.core.JsonUtils

const val EVENT_TYPE_STATE_ROOM_RETENTION = "m.room.retention"

const val STATE_EVENT_CONTENT_MAX_LIFETIME = "max_lifetime"
const val STATE_EVENT_CONTENT_EXPIRE_ON_CLIENTS = "expire_on_clients"

const val DEFAULT_RETENTION_VALUE = 365

data class RoomRetentionContent(
        @JvmField
        @SerializedName("max_lifetime")
        var maxLifetime: Long? = null,

        @JvmField
        @SerializedName("expire_on_clients")
        var expireOnClients: Boolean? = null
)

fun getMaxLifetime(event: Event): Long? {
    val content = JsonUtils.toClass(event.contentAsJsonObject, RoomRetentionContent::class.java)
    return content?.maxLifetime
}

fun getExpireOnClients(event: Event): Boolean {
    val content = JsonUtils.toClass(event.contentAsJsonObject, RoomRetentionContent::class.java)
    return content?.expireOnClients ?: false
}