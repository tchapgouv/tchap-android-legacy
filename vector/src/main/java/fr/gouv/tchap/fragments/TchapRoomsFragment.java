/*
 * Copyright 2017 Vector Creations Ltd
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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Spinner;

import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomPreviewData;
import org.matrix.androidsdk.data.RoomSummary;
import org.matrix.androidsdk.data.RoomTag;
import org.matrix.androidsdk.data.store.IMXStore;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.publicroom.PublicRoom;
import org.matrix.androidsdk.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.RoomDirectoryPickerActivity;
import im.vector.activity.VectorRoomActivity;
import im.vector.fragments.AbsHomeFragment;
import im.vector.util.RoomDirectoryData;
import im.vector.view.EmptyViewItemDecoration;
import im.vector.view.SimpleDividerItemDecoration;
import fr.gouv.tchap.adapters.TchapRoomAdapter;

public class TchapRoomsFragment extends AbsHomeFragment implements AbsHomeFragment.OnRoomChangedListener {
    private static final String LOG_TAG = TchapRoomsFragment.class.getSimpleName();

    // activity result codes
    private static final int DIRECTORY_SOURCE_ACTIVITY_REQUEST_CODE = 314;

    //
    private static final String SELECTED_ROOM_DIRECTORY = "SELECTED_ROOM_DIRECTORY";

    @BindView(R.id.recyclerview)
    RecyclerView mRecycler;

    // rooms management
    private TchapRoomAdapter mAdapter;

    // the selected room directory
    private RoomDirectoryData mSelectedRoomDirectory;

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

        if (savedInstanceState != null) {
            mSelectedRoomDirectory = (RoomDirectoryData) savedInstanceState.getSerializable(SELECTED_ROOM_DIRECTORY);
        }
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save the selected room directory
        outState.putSerializable(SELECTED_ROOM_DIRECTORY, mSelectedRoomDirectory);
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
                    mRooms.add(room);
                }
            }
        }

        mAdapter.setRooms(mRooms);
    }

    /*
     * *********************************************************************************************
     * Public rooms management
     * *********************************************************************************************
     */

    // spinner text
    private ArrayAdapter<CharSequence> mRoomDirectoryAdapter;


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
