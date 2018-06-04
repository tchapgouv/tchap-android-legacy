/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

package fr.gouv.tchap.activity;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomState;
import org.matrix.androidsdk.data.RoomSummary;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.util.Log;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import im.vector.Matrix;
import im.vector.MyPresenceManager;
import im.vector.PublicRoomsManager;
import im.vector.R;
import im.vector.VectorApp;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.RiotAppCompatActivity;
import im.vector.fragments.AbsHomeFragment;
import im.vector.services.EventStreamService;
import fr.gouv.tchap.fragments.PublicRoomsFragment;
/**
 * Displays the main screen of the app, with rooms the user has joined and the ability to create
 * new rooms.
 */
public class TchapPublicRoomSelectionActivity extends RiotAppCompatActivity implements SearchView.OnQueryTextListener {

    private static final String LOG_TAG = TchapPublicRoomSelectionActivity.class.getSimpleName();

    // shared instance
    private static TchapPublicRoomSelectionActivity sharedInstance = null;


    private static final boolean WAITING_VIEW_STOP = false;
    public static final boolean WAITING_VIEW_START = true;


    private static final String TAG_FRAGMENT_HOME = "TAG_FRAGMENT_HOME";
    private static final String TAG_FRAGMENT_FAVOURITES = "TAG_FRAGMENT_FAVOURITES";
    private static final String TAG_FRAGMENT_PEOPLE = "TAG_FRAGMENT_PEOPLE";
    private static final String TAG_FRAGMENT_ROOMS = "TAG_FRAGMENT_ROOMS";
    private static final String TAG_FRAGMENT_GROUPS = "TAG_FRAGMENT_GROUPS";



    // switch to a room activity
    private Map<String, Object> mAutomaticallyOpenedRoomParams = null;


    @BindView(R.id.listView_spinner_views)
    View waitingView;


    private MXEventListener mEventsListener;


    private MXSession mSession;

    @BindView(R.id.drawer_layout_public_room)
    DrawerLayout mDrawerLayout;

    //jp     @BindView(R.id.tab_layout)
//jp     TabLayout mTopNavigationView;

    // calls
//jp     @BindView(R.id.listView_pending_callview)
    //jp    VectorPendingCallView mVectorPendingCallView;

    @BindView(R.id.home_recents_sync_in_progress)
    ProgressBar mSyncInProgressView;

    @BindView(R.id.search_view)
    SearchView mSearchView;

    private boolean mStorePermissionCheck = false;

    // a shared files intent is waiting the store init
//jp     private Intent mSharedFilesIntent = null;

    private final BroadcastReceiver mBrdRcvStopWaitingView = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideWaitingView();
        }
    };

    private FragmentManager mFragmentManager;


    // the current displayed fragment
    private String mCurrentFragmentTag;

    private List<Room> mDirectChatInvitations;
    private List<Room> mRoomInvitations;


    /*
     * *********************************************************************************************
     * Static methods
     * *********************************************************************************************
     */

    /**
     * @return the current instance
     */
    public static TchapPublicRoomSelectionActivity getInstance() {
        return sharedInstance;
    }

    /*
     * *********************************************************************************************
     * Activity lifecycle
     * *********************************************************************************************
     */

    @Override
    public int getLayoutRes() {
        return R.layout.activity_tchap_public_room;
    }

    @Override
    public void initUiAndData() {
        mFragmentManager = getSupportFragmentManager();

        /*jp        if (CommonActivityUtils.shouldRestartApp(this)) {
            Log.e(LOG_TAG, "Restart the application.");
            CommonActivityUtils.restartApp(this);
            return;
        }

        if (CommonActivityUtils.isGoingToSplash(this)) {
            Log.d(LOG_TAG, "onCreate : Going to splash screen");
            return;
        }
*/
        this.setTitle(R.string.room_join_public_room_alt_title);
        sharedInstance = this;

        setupNavigation();

        mSession = Matrix.getInstance(this).getDefaultSession();

        // process intent parameters
        final Intent intent = getIntent();



        // initialize the public rooms list
        PublicRoomsManager.getInstance().setSession(mSession);
//        PublicRoomsManager.getInstance().refreshPublicRoomsCount(null);

        initViews();
    }


    @Override
    protected void onResume() {
        super.onResume();
        MyPresenceManager.createPresenceManager(this, Matrix.getInstance(this).getSessions());
        MyPresenceManager.advertiseAllOnline();


        Intent intent = getIntent();

         if (mSession.isAlive()) {
            addEventsListener();
        }

    }



    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if (!TextUtils.isEmpty(mSearchView.getQuery().toString())) {
            mSearchView.setQuery("", true);
            return;
        }

        // Clear backstack
        mFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        super.onBackPressed();
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        CommonActivityUtils.onLowMemory(this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        CommonActivityUtils.onTrimMemory(this, level);
    }


     /*
     * *********************************************************************************************
     * UI management
     * *********************************************************************************************
     */

    /**
     * Setup navigation components of the screen (toolbar, etc.)
     */
    private void setupNavigation() {
     }

    /**
     * Update the displayed fragment according to the selected menu
     *
     * @param item menu item selected by the user
     */
    /*
    private void updateSelectedFragment(final TabLayout.Tab item) {
        int position = item.getPosition();
        if (mCurrentMenuId == position) {
            return;
        }

        Fragment fragment = null;

        switch (position) {
             case TAB_POSITION_CONTACT:
                Log.d(LOG_TAG, "onNavigationItemSelected PEOPLE");
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_PEOPLE);
                if (fragment == null) {
                    fragment = ContactFragment.newInstance();
                }
                mCurrentFragmentTag = TAG_FRAGMENT_PEOPLE;
                mSearchView.setQueryHint(getString(R.string.home_filter_placeholder_people));
                break;
            case TAB_POSITION_CONVERSATION:
                Log.d(LOG_TAG, "onNavigationItemSelected ROOMS");
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_ROOMS);
                if (fragment == null) {
                    fragment = RoomsFragment.newInstance();
                }
                mCurrentFragmentTag = TAG_FRAGMENT_ROOMS;
                mSearchView.setQueryHint(getString(R.string.home_filter_placeholder_rooms));
                break;
        }


        // hide waiting view
        hideWaitingView();


        if (fragment != null) {
            resetFilter();
            try {
                mFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment, mCurrentFragmentTag)
                        .addToBackStack(mCurrentFragmentTag)
                        .commit();
            } catch (Exception e) {
                Log.e(LOG_TAG, "## updateSelectedFragment() failed : " + e.getMessage());
            }
        }
    }
*/
     /**
     * Init views
     */
    private void initViews() {

        // init the search view
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        // Remove unwanted left margin
        LinearLayout searchEditFrame = mSearchView.findViewById(R.id.search_edit_frame);
        if (searchEditFrame != null) {
            ViewGroup.MarginLayoutParams searchEditFrameParams = (ViewGroup.MarginLayoutParams) searchEditFrame.getLayoutParams();
            searchEditFrameParams.leftMargin = 0;
            searchEditFrame.setLayoutParams(searchEditFrameParams);
        }
        ImageView searchIcon = mSearchView.findViewById(R.id.search_mag_icon);
        if (searchIcon != null) {
            ViewGroup.MarginLayoutParams searchIconParams = (ViewGroup.MarginLayoutParams) searchIcon.getLayoutParams();
            searchIconParams.leftMargin = 0;
            searchIcon.setLayoutParams(searchIconParams);
        }

        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnQueryTextListener(this);


    }

    /**
     * Reset the filter
     */
    private void resetFilter() {
        mSearchView.setQuery("", false);
        mSearchView.clearFocus();
        hideKeyboard();
    }

    /**
     * Hide the keyboard
     */
    private void hideKeyboard() {
        final View view = getCurrentFocus();
        if (view != null) {
            final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /*
     * *********************************************************************************************
     * User action management
     * *********************************************************************************************
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        // compute an unique pattern
        final String filter = newText + "-";

        // wait before really triggering the search
        // else a search is triggered for each new character
        // eg "matt" triggers
        // 1 - search for m
        // 2 - search for ma
        // 3 - search for mat
        // 4 - search for matt
        // whereas only one search should have been triggered
        // else it might trigger some lags evenif the search is done in a background thread
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                String queryText = mSearchView.getQuery().toString();
                String currentFilter = queryText + "-";

                // display if the pattern matched
                if (TextUtils.equals(currentFilter, filter)) {
                    applyFilter(queryText);
                }
            }
        }, 500);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    /**
     * Provides the selected fragment.
     *
     * @return the displayed fragment
     */
    private Fragment getSelectedFragment() {

        Fragment fragment = null;
        fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_ROOMS);
        PublicRoomsFragment roomsFragment = (PublicRoomsFragment) mFragmentManager.findFragmentByTag(TAG_FRAGMENT_ROOMS);

        if (null == roomsFragment) {
            String pattern = null;
/*
            if (intent.hasExtra(EXTRA_SEARCHED_PATTERN)) {
                pattern = intent.getStringExtra(EXTRA_SEARCHED_PATTERN);
            }
*/
            roomsFragment = PublicRoomsFragment.newInstance();
            //session.getMyUserId(), R.layout.fragment_vector_public_rooms_list, pattern);
            mFragmentManager.beginTransaction().add(R.id.fragment_container, roomsFragment, TAG_FRAGMENT_ROOMS).commit();
        }

   /*     switch (mCurrentMenuId) {
            case TAB_POSITION_CONTACT:
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_PEOPLE);
                break;
            case TAB_POSITION_CONVERSATION:
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_ROOMS);
                break;

        }
*/
        return fragment;

    }

    /**
     * Communicate the search pattern to the currently displayed fragment
     * Note: fragments will handle the search using @{@link android.widget.Filter} which means
     * asynchronous filtering operations
     *
     * @param pattern
     */
    private void applyFilter(final String pattern) {
        Fragment fragment = getSelectedFragment();

        if (fragment instanceof AbsHomeFragment) {
            ((AbsHomeFragment) fragment).applyFilter(pattern.trim());
        }

        //TODO add listener to know when filtering is done and dismiss the keyboard
    }




    //==============================================================================================================
    // Events listener
    //==============================================================================================================

    /**
     * Warn the displayed fragment about summary updates.
     */
    public void dispatchOnSummariesUpdate() {
        Fragment fragment = getSelectedFragment();

        if ((null != fragment) && (fragment instanceof AbsHomeFragment)) {
            ((AbsHomeFragment) fragment).onSummariesUpdate();
        }
    }

    /**
     * Add a MXEventListener to the session listeners.
     */
    private void addEventsListener() {
        mEventsListener = new MXEventListener() {
            // set to true when a refresh must be triggered
            private boolean mRefreshOnChunkEnd = false;

            private void onForceRefresh() {
                if (View.VISIBLE != mSyncInProgressView.getVisibility()) {
                    dispatchOnSummariesUpdate();
                }
            }


            @Override
            public void onInitialSyncComplete(String toToken) {
                Log.d(LOG_TAG, "## onInitialSyncComplete()");
                dispatchOnSummariesUpdate();
            }

            @Override
            public void onLiveEventsChunkProcessed(String fromToken, String toToken) {
                if ((VectorApp.getCurrentActivity() == TchapPublicRoomSelectionActivity.this) && mRefreshOnChunkEnd) {
                    dispatchOnSummariesUpdate();
                }

                mRefreshOnChunkEnd = false;
                mSyncInProgressView.setVisibility(View.GONE);

            }

            @Override
            public void onLiveEvent(final Event event, final RoomState roomState) {
                String eventType = event.getType();

                // refresh the UI at the end of the next events chunk
                mRefreshOnChunkEnd |= ((event.roomId != null) && RoomSummary.isSupportedEvent(event)) ||
                        Event.EVENT_TYPE_STATE_ROOM_MEMBER.equals(eventType) ||
                        Event.EVENT_TYPE_TAGS.equals(eventType) ||
                        Event.EVENT_TYPE_REDACTION.equals(eventType) ||
                        Event.EVENT_TYPE_RECEIPT.equals(eventType) ||
                        Event.EVENT_TYPE_STATE_ROOM_AVATAR.equals(eventType) ||
                        Event.EVENT_TYPE_STATE_ROOM_THIRD_PARTY_INVITE.equals(eventType);
            }

            @Override
            public void onReceiptEvent(String roomId, List<String> senderIds) {
                // refresh only if the current user read some messages (to update the unread messages counters)
                mRefreshOnChunkEnd |= (senderIds.indexOf(mSession.getCredentials().userId) >= 0);
            }

            @Override
            public void onRoomTagEvent(String roomId) {
                mRefreshOnChunkEnd = true;
            }

            @Override
            public void onStoreReady() {
                onForceRefresh();
            }

            @Override
            public void onLeaveRoom(final String roomId) {
                // clear any pending notification for this room
                EventStreamService.cancelNotificationsForRoomId(mSession.getMyUserId(), roomId);
                onForceRefresh();
            }

            @Override
            public void onNewRoom(String roomId) {
                onForceRefresh();
            }

            @Override
            public void onJoinRoom(String roomId) {
                onForceRefresh();
            }

            @Override
            public void onDirectMessageChatRoomsListUpdate() {
                mRefreshOnChunkEnd = true;
            }

            @Override
            public void onEventDecrypted(Event event) {
                RoomSummary summary = mSession.getDataHandler().getStore().getSummary(event.roomId);

                if (null != summary) {
                    // test if the latest event is refreshed
                    Event latestReceivedEvent = summary.getLatestReceivedEvent();
                    if ((null != latestReceivedEvent) && TextUtils.equals(latestReceivedEvent.eventId, event.eventId)) {
                        dispatchOnSummariesUpdate();
                    }
                }
            }
        };

        mSession.getDataHandler().addListener(mEventsListener);
    }

    /**
     * Remove the MXEventListener to the session listeners.
     */
    private void removeEventsListener() {
        if (mSession.isAlive()) {
            mSession.getDataHandler().removeListener(mEventsListener);
        }
    }
}
