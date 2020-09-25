/*
 * Copyright 2017 Vector Creations Ltd
 * Copyright 2018 New Vector Ltd
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

package im.vector.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.Log;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomSummary;
import org.matrix.androidsdk.data.RoomTag;
import org.matrix.androidsdk.data.store.IMXStore;
import org.matrix.androidsdk.core.BingRulesManager;
import org.matrix.androidsdk.rest.model.User;

import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.gouv.tchap.sdk.session.room.model.RoomAccessRulesKt;
import fr.gouv.tchap.util.DinsicUtils;
import fr.gouv.tchap.util.HexagonMaskView;
import im.vector.R;
import im.vector.ui.themes.ThemeUtils;
import im.vector.util.RoomUtils;
import im.vector.util.VectorUtils;
import im.vector.util.ViewUtilKt;

public class RoomViewHolder extends RecyclerView.ViewHolder {
    private static final String LOG_TAG = RoomViewHolder.class.getSimpleName();

    @BindView(R.id.room_pin_ic)
    @Nullable
    View vRoomPinFavorite;

    @Nullable
    @BindView(R.id.room_avatar)
    ImageView vRoomAvatar;

    @Nullable
    @BindView(R.id.room_avatar_hexagon)
    HexagonMaskView vRoomAvatarHexagon;

    @BindView(R.id.room_name)
    TextView vRoomName;

    @BindView(R.id.notification_mute_bell)
    @Nullable
    ImageView vRoomNotificationMute;

    @BindView(R.id.room_member_domain)
    @Nullable
    TextView vRoomDomain;

    @BindView(R.id.sender_name)
    @Nullable
    TextView vSenderDisplayName;

    @BindView(R.id.room_message)
    @Nullable
    TextView vRoomLastMessage;

    @BindView(R.id.room_update_date)
    @Nullable
    TextView vRoomTimestamp;

    @BindView(R.id.room_unread_count)
    TextView vRoomUnreadCount;

    @BindView(R.id.room_avatar_encrypted_icon)
    View vRoomEncryptedIcon;

    @BindView(R.id.room_more_action_click_area)
    @Nullable
    View vRoomMoreActionClickArea;

    @BindView(R.id.room_more_action_anchor)
    @Nullable
    View vRoomMoreActionAnchor;

    public RoomViewHolder(final View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    /**
     * Refresh the holder layout
     *
     * @param room                   the room
     * @param isDirectChat           true when the room is a direct chat one
     * @param isInvitation           true when the room is an invitation one
     * @param moreRoomActionListener
     */
    public void populateViews(final Context context,
                              final MXSession session,
                              final Room room,
                              final boolean isDirectChat,
                              final boolean isInvitation,
                              final AbsAdapter.MoreRoomActionListener moreRoomActionListener) {
        // sanity check
        if (null == room) {
            Log.e(LOG_TAG, "## populateViews() : null room");
            return;
        }

        if (null == session) {
            Log.e(LOG_TAG, "## populateViews() : null session");
            return;
        }

        if (null == session.getDataHandler()) {
            Log.e(LOG_TAG, "## populateViews() : null dataHandler");
            return;
        }

        IMXStore store = session.getDataHandler().getStore(room.getRoomId());

        if (null == store) {
            Log.e(LOG_TAG, "## populateViews() : null Store");
            return;
        }

        final RoomSummary roomSummary = store.getSummary(room.getRoomId());

        if (null == roomSummary) {
            Log.e(LOG_TAG, "## populateViews() : null roomSummary");
            return;
        }

        int unreadMsgCount = roomSummary.getUnreadEventsCount();
        int highlightCount;
        int notificationCount;

        highlightCount = roomSummary.getHighlightCount();
        notificationCount = roomSummary.getNotificationCount();

        // fix a crash reported by GA
        if ((null != room.getDataHandler()) && room.getDataHandler().getBingRulesManager().isRoomMentionOnly(room.getRoomId())) {
            notificationCount = highlightCount;
        }

        int bingUnreadColor;

        if (isInvitation || (0 != highlightCount)) {
            bingUnreadColor = ContextCompat.getColor(context, R.color.vector_fuchsia_color);
        } else if (0 != notificationCount) {
            bingUnreadColor = ThemeUtils.INSTANCE.getColor(context, R.attr.vctr_notice_secondary);
        } else if (0 != unreadMsgCount) {
            bingUnreadColor = ThemeUtils.INSTANCE.getColor(context, R.attr.vctr_unread_room_indent_color);
        } else {
            bingUnreadColor = Color.TRANSPARENT;
        }

        if (isInvitation || notificationCount > 0) {
            vRoomUnreadCount.setText(isInvitation ? "!" : RoomUtils.formatUnreadMessagesCounter(notificationCount));
            vRoomUnreadCount.setTypeface(null, Typeface.BOLD);
            ViewUtilKt.setRoundBackground(vRoomUnreadCount, bingUnreadColor);
            vRoomUnreadCount.setVisibility(View.VISIBLE);
        } else {
            vRoomUnreadCount.setVisibility(View.GONE);
        }

        String displayName = DinsicUtils.getRoomDisplayName(context, room);

        if (null != vRoomDomain) {
            vRoomDomain.setText(DinsicUtils.getDomainFromDisplayName(displayName));
            vRoomName.setText(DinsicUtils.getNameFromDisplayName(displayName));
        } else {
            vRoomName.setText(displayName);
        }

        if (null != vSenderDisplayName && null != roomSummary.getLatestReceivedEvent()) {
            String userNameWithoutDomain;
            String senderId = roomSummary.getLatestReceivedEvent().getSender();
            User sender = session.getDataHandler().getUser(senderId);
            if (null != sender && null != sender.displayname) {
                userNameWithoutDomain = DinsicUtils.getNameFromDisplayName(sender.displayname);
            } else {
                userNameWithoutDomain = DinsicUtils.computeDisplayNameFromUserId(senderId);
            }
            vSenderDisplayName.setText(userNameWithoutDomain);
            vSenderDisplayName.setVisibility(View.VISIBLE);
        }

        // Check whether both avatar shapes have been defined in the layout.
        // This is the case for the invitations.
        if (null != vRoomAvatarHexagon && null != vRoomAvatar) {
            if (isDirectChat) {
                vRoomAvatar.setVisibility(View.VISIBLE);
                vRoomAvatarHexagon.setVisibility(View.GONE);
                VectorUtils.loadRoomAvatar(context, session, vRoomAvatar, room);
            } else {
                vRoomAvatar.setVisibility(View.GONE);
                vRoomAvatarHexagon.setVisibility(View.VISIBLE);
                VectorUtils.loadRoomAvatar(context, session, vRoomAvatarHexagon, room);

                // The room_access_rules state event is not available in the invite state.
                // That is why we keep the default settings for the avatar border.
            }
        } else if (null != vRoomAvatarHexagon) {
            VectorUtils.loadRoomAvatar(context, session, vRoomAvatarHexagon, room);
            // Set the right border color
            if (TextUtils.equals(DinsicUtils.getRoomAccessRule(room), RoomAccessRulesKt.RESTRICTED)) {
                vRoomAvatarHexagon.setBorderSettings(ContextCompat.getColor(context, R.color.restricted_room_avatar_border_color), 3);
            } else {
                vRoomAvatarHexagon.setBorderSettings(ContextCompat.getColor(context, R.color.unrestricted_room_avatar_border_color), 10);
            }
        } else if (null != vRoomAvatar) {
            VectorUtils.loadRoomAvatar(context, session, vRoomAvatar, room);
        }

        // get last message to be displayed
        if (vRoomLastMessage != null) {
            CharSequence lastMsgToDisplay = RoomUtils.getRoomMessageToDisplay(context, session, roomSummary);
            if (lastMsgToDisplay != null) {
                vRoomLastMessage.setText(lastMsgToDisplay);
                vRoomLastMessage.setVisibility(View.VISIBLE);

                if (notificationCount > 0) {
                    vRoomLastMessage.setTypeface(null, Typeface.BOLD);
                    vRoomLastMessage.setTextColor(ContextCompat.getColor(context, R.color.tchap_primary_text_color));
                } else {
                    vRoomLastMessage.setTypeface(null, Typeface.NORMAL);
                    vRoomLastMessage.setTextColor(ContextCompat.getColor(context, R.color.tchap_third_text_color));
                }

                if (null != vSenderDisplayName) {
                    // Hide the sender display name if the message starts with his name
                    if (lastMsgToDisplay.toString().startsWith(vSenderDisplayName.getText().toString())) {
                        vSenderDisplayName.setVisibility(View.GONE);
                    }
                }
            } else {
                vRoomLastMessage.setVisibility(View.GONE);
                // Hide the sender display name if the message is empty
                if (null != vSenderDisplayName) {
                    vSenderDisplayName.setVisibility(View.GONE);
                }
            }
        }

        vRoomEncryptedIcon.setVisibility(room.isEncrypted() ? View.VISIBLE : View.INVISIBLE);

        if (vRoomTimestamp != null) {
            vRoomTimestamp.setText(RoomUtils.getRoomTimestamp(context, roomSummary.getLatestReceivedEvent()));
            vRoomTimestamp.setVisibility(vRoomLastMessage != null ? vRoomLastMessage.getVisibility() : View.VISIBLE);
        }

        if (vRoomMoreActionClickArea != null && vRoomMoreActionAnchor != null) {
            vRoomMoreActionClickArea.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != moreRoomActionListener) {
                        // In this case, we have a feedback issue because the notification mode change
                        // is handled by an async task (with a very long delay).
                        // We have overridden onMoreActionClick by onTchapMoreActionClick
                        // in order to set the visibility of the vRoomNotificationMute on the notification option click
                        // in RoomUtils.
                        moreRoomActionListener.onTchapMoreActionClick(vRoomMoreActionAnchor, room, vRoomNotificationMute);
                    }
                }
            });
        }

        BingRulesManager.RoomNotificationState roomNotificationState = session.getDataHandler().getBingRulesManager().getRoomNotificationState(room.getRoomId());
        if (null != vRoomNotificationMute && null != roomNotificationState) {
            switch (roomNotificationState) {
                case ALL_MESSAGES_NOISY:
                case ALL_MESSAGES:
                    vRoomNotificationMute.setVisibility(View.GONE);
                    break;
                case MENTIONS_ONLY:
                    if (room.isDirect()) {
                        // Tchap: This mode is not suggested anymore for the direct chats
                        // We consider people will not mention the other member in 1:1.
                        // The room is considered mute.
                        vRoomNotificationMute.setVisibility(View.VISIBLE);
                    } else {
                        vRoomNotificationMute.setVisibility(View.GONE);
                    }
                    break;
                case MUTE:
                    vRoomNotificationMute.setVisibility(View.VISIBLE);
            }
        }

        if (null != room && null != vRoomPinFavorite) {
            // Check first whether the room is pinned
            final Set<String> tagsRoom = room.getAccountData().getRoomTagsKeys();
            final boolean isPinnedRoom = tagsRoom != null && tagsRoom.contains(RoomTag.ROOM_TAG_FAVOURITE);

            if (isPinnedRoom) {
                vRoomPinFavorite.setVisibility(View.VISIBLE);
            } else {
                vRoomPinFavorite.setVisibility(View.INVISIBLE);
            }
        }
    }
}
