/*
 * Copyright 2017 Vector Creations Ltd
 * Copyright 2018 DINSIC
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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;

import butterknife.BindView;
import fr.gouv.tchap.util.HexagonMaskView;
import im.vector.R;
import im.vector.util.VectorUtils;

public class RoomInvitationViewHolder extends RoomViewHolder {

    @BindView(R.id.room_avatar)
    ImageView vRoomAvatar;

    @BindView(R.id.room_avatar_hexagon)
    HexagonMaskView vRoomAvatarHexagon;

    @BindView(R.id.recents_invite_reject_button)
    Button vRejectButton;

    @BindView(R.id.recents_invite_join_button)
    Button vJoinButton;

    RoomInvitationViewHolder(View itemView) {
        super(itemView);
    }

    void populateViews(final Context context, final MXSession session, final Room room,
                       final AbsAdapter.RoomInvitationListener invitationListener, final AbsAdapter.MoreRoomActionListener moreRoomActionListener) {
        super.populateViews(context, session, room, room.isDirectChatInvitation(), true, moreRoomActionListener);

        if (null != room && room.isDirect()) {
            vRoomAvatar.setVisibility(View.VISIBLE);
            vRoomAvatarHexagon.setVisibility(View.GONE);
            VectorUtils.loadRoomAvatar(context, session, vRoomAvatar, room);
        } else {
            vRoomAvatar.setVisibility(View.GONE);
            vRoomAvatarHexagon.setVisibility(View.VISIBLE);
            VectorUtils.loadRoomAvatar(context, session, vRoomAvatarHexagon, room);
        }

        vJoinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != invitationListener) {
                    invitationListener.onJoinRoom(session, room.getRoomId());
                }
            }
        });

        vRejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != invitationListener) {
                    invitationListener.onRejectInvitation(session, room.getRoomId());
                }
            }
        });
    }
}