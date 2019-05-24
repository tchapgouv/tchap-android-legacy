/*
 * Copyright 2015 OpenMarket Ltd
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
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.matrix.androidsdk.data.RoomSummary;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.core.EventDisplay;
import org.matrix.androidsdk.core.Log;

import fr.gouv.tchap.model.TchapRoom;
import fr.gouv.tchap.model.TchapSession;
import im.vector.R;
import im.vector.ui.themes.ThemeUtils;
import im.vector.util.RiotEventDisplay;
import im.vector.util.VectorUtils;

/**
 * An adapter which display the rooms list
 */
public class VectorRoomsSelectionAdapter extends ArrayAdapter<RoomSummary> {
    private static final String LOG_TAG = VectorRoomsSelectionAdapter.class.getSimpleName();

    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final int mLayoutResourceId;
    private final TchapSession mTchapSession;

    /**
     * Constructor of a public rooms adapter.
     *
     * @param context          the context
     * @param layoutResourceId the layout
     */
    public VectorRoomsSelectionAdapter(Context context, int layoutResourceId, TchapSession tchapSession) {
        super(context, layoutResourceId);
        mContext = context;
        mLayoutResourceId = layoutResourceId;
        mLayoutInflater = LayoutInflater.from(mContext);
        mTchapSession = tchapSession;
    }

    /**
     * Provides the formatted timestamp to display.
     * null means that the timestamp text must be hidden.
     *
     * @param event the event.
     * @return the formatted timestamp to display.
     */
    private String getFormattedTimestamp(Event event) {
        String text = AdapterUtils.tsToString(mContext, event.getOriginServerTs(), false);

        // don't display the today before the time
        String today = mContext.getString(R.string.today) + " ";
        if (text.startsWith(today)) {
            text = text.substring(today.length());
        }

        return text;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(mLayoutResourceId, parent, false);
        }

        if (!mTchapSession.isAlive()) {
            Log.e(LOG_TAG, "getView : the session is not anymore valid");
            return convertView;
        }

        RoomSummary roomSummary = getItem(position);

        // retrieve the UI items
        ImageView avatarImageView = convertView.findViewById(R.id.room_avatar);
        TextView roomNameTxtView = convertView.findViewById(R.id.roomSummaryAdapter_roomName);
        TextView roomMessageTxtView = convertView.findViewById(R.id.roomSummaryAdapter_roomMessage);

        TextView timestampTxtView = convertView.findViewById(R.id.roomSummaryAdapter_ts);
        View separatorView = convertView.findViewById(R.id.recents_separator);

        // Retrieve the room avatar and name
        TchapRoom tchapRoom = mTchapSession.getRoom(roomSummary.getRoomId());
        if (null != tchapRoom) {
            // display the room avatar
            VectorUtils.loadRoomAvatar(mContext, tchapRoom.getSession(), avatarImageView, tchapRoom.getRoom());

            // display the room name
            String roomName = tchapRoom.getRoom().getRoomDisplayName(mContext);
            roomNameTxtView.setText(roomName);
        } else {
            roomNameTxtView.setText(null);
        }

        if (roomSummary.getLatestReceivedEvent() != null) {
            EventDisplay eventDisplay = new RiotEventDisplay(mContext);
            eventDisplay.setPrependMessagesWithAuthor(true);
            roomMessageTxtView.setText(eventDisplay.getTextualDisplay(ThemeUtils.INSTANCE.getColor(mContext, R.attr.vctr_riot_primary_text_color),
                    roomSummary.getLatestReceivedEvent(),
                    roomSummary.getLatestRoomState()));

            timestampTxtView.setText(getFormattedTimestamp(roomSummary.getLatestReceivedEvent()));
            timestampTxtView.setTextColor(ThemeUtils.INSTANCE.getColor(mContext, R.attr.vctr_default_text_light_color));
            timestampTxtView.setTypeface(null, Typeface.NORMAL);
            timestampTxtView.setVisibility(View.VISIBLE);
        } else {
            roomMessageTxtView.setText("");
            timestampTxtView.setVisibility(View.GONE);
        }

        // separator
        separatorView.setVisibility(View.VISIBLE);

        convertView.findViewById(R.id.bing_indicator_unread_message).setVisibility(View.INVISIBLE);
        convertView.findViewById(R.id.recents_groups_separator_line).setVisibility(View.GONE);
        convertView.findViewById(R.id.roomSummaryAdapter_action).setVisibility(View.GONE);
        convertView.findViewById(R.id.roomSummaryAdapter_action_image).setVisibility(View.GONE);

        convertView.findViewById(R.id.recents_groups_invitation_group).setVisibility(View.GONE);

        return convertView;
    }
}
