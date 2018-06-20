/*
 * Copyright 2018 New Vector Ltd
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

package fr.gouv.tchap.fragments;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomSummary;
import org.matrix.androidsdk.data.RoomTag;
import org.matrix.androidsdk.data.store.IMXStore;
import org.matrix.androidsdk.rest.model.RoomMember;
import org.matrix.androidsdk.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import im.vector.R;
import im.vector.fragments.AbsHomeFragment;
import im.vector.view.EmptyViewItemDecoration;
import im.vector.view.SimpleDividerItemDecoration;
import fr.gouv.tchap.adapters.TchapRoomAdapter;

public class TchapRoomsFragment extends AbsHomeFragment implements AbsHomeFragment.OnRoomChangedListener {
    private static final String LOG_TAG = TchapRoomsFragment.class.getSimpleName();

    @BindView(R.id.recyclerview)
    RecyclerView mRecycler;

    // rooms management
    private TchapRoomAdapter mAdapter;

    // rooms list
    private final List<Room> mRooms = new ArrayList<>();

    /*
     * *********************************************************************************************
     * Static methods
     * *********************************************************************************************
     */

    public static TchapRoomsFragment newInstance() {
        return new TchapRoomsFragment();
    }

    /*
     * *********************************************************************************************
     * Fragment lifecycle
     * *********************************************************************************************
     */

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rooms, container, false);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPrimaryColor = ContextCompat.getColor(getActivity(), R.color.tab_rooms);
        mSecondaryColor = ContextCompat.getColor(getActivity(), R.color.tab_rooms_secondary);

        initViews();

        mOnRoomChangedListener = this;

        mAdapter.onFilterDone(mCurrentFilter);
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshRooms();
        if (null != mActivity) {
            mAdapter.setInvitation(mActivity.getRoomInvitations());
        }
        mRecycler.addOnScrollListener(mScrollListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mRecycler.removeOnScrollListener(mScrollListener);
    }

    /*
     * *********************************************************************************************
     * Abstract methods implementation
     * *********************************************************************************************
     */

    @Override
    protected List<Room> getRooms() {
        return new ArrayList<>(mRooms);
    }

    @Override
    protected void onFilter(String pattern, final OnFilterListener listener) {
        mAdapter.getFilter().filter(pattern, new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                Log.i(LOG_TAG, "onFilterComplete " + count);
                if (listener != null) {
                    listener.onFilterDone(count);
                }

            }
        });
    }

    @Override
    protected void onResetFilter() {
        mAdapter.getFilter().filter("", new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                Log.i(LOG_TAG, "onResetFilter " + count);

            }
        });
    }

    /*
     * *********************************************************************************************
     * Public methods
     * *********************************************************************************************
     */

    @Override
    public void onSummariesUpdate() {
        super.onSummariesUpdate();

        if (isResumed()) {
            refreshRooms();
            mAdapter.setInvitation(mActivity.getRoomInvitations());
        }
    }

    /*
     * *********************************************************************************************
     * UI management
     * *********************************************************************************************
     */

    private void initViews() {
        int margin = (int) getResources().getDimension(R.dimen.item_decoration_left_margin);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mRecycler.addItemDecoration(new SimpleDividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL, margin));
        mRecycler.addItemDecoration(new EmptyViewItemDecoration(getActivity(), DividerItemDecoration.VERTICAL, 40, 16, 14));

        mAdapter = new TchapRoomAdapter(getActivity(), new TchapRoomAdapter.OnSelectItemListener() {
            @Override
            public void onSelectItem(Room room, int position) {
                openRoom(room);
            }

        }, this, this);
        mRecycler.setAdapter(mAdapter);

    }

    /*
     * *********************************************************************************************
     * rooms management
     * *********************************************************************************************
     */

    /**
     * Init the rooms display
     */
    private void refreshRooms() {
        if ((null == mSession) || (null == mSession.getDataHandler())) {
            Log.e(LOG_TAG, "## refreshRooms() : null session");
            return;
        }

        IMXStore store = mSession.getDataHandler().getStore();

        if (null == store) {
            Log.e(LOG_TAG, "## refreshRooms() : null store");
            return;
        }

        // update/retrieve the complete summary list
        List<RoomSummary> roomSummaries = new ArrayList<>(store.getSummaries());
        HashSet<String> lowPriorityRoomIds = new HashSet<>(mSession.roomIdsWithTag(RoomTag.ROOM_TAG_LOW_PRIORITY));

        mRooms.clear();

        for (RoomSummary summary : roomSummaries) {
            // don't display the invitations
            if (!summary.isInvited()) {
                Room room = store.getRoom(summary.getRoomId());

                // test
                if ((null != room) && // if the room still exists,even if it's a direct room
                        !room.isConferenceUserRoom() && // not a VOIP conference room
                        !lowPriorityRoomIds.contains(room.getRoomId())) {
                    // In the case of Tchap, we hide the direct chat in which the other member is a 3PID invite.
                    if (room.isDirect()) {
                        Collection<RoomMember> members = room.getMembers();
                        for (RoomMember member : members) {
                            if (member.getUserId().equals(mSession.getMyUserId())) {
                                continue;
                            }
                            if (!member.membership.equalsIgnoreCase(RoomMember.MEMBERSHIP_INVITE) && null == member.getThirdPartyInviteToken()) {
                                mRooms.add(room);
                            }
                        }
                    } else {
                        mRooms.add(room);
                    }
                }
            }
        }
        mAdapter.setRooms(mRooms);
    }

    /*
     * *********************************************************************************************
     * Listeners
     * *********************************************************************************************
     */

    @Override
    public void onToggleDirectChat(String roomId, boolean isDirectChat) {
    }

    @Override
    public void onRoomLeft(String roomId) {
    }

    @Override
    public void onRoomForgot(String roomId) {
        // there is no sync event when a room is forgotten
        refreshRooms();
    }
}
