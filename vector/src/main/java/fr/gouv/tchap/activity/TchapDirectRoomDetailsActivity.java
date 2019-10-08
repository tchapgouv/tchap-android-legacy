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

package fr.gouv.tchap.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.ActionBar;
import android.view.MenuItem;
import android.view.View;

import org.matrix.androidsdk.fragments.MatrixMessageListFragment;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.core.Log;

import java.util.List;

import fr.gouv.tchap.util.DinsicUtils;
import im.vector.Matrix;
import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.VectorHomeActivity;
import im.vector.fragments.VectorSearchRoomFilesListFragment;

/**
 * This class implements the direct room details screen
 */
public class TchapDirectRoomDetailsActivity extends TchapContactActionBarActivity {
    private static final String LOG_TAG = TchapDirectRoomDetailsActivity.class.getSimpleName();

    // exclude the room ID
    public static final String EXTRA_ROOM_ID = "TchapRoomDetailsActivity.EXTRA_ROOM_ID";

    private static final String TAG_FRAGMENT_FILES_DETAILS = "im.vector.activity.TAG_FRAGMENT_FILES_DETAILS";

    private VectorSearchRoomFilesListFragment mSearchFilesFragment;

    private String mRoomId;
    private String mMatrixId;

    private androidx.appcompat.widget.Toolbar mToolbar;
    private FragmentManager mFragmentManager;

    private final MXEventListener mEventListener = new MXEventListener() {
        @Override
        public void onLeaveRoom(String roomId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // pop to the home activity
                    Intent intent = new Intent(TchapDirectRoomDetailsActivity.this, VectorHomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    TchapDirectRoomDetailsActivity.this.startActivity(intent);
                }
            });
        }
    };

    @Override
    public int getLayoutRes() {
        return R.layout.activity_tchap_direct_room_details;

    }

    @Override
    public void initUiAndData() {
        super.initUiAndData();
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

        // UI widgets binding & init fields
        setWaitingView(findViewById(R.id.settings_loading_layout));

        // use a toolbar instead of the actionbar

        mToolbar = findViewById(R.id.room_toolbar);
        setSupportActionBar(mToolbar);

        mFragmentManager = getSupportFragmentManager();
        if (null != getSupportActionBar()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle();
        setTopic();
        setAvatar();

        mSearchFilesFragment = (VectorSearchRoomFilesListFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_FILES_DETAILS);
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        if (null == mSearchFilesFragment) {
            mSearchFilesFragment = VectorSearchRoomFilesListFragment.newInstance(mSession.getCredentials().userId, mRoomId, org.matrix.androidsdk.R.layout.fragment_matrix_message_list_fragment);
            ft.replace(R.id.room_details_fragment_container, mSearchFilesFragment, TAG_FRAGMENT_FILES_DETAILS).commit();
            Log.d(LOG_TAG, "## TchapDirectRoomDetailsActivity init file frag replace");
        } else {
            ft.attach(mSearchFilesFragment).commit();
            Log.d(LOG_TAG, "## TchapDirectRoomDetailsActivity init file frag attach");
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        List<Fragment> allFragments = getSupportFragmentManager().getFragments();

        // dispatch the result to each fragments
        for (Fragment fragment : allFragments) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(LOG_TAG, "## onSaveInstanceState(): ");
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
     * Set the title value in the action bar and in the
     * room header layout
     */
    protected void setTitle() {
        String titleToApply = "";
        if ((null != mSession) && (null != mRoom)) {
            titleToApply = DinsicUtils.getRoomDisplayName(this, mRoom);
            titleToApply = DinsicUtils.getNameFromDisplayName(titleToApply);
        }
        super.setTitle(titleToApply);
    }

    /**
     * Set the topic
     */
    protected void setTopic() {
        String topic = "";
        if (null != mRoom) {
            topic = DinsicUtils.getDomainFromDisplayName(DinsicUtils.getRoomDisplayName(this, mRoom));
        super.setTopic(topic);
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
                Intent intent = new Intent(TchapDirectRoomDetailsActivity.this, VectorHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                TchapDirectRoomDetailsActivity.this.startActivity(intent);
                return;
            }

            // listen for room leave event
            mRoom.addEventListener(mEventListener);

            // start the file search
            startFileSearch();
        }
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
    }

    /**
     * Start a file search
     */
    private void startFileSearch() {
        showWaitingView();
        mSearchFilesFragment.startFilesSearch(new MatrixMessageListFragment.OnSearchResultListener() {
            @Override
            public void onSearchSucceed(int nbrMessages) {
                onSearchEnd( nbrMessages);
            }

            @Override
            public void onSearchFailed() {
                onSearchEnd(0);
            }
        });

    }

    /**
     * The search is done.
     *
     *
     * @param nbrMessages the number of found messages.
     */
    private void onSearchEnd(int nbrMessages) {
            Log.d(LOG_TAG, "## onSearchEnd() nbrMsg=" + nbrMessages);
            // stop "wait while searching" screen
            hideWaitingView();
    }
}