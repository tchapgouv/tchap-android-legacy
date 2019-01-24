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
import android.support.annotation.CallSuper;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Toast;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomPreviewData;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.publicroom.PublicRoom;
import org.matrix.androidsdk.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import fr.gouv.tchap.model.TchapPublicRoom;
import fr.gouv.tchap.model.TchapRoom;
import fr.gouv.tchap.model.TchapSession;
import fr.gouv.tchap.util.DinsicUtils;
import im.vector.Matrix;
import im.vector.PublicRoomsManager;
import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.VectorAppCompatActivity;
import im.vector.activity.VectorRoomActivity;
import im.vector.adapters.AdapterSection;
import im.vector.fragments.VectorBaseFragment;
import im.vector.view.EmptyViewItemDecoration;
import im.vector.view.SectionView;
import im.vector.view.SimpleDividerItemDecoration;
import fr.gouv.tchap.adapters.TchapPublicRoomAdapter;

public class TchapPublicRoomsFragment extends VectorBaseFragment {
    private static final String LOG_TAG = TchapPublicRoomsFragment.class.getSimpleName();
    private static final String CURRENT_FILTER = "CURRENT_FILTER";

    protected VectorAppCompatActivity mActivity;

    protected String mCurrentFilter;

    protected TchapSession mTchapSession;

    private boolean mMorePublicRooms = false;
    @BindView(R.id.recyclerview)
    RecyclerView mRecycler;

    // rooms management
    private TchapPublicRoomAdapter mAdapter;

    private List<String> mCurrentHosts = null;
    private List<PublicRoomsManager> mPublicRoomsManagers = null;
    private int mFirstShadowSessionManagerIndex = 0;

    /*
     * *********************************************************************************************
     * Static methods
     * *********************************************************************************************
     */

    public static TchapPublicRoomsFragment newInstance() {
        return new TchapPublicRoomsFragment();
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

        if (getActivity() instanceof VectorAppCompatActivity) {
            mActivity = (VectorAppCompatActivity) getActivity();
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(CURRENT_FILTER)) {
            mCurrentFilter = savedInstanceState.getString(CURRENT_FILTER);
        }

        // Prepare all the public rooms managers
        mCurrentHosts = new ArrayList<>();
        mPublicRoomsManagers = new ArrayList<>();

        mTchapSession = Matrix.getInstance(getActivity()).getDefaultTchapSession();
        if (mTchapSession != null) {
            MXSession session = mTchapSession.getMainSession();
            initPublicRoomsManagers(session, mTchapSession.getConfig().getHasProtectedAccess());

            // Check whether there is a shadow session
            mFirstShadowSessionManagerIndex = mPublicRoomsManagers.size();
            session = mTchapSession.getShadowSession();
            if (session != null) {
                initPublicRoomsManagers(session, false);
            }
        }

        initViews();

        mAdapter.onFilterDone(mCurrentFilter);

        initPublicRooms(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_FILTER, mCurrentFilter);
    }

    @Override
    @CallSuper
    public void onDestroyView() {
        super.onDestroyView();
        mCurrentFilter = null;
    }

    @Override
    @CallSuper
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    /*
     * *********************************************************************************************
     * Public methods
     * *********************************************************************************************
     */

    /**
     * Apply the filter
     *
     * @param pattern
     */
    public void applyFilter(final String pattern) {
        if (TextUtils.isEmpty(pattern)) {
            if (mCurrentFilter != null) {
                // Reset the filter
                mAdapter.getFilter().filter("", new Filter.FilterListener() {
                    @Override
                    public void onFilterComplete(int count) {
                        Log.i(LOG_TAG, "onResetFilter " + count);

                        // trigger the public rooms search to avoid unexpected list refresh
                        initPublicRooms(false);
                    }
                });
                mCurrentFilter = null;
            }
        } else if (!TextUtils.equals(mCurrentFilter, pattern)) {
            mAdapter.getFilter().filter(pattern, new Filter.FilterListener() {
                @Override
                public void onFilterComplete(int count) {
                    Log.i(LOG_TAG, "onFilterComplete " + count);
                    mCurrentFilter = pattern;

                    // trigger the public rooms search to avoid unexpected list refresh
                    initPublicRooms(false);
                }
            });
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

        mAdapter = new TchapPublicRoomAdapter(getActivity(), new TchapPublicRoomAdapter.OnSelectItemListener() {
            @Override
            public void onSelectItem(Room room, int position) {
                // Ignore this case here. Only public rooms is handled here.
            }

            @Override
            public void onSelectItem(TchapPublicRoom publicRoom) {
                onPublicRoomSelected(publicRoom);
            }
        });
        mRecycler.setAdapter(mAdapter);
    }

    /*
     * *********************************************************************************************
     * Public rooms management
     * *********************************************************************************************
     */

    // spinner text
    private ArrayAdapter<CharSequence> mRoomDirectoryAdapter;

    /**
     * Handle a public room selection
     *
     * @param tchapPublicRoom the public room
     */
    private void onPublicRoomSelected(final TchapPublicRoom tchapPublicRoom) {
        // sanity check
        final PublicRoom publicRoom = tchapPublicRoom.getRoom();
        if (null != publicRoom.roomId) {
            final RoomPreviewData roomPreviewData = new RoomPreviewData(tchapPublicRoom.getSession(), publicRoom.roomId, null, publicRoom.canonicalAlias, null);

            // Check whether the room exists to handled the cases where the user is invited or he has joined.
            // CAUTION: the room may exist whereas the user membership is neither invited nor joined.
            final TchapRoom tchapRoom = mTchapSession.getRoomWithId(publicRoom.roomId);
            if (tchapRoom != null && tchapRoom.getRoom().isInvited()) {
                Log.d(LOG_TAG, "manageRoom : the user is invited -> display the preview " + getActivity());
                CommonActivityUtils.previewRoom(getActivity(), roomPreviewData);
            } else if (tchapRoom != null && tchapRoom.getRoom().isJoined()) {
                Log.d(LOG_TAG, "manageRoom : the user joined the room -> open the room");
                final Map<String, Object> params = new HashMap<>();
                params.put(VectorRoomActivity.EXTRA_MATRIX_ID, tchapPublicRoom.getSession().getMyUserId());
                params.put(VectorRoomActivity.EXTRA_ROOM_ID, publicRoom.roomId);

                if (!TextUtils.isEmpty(publicRoom.name)) {
                    params.put(VectorRoomActivity.EXTRA_DEFAULT_NAME, publicRoom.name);
                }

                if (!TextUtils.isEmpty(publicRoom.topic)) {
                    params.put(VectorRoomActivity.EXTRA_DEFAULT_TOPIC, publicRoom.topic);
                }

                CommonActivityUtils.goToRoomPage(getActivity(), tchapPublicRoom.getSession(), params);
            } else {
                Log.d(LOG_TAG, "manageRoom : display the preview");
                if (null != mActivity) {
                    mActivity.showWaitingView();
                }

                roomPreviewData.fetchPreviewData(new ApiCallback<Void>() {
                    private void onDone() {
                        if (null != mActivity) {
                            mActivity.hideWaitingView();
                        }
                        CommonActivityUtils.previewRoom(getActivity(), roomPreviewData);
                    }

                    @Override
                    public void onSuccess(Void info) {
                        onDone();
                    }

                    private void onError() {
                        roomPreviewData.setPublicRoom(publicRoom);
                        roomPreviewData.setRoomName(publicRoom.name);
                        onDone();
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        onError();
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        if (MatrixError.M_CONSENT_NOT_GIVEN.equals(e.errcode) && isAdded() && null != mActivity) {
                            mActivity.getConsentNotGivenHelper().displayDialog(e);
                        } else {
                            onError();
                        }
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        onError();
                    }
                });
            }
        }
    }

    /**
     * Scroll events listener to forward paginate when it is required.
     */
    private final RecyclerView.OnScrollListener mPublicRoomScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            LinearLayoutManager layoutManager = (LinearLayoutManager) mRecycler.getLayoutManager();
            int lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();

            // we load public rooms 20 by 20, when the 10th one becomes visible, starts loading the next 20
            SectionView sectionView = mAdapter.getSectionViewForSectionIndex(mAdapter.getSectionsCount() - 1);
            AdapterSection lastSection = sectionView != null ? sectionView.getSection() : null;

            if (null != lastSection) {
                // detect if the last visible item is inside another section
                for (int i = 0; i < mAdapter.getSectionsCount() - 1; i++) {
                    SectionView prevSectionView = mAdapter.getSectionViewForSectionIndex(i);

                    if ((null != prevSectionView) && (null != prevSectionView.getSection())) {
                        lastVisibleItemPosition -= prevSectionView.getSection().getNbItems();

                        // the item is in a previous section
                        if (lastVisibleItemPosition <= 0) {
                            return;
                        }
                    }
                }

                // trigger a forward paginate when there are only 10 items left
                if ((lastSection.getNbItems() - lastVisibleItemPosition) < 10) {
                    forwardPaginate();
                }
            }
        }
    };

    /**
     * Init the public rooms.
     *
     * @param displayOnTop true to display the public rooms in full screen
     */
    private void initPublicRooms(final boolean displayOnTop) {
        mAdapter.setNoMorePublicRooms(false);
        mAdapter.setPublicRooms(null);
        mMorePublicRooms = false;
        if (null != mActivity) {
            mActivity.showWaitingView();
        }

        initPublicRoomsCascade(displayOnTop, 0);
    }

    private void initPublicRoomsCascade(final boolean displayOnTop, final int managerIndex) {
        // Check end of the recursive process
        if (managerIndex >= mPublicRoomsManagers.size()) {
            if (null != mActivity) {
                mActivity.hideWaitingView();
            }
            if (mMorePublicRooms) {
                // plug the scroll listener
                addPublicRoomsListener();
            } else {
                mAdapter.setNoMorePublicRooms(true);
            }

            // trick to display the full public rooms list
            if (displayOnTop) {
                // wait that the list is refreshed
                mRecycler.post(new Runnable() {
                    @Override
                    public void run() {
                        SectionView publicSectionView = mAdapter.getSectionViewForSectionIndex(mAdapter.getSectionsCount() - 1);

                        // simulate a click on the header is to display the full list
                        if ((null != publicSectionView) && !publicSectionView.isStickyHeader()) {
                            publicSectionView.callOnClick();
                        }
                    }
                });
            }
            return;
        }

        final PublicRoomsManager publicRoomsManager = mPublicRoomsManagers.get(managerIndex);
        publicRoomsManager.startPublicRoomsSearch(mCurrentHosts.get(managerIndex),
                null,
                false,
                mCurrentFilter, new ApiCallback<List<PublicRoom>>() {
                    @Override
                    public void onSuccess(List<PublicRoom> publicRooms) {
                        if (null != getActivity()) {
                            if (publicRoomsManager.hasMoreResults()) {
                                // Add the scroll listener at the end of the recursive process
                                mMorePublicRooms = true;
                            }

                            mAdapter.addPublicRooms(convertPublicRoomList(publicRooms, managerIndex));

                            // Next
                            initPublicRoomsCascade(displayOnTop, managerIndex+1);
                        }
                    }

                    private void onError(String message) {
                        if (null != getActivity()) {
                            Log.e(LOG_TAG, "## initPublicRoomsCascade() failed " + message);
                            // Pb here when a lot of federation doesn't work, a lot of messages make a crash
                            //    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

                            // Next
                            initPublicRoomsCascade(displayOnTop, managerIndex+1);
                        }
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        onError(e.getLocalizedMessage());
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        if (MatrixError.M_CONSENT_NOT_GIVEN.equals(e.errcode) && isAdded() && null != mActivity) {
                            mActivity.hideWaitingView();
                            mActivity.getConsentNotGivenHelper().displayDialog(e);
                        } else {
                            onError(e.getLocalizedMessage());
                        }
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        onError(e.getLocalizedMessage());
                    }
                });
    }

    /**
     * Update Public rooms managers for a matrix session
     */
    private void initPublicRoomsManagers(MXSession session, boolean isProtected) {
        String userHSName = DinsicUtils.getHomeServerNameFromMXIdentifier(session.getMyUserId());
        List<String> servers;

        if (isProtected) {
            servers = Arrays.asList(getResources().getStringArray(R.array.protected_room_directory_servers));
        } else {
            servers = Arrays.asList(getResources().getStringArray(R.array.room_directory_servers));
        }

        // Add the host name of each known server.
        boolean isUserHSNameAdded = false;
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).compareTo(userHSName) == 0) {
                mCurrentHosts.add(null);
                isUserHSNameAdded = true;
            }
            else {
                mCurrentHosts.add(servers.get(i));
            }
        }
        if (!isUserHSNameAdded) {
            mCurrentHosts.add(null);
        }

        // Create a manager for each host
        for (int i=0;i<mCurrentHosts.size();i++) {
            PublicRoomsManager myPRM = new PublicRoomsManager();
            myPRM.setSession(session);
            mPublicRoomsManagers.add(myPRM);
        }
    }

    private List<TchapPublicRoom> convertPublicRoomList(List<PublicRoom> publicRooms, int managerIndex) {
        List<TchapPublicRoom> tchapPublicRooms = new ArrayList<>(publicRooms.size());
        MXSession session;
        boolean isProtected = false;

        if (managerIndex < mFirstShadowSessionManagerIndex) {
            session = mTchapSession.getMainSession();
            isProtected = mTchapSession.getConfig().getHasProtectedAccess();
        } else {
            session = mTchapSession.getShadowSession();
        }

        for (PublicRoom publicRoom : publicRooms) {
            tchapPublicRooms.add(new TchapPublicRoom(publicRoom, session, isProtected));
        }
        return tchapPublicRooms;
    }

    /**
     * Trigger a forward room pagination
     */
    private void forwardPaginate() {
        for (int i = 0; i < mPublicRoomsManagers.size(); i++)
            if (mPublicRoomsManagers.get(i).isRequestInProgress()) {
                return;
            }
        mMorePublicRooms = false;
        if (null != mActivity) {
            mActivity.showWaitingView();
        }
        cascadeForwardPaginate(0);
    }

    private void cascadeForwardPaginate(final int managerIndex) {
        // Check end of the recursive process
        if (managerIndex >= mPublicRoomsManagers.size()) {
            if (null != mActivity) {
                mActivity.hideWaitingView();
            }
            if (!mMorePublicRooms) {
                // unplug the scroll listener if there is no more data to find
                mAdapter.setNoMorePublicRooms(true);
                removePublicRoomsListener();
            }
            return;
        }

        final PublicRoomsManager publicRoomsManager = mPublicRoomsManagers.get(managerIndex);
        boolean isForwarding = publicRoomsManager.forwardPaginate(new ApiCallback<List<PublicRoom>>() {
            @Override
            public void onSuccess(final List<PublicRoom> publicRooms) {
                if (null != getActivity()) {
                    if (publicRoomsManager.hasMoreResults()) {
                        // Keep the scroll listener at the end of the recursive process
                        mMorePublicRooms = true;
                    }
                    mAdapter.addPublicRooms(convertPublicRoomList(publicRooms, managerIndex));
                }

                // Go next
                cascadeForwardPaginate(managerIndex+1);
            }

            private void onError(String message) {
                if (null != getActivity()) {
                    Log.e(LOG_TAG, "## cascadeForwardPaginate() failed " + message);
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                }
                // Go next
                cascadeForwardPaginate(managerIndex+1);
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(MatrixError e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onUnexpectedError(Exception e) {
                onError(e.getLocalizedMessage());
            }
        });

        if (!isForwarding) {
            // Go next
            cascadeForwardPaginate(managerIndex+1);
        }
    }

    /**
     * Add the public rooms listener
     */
    private void addPublicRoomsListener() {
        mRecycler.addOnScrollListener(mPublicRoomScrollListener);
    }

    /**
     * Remove the public rooms listener
     */
    private void removePublicRoomsListener() {
        mRecycler.removeOnScrollListener(null);
    }

}
