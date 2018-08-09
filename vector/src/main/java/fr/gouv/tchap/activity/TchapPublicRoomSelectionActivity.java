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

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
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

import butterknife.BindView;
import im.vector.Matrix;
import im.vector.MyPresenceManager;
import im.vector.PublicRoomsManager;
import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.RiotAppCompatActivity;
import fr.gouv.tchap.fragments.TchapPublicRoomsFragment;

/**
 * List all the public rooms by considering all known room directories.
 */
public class TchapPublicRoomSelectionActivity extends RiotAppCompatActivity implements android.support.v7.widget.SearchView.OnQueryTextListener {

    private static final String LOG_TAG = TchapPublicRoomSelectionActivity.class.getSimpleName();

    // shared instance
    private static TchapPublicRoomSelectionActivity sharedInstance = null;

    private static final String TAG_FRAGMENT_ROOMS = "TAG_FRAGMENT_ROOMS";

    @BindView(R.id.listView_spinner_views)
    View waitingView;

    private MXSession mSession;

    @BindView(R.id.drawer_layout_public_room)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.search_view)
    SearchView mSearchView;

    private final BroadcastReceiver mBrdRcvStopWaitingView = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideWaitingView();
        }
    };

    private FragmentManager mFragmentManager;

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

        if (isFirstCreation()) {
            // Add public room fragment
            mFragmentManager.beginTransaction().add(R.id.fragment_container, TchapPublicRoomsFragment.newInstance(), TAG_FRAGMENT_ROOMS).commit();
        }

        this.setTitle(R.string.room_join_public_room_alt_title);

        setWaitingView(waitingView);

        sharedInstance = this;

        mSession = Matrix.getInstance(this).getDefaultSession();

        // initialize the public rooms list
        PublicRoomsManager.getInstance().setSession(mSession);

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyPresenceManager.createPresenceManager(this, Matrix.getInstance(this).getSessions());
        MyPresenceManager.advertiseAllOnline();

        Intent intent = getIntent();
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
     * Init views
     */
    private void initViews() {
        // init the search view
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        // Remove unwanted left margin before the search icon.
        LinearLayout searchEditFrame = mSearchView.findViewById(R.id.search_edit_frame);
        if (searchEditFrame != null) {
            ViewGroup.MarginLayoutParams searchEditFrameParams = (ViewGroup.MarginLayoutParams) searchEditFrame.getLayoutParams();
            searchEditFrameParams.leftMargin = -30;
            searchEditFrameParams.rightMargin = -15;
            searchEditFrame.setLayoutParams(searchEditFrameParams);
        }

        ImageView searchMagIcon = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_mag_icon);
        searchMagIcon.setColorFilter(ContextCompat.getColor(this, R.color.tchap_search_bar_text));

        ImageView searchCloseIcon = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
        searchCloseIcon.setColorFilter(ContextCompat.getColor(this, R.color.tchap_search_bar_text));

        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setQueryHint(getString(R.string.search_hint));
        mSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v != null) {
                    mSearchView.setIconified(false);
                }

            }
        });
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
     * Communicate the search pattern to the currently displayed fragment
     * Note: fragments will handle the search using @{@link android.widget.Filter} which means
     * asynchronous filtering operations
     *
     * @param pattern
     */
    private void applyFilter(final String pattern) {
        Fragment fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_ROOMS);

        if (fragment instanceof TchapPublicRoomsFragment) {
            ((TchapPublicRoomsFragment) fragment).applyFilter(pattern.trim());
        }

        //TODO add listener to know when filtering is done and dismiss the keyboard
    }
}
