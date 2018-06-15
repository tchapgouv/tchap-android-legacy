/*
 * Copyright 2014 OpenMarket Ltd
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

package im.vector.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.matrix.androidsdk.fragments.MatrixMessageListFragment;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.util.Log;
import org.matrix.androidsdk.data.Room;

import java.util.List;

import butterknife.BindView;
import fr.gouv.tchap.util.DinsicUtils;
import im.vector.Matrix;
import im.vector.R;
import im.vector.contacts.ContactsManager;
import im.vector.fragments.VectorRoomDetailsMembersFragment;
import im.vector.fragments.VectorRoomSettingsFragment;
import im.vector.fragments.VectorSearchRoomFilesListFragment;
import im.vector.util.VectorUtils;

/**
 * This class implements the room details screen, using a tab UI pattern.
 * Each tab is filled in with its corresponding fragment.
 * There are 2 tabs:
 * - People tab: the members of the room
 * - Settings tab: the settings of the room
 */
public class VectorRoomDetailsActivity extends MXCActionBarActivity {
    private static final String LOG_TAG = VectorRoomDetailsActivity.class.getSimpleName();

    // exclude the room ID
    public static final String EXTRA_ROOM_ID = "VectorRoomDetailsActivity.EXTRA_ROOM_ID";
    // open a dedicated tab at launch
    public static final String EXTRA_SELECTED_TAB_ID = "VectorRoomDetailsActivity.EXTRA_SELECTED_TAB_ID";

    // tab related items
    private static final String TAG_FRAGMENT_PEOPLE_ROOM_DETAILS = "im.vector.activity.TAG_FRAGMENT_PEOPLE_ROOM_DETAILS";
    private static final String TAG_FRAGMENT_FILES_DETAILS = "im.vector.activity.TAG_FRAGMENT_FILES_DETAILS";
    private static final String TAG_FRAGMENT_SETTINGS_ROOM_DETAIL = "im.vector.activity.TAG_FRAGMENT_SETTINGS_ROOM_DETAIL";
    private static final String KEY_FRAGMENT_TAG = "KEY_FRAGMENT_TAG";

    // a tab can be selected at launch (with EXTRA_SELECTED_TAB_ID)
    // so the tab index must be fixed.
    public static final int PEOPLE_TAB_INDEX = 0;
    public static final int FILE_TAB_INDEX = 1;
    public static final int SETTINGS_TAB_INDEX = 2;

    private int mCurrentTabIndex = -1;
    private Toolbar mToolbar;
    private ActionBar mActionBar;

    @BindView(R.id.tab_layout)
    TabLayout mTabNavigationView;


    private VectorRoomDetailsMembersFragment mRoomDetailsMembersFragment;
    private VectorSearchRoomFilesListFragment mSearchFilesFragment;
    private VectorRoomSettingsFragment mRoomSettingsFragment;

    private TextView mActionBarCustomTitle;
    private TextView mActionBarCustomTopic;
    private ImageView mAvatar;

    // activity life cycle management:
    // - Bundle keys
    private static final String KEY_STATE_CURRENT_TAB_INDEX = "CURRENT_SELECTED_TAB";

    // UI items
    private View mLoadOldestContentView;

    private String mRoomId;
    private String mMatrixId;

    // request the contacts permission
    private boolean mIsContactsPermissionChecked;

    private final MXEventListener mEventListener = new MXEventListener() {
        @Override
        public void onLeaveRoom(String roomId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // pop to the home activity
                    Intent intent = new Intent(VectorRoomDetailsActivity.this, VectorHomeActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    VectorRoomDetailsActivity.this.startActivity(intent);
                }
            });
        }
    };

    @Override
    public int getLayoutRes() {
        return R.layout.activity_vector_room_details;
    }

    @Override
    public void initUiAndData() {
        if (CommonActivityUtils.shouldRestartApp(this)) {
            Log.e(LOG_TAG, "Restart the application.");
            CommonActivityUtils.restartApp(this);
            return;
        }

        if (CommonActivityUtils.isGoingToSplash(this)) {
            Log.d(LOG_TAG, "onCreate : Going to splash screen");
            return;
        }

        Intent intent = getIntent();

        if (!intent.hasExtra(EXTRA_ROOM_ID)) {
            Log.e(LOG_TAG, "No room ID extra.");
            finish();
            return;
        }

        if (intent.hasExtra(EXTRA_MATRIX_ID)) {
            mMatrixId = intent.getStringExtra(EXTRA_MATRIX_ID);
        }

        // get current session
        mSession = Matrix.getInstance(getApplicationContext()).getSession(mMatrixId);

        if ((null == mSession) || !mSession.isAlive()) {
            finish();
            return;
        }

        mRoomId = intent.getStringExtra(EXTRA_ROOM_ID);
        mRoom = mSession.getDataHandler().getRoom(mRoomId);
        int selectedTab = intent.getIntExtra(EXTRA_SELECTED_TAB_ID, -1);

        // UI widgets binding & init fields
        setWaitingView(findViewById(R.id.settings_loading_layout));
        mLoadOldestContentView = findViewById(R.id.search_load_oldest_progress);

        // tab creation and restore tabs UI context
        // use a toolbar instead of the actionbar
         mToolbar = findViewById(R.id.room_toolbar);
        setSupportActionBar(mToolbar);

        if (null != getSupportActionBar()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mActionBar = getSupportActionBar();
        mActionBarCustomTitle = findViewById(R.id.room_action_bar_title);
        mActionBarCustomTopic = findViewById(R.id.room_action_bar_topic);
        mAvatar = findViewById(R.id.avatar_img);
        setRoomTitle();
        setTopic();
        setAvatar();


        mTabNavigationView.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                updateSelectedFragment(mTabNavigationView.getSelectedTabPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                int myPosition = tab.getPosition();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

                if (myPosition == PEOPLE_TAB_INDEX) {
                    if (null != mRoomDetailsMembersFragment) {
                        ft.detach(mRoomDetailsMembersFragment).commit();
                    }
                } else if (myPosition == SETTINGS_TAB_INDEX) {
                    onTabUnselectedSettingsFragment();
                } else if (myPosition == FILE_TAB_INDEX) {
                    if (null != mSearchFilesFragment) {
                        mSearchFilesFragment.cancelCatchingRequests();
                        ft.detach(mSearchFilesFragment).commit();
                    }
                } else {
                    Log.w(LOG_TAG, "## onTabUnselected() unknown tab selected!!");
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });



        final TabLayout.Tab myTab;
        int myPosition = PEOPLE_TAB_INDEX;
        if (!isFirstCreation()) {
            if (getSavedInstanceState().getInt(KEY_STATE_CURRENT_TAB_INDEX, PEOPLE_TAB_INDEX)!= PEOPLE_TAB_INDEX) {
                myPosition = getSavedInstanceState().getInt(KEY_STATE_CURRENT_TAB_INDEX, PEOPLE_TAB_INDEX);
            }
        }

        updateSelectedFragment(myPosition);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        List<android.support.v4.app.Fragment> allFragments = getSupportFragmentManager().getFragments();

        // dispatch the result to each fragments
        for (android.support.v4.app.Fragment fragment : allFragments) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(LOG_TAG, "## onSaveInstanceState(): ");

        // save current tab
        if (null != mActionBar) {
            int currentIndex = mActionBar.getSelectedNavigationIndex();
            outState.putInt(KEY_STATE_CURRENT_TAB_INDEX, currentIndex);
        }
    }


    @Override
    public void onRequestPermissionsResult(int aRequestCode, @NonNull String[] aPermissions, @NonNull int[] aGrantResults) {
        if (0 == aPermissions.length) {
            Log.e(LOG_TAG, "## onRequestPermissionsResult(): cancelled " + aRequestCode);
        } else if (aRequestCode == CommonActivityUtils.REQUEST_CODE_PERMISSION_MEMBER_DETAILS) {
            if (Manifest.permission.READ_CONTACTS.equals(aPermissions[0])) {
                if (PackageManager.PERMISSION_GRANTED == aGrantResults[0]) {
                    Log.d(LOG_TAG, "## onRequestPermissionsResult(): READ_CONTACTS permission granted");
                } else {
                    Log.w(LOG_TAG, "## onRequestPermissionsResult(): READ_CONTACTS permission not granted");
                    CommonActivityUtils.displayToast(this, getString(R.string.missing_permissions_warning));
                }

                ContactsManager.getInstance().refreshLocalContactsSnapshot();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Back key management
     */
    public void onBackPressed() {
        boolean isTrapped = false;

        if (PEOPLE_TAB_INDEX == mCurrentTabIndex) {
            isTrapped = mRoomDetailsMembersFragment.onBackPressed();
        }

        if (!isTrapped) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // listen for room leave event
        mRoom.removeEventListener(mEventListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSession.isAlive()) {
            // check if the room has been left from another client
            if ((null == mRoom.getMember(mSession.getMyUserId())) || !mSession.getDataHandler().doesRoomExist(mRoom.getRoomId())) {
                // pop to the home activity
                Intent intent = new Intent(VectorRoomDetailsActivity.this, VectorHomeActivity.class);
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                VectorRoomDetailsActivity.this.startActivity(intent);
                return;
            }

            // listen for room leave event
            mRoom.addEventListener(mEventListener);

            // start the file search if the selected tab is the file one
            startFileSearch();
        }
    }

    /**
     * Update the tag of the tab with its the UI values
     *
     * @param aTabToUpdate the tab to be updated
     */
    private void saveUiTabContext(ActionBar.Tab aTabToUpdate) {
        Bundle tabTag = (Bundle) aTabToUpdate.getTag();
        aTabToUpdate.setTag(tabTag);
    }

    /**
     * Reset the UI to its init state:
     * - "waiting while searching" screen disabled
     * - background image visible
     * - no results message disabled
     */
    private void resetUi() {
        // stop "wait while searching" screen
        hideWaitingView();

        if (null != mLoadOldestContentView) {
            mLoadOldestContentView.setVisibility(View.GONE);
        }
    }


    /**
     * Called when a tab enters the selected state.
     *
     * @param myPosition The tab that was selected
     */
    public void updateSelectedFragment(int myPosition) {
        TabLayout.Tab myTab = mTabNavigationView.getTabAt(myPosition);

            //Bundle tabHolder = (Bundle) tab.getTag();
        //String fragmentTag = getFragmentManager().beginTransaction().replace(R.id.tabHolder.getString(KEY_FRAGMENT_TAG, "");
        Log.d(LOG_TAG, "## onTabSelected()");

        resetUi();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (myPosition == PEOPLE_TAB_INDEX) {
            mRoomDetailsMembersFragment = (VectorRoomDetailsMembersFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_PEOPLE_ROOM_DETAILS);
            if (null == mRoomDetailsMembersFragment) {
                mRoomDetailsMembersFragment = VectorRoomDetailsMembersFragment.newInstance();
                ft.replace(R.id.room_details_fragment_container, mRoomDetailsMembersFragment, TAG_FRAGMENT_PEOPLE_ROOM_DETAILS)
                .commit();
                Log.d(LOG_TAG, "## onTabSelected() people frag replace");
            } else {
                ft.attach(mRoomDetailsMembersFragment).commit();
                Log.d(LOG_TAG, "## onTabSelected() people frag attach");
            }
            mCurrentTabIndex = PEOPLE_TAB_INDEX;

            if (!mIsContactsPermissionChecked) {
                mIsContactsPermissionChecked = true;
                CommonActivityUtils.checkPermissions(CommonActivityUtils.REQUEST_CODE_PERMISSION_MEMBER_DETAILS, this);
            }
        } else if (myPosition == SETTINGS_TAB_INDEX) {
            int permissionToBeGranted = CommonActivityUtils.REQUEST_CODE_PERMISSION_ROOM_DETAILS;
            onTabSelectSettingsFragment();

            // remove camera permission request if the user has not enough power level
            if (!CommonActivityUtils.isPowerLevelEnoughForAvatarUpdate(mRoom, mSession)) {
                permissionToBeGranted &= ~CommonActivityUtils.PERMISSION_CAMERA;
            }
            CommonActivityUtils.checkPermissions(permissionToBeGranted, this);
            mCurrentTabIndex = SETTINGS_TAB_INDEX;
        } else if (myPosition == FILE_TAB_INDEX) {
            mSearchFilesFragment = (VectorSearchRoomFilesListFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_FILES_DETAILS);
            if (null == mSearchFilesFragment) {
                mSearchFilesFragment = VectorSearchRoomFilesListFragment.newInstance(mSession.getCredentials().userId, mRoomId, org.matrix.androidsdk.R.layout.fragment_matrix_message_list_fragment);
                ft.replace(R.id.room_details_fragment_container, mSearchFilesFragment, TAG_FRAGMENT_FILES_DETAILS)
                        .commit();
                Log.d(LOG_TAG, "## onTabSelected() file frag replace");
            } else {
                ft.attach(mSearchFilesFragment).commit();
                Log.d(LOG_TAG, "## onTabSelected() file frag attach");
            }

            mCurrentTabIndex = FILE_TAB_INDEX;
            startFileSearch();
        } else {
            Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT).show();
            mCurrentTabIndex = SETTINGS_TAB_INDEX;
            Log.w(LOG_TAG, "## onTabSelected() unknown tab selected!!");
        }

    }

    /**
     * Start a file search
     */
    private void startFileSearch() {
        if (mCurrentTabIndex == FILE_TAB_INDEX) {
            showWaitingView();
            mSearchFilesFragment.startFilesSearch(new MatrixMessageListFragment.OnSearchResultListener() {
                @Override
                public void onSearchSucceed(int nbrMessages) {
                    onSearchEnd(FILE_TAB_INDEX, nbrMessages);
                }

                @Override
                public void onSearchFailed() {
                    onSearchEnd(FILE_TAB_INDEX, 0);
                }
            });
        }
    }

    /**
     * The search is done.
     *
     * @param tabIndex    the tab index
     * @param nbrMessages the number of found messages.
     */
    private void onSearchEnd(int tabIndex, int nbrMessages) {
        if (mCurrentTabIndex == tabIndex) {
            Log.d(LOG_TAG, "## onSearchEnd() nbrMsg=" + nbrMessages);
            // stop "wait while searching" screen
            hideWaitingView();
        }
    }


    /**
     * Specific method to add the fragment, to avoid using the FragmentTransaction
     * that requires a Fragment based on the support V4.
     */
    private void onTabSelectSettingsFragment() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null == mRoomSettingsFragment) {
                    mRoomSettingsFragment = VectorRoomSettingsFragment.newInstance(mMatrixId, mRoomId);
                    getFragmentManager().beginTransaction().replace(R.id.room_details_fragment_container, mRoomSettingsFragment, TAG_FRAGMENT_SETTINGS_ROOM_DETAIL).commit();
                    Log.d(LOG_TAG, "## onTabSelectSettingsFragment() settings frag replace");
                } else {
                    getFragmentManager().beginTransaction().attach(mRoomSettingsFragment).commit();
                    Log.d(LOG_TAG, "## onTabSelectSettingsFragment() settings frag attach");
                }
            }
        });
    }

    /**
     * Specific method to add the fragment, to avoid using the FragmentTransaction
     * that requires a Fragment based on the support V4.
     */
    private void onTabUnselectedSettingsFragment() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != mRoomSettingsFragment)
                    getFragmentManager().beginTransaction().detach(mRoomSettingsFragment).commit();
            }
        });
    }
    // ==========================================================================================

    /**
     * Set the title value in the action bar and in the
     * room header layout
     */
    private void setRoomTitle() {
        String titleToApply = "";
        if ((null != mSession) && (null != mRoom)) {
            titleToApply = VectorUtils.getRoomDisplayName(this, mSession, mRoom);
            titleToApply = DinsicUtils.getDisplaynameNamePart(titleToApply);
        }

        // set action bar title
        setRoomTitle(titleToApply);
    }
    public void setRoomTitle(String titleToApply) {
        // set action bar title
        mActionBarCustomTitle.setText(titleToApply);
    }
    /**
     * Set the topic
     */
    private void setTopic() {
        String topic = "";
        if (null != mRoom) {
            topic = mRoom.getTopic();

            setTopic(topic);
        }
    }
    public void setTopic(String topic) {
            mActionBarCustomTopic.setText(topic);
    }

    /**
     * Refresh the room avatar.
     */
    private void setAvatar() {
        if (null != mRoom) {
            VectorUtils.loadRoomAvatar(this, mSession, mAvatar, mRoom);
        }
    }
    public void setAvatar(Room myRoom) {
        if (null != mRoom) {
            VectorUtils.loadRoomAvatar(this, mSession, mAvatar, myRoom);
        }
    }

}
