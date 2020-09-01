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

package fr.gouv.tchap.activity

import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import butterknife.BindView
import butterknife.OnCheckedChanged
import butterknife.OnClick
import fr.gouv.tchap.sdk.session.room.model.RESTRICTED
import fr.gouv.tchap.sdk.session.room.model.UNRESTRICTED
import fr.gouv.tchap.util.DinsicUtils
import fr.gouv.tchap.util.HexagonMaskView
import fr.gouv.tchap.util.createPermalink
import fr.gouv.tchap.util.createRoomAlias
import im.vector.Matrix
import im.vector.R
import im.vector.activity.CommonActivityUtils
import im.vector.activity.VectorAppCompatActivity
import im.vector.util.VectorUtils
import im.vector.util.copyToClipboard
import org.jetbrains.anko.longToast
import org.matrix.androidsdk.MXSession
import org.matrix.androidsdk.core.Log
import org.matrix.androidsdk.core.MXPatterns
import org.matrix.androidsdk.core.callback.ApiCallback
import org.matrix.androidsdk.core.callback.ApiFailureCallback
import org.matrix.androidsdk.core.callback.SimpleApiCallback
import org.matrix.androidsdk.core.model.MatrixError
import org.matrix.androidsdk.data.Room
import org.matrix.androidsdk.data.RoomState
import org.matrix.androidsdk.rest.model.PowerLevels
import org.matrix.androidsdk.rest.model.RoomDirectoryVisibility

class TchapRoomAccessByLinkActivity : VectorAppCompatActivity(){
    /* ==========================================================================================
     * UI
     * ========================================================================================== */

    @BindView(R.id.avatar_img)
    lateinit var roomAvatar: HexagonMaskView

    @BindView(R.id.room_access_by_link_status)
    lateinit var roomAccessByLinkStatus: TextView

    @BindView(R.id.switch_room_access_by_link)
    lateinit var switchRoomAccessByLink: Switch

    @BindView(R.id.room_access_by_link_info)
    lateinit var roomAccessByLinkInfo: TextView

    @BindView(R.id.room_access_link)
    lateinit var roomAccessLink: TextView

    @BindView(R.id.buttons_container)
    lateinit var buttonsContainer: RelativeLayout

    @BindView(R.id.forward_link_button)
    lateinit var forwardLinkButton: Button

    @BindView(R.id.share_link_button)
    lateinit var shareLinkButton: Button

    /* ==========================================================================================
     * DATA
     * ========================================================================================== */

    private lateinit var session: MXSession
    private lateinit var room: Room

    private var isForum: Boolean? = null

    /* ==========================================================================================
     * Life cycle
     * ========================================================================================== */

    override fun getLayoutRes() = R.layout.activity_tchap_room_access_by_link

    override fun initUiAndData() {
        super.initUiAndData()

        waitingView = findViewById(R.id.waiting_view)

        // Get the session
        val defaultSession = Matrix.getInstance(this).defaultSession
        if (defaultSession == null || defaultSession.isAlive == false) {
            Log.e(LOG_TAG, "No Session!")
            finish()
            return
        }
        session = defaultSession

        val roomId = intent.getStringExtra(EXTRA_ROOM_ID)
        val selectedRoom: Room? = session.dataHandler.getRoom(roomId, false)
        if (selectedRoom == null) {
            Log.e(LOG_TAG, "## onCreate() : undefined parameters")
            finish()
            return
        }
        room = selectedRoom

        configureToolbar()
        setAvatar()

        if (intent.hasExtra(EXTRA_IS_FORUM_ROOM)) {
            isForum = intent.getBooleanExtra(EXTRA_IS_FORUM_ROOM, false)
        }

        refreshDisplay()
    }

    private fun setAvatar() {
        VectorUtils.loadRoomAvatar(this, session, roomAvatar, room)
        // Set the right border color
        if (TextUtils.equals(DinsicUtils.getRoomAccessRule(room), RESTRICTED)) {
            roomAvatar.setBorderSettings(ContextCompat.getColor(this, R.color.restricted_room_avatar_border_color), 3)
        } else {
            roomAvatar.setBorderSettings(ContextCompat.getColor(this, R.color.unrestricted_room_avatar_border_color), 10)
        }
    }

    private fun refreshDisplay() {
        if (isForum == null) {
            getRoomDirectoryVisibility()
            return
        }

        var isAdmin = false
        val isConnected = Matrix.getInstance(this).isConnected
        val joinRule: String = room.state.join_rule
        val powerLevels: PowerLevels = room.state.powerLevels
        if (null != powerLevels) {
            val powerLevel = powerLevels.getUserPowerLevel(session.myUserId)
            isAdmin = powerLevel >= CommonActivityUtils.UTILS_POWER_LEVEL_ADMIN
        }

        if (isAdmin && isConnected && isForum == false) {
            roomAccessByLinkStatus.visibility = View.GONE
            switchRoomAccessByLink.visibility = View.VISIBLE
        } else {
            roomAccessByLinkStatus.visibility = View.VISIBLE
            switchRoomAccessByLink.visibility = View.GONE
        }

        switchRoomAccessByLink.isEnabled = false
        if (RoomState.JOIN_RULE_INVITE == joinRule) {
            roomAccessByLinkStatus.text = getString(R.string.tchap_room_settings_room_access_by_link_disabled)
            switchRoomAccessByLink.isChecked = false
            roomAccessByLinkInfo.text = getString(R.string.tchap_room_settings_enable_room_access_by_link_info_off)
            roomAccessLink.visibility = View.GONE
            buttonsContainer.visibility = View.GONE
        } else {
            roomAccessByLinkStatus.text = getString(R.string.tchap_room_settings_room_access_by_link_enabled)
            switchRoomAccessByLink.isChecked = true
            roomAccessByLinkInfo.text = getString(R.string.tchap_room_settings_enable_room_access_by_link_info_on)
            roomAccessLink.text = createPermalink(getString(R.string.permalink_prefix), room.state.canonicalAlias)
            roomAccessLink.visibility = View.VISIBLE
            buttonsContainer.visibility = View.VISIBLE
        }
        switchRoomAccessByLink.isEnabled = true
    }

    private fun getRoomDirectoryVisibility() {
        showWaitingView()
        room.getDirectoryVisibility(room.roomId, object: ApiCallback<String> {
            override fun onSuccess(visibility: String) {
                isForum = RoomDirectoryVisibility.DIRECTORY_VISIBILITY_PUBLIC == visibility
                hideWaitingView()
                refreshDisplay()
            }

            private fun onError(errorMessage: String) {
                Log.e(LOG_TAG, "## getRoomDirectoryVisibility: failed $errorMessage")
                // We consider here the room as a forum in order to prevent edition of the option
                // Indeed the switch is hidden in case of a forum room
                isForum = true
                hideWaitingView()
                refreshDisplay()
            }

            override fun onNetworkError(e: Exception) {
                onError(e.localizedMessage)
            }

            override fun onMatrixError(e: MatrixError) {
                onError(e.message)
            }

            override fun onUnexpectedError(e: Exception) {
                onError(e.localizedMessage)
            }
        })
    }

    private fun forbidGuestAccess(callback: ApiCallback<Void>) {
        if (room.state.guestAccess == RoomState.GUEST_ACCESS_FORBIDDEN) {
            callback.onSuccess(null)
        } else {
            room.updateGuestAccess(RoomState.GUEST_ACCESS_FORBIDDEN, callback)
        }
    }

    private fun setCanonicalAlias(callback: ApiCallback<Void>) {
        // Check whether a canonical alias is defined,
        // and check it is correct (some alias were created with invalid character).
        val canonicalAlias = room.state.canonicalAlias
        if (canonicalAlias != null && MXPatterns.isRoomAlias(canonicalAlias)) {
            callback.onSuccess(null)
        } else {
            val roomAlias = createRoomAlias(session, room.state.name ?: "")
            room.addAlias(roomAlias, object: SimpleApiCallback<Void>(callback) {
                override fun onSuccess(v: Void?) {
                    room.updateCanonicalAlias(roomAlias, callback)
                }
            })
        }
    }

    private fun enableRoomAccessByLink() {
        Log.d(LOG_TAG, "## enableRoomAccessByLink")
        showWaitingView()

        val failureCallBack = object : ApiFailureCallback {
            private fun onError(errorMessage: String) {
                Log.e(LOG_TAG, "## enableRoomAccessByLink: failed $errorMessage")
                hideWaitingView()
                refreshDisplay()
                val rule = DinsicUtils.getRoomAccessRule(room)
                if (TextUtils.equals(rule, UNRESTRICTED)) {
                    longToast(R.string.tchap_room_settings_room_access_by_link_forbidden)
                } else {
                    longToast(R.string.tchap_error_message_default)
                }
            }

            override fun onNetworkError(e: Exception) {
                onError(e.localizedMessage)
            }

            override fun onMatrixError(e: MatrixError) {
                onError(e.message)
            }

            override fun onUnexpectedError(e: Exception) {
                onError(e.localizedMessage)
            }
        }

        forbidGuestAccess(object: SimpleApiCallback<Void>(failureCallBack) {
            override fun onSuccess(v: Void?) {
                setCanonicalAlias(object: SimpleApiCallback<Void>(failureCallBack) {
                    override fun onSuccess(v: Void?) {
                        room.updateJoinRules(RoomState.JOIN_RULE_PUBLIC, object: SimpleApiCallback<Void>(failureCallBack) {
                            override fun onSuccess(v: Void?) {
                                hideWaitingView()
                                refreshDisplay()
                            }
                        })
                    }
                })
            }
        })
    }

    private fun disableRoomAccessByLink() {
        Log.d(LOG_TAG, "## disableRoomAccessByLink")
        showWaitingView()

        room.updateJoinRules(RoomState.JOIN_RULE_INVITE, object: ApiCallback<Void> {
            override fun onSuccess(v: Void?) {
                hideWaitingView()
                refreshDisplay()
            }

            private fun onError(errorMessage: String) {
                Log.e(LOG_TAG, "## disableRoomAccessByLink: failed $errorMessage")
                hideWaitingView()
                refreshDisplay()
                longToast(R.string.tchap_error_message_default)
            }

            override fun onNetworkError(e: Exception) {
                onError(e.localizedMessage)
            }

            override fun onMatrixError(e: MatrixError) {
                onError(e.message)
            }

            override fun onUnexpectedError(e: Exception) {
                onError(e.localizedMessage)
            }
        })
    }

    @OnCheckedChanged(R.id.switch_room_access_by_link)
    fun setRoomAccessByLink() {
        if (switchRoomAccessByLink.isEnabled) {
            if (switchRoomAccessByLink.isChecked()) {
                enableRoomAccessByLink()
            } else {
                disableRoomAccessByLink()
            }
        }
    }

    @OnClick(R.id.room_access_link)
    fun copyRoomLink() {
        copyToClipboard(this, roomAccessLink.text)
    }

    @OnClick(R.id.forward_link_button)
    fun forwardRoomLink() {
        roomAccessLink.text?.let { link ->
            val sendIntent = Intent()

            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, link)
            sendIntent.type = "text/plain"

            CommonActivityUtils.sendFilesTo(this, sendIntent)
        }
    }

    @OnClick(R.id.share_link_button)
    fun shareRoomLink() {
        roomAccessLink.text?.let { link ->
            val sendIntent = Intent()

            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, link)
            sendIntent.type = "text/plain"

            startActivity(sendIntent)
        }
    }

    companion object {
        private val LOG_TAG = TchapRoomAccessByLinkActivity::class.java.simpleName

        const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
        const val EXTRA_IS_FORUM_ROOM = "EXTRA_IS_FORUM_ROOM"
    }
}