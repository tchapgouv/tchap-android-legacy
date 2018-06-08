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

package fr.gouv.tchap.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.rest.model.publicroom.PublicRoom;
import org.matrix.androidsdk.util.Log;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.gouv.tchap.util.DinsicUtils;
import im.vector.R;
import im.vector.adapters.AbsAdapter;
import im.vector.adapters.AdapterSection;
import im.vector.adapters.RoomViewHolder;
import im.vector.util.VectorUtils;

public class TchapRoomAdapter extends AbsAdapter {

    private static final String LOG_TAG = TchapRoomAdapter.class.getSimpleName();

    private static final int TYPE_HEADER_PUBLIC_ROOM = 0;

    private static final int TYPE_PUBLIC_ROOM = 1;

    private final AdapterSection<Room> mRoomsSection;

    private final OnSelectItemListener mListener;

    /*
     * *********************************************************************************************
     * Constructor
     * *********************************************************************************************
     */

    public TchapRoomAdapter(final Context context, final OnSelectItemListener listener, final RoomInvitationListener invitationListener, final MoreRoomActionListener moreActionListener) {
        super(context, invitationListener, moreActionListener);

        mListener = listener;

        mRoomsSection = new AdapterSection<>(context, context.getString(R.string.rooms_header), -1,
                R.layout.adapter_item_room_view, TYPE_HEADER_DEFAULT, TYPE_ROOM, new ArrayList<Room>(), DinsicUtils.getRoomsComparator(mSession, false));
        mRoomsSection.setEmptyViewPlaceholder(context.getString(R.string.no_room_placeholder), context.getString(R.string.no_result_placeholder));

        addSection(mRoomsSection);

    }

    /*
     * *********************************************************************************************
     * Abstract methods implementation
     * *********************************************************************************************
     */

    @Override
    protected RecyclerView.ViewHolder createSubViewHolder(ViewGroup viewGroup, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

        View itemView;

        if (viewType == TYPE_HEADER_PUBLIC_ROOM) {
            itemView = inflater.inflate(R.layout.adapter_section_header_public_room, viewGroup, false);
            itemView.setBackgroundColor(Color.MAGENTA);
            return new HeaderViewHolder(itemView);
        } else {
            switch (viewType) {
                case TYPE_ROOM:
                    itemView = inflater.inflate(R.layout.adapter_item_room_view, viewGroup, false);
                    return new RoomViewHolder(itemView);
            }
        }
        return null;
    }

    @Override
    protected void populateViewHolder(int viewType, RecyclerView.ViewHolder viewHolder, int position) {
        switch (viewType) {
            case TYPE_HEADER_PUBLIC_ROOM:
                // Local header
                final HeaderViewHolder headerViewHolder = (HeaderViewHolder) viewHolder;
                for (Pair<Integer, AdapterSection> adapterSection : getSectionsArray()) {
                    if (adapterSection.first == position) {
                        headerViewHolder.populateViews(adapterSection.second);
                        break;
                    }
                }
                break;
            case TYPE_ROOM:
                final RoomViewHolder roomViewHolder = (RoomViewHolder) viewHolder;
                final Room room = (Room) getItemForPosition(position);
                roomViewHolder.populateViews(mContext, mSession, room, false, false, mMoreRoomActionListener);
                roomViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.onSelectItem(room, -1);
                    }
                });
                break;
        }
    }

    @Override
    protected int applyFilter(String pattern) {
        int nbResults = 0;

        nbResults += filterRoomSection(mRoomsSection, pattern);


        return nbResults;
    }

    /*
     * *********************************************************************************************
     * Public methods
     * *********************************************************************************************
     */

    public void setRooms(final List<Room> rooms) {
        mRoomsSection.setItems(rooms, mCurrentFilterPattern);
        if (!TextUtils.isEmpty(mCurrentFilterPattern)) {
            filterRoomSection(mRoomsSection, String.valueOf(mCurrentFilterPattern));
        }
        updateSections();
    }


    /*
     * *********************************************************************************************
     * View holder
     * *********************************************************************************************
     */

    class PublicRoomViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.public_room_avatar)
        ImageView vPublicRoomAvatar;

        @BindView(R.id.public_room_topic)
        TextView vRoomTopic;

        @BindView(R.id.public_room_members_count)
        TextView vPublicRoomsMemberCountTextView;

        private PublicRoomViewHolder(final View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }

    /*
     * *********************************************************************************************
     * Inner classes
     * *********************************************************************************************
     */

    public interface OnSelectItemListener {
        void onSelectItem(Room item, int position);

    }
}
