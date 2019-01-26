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

package im.vector.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.AddFloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import org.jetbrains.annotations.NotNull;
import org.matrix.androidsdk.MXDataHandler;
import org.matrix.androidsdk.MXPatterns;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.call.IMXCall;
import org.matrix.androidsdk.crypto.data.MXDeviceInfo;
import org.matrix.androidsdk.crypto.data.MXUsersDevicesMap;
import org.matrix.androidsdk.data.MyUser;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomPreviewData;
import org.matrix.androidsdk.data.RoomState;
import org.matrix.androidsdk.data.RoomSummary;
import org.matrix.androidsdk.data.RoomTag;
import org.matrix.androidsdk.data.store.IMXStore;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.callback.SimpleApiCallback;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.util.BingRulesManager;
import org.matrix.androidsdk.util.Log;
import org.matrix.androidsdk.util.PermalinkUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import fr.gouv.tchap.activity.TchapLoginActivity;
import fr.gouv.tchap.activity.TchapRoomCreationActivity;
import fr.gouv.tchap.activity.TchapPublicRoomSelectionActivity;
import fr.gouv.tchap.model.TchapRoom;
import fr.gouv.tchap.model.TchapSession;
import fr.gouv.tchap.util.LiveSecurityChecks;

import butterknife.OnClick;
import im.vector.BuildConfig;
import im.vector.Matrix;
import im.vector.MyPresenceManager;
import im.vector.R;
import im.vector.VectorApp;
import im.vector.activity.util.RequestCodesKt;
import im.vector.fragments.AbsHomeFragment;
import fr.gouv.tchap.fragments.TchapContactFragment;
import im.vector.push.PushManager;
import im.vector.receiver.VectorUniversalLinkReceiver;
import im.vector.services.EventStreamService;
import im.vector.ui.themes.ActivityOtherThemes;
import im.vector.ui.themes.ThemeUtils;
import im.vector.util.BugReporter;
import im.vector.util.CallsManager;
import fr.gouv.tchap.util.DinsicUtils;
import im.vector.util.HomeRoomsViewModel;
import im.vector.util.PreferencesManager;
import im.vector.util.RoomUtils;
import im.vector.util.SystemUtilsKt;
import im.vector.util.VectorUtils;
import im.vector.view.UnreadCounterBadgeView;
import im.vector.view.VectorPendingCallView;
import fr.gouv.tchap.fragments.TchapRoomsFragment;

/**
 * Displays the main screen of the app, with rooms the user has joined and the ability to create
 * new rooms.
 */
public class VectorHomeActivity extends VectorAppCompatActivity implements SearchView.OnQueryTextListener {

    private static final String LOG_TAG = VectorHomeActivity.class.getSimpleName();

    // shared instance
    // only one instance of VectorHomeActivity should be used.
    private static VectorHomeActivity sharedInstance = null;

    public static final String EXTRA_JUMP_TO_ROOM_PARAMS = "VectorHomeActivity.EXTRA_JUMP_TO_ROOM_PARAMS";

    // jump to a member details sheet
    public static final String EXTRA_MEMBER_ID = "VectorHomeActivity.EXTRA_MEMBER_ID";

    // jump to a group details sheet
    public static final String EXTRA_GROUP_ID = "VectorHomeActivity.EXTRA_GROUP_ID";

    // there are two ways to open an external link
    // 1- EXTRA_UNIVERSAL_LINK_URI : the link is opened as soon there is an event check processed (application is launched when clicking on the URI link)
    // 2- EXTRA_JUMP_TO_UNIVERSAL_LINK : do not wait that an event chunk is processed.
    public static final String EXTRA_JUMP_TO_UNIVERSAL_LINK = "VectorHomeActivity.EXTRA_JUMP_TO_UNIVERSAL_LINK";
    public static final String EXTRA_WAITING_VIEW_STATUS = "VectorHomeActivity.EXTRA_WAITING_VIEW_STATUS";

    // call management
    // the home activity is launched to start a call.
    public static final String EXTRA_CALL_SESSION_ID = "VectorHomeActivity.EXTRA_CALL_SESSION_ID";
    public static final String EXTRA_CALL_ID = "VectorHomeActivity.EXTRA_CALL_ID";
    public static final String EXTRA_CALL_UNKNOWN_DEVICES = "VectorHomeActivity.EXTRA_CALL_UNKNOWN_DEVICES";

    // the home activity is launched in shared files mode
    // i.e the user tries to send several files with VECTOR
    public static final String EXTRA_SHARED_INTENT_PARAMS = "VectorHomeActivity.EXTRA_SHARED_INTENT_PARAMS";

    private static final boolean WAITING_VIEW_STOP = false;
    public static final boolean WAITING_VIEW_START = true;

    public static final String BROADCAST_ACTION_STOP_WAITING_VIEW = "im.vector.activity.ACTION_STOP_WAITING_VIEW";

    private static final String TAG_FRAGMENT_HOME = "TAG_FRAGMENT_HOME";
    private static final String TAG_FRAGMENT_FAVOURITES = "TAG_FRAGMENT_FAVOURITES";
    private static final String TAG_FRAGMENT_PEOPLE = "TAG_FRAGMENT_PEOPLE";
    private static final String TAG_FRAGMENT_ROOMS = "TAG_FRAGMENT_ROOMS";
    private static final String TAG_FRAGMENT_GROUPS = "TAG_FRAGMENT_GROUPS";

    // Key used to restore the proper fragment after orientation change
    private static final String CURRENT_MENU_ID = "CURRENT_MENU_ID";

    private static final int TAB_POSITION_CONVERSATION=0;
    private static final int TAB_POSITION_CONTACT=1;

    // switch to a room activity
    private Map<String, Object> mAutomaticallyOpenedRoomParams = null;

    private Uri mUniversalLinkToOpen = null;

    private String mMemberIdToOpen = null;

    private String mGroupIdToOpen = null;

    @BindView(R.id.floating_action_menu)
    FloatingActionsMenu mFloatingActionsMenu;

    @BindView(com.getbase.floatingactionbutton.R.id.fab_expand_menu_button)
    AddFloatingActionButton mFabMain;

    @BindView(R.id.button_start_chat)
    FloatingActionButton mFabStartChat;

    @BindView(R.id.button_create_room)
    FloatingActionButton mFabCreateRoom;

    @BindView(R.id.button_join_room)
    FloatingActionButton mFabJoinRoom;

    // mFloatingActionButton is hidden for 1s when there is scroll. This Runnable will show it again
    private Runnable mShowFloatingActionButtonRunnable;

    private MXEventListener mEventsListener;

    // sliding menu management
    private int mSlidingMenuIndex = -1;

    private TchapSession mTchapSession;

    private HomeRoomsViewModel mRoomsViewModel;

    @BindView(R.id.home_toolbar)
    Toolbar mToolbar;

    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.tab_layout)
    TabLayout mTopNavigationView;

    @BindView(R.id.navigation_view)
    NavigationView navigationView;

    // calls
    @BindView(R.id.listView_pending_callview)
    VectorPendingCallView mVectorPendingCallView;

    @BindView(R.id.home_recents_sync_in_progress)
    ProgressBar mSyncInProgressView;

    @BindView(R.id.search_view)
    SearchView mSearchView;

    @BindView(R.id.floating_action_menu_touch_guard)
    View touchGuard;

    // a shared files intent is waiting the store init
    private Intent mSharedFilesIntent = null;

    private final BroadcastReceiver mBrdRcvStopWaitingView = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideWaitingView();
        }
    };

    private final VectorUniversalLinkReceiver mUniversalLinkReceiver = new VectorUniversalLinkReceiver();

    private FragmentManager mFragmentManager;

    // The current item selected (top navigation)
    private int mCurrentMenuId=-1;

    // the current displayed fragment
    private String mCurrentFragmentTag;

    // security
    private LiveSecurityChecks securityChecks = new LiveSecurityChecks(this);

    /*
     * *********************************************************************************************
     * Static methods
     * *********************************************************************************************
     */

    /**
     * @return the current instance
     */
    public static VectorHomeActivity getInstance() {
        return sharedInstance;
    }

    /*
     * *********************************************************************************************
     * Activity lifecycle
     * *********************************************************************************************
     */

    @NotNull
    @Override
    public ActivityOtherThemes getOtherThemes() {
        return ActivityOtherThemes.Home.INSTANCE;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.activity_home;
    }

    @Override
    public void initUiAndData() {
        mFragmentManager = getSupportFragmentManager();

        if (CommonActivityUtils.shouldRestartApp(this)) {
            Log.e(LOG_TAG, "Restart the application.");
            CommonActivityUtils.restartApp(this);
            return;
        }

        if (CommonActivityUtils.isGoingToSplash(this)) {
            Log.d(LOG_TAG, "onCreate : Going to splash screen");
            return;
        }

        // Waiting View
        setWaitingView(findViewById(R.id.listView_spinner_views));

        sharedInstance = this;

        setupNavigation();

        initSlidingMenu();

        mTchapSession = Matrix.getInstance(this).getDefaultTchapSession();
        mRoomsViewModel = new HomeRoomsViewModel(mTchapSession);
        // track if the application update
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int version = preferences.getInt(PreferencesManager.VERSION_BUILD, 0);

        if (version != BuildConfig.VERSION_CODE) {
            Log.d(LOG_TAG, "The application has been updated from version " + version + " to version " + BuildConfig.VERSION_CODE);

            // TODO add some dedicated actions here

            preferences.edit()
                    .putInt(PreferencesManager.VERSION_BUILD, BuildConfig.VERSION_CODE)
                    .apply();
        }

        // Remove Analytics tracking until Tchap defines its own instance
        // Check whether the user has agreed to the use of analytics tracking
        /*if (!PreferencesManager.didAskToUseAnalytics(this)) {
            promptForAnalyticsTracking();
        }*/

        // process intent parameters
        final Intent intent = getIntent();

        if (!isFirstCreation()) {
            // fix issue #1276
            // if there is a saved instance, it means that onSaveInstanceState has been called.
            // theses parameters must only be used once.
            // The activity might have been created after being killed by android while the application is in background
            intent.removeExtra(EXTRA_SHARED_INTENT_PARAMS);
            intent.removeExtra(EXTRA_CALL_SESSION_ID);
            intent.removeExtra(EXTRA_CALL_ID);
            intent.removeExtra(EXTRA_CALL_UNKNOWN_DEVICES);
            intent.removeExtra(EXTRA_WAITING_VIEW_STATUS);
            intent.removeExtra(EXTRA_JUMP_TO_UNIVERSAL_LINK);
            intent.removeExtra(EXTRA_JUMP_TO_ROOM_PARAMS);
            intent.removeExtra(EXTRA_MEMBER_ID);
            intent.removeExtra(EXTRA_GROUP_ID);
            intent.removeExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI);
        } else {

            if (intent.hasExtra(EXTRA_CALL_SESSION_ID) && intent.hasExtra(EXTRA_CALL_ID)) {
                startCall(intent.getStringExtra(EXTRA_CALL_SESSION_ID),
                        intent.getStringExtra(EXTRA_CALL_ID),
                        (MXUsersDevicesMap<MXDeviceInfo>) intent.getSerializableExtra(EXTRA_CALL_UNKNOWN_DEVICES));
                intent.removeExtra(EXTRA_CALL_SESSION_ID);
                intent.removeExtra(EXTRA_CALL_ID);
                intent.removeExtra(EXTRA_CALL_UNKNOWN_DEVICES);
            }

            // the activity could be started with a spinner
            // because there is a pending action (like universalLink processing)
            if (intent.getBooleanExtra(EXTRA_WAITING_VIEW_STATUS, WAITING_VIEW_STOP)) {
                showWaitingView();
            } else {
                hideWaitingView();
            }
            intent.removeExtra(EXTRA_WAITING_VIEW_STATUS);

            mAutomaticallyOpenedRoomParams = (Map<String, Object>) intent.getSerializableExtra(EXTRA_JUMP_TO_ROOM_PARAMS);
            intent.removeExtra(EXTRA_JUMP_TO_ROOM_PARAMS);

            mUniversalLinkToOpen = intent.getParcelableExtra(EXTRA_JUMP_TO_UNIVERSAL_LINK);
            intent.removeExtra(EXTRA_JUMP_TO_UNIVERSAL_LINK);

            mMemberIdToOpen = intent.getStringExtra(EXTRA_MEMBER_ID);
            intent.removeExtra(EXTRA_MEMBER_ID);

            mGroupIdToOpen = intent.getStringExtra(EXTRA_GROUP_ID);
            intent.removeExtra(EXTRA_GROUP_ID);

            // the home activity has been launched with an universal link
            if (intent.hasExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI)) {
                Log.d(LOG_TAG, "Has an universal link");

                final Uri uri = intent.getParcelableExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI);
                intent.removeExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI);

                // detect the room could be opened without waiting the next sync
                Map<String, String> params = VectorUniversalLinkReceiver.parseUniversalLink(uri);

                if ((null != params) && params.containsKey(PermalinkUtils.ULINK_ROOM_ID_OR_ALIAS_KEY)) {
                    Log.d(LOG_TAG, "Has a valid universal link");

                    final String roomIdOrAlias = params.get(PermalinkUtils.ULINK_ROOM_ID_OR_ALIAS_KEY);

                    // it is a room ID ?
                    if (MXPatterns.isRoomId(roomIdOrAlias)) {
                        Log.d(LOG_TAG, "Has a valid universal link to the room ID " + roomIdOrAlias);
                        TchapRoom tchapRoom = mTchapSession.getRoom(roomIdOrAlias);

                        if (null != tchapRoom) {
                            Log.d(LOG_TAG, "Has a valid universal link to a known room");
                            // open the room asap
                            mUniversalLinkToOpen = uri;
                        } else {
                            Log.d(LOG_TAG, "Has a valid universal link but the room is not yet known");
                            // wait the next sync
                            intent.putExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI, uri);
                        }
                    } else if (MXPatterns.isRoomAlias(roomIdOrAlias)) {
                        Log.d(LOG_TAG, "Has a valid universal link of the room Alias " + roomIdOrAlias);

                        showWaitingView();

                        // it is a room alias
                        // convert the room alias to room Id
                        mTchapSession.roomIdByAlias(roomIdOrAlias, new SimpleApiCallback<String>() {
                            @Override
                            public void onSuccess(String roomId) {
                                Log.d(LOG_TAG, "Retrieve the room ID " + roomId);

                                getIntent().putExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI, uri);

                                // the room exists, opens it
                                if (null != mTchapSession.getRoom(roomId)) {
                                    Log.d(LOG_TAG, "Find the room from room ID : process it");
                                    processIntentUniversalLink();
                                } else {
                                    Log.d(LOG_TAG, "Don't know the room");
                                }
                            }
                        });
                    }
                }
            } else {
                Log.d(LOG_TAG, "create with no universal link");
            }

            if (intent.hasExtra(EXTRA_SHARED_INTENT_PARAMS)) {
                final Intent sharedFilesIntent = intent.getParcelableExtra(EXTRA_SHARED_INTENT_PARAMS);
                Log.d(LOG_TAG, "Has shared intent");

                if (mTchapSession.isReady()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(LOG_TAG, "shared intent : The store is ready -> display sendFilesTo");
                            CommonActivityUtils.sendFilesTo(VectorHomeActivity.this, sharedFilesIntent);
                        }
                    });
                } else {
                    Log.d(LOG_TAG, "shared intent : Wait that the store is ready");
                    mSharedFilesIntent = sharedFilesIntent;
                }

                // ensure that it should be called once
                intent.removeExtra(EXTRA_SHARED_INTENT_PARAMS);
            }
        }

        final TabLayout.Tab myTab;
        int myPosition = TAB_POSITION_CONVERSATION;
        if (!isFirstCreation()) {
            if (getSavedInstanceState().getInt(CURRENT_MENU_ID, TAB_POSITION_CONVERSATION)!= TAB_POSITION_CONVERSATION) {
                myPosition = TAB_POSITION_CONTACT;
            }
        }
        myTab = mTopNavigationView.getTabAt(myPosition);
        if (myTab != null) {
            updateSelectedFragment(myTab, false);
        }

        initViews();
    }

    /**
     * Display the Floating Action Menu if it is required
     */
    private void showFloatingActionMenuIfRequired() {
        // Tchap: the floating button is always dispayed
        /*if ((mCurrentMenuId == R.id.bottom_action_favourites) || (mCurrentMenuId == R.id.bottom_action_groups)) {
            concealFloatingActionMenu();
        } else {
            revealFloatingActionMenu();
        }*/
        revealFloatingActionMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(mUniversalLinkReceiver,
                new IntentFilter(VectorUniversalLinkReceiver.BROADCAST_ACTION_UNIVERSAL_LINK_RESUME));

        securityChecks.checkOnActivityStart();
        applyScreenshotSecurity();

        MyPresenceManager.createPresenceManager(this, Matrix.getInstance(this).getSessions());
        MyPresenceManager.advertiseAllOnline();

        // Broadcast receiver to stop waiting screen
        LocalBroadcastManager.getInstance(this).registerReceiver(mBrdRcvStopWaitingView, new IntentFilter(BROADCAST_ACTION_STOP_WAITING_VIEW));

        Intent intent = getIntent();

        if (null != mAutomaticallyOpenedRoomParams) {
            CommonActivityUtils.goToRoomPage(VectorHomeActivity.this, null, mAutomaticallyOpenedRoomParams);
            mAutomaticallyOpenedRoomParams = null;
        }

        // jump to an external link
        if (null != mUniversalLinkToOpen) {
            intent.putExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI, mUniversalLinkToOpen);

            new Handler(getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    processIntentUniversalLink();
                    mUniversalLinkToOpen = null;
                }
            }, 100);
        }

        if (mTchapSession.isAlive()) {
            addEventsListener();
        }

        showFloatingActionMenuIfRequired();

        refreshSlidingMenu();

        mVectorPendingCallView.checkPendingCall();

        if ((null != VectorApp.getInstance()) && VectorApp.getInstance().didAppCrash()) {
            // crash reported by a rage shake
            try {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.send_bug_report_app_crashed)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BugReporter.sendBugReport();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BugReporter.deleteCrashFile(VectorHomeActivity.this);
                            }
                        })
                        .show();

                VectorApp.getInstance().clearAppCrashStatus();
            } catch (Exception e) {
                Log.e(LOG_TAG, "## onResume() : appCrashedAlert failed " + e.getMessage(), e);
            }
        }

        if (null != mMemberIdToOpen) {
            Intent startRoomInfoIntent = new Intent(VectorHomeActivity.this, VectorMemberDetailsActivity.class);
            startRoomInfoIntent.putExtra(VectorMemberDetailsActivity.EXTRA_MEMBER_ID, mMemberIdToOpen);
            // FIXME MULTI-ACCOUNT: There is no reason to use here the main session without considering the potential shadow session.
            startRoomInfoIntent.putExtra(VectorMemberDetailsActivity.EXTRA_MATRIX_ID, mTchapSession.getMainSession().getCredentials().userId);
            startActivity(startRoomInfoIntent);
            mMemberIdToOpen = null;
        }

        if (null != mGroupIdToOpen) {
            Intent groupIntent = new Intent(VectorHomeActivity.this, VectorGroupDetailsActivity.class);
            groupIntent.putExtra(VectorGroupDetailsActivity.EXTRA_GROUP_ID, mGroupIdToOpen);
            // FIXME MULTI-ACCOUNT: There is no reason to use here the main session without considering the potential shadow session.
            groupIntent.putExtra(VectorGroupDetailsActivity.EXTRA_MATRIX_ID, mTchapSession.getMainSession().getCredentials().userId);
            startActivity(groupIntent);
            mGroupIdToOpen = null;
        }

        // https://github.com/vector-im/vector-android/issues/323
        // the tool bar color is not restored on some devices.
        TypedValue vectorActionBarColor = new TypedValue();
        getTheme().resolveAttribute(R.attr.vctr_riot_primary_background_color, vectorActionBarColor, true);
        mToolbar.setBackgroundResource(vectorActionBarColor.resourceId);

        mSyncInProgressView.setVisibility(VectorApp.isSessionSyncing(mTchapSession.getMainSession()) ? View.VISIBLE : View.GONE);

        displayCryptoCorruption();

        addBadgeEventsListener();

        checkNotificationPrivacySetting();

        setSelectedTabStyle();
        updateSelectedFragment(mTopNavigationView.getTabAt(mTopNavigationView.getSelectedTabPosition()), false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == RequestCodesKt.BATTERY_OPTIMIZATION_REQUEST_CODE) {
                // Ok, we can set the NORMAL privacy setting
                Matrix.getInstance(this)
                        .getPushManager()
                        .setNotificationPrivacy(PushManager.NotificationPrivacy.NORMAL, null);
            }
        }
    }

    /**
     * Manage bold typeface on tab items
     */
    private void setSelectedTabStyle() {
        for (int menuIndex = 0; menuIndex < mTopNavigationView.getTabCount(); menuIndex++) {
            LinearLayout customTab = (LinearLayout) mTopNavigationView.getTabAt(menuIndex).getCustomView();
            TextView myText = (TextView)customTab.getChildAt(0);
            if (null != myText) {
                if (menuIndex == mTopNavigationView.getSelectedTabPosition()) {
                    myText.setTypeface(null, Typeface.BOLD);
                }
                else {
                    myText.setTypeface(null, Typeface.NORMAL);
                }
            }
        }
    }


    /**
     * Ask the user to choose a notification privacy policy.
     */
    private void checkNotificationPrivacySetting() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // The "Run in background" permission exists from android 6
            return;
        }

        final PushManager pushManager = Matrix.getInstance(this).getPushManager();

        if (!pushManager.useFcm()) {
            // f-droid does not need the permission.
            // It is still using the technique of sticky "Listen for events" notification
            return;
        }

        // ask user what notification privacy they want. Ask it once
        if (!PreferencesManager.didAskUserToIgnoreBatteryOptimizations(this)) {
            PreferencesManager.setDidAskUserToIgnoreBatteryOptimizations(this);

            if (SystemUtilsKt.isIgnoringBatteryOptimizations(this)) {
                // No need to ask permission, we already have it
                // Set the NORMAL privacy setting
                pushManager.setNotificationPrivacy(PushManager.NotificationPrivacy.NORMAL, null);
            } else {
                // by default, use FCM and low detail notifications
                pushManager.setNotificationPrivacy(PushManager.NotificationPrivacy.LOW_DETAIL, null);

                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.startup_notification_privacy_title)
                        .setMessage(R.string.startup_notification_privacy_message)
                        .setPositiveButton(R.string.startup_notification_privacy_button_grant, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(LOG_TAG, "checkNotificationPrivacySetting: user wants to grant the IgnoreBatteryOptimizations permission");

                                // Request the battery optimization cancellation to the user
                                SystemUtilsKt.requestDisablingBatteryOptimization(VectorHomeActivity.this,
                                        RequestCodesKt.BATTERY_OPTIMIZATION_REQUEST_CODE);
                            }
                        })
                        .setNegativeButton(R.string.startup_notification_privacy_button_other, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(LOG_TAG, "checkNotificationPrivacySetting: user opens notification policy setting screen");

                                // open the notification policy setting screen
                                startActivity(NotificationPrivacyActivity.getIntent(VectorHomeActivity.this));
                            }
                        })
                        .show();
            }
        }
    }

    /**
     * Display a dialog to let the user chooses if he would like to use analytics tracking
     */
    private void promptForAnalyticsTracking() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.settings_opt_in_of_analytics_prompt)
                .setPositiveButton(R.string.settings_opt_in_of_analytics_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setAnalyticsAuthorization(true);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setAnalyticsAuthorization(false);
                    }
                })
                .show();
    }

    private void setAnalyticsAuthorization(boolean useAnalytics) {
        PreferencesManager.setUseAnalytics(this, useAnalytics);
        PreferencesManager.setDidAskToUseAnalytics(this);
    }

    @Override
    public int getMenuRes() {
        //no more menus on tchap ... until when ?
        return -1;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // the application is in a weird state
        if (CommonActivityUtils.shouldRestartApp(this)) {
            return false;
        }
        //no more menus on tchap ... until when ?
        return true;
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

        if (mFloatingActionsMenu.isExpanded()) {
            mFloatingActionsMenu.collapse();
            return;
        }

        // Clear backstack
        mFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_MENU_ID, mCurrentMenuId);
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUniversalLinkReceiver);

        securityChecks.activityStopped();

        // Unregister Broadcast receiver
        hideWaitingView();

        resetFilter();

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBrdRcvStopWaitingView);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## onPause() : unregisterReceiver fails " + e.getMessage(), e);
        }

        if (mTchapSession.isAlive()) {
            removeEventsListener();
        }

        if (mShowFloatingActionButtonRunnable != null && mFloatingActionsMenu != null) {
            mFloatingActionsMenu.removeCallbacks(mShowFloatingActionButtonRunnable);
            mShowFloatingActionButtonRunnable = null;
        }

        removeBadgeEventsListener();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        securityChecks.activityStopped();

        // release the static instance if it is the current implementation
        if (sharedInstance == this) {
            sharedInstance = null;
        }

        resetFilter();
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mAutomaticallyOpenedRoomParams = (Map<String, Object>) intent.getSerializableExtra(EXTRA_JUMP_TO_ROOM_PARAMS);
        intent.removeExtra(EXTRA_JUMP_TO_ROOM_PARAMS);

        mUniversalLinkToOpen = intent.getParcelableExtra(EXTRA_JUMP_TO_UNIVERSAL_LINK);
        intent.removeExtra(EXTRA_JUMP_TO_UNIVERSAL_LINK);

        mMemberIdToOpen = intent.getStringExtra(EXTRA_MEMBER_ID);
        intent.removeExtra(EXTRA_MEMBER_ID);

        mGroupIdToOpen = intent.getStringExtra(EXTRA_GROUP_ID);
        intent.removeExtra(EXTRA_GROUP_ID);

        // start waiting view
        if (intent.getBooleanExtra(EXTRA_WAITING_VIEW_STATUS, VectorHomeActivity.WAITING_VIEW_STOP)) {
            showWaitingView();
        } else {
            hideWaitingView();
        }
        intent.removeExtra(EXTRA_WAITING_VIEW_STATUS);

    }

    /**
     * @return
     */
    public HomeRoomsViewModel getRoomsViewModel() {
        return mRoomsViewModel;
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
        // Toolbar
        setSupportActionBar(mToolbar);

        //load tab items of tab layout
        LinearLayout headerView = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.tab_icon, null);
        LinearLayout customTabConversations = headerView.findViewById(R.id.tab_icon_conversations);
        LinearLayout customTabContacts = headerView.findViewById(R.id.tab_icon_contacts);
        mTopNavigationView.getTabAt(TAB_POSITION_CONVERSATION).setCustomView(customTabConversations);
        mTopNavigationView.getTabAt(TAB_POSITION_CONTACT).setCustomView(customTabContacts);

        mTopNavigationView.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //make tab selected bold
                setSelectedTabStyle();
                updateSelectedFragment(tab, true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    /**
     * Update the displayed fragment according to the selected menu
     *
     * @param item menu item selected by the user
     */
    private void updateSelectedFragment(final TabLayout.Tab item, boolean isAnimated) {
        int position = item.getPosition();
        if (mCurrentMenuId == position) {
            return;
        }

        Fragment fragment = null;

        switch (position) {
            //no more home nor favourite
            /*
            case R.id.bottom_action_home:
                Log.d(LOG_TAG, "onNavigationItemSelected HOME");
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_HOME);
                if (fragment == null) {
                    fragment = HomeFragment.newInstance();
                }
                mCurrentFragmentTag = TAG_FRAGMENT_HOME;
                mSearchView.setQueryHint(getString(R.string.home_filter_placeholder_home));
                break;
            case R.id.bottom_action_favourites:
                Log.d(LOG_TAG, "onNavigationItemSelected FAVOURITES");
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_FAVOURITES);
                if (fragment == null) {
                    fragment = FavouritesFragment.newInstance();
                }
                mCurrentFragmentTag = TAG_FRAGMENT_FAVOURITES;
                mSearchView.setQueryHint(getString(R.string.home_filter_placeholder_favorites));
                break;
                */
            case TAB_POSITION_CONTACT:
                Log.d(LOG_TAG, "onNavigationItemSelected PEOPLE");
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_PEOPLE);
                if (fragment == null) {
                    fragment = TchapContactFragment.newInstance();
                }
                mCurrentFragmentTag = TAG_FRAGMENT_PEOPLE;
                mSearchView.setQueryHint(getString(R.string.home_filter_placeholder_people));
                break;
            case TAB_POSITION_CONVERSATION:
                Log.d(LOG_TAG, "onNavigationItemSelected ROOMS");
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_ROOMS);
                if (fragment == null) {
                    fragment = TchapRoomsFragment.newInstance();
                }
                mCurrentFragmentTag = TAG_FRAGMENT_ROOMS;
                mSearchView.setQueryHint(getString(R.string.home_filter_placeholder_rooms));
                break;
            /*case R.id.bottom_action_groups:
                Log.d(LOG_TAG, "onNavigationItemSelected GROUPS");
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_GROUPS);
                if (fragment == null) {
                    fragment = GroupsFragment.newInstance();
                }
                mCurrentFragmentTag = TAG_FRAGMENT_GROUPS;
                mSearchView.setQueryHint(getString(R.string.home_filter_placeholder_groups));
                break;*/
        }

        if (mShowFloatingActionButtonRunnable != null && mFloatingActionsMenu != null) {
            mFloatingActionsMenu.removeCallbacks(mShowFloatingActionButtonRunnable);
            mShowFloatingActionButtonRunnable = null;
        }

        // hide waiting view
        hideWaitingView();

        mCurrentMenuId = position;

        showFloatingActionMenuIfRequired();

        if (fragment != null) {
            try {
                int myAnimEnter = R.anim.tchap_anim_slide_in_right;
                int myAnimExit = R.anim.tchap_anim_slide_out_right;
                if (position == TAB_POSITION_CONTACT) {
                    myAnimExit = R.anim.tchap_anim_slide_out_left;
                    myAnimEnter = R.anim.tchap_anim_slide_in_left;
                }
                FragmentTransaction myFt = mFragmentManager.beginTransaction();
                if (isAnimated) {
                    myFt.setCustomAnimations(myAnimEnter, myAnimExit);
                }
                myFt.replace(R.id.fragment_container, fragment, mCurrentFragmentTag)
                        .addToBackStack(mCurrentFragmentTag)
                        .commit();
                getSupportFragmentManager().executePendingTransactions();
                String queryText = mSearchView.getQuery().toString();
                if (queryText.length() == 0) {
                    resetFilter();
                } else {
                    //move applyfilter from here to fragment.
                    // Here it causes a crash, probably because the fragment is not completed.
                    //It strange because commit is supposed to synchronyse the fragment completion
                    // The best would have been to listen to fragment complete
                    // applyFilter(queryText);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "## updateSelectedFragment() failed : " + e.getMessage(), e);
            }
        }
    }

    public String getSearchQuery(){
        return  mSearchView.getQuery().toString();
    }

    /**
     * Update UI colors to match the selected tab
     *
     * @param primaryColor
     * @param secondaryColor
     */
    public void updateTabStyle(final int primaryColor, final int secondaryColor) {
        mToolbar.setBackgroundColor(primaryColor);

        Class menuClass = FloatingActionsMenu.class;
        try {
            Field normal = menuClass.getDeclaredField("mAddButtonColorNormal");
            normal.setAccessible(true);
            Field pressed = menuClass.getDeclaredField("mAddButtonColorPressed");
            pressed.setAccessible(true);

            normal.set(mFloatingActionsMenu, primaryColor);
            pressed.set(mFloatingActionsMenu, secondaryColor);

            mFabMain.setColorNormal(primaryColor);
            mFabMain.setColorPressed(secondaryColor);
        } catch (Exception ignored) {

        }

        mFabJoinRoom.setColorNormal(primaryColor);
        mFabJoinRoom.setColorPressed(secondaryColor);
        mFabCreateRoom.setColorNormal(primaryColor);
        mFabCreateRoom.setColorPressed(secondaryColor);
        mFabStartChat.setColorNormal(primaryColor);
        mFabStartChat.setColorPressed(secondaryColor);

        mVectorPendingCallView.updateBackgroundColor(primaryColor);
        mSyncInProgressView.setBackgroundColor(primaryColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSyncInProgressView.setIndeterminateTintList(ColorStateList.valueOf(secondaryColor));
        } else {
            mSyncInProgressView.getIndeterminateDrawable().setColorFilter(
                    secondaryColor, android.graphics.PorterDuff.Mode.SRC_IN);
        }

        //keep the default staus bar color
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(secondaryColor);
        }
        */
        // Set color of toolbar search view
        EditText edit = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        edit.setTextColor(ThemeUtils.INSTANCE.getColor(this, R.attr.vctr_primary_text_color));
        edit.setHintTextColor(ThemeUtils.INSTANCE.getColor(this, R.attr.vctr_primary_hint_text_color));
        edit.setTextSize(15);
    }

    /**
     * Init views
     */
    private void initViews() {
        mVectorPendingCallView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IMXCall call = CallsManager.getSharedInstance().getActiveCall();
                if (null != call) {
                    final Intent intent = new Intent(VectorHomeActivity.this, VectorCallViewActivity.class);
                    intent.putExtra(VectorCallViewActivity.EXTRA_MATRIX_ID, call.getSession().getCredentials().userId);
                    intent.putExtra(VectorCallViewActivity.EXTRA_CALL_ID, call.getCallId());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(intent);
                        }
                    });
                }
            }
        });

        addUnreadBadges();

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
        mToolbar.setContentInsetStartWithNavigation(0);

        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v != null) {
                    mSearchView.setIconified(false);
                }
            }
        });

        // Set here background of labels, cause we cannot set attr color in drawable on API < 21
        Class menuClass = FloatingActionsMenu.class;
        try {
            Field fabLabelStyle = menuClass.getDeclaredField("mLabelsStyle");
            fabLabelStyle.setAccessible(true);
            fabLabelStyle.set(mFloatingActionsMenu, ThemeUtils.INSTANCE.getResourceId(this, R.style.Floating_Actions_Menu));

            Method createLabels = menuClass.getDeclaredMethod("createLabels");
            createLabels.setAccessible(true);
            createLabels.invoke(mFloatingActionsMenu);
        } catch (Exception ignored) {

        }

        mFabStartChat.setIconDrawable(ThemeUtils.INSTANCE.tintDrawableWithColor(
                ContextCompat.getDrawable(this, R.drawable.tchap_ic_new_discussion),
                ContextCompat.getColor(this, android.R.color.white)
        ));

        mFabCreateRoom.setIconDrawable(ThemeUtils.INSTANCE.tintDrawableWithColor(
                ContextCompat.getDrawable(this, R.drawable.tchap_ic_new_room),
                ContextCompat.getColor(this, android.R.color.white)
        ));

        mFabJoinRoom.setIconDrawable(ThemeUtils.INSTANCE.tintDrawableWithColor(
                ContextCompat.getDrawable(this, R.drawable.tchap_ic_join_public),
                ContextCompat.getColor(this, android.R.color.white)
        ));

        mFloatingActionsMenu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                // ignore any action if there is a pending one
                if (!isWaitingViewVisible()) {
                    if (!TchapLoginActivity.isUserExternal(mTchapSession.getMainSession())) {
                        touchGuard.animate().alpha(0.6f);

                        touchGuard.setClickable(true);
                    } else {
                        // the FAB action is temporarily blocked for external users to prevent them from
                        // creating a new direct chat, a new discussion or invite people to Tchap
                        DinsicUtils.alertSimpleMsg(VectorHomeActivity.this, getString(R.string.action_forbidden));
                    }
                }
            }

            @Override
            public void onMenuCollapsed() {
                touchGuard.animate().alpha(0);

                touchGuard.setClickable(false);
            }
        });

        touchGuard.setClickable(false);
    }

    /**
     * Reset the filter
     */
    private void resetFilter() {
        // sanity check to fix crash in log-out
        if (null != mSearchView) {
            mSearchView.setQuery("", false);
            mSearchView.clearFocus();
        }
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
        final String filter = newText + "-" + mCurrentMenuId;

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
                String currentFilter = queryText + "-" + mCurrentMenuId;

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
        switch (mCurrentMenuId) {
            //no more home nor favourite
            /*
            case R.id.bottom_action_home:
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_HOME);
                break;
            case R.id.bottom_action_favourites:
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_FAVOURITES);
                break;
                */
            case TAB_POSITION_CONTACT:
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_PEOPLE);
                break;
            case TAB_POSITION_CONVERSATION:
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_ROOMS);
                break;
            /*case R.id.bottom_action_groups:
                fragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_GROUPS);
                break;*/

        }

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

    /**
     * Display an alert to warn the user that some crypto data is corrupted.
     */
    private void displayCryptoCorruption() {
        if ((null != mTchapSession)
                && (null != mTchapSession.getMainSession().getCrypto())
                && mTchapSession.getMainSession().getCrypto().isCorrupted()) {
            final String isFirstCryptoAlertKey = "isFirstCryptoAlertKey";

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

            if (preferences.getBoolean(isFirstCryptoAlertKey, true)) {
                preferences
                        .edit()
                        .putBoolean(isFirstCryptoAlertKey, false)
                        .apply();

                new AlertDialog.Builder(this)
                        .setMessage(R.string.e2e_need_log_in_again)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        CommonActivityUtils.logout(VectorApp.getCurrentActivity());
                                    }
                                })
                        .show();
            }
        }
    }

    /**
     * Process the content of the current intent to detect universal link data.
     * If data present, it means that the app was started through an URL link, but due
     * to the App was not initialized properly, it has been required to re start the App.
     * <p>
     * To indicate the App has finished its Login/Splash/Home flow, a resume action
     * is sent to the receiver.
     */
    private void processIntentUniversalLink() {
        Intent intent;
        Uri uri;

        if (null != (intent = getIntent())) {
            if (intent.hasExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI)) {
                Log.d(LOG_TAG, "## processIntentUniversalLink(): EXTRA_UNIVERSAL_LINK_URI present1");
                uri = intent.getParcelableExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI);

                if (null != uri) {
                    // since android O
                    // set the class to avoid having "Background execution not allowed"
                    Intent myBroadcastIntent = new Intent(VectorApp.getInstance(), VectorUniversalLinkReceiver.class);
                    myBroadcastIntent.setAction(VectorUniversalLinkReceiver.BROADCAST_ACTION_UNIVERSAL_LINK_RESUME);
                    myBroadcastIntent.putExtras(getIntent().getExtras());
                    myBroadcastIntent.putExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_SENDER_ID, VectorUniversalLinkReceiver.HOME_SENDER_ID);
                    sendBroadcast(myBroadcastIntent);

                    showWaitingView();

                    // use only once, remove since it has been used
                    intent.removeExtra(VectorUniversalLinkReceiver.EXTRA_UNIVERSAL_LINK_URI);
                    Log.d(LOG_TAG, "## processIntentUniversalLink(): Broadcast BROADCAST_ACTION_UNIVERSAL_LINK_RESUME sent");
                }
            }
        }
    }

    /*
     * *********************************************************************************************
     * Floating button management
     * *********************************************************************************************
     */

    private void revealFloatingActionMenu() {
        if (null != mFloatingActionsMenu) {
            mFloatingActionsMenu.collapse();
            mFloatingActionsMenu.setVisibility(View.VISIBLE);
            ViewPropertyAnimator animator = mFabMain.animate().scaleX(1).scaleY(1).alpha(1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (mFloatingActionsMenu != null) {
                        mFloatingActionsMenu.setVisibility(View.VISIBLE);
                    }
                }
            });
            animator.start();
        }
    }

    private void concealFloatingActionMenu() {
        if (null != mFloatingActionsMenu) {
            mFloatingActionsMenu.collapse();
            ViewPropertyAnimator animator = mFabMain.animate().scaleX(0).scaleY(0).alpha(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (mFloatingActionsMenu != null) {
                        mFloatingActionsMenu.setVisibility(View.GONE);
                    }
                }
            });
            animator.start();
        }
    }

    /**
     * Hide the floating action button for 1 second
     *
     * @param fragmentTag the calling fragment tag
     */
    public void hideFloatingActionButton(String fragmentTag) {
        synchronized (this) {
            // check if the calling fragment is the current one
            // during the fragment switch, the unplugged one might call this method
            // before the new one is plugged.
            // for example, if the switch is performed while the current list is scrolling.
            if (TextUtils.equals(mCurrentFragmentTag, fragmentTag)) {
                if (null != mFloatingActionsMenu) {
                    if (mShowFloatingActionButtonRunnable == null) {
                        // Avoid repeated calls.
                        concealFloatingActionMenu();
                        mShowFloatingActionButtonRunnable = new Runnable() {
                            @Override
                            public void run() {
                                mShowFloatingActionButtonRunnable = null;
                                showFloatingActionMenuIfRequired();
                            }
                        };
                    } else {
                        mFloatingActionsMenu.removeCallbacks(mShowFloatingActionButtonRunnable);
                    }

                    try {
                        mFloatingActionsMenu.postDelayed(mShowFloatingActionButtonRunnable, 1000);
                    } catch (Throwable throwable) {
                        Log.e(LOG_TAG, "failed to postDelayed " + throwable.getMessage(), throwable);

                        if (mShowFloatingActionButtonRunnable != null && mFloatingActionsMenu != null) {
                            mFloatingActionsMenu.removeCallbacks(mShowFloatingActionButtonRunnable);
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showFloatingActionMenuIfRequired();
                            }
                        });

                    }
                }
            }
        }
    }

    /**
     * Getter for the floating action button
     *
     * @return fab view
     */
    public View getFloatingActionButton() {
        return mFabMain;
    }

    /**
     * Open the room creation with inviting people.
     */
    public void createNewDirectChat(VectorRoomInviteMembersActivity.ActionMode mode, VectorRoomInviteMembersActivity.ContactsFilter contactsFilter) {
        final Intent intent = new Intent(VectorHomeActivity.this, VectorRoomInviteMembersActivity.class);
        intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_ACTION_ACTIVITY_MODE, mode);
        intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_CONTACTS_FILTER, contactsFilter);

        // Check whether a shadow session is running
        if (mTchapSession.getShadowSession() != null) {
            // Prompt the user to know which session we have to use.
            new AlertDialog.Builder(this)
                    .setMessage(R.string.tchap_account_selection_message)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_MATRIX_ID, mTchapSession.getMainSession().getMyUserId());
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_MATRIX_ID, mTchapSession.getShadowSession().getMyUserId());
                            startActivity(intent);
                        }
                    })
                    .show();
        } else {
            // Use the main session by default
            intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_MATRIX_ID, mTchapSession.getMainSession().getMyUserId());
            startActivity(intent);
        }
    }

    /*
     * *********************************************************************************************
     * Room invitation management
     * *********************************************************************************************
     */

    @NonNull
    public List<TchapRoom> getRoomInvitations() {
        List<TchapRoom> roomInvitations = new ArrayList<>();
        MXSession session = mTchapSession.getMainSession();
        boolean isProtected = mTchapSession.getConfig().getHasProtectedAccess();

        if (null == session.getDataHandler().getStore()) {
            Log.e(LOG_TAG, "## getRoomInvitations() : null store");
            return new ArrayList<>();
        }

        Collection<RoomSummary> roomSummaries = session.getDataHandler().getStore().getSummaries();
        for (RoomSummary roomSummary : roomSummaries) {
            // reported by rageshake
            // i don't see how it is possible to have a null roomSummary
            if (null != roomSummary) {
                Room room = session.getDataHandler().getStore().getRoom(roomSummary.getRoomId());

                // check if the room exists
                // the user conference rooms are not displayed.
                if (room != null && !room.isConferenceUserRoom() && room.isInvited()) {
                    TchapRoom tchapRoom = new TchapRoom(room, session, isProtected);
                    roomInvitations.add(tchapRoom);
                }
            }
        }

        // Consider now the potential shadow session
        session = mTchapSession.getShadowSession();
        if (session != null) {
            roomSummaries = session.getDataHandler().getStore().getSummaries();
            for (RoomSummary roomSummary : roomSummaries) {
                if (null != roomSummary) {
                    Room room = session.getDataHandler().getStore().getRoom(roomSummary.getRoomId());

                    // check if the room exists
                    // the user conference rooms are not displayed.
                    if (room != null && !room.isConferenceUserRoom() && room.isInvited()) {
                        TchapRoom tchapRoom = new TchapRoom(room, session, false);
                        roomInvitations.add(tchapRoom);
                    }
                }
            }
        }

        // the invitations are sorted from the oldest to the more recent one
        Comparator<TchapRoom> invitationComparator = RoomUtils.getRoomsDateComparator( true);
        Collections.sort(roomInvitations, invitationComparator);
        return roomInvitations;
    }

    // Tchap: The room preview is disabled, this option is replaced by "join the room".
    public void onJoinRoom(MXSession session, String roomId) {
        String roomAlias = null;

        Room room = session.getDataHandler().getRoom(roomId);
        if ((null != room) && (null != room.getState())) {
            roomAlias = room.getState().getCanonicalAlias();
        }

        final RoomPreviewData roomPreviewData = new RoomPreviewData(session, roomId, null, roomAlias, null);
        showWaitingView();
        DinsicUtils.joinRoom(roomPreviewData, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void info) {
                hideWaitingView();
                DinsicUtils.onNewJoinedRoom(VectorHomeActivity.this, roomPreviewData);
            }

            private void onError(String errorMessage) {
                hideWaitingView();
                Toast.makeText(VectorHomeActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(MatrixError e) {
                // Catch here the consent request if any.
                if (MatrixError.M_CONSENT_NOT_GIVEN.equals(e.errcode)) {
                    hideWaitingView();
                    getConsentNotGivenHelper().displayDialog(e);
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
     * Create the room forget / leave callback
     *
     * @param session           the matrix session
     * @param roomId            the room id
     * @param onSuccessCallback the success callback
     * @return the asynchronous callback
     */
    private ApiCallback<Void> createForgetLeaveCallback(final MXSession session, final String roomId, final ApiCallback<Void> onSuccessCallback) {
        return new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void info) {
                // clear any pending notification for this room
                EventStreamService.cancelNotificationsForRoomId(session.getMyUserId(), roomId);
                hideWaitingView();

                if (null != onSuccessCallback) {
                    onSuccessCallback.onSuccess(null);
                }
            }

            private void onError(final String message) {
                hideWaitingView();
                Toast.makeText(VectorHomeActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(MatrixError e) {
                if (MatrixError.M_CONSENT_NOT_GIVEN.equals(e.errcode)) {
                    hideWaitingView();
                    getConsentNotGivenHelper().displayDialog(e);
                } else {
                    onError(e.getLocalizedMessage());
                }
            }

            @Override
            public void onUnexpectedError(Exception e) {
                onError(e.getLocalizedMessage());
            }
        };
    }

    /**
     * Trigger the room forget
     *
     * @param roomId            the room id
     * @param onSuccessCallback the success asynchronous callback
     */
    public void onForgetRoom(final String roomId, final ApiCallback<Void> onSuccessCallback) {
        TchapRoom tchapRoom = mTchapSession.getRoom(roomId);

        if (null != tchapRoom) {
            showWaitingView();
            tchapRoom.getRoom().forget(createForgetLeaveCallback(tchapRoom.getSession(), roomId, onSuccessCallback));
        }
    }

    /**
     * Trigger the room leave / invitation reject.
     *
     * @param roomId            the room id
     * @param onSuccessCallback the success asynchronous callback
     */
    public void onRejectInvitation(final String roomId, final ApiCallback<Void> onSuccessCallback) {
        TchapRoom tchapRoom = mTchapSession.getRoom(roomId);

        if (null != tchapRoom) {
            showWaitingView();
            tchapRoom.getRoom().leave(createForgetLeaveCallback(tchapRoom.getSession(), roomId, onSuccessCallback));
        }
    }

    /*
     * *********************************************************************************************
     * Sliding menu management
     * *********************************************************************************************
     */

    /**
     * Manage the e2e keys export.
     */
    private void exportKeysAndSignOut() {
        View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_export_e2e_keys, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.encryption_export_room_keys)
                .setView(dialogLayout);

        final TextInputEditText passPhrase1EditText = dialogLayout.findViewById(R.id.dialog_e2e_keys_passphrase_edit_text);
        final TextInputEditText passPhrase2EditText = dialogLayout.findViewById(R.id.dialog_e2e_keys_confirm_passphrase_edit_text);
        final Button exportButton = dialogLayout.findViewById(R.id.dialog_e2e_keys_export_button);
        final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                exportButton.setEnabled(!TextUtils.isEmpty(passPhrase1EditText.getText())
                        && TextUtils.equals(passPhrase1EditText.getText(), passPhrase2EditText.getText()));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        passPhrase1EditText.addTextChangedListener(textWatcher);
        passPhrase2EditText.addTextChangedListener(textWatcher);

        exportButton.setEnabled(false);

        final AlertDialog exportDialog = builder.show();

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWaitingView();

                // FIXME MULTI-ACCOUNT: Support keys export for the shadow session if any
                CommonActivityUtils.exportKeys(mTchapSession.getMainSession(), passPhrase1EditText.getText().toString(), new SimpleApiCallback<String>(VectorHomeActivity.this) {

                    @Override
                    public void onSuccess(final String filename) {
                        hideWaitingView();

                        new AlertDialog.Builder(VectorHomeActivity.this)
                                .setMessage(getString(R.string.encryption_export_saved_as, filename))
                                .setCancelable(false)
                                .setPositiveButton(R.string.action_sign_out, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        showWaitingView();
                                        CommonActivityUtils.logout(VectorHomeActivity.this);
                                    }
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    }
                });

                exportDialog.dismiss();
            }
        });
    }

    private void initSlidingMenu() {
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                /* host Activity */
                this,
                /* DrawerLayout object */
                mDrawerLayout,
                mToolbar,
                /* "open drawer" description */
                R.string.action_open,
                /* "close drawer" description */
                R.string.action_close) {

            @Override
            public void onDrawerClosed(View view) {
                switch (mSlidingMenuIndex) {
                    case R.id.sliding_menu_contacts:
                        mTopNavigationView.getTabAt(TAB_POSITION_CONTACT).select();
                        break;

                    case R.id.sliding_menu_public_rooms:
                        final Intent intent = new Intent(VectorHomeActivity.this, TchapPublicRoomSelectionActivity.class);
                        startActivity(intent);
                        break;

                    case R.id.sliding_menu_settings:
                        // launch the settings activity
                        // FIXME MULTI-ACCOUNT: Handle multi sessions in settings
                        startActivity(VectorSettingsActivity.getIntent(VectorHomeActivity.this, mTchapSession.getMainSession().getMyUserId()));
                        break;

                    /* case R.id.sliding_copyright_terms:
                        VectorUtils.displayAppCopyright();
                        break;
                    */
                    case R.id.sliding_menu_app_tac:
                        VectorUtils.displayAppTac();
                        break;

                    case R.id.sliding_menu_send_bug_report:
                        BugReporter.sendBugReport();
                        break;

                    case R.id.sliding_menu_sign_out:
                        new AlertDialog.Builder(VectorHomeActivity.this)
                                .setMessage(R.string.action_sign_out_confirmation)
                                .setCancelable(false)
                                .setPositiveButton(R.string.action_sign_out,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                showWaitingView();
                                                CommonActivityUtils.logout(VectorHomeActivity.this);
                                            }
                                        })
                                .setNeutralButton(R.string.encryption_export_export, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                exportKeysAndSignOut();
                                            }
                                        });
                                    }
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .show();

                        break;
                }

                mSlidingMenuIndex = -1;
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }
        };

        NavigationView.OnNavigationItemSelectedListener listener = new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                mDrawerLayout.closeDrawers();
                mSlidingMenuIndex = menuItem.getItemId();
                return true;
            }
        };

        navigationView.setNavigationItemSelectedListener(listener);
        mDrawerLayout.setDrawerListener(drawerToggle);

        // display the home and title button
        if (null != getSupportActionBar()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(ContextCompat.getDrawable(this, R.drawable.ic_material_menu_white));
        }
    }

    private void refreshSlidingMenu() {
        if (navigationView == null) {
            // Activity is not resumed
            return;
        }

        Menu menuNav = navigationView.getMenu();
        MenuItem signOutMenuItem = menuNav.findItem(R.id.sliding_menu_sign_out);
        if (null != signOutMenuItem) {
            setTextColorForMenuItem(signOutMenuItem, R.color.vector_fuchsia_color);
        }

        TextView aboutMenuItem = findViewById(R.id.sliding_menu_app_version);
        if (null != aboutMenuItem) {
            String version = getString(R.string.room_sliding_menu_version) + " " + VectorUtils.getApplicationVersion(this);
            aboutMenuItem.setText(version);
        }

        TextView infoMenuItem = findViewById(R.id.sliding_menu_infos);
        if (null != infoMenuItem) {
            String info = this.getString(R.string.tchap_burger_menu_info);
            infoMenuItem.setText(info);
        }

        MXSession mainSession = mTchapSession.getMainSession();

        // Display name in the header of the burger menu
        TextView displayNameTextView = navigationView.findViewById(R.id.home_menu_main_displayname);
        if (null != displayNameTextView) {
            displayNameTextView.setText(DinsicUtils.getNameFromDisplayName(mainSession.getMyUser().displayname));
        }

        TextView userIdTextView = navigationView.findViewById(R.id.home_menu_main_matrix_id);
        if (null != userIdTextView && null != mainSession) {
            // Note the user's email is retrieved by a server request here
            // It is not available when the device is offline
            // TODO store this email locally with the user's credentials
            List<org.matrix.androidsdk.rest.model.pid.ThirdPartyIdentifier> emailslist = mainSession.getMyUser().getlinkedEmails();
            if (emailslist != null)
                if (emailslist.size() != 0)
                    userIdTextView.setText(emailslist.get(0).address);
        }

        ImageView mainAvatarView = navigationView.findViewById(R.id.home_menu_main_avatar);

        if (null != mainAvatarView) {
            VectorUtils.loadUserAvatar(this, mainSession, mainAvatarView, mainSession.getMyUser());

            mainAvatarView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Open the settings
                    mSlidingMenuIndex = R.id.sliding_menu_settings;
                    mDrawerLayout.closeDrawers();
                }
            });
        } else {
            // on Android M, the mNavigationView is not loaded at launch
            // so launch asap it is rendered.
            navigationView.post(new Runnable() {
                @Override
                public void run() {
                    refreshSlidingMenu();
                }
            });
        }
    }

    private void setTextColorForMenuItem(MenuItem menuItem, @ColorRes int color) {
        SpannableString spanString = new SpannableString(menuItem.getTitle().toString());
        spanString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, color)), 0, spanString.length(), 0);
        menuItem.setTitle(spanString);
    }

    //==============================================================================================================
    // VOIP call management
    //==============================================================================================================

    /**
     * Start a call with a session Id and a call Id
     *
     * @param sessionId      the session Id
     * @param callId         the call Id
     * @param unknownDevices the unknown e2e devices
     */
    public void startCall(String sessionId, String callId, MXUsersDevicesMap<MXDeviceInfo> unknownDevices) {
        // sanity checks
        if ((null != sessionId) && (null != callId)) {
            final Intent intent = new Intent(VectorHomeActivity.this, VectorCallViewActivity.class);

            intent.putExtra(VectorCallViewActivity.EXTRA_MATRIX_ID, sessionId);
            intent.putExtra(VectorCallViewActivity.EXTRA_CALL_ID, callId);

            if (null != unknownDevices) {
                intent.putExtra(VectorCallViewActivity.EXTRA_UNKNOWN_DEVICES, unknownDevices);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startActivity(intent);
                }
            });
        }
    }

    //==============================================================================================================
    // Unread counter badges
    //==============================================================================================================

    // Badge view <-> menu entry id
    private final Map<Integer, UnreadCounterBadgeView> mBadgeViewByIndex = new HashMap<>();

    // events listener to track required refresh
    private final MXEventListener mBadgeEventsListener = new MXEventListener() {
        private boolean mRefreshBadgeOnChunkEnd = false;

        @Override
        public void onLiveEventsChunkProcessed(String fromToken, String toToken) {
            if (mRefreshBadgeOnChunkEnd) {
                refreshUnreadBadges();
                mRefreshBadgeOnChunkEnd = false;
            }
        }

        @Override
        public void onLiveEvent(final Event event, final RoomState roomState) {
            String eventType = event.getType();

            // refresh the UI at the end of the next events chunk
            mRefreshBadgeOnChunkEnd |= ((event.roomId != null) && RoomSummary.isSupportedEvent(event))
                    || Event.EVENT_TYPE_STATE_ROOM_MEMBER.equals(eventType)
                    || Event.EVENT_TYPE_REDACTION.equals(eventType)
                    || Event.EVENT_TYPE_TAGS.equals(eventType)
                    || Event.EVENT_TYPE_STATE_ROOM_THIRD_PARTY_INVITE.equals(eventType);

        }

        @Override
        public void onReceiptEvent(String roomId, List<String> senderIds) {
            // refresh only if the current user read some messages (to update the unread messages counters)
            for (MXSession session: mTchapSession.getSessions()) {
                mRefreshBadgeOnChunkEnd |= (senderIds.indexOf(session.getCredentials().userId) >= 0);
            }
        }

        @Override
        public void onLeaveRoom(final String roomId) {
            mRefreshBadgeOnChunkEnd = true;
        }

        @Override
        public void onNewRoom(String roomId) {
            mRefreshBadgeOnChunkEnd = true;
        }

        @Override
        public void onJoinRoom(String roomId) {
            mRefreshBadgeOnChunkEnd = true;
        }

        @Override
        public void onDirectMessageChatRoomsListUpdate() {
            mRefreshBadgeOnChunkEnd = true;
        }

        @Override
        public void onRoomTagEvent(String roomId) {
            mRefreshBadgeOnChunkEnd = true;
        }
    };

    /**
     * Add the badge events listener
     */
    private void addBadgeEventsListener() {
        for (MXSession session: mTchapSession.getSessions()) {
            session.getDataHandler().addListener(mBadgeEventsListener);
        }
        refreshUnreadBadges();
    }

    /**
     * Remove the badge events listener
     */
    private void removeBadgeEventsListener() {
        for (MXSession session: mTchapSession.getSessions()) {
            session.getDataHandler().removeListener(mBadgeEventsListener);
        }
    }

    /**
     * Add the unread messages badges.
     */
    @SuppressLint("RestrictedApi")
    private void addUnreadBadges() {
        final float scale = getResources().getDisplayMetrics().density;
        int badgeOffsetY = (int) (7 * scale + 0.5f);


        for (int menuIndex = 0; menuIndex < mTopNavigationView.getTabCount(); menuIndex++) {
            try {
                LinearLayout customTab = (LinearLayout) mTopNavigationView.getTabAt(menuIndex).getCustomView();

                UnreadCounterBadgeView badgeView = new UnreadCounterBadgeView(customTab.getContext());
                FrameLayout.LayoutParams badgeLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                badgeLayoutParams.setMargins(0, badgeOffsetY, 0, 0);//, iconViewLayoutParams.rightMargin, iconViewLayoutParams.bottomMargin);
                customTab.addView(badgeView,badgeLayoutParams);
                mBadgeViewByIndex.put(menuIndex, badgeView);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## addUnreadBadges failed " + e.getMessage(), e);
            }
        }

        refreshUnreadBadges();
    }

    /**
     * Refresh the badges
     */
    public void refreshUnreadBadges() {
        int roomCount = unreadRoomsCount(mTchapSession.getMainSession());
        if (roomCount != -1) {
            if (mTchapSession.getShadowSession() != null) {
                int shadowRoomCount = unreadRoomsCount(mTchapSession.getShadowSession());
                if (shadowRoomCount > 0) {
                    roomCount += shadowRoomCount;
                }
            }

            //always highligted
            mBadgeViewByIndex.get(TAB_POSITION_CONVERSATION).updateCounter(roomCount, UnreadCounterBadgeView.HIGHLIGHTED);
        }
    }

    private int unreadRoomsCount(MXSession session) {
        MXDataHandler dataHandler = session.getDataHandler();
        // fix a crash reported by GA
        if (null == dataHandler) {
            return -1;
        }

        IMXStore store = dataHandler.getStore();
        // fix a crash reported by GA
        if (null == store) {
            return -1;
        }

        BingRulesManager bingRulesManager = dataHandler.getBingRulesManager();
        Collection<RoomSummary> summaries2 = store.getSummaries();

        // compute the unread room count
        int roomCount = 0;

        for (RoomSummary summary : summaries2) {
            String roomId = summary.getRoomId();
            Room room = store.getRoom(roomId);

            if (null != room && !room.isConferenceUserRoom()) {
                if (room.isInvited()) {
                    roomCount++;
                } else {
                    int notificationCount = room.getNotificationCount();

                    if (bingRulesManager.isRoomMentionOnly(roomId)) {
                        notificationCount = room.getHighlightCount();
                    }

                    if (notificationCount > 0) {
                        roomCount++;
                    }
                }
            }
        }

        return roomCount;
    }

    /* ==========================================================================================
     * UI Event
     * ========================================================================================== */

    @OnClick(R.id.floating_action_menu_touch_guard)
    void touchGuardClicked() {
        mFloatingActionsMenu.collapse();
    }

    @OnClick(R.id.button_start_chat)
    void fabMenuStartChat() {
        mFloatingActionsMenu.collapse();
        // Create a new direct chat with an existing tchap user
        // Multi-selection will be disabled
        createNewDirectChat(VectorRoomInviteMembersActivity.ActionMode.START_DIRECT_CHAT, VectorRoomInviteMembersActivity.ContactsFilter.TCHAP_ONLY);
    }

    @OnClick(R.id.button_create_room)
    void fabMenuCreateRoom() {
        mFloatingActionsMenu.collapse();
        // Launch the new screen to create an empty room
        final Intent intent = new Intent(VectorHomeActivity.this, TchapRoomCreationActivity.class);
        // Check whether a shadow session is running
        if (mTchapSession.getShadowSession() != null) {
            // Prompt the user to know which session we have to use.
            new AlertDialog.Builder(this)
                    .setMessage(R.string.tchap_account_selection_message)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            intent.putExtra(TchapRoomCreationActivity.EXTRA_MATRIX_ID, mTchapSession.getMainSession().getMyUserId());
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            intent.putExtra(TchapRoomCreationActivity.EXTRA_MATRIX_ID, mTchapSession.getShadowSession().getMyUserId());
                            startActivity(intent);
                        }
                    })
                    .show();
        } else {
            // Use the main session by default
            intent.putExtra(TchapRoomCreationActivity.EXTRA_MATRIX_ID, mTchapSession.getMainSession().getMyUserId());
            startActivity(intent);
        }
    }

    @OnClick(R.id.button_join_room)
    void fabMenuJoinRoom() {
        mFloatingActionsMenu.collapse();
        // Open the rooms directory
        final Intent intent = new Intent(VectorHomeActivity.this, TchapPublicRoomSelectionActivity.class);
        startActivity(intent);
    }

    //==============================================================================================================
    // Events listener
    //==============================================================================================================

    /**
     * Warn the displayed fragment about room data updates.
     */
    public void onRoomDataUpdated() {
        final HomeRoomsViewModel.Result result = mRoomsViewModel.update();
        final Fragment fragment = getSelectedFragment();
        if ((null != fragment) && (fragment instanceof AbsHomeFragment)) {
            ((AbsHomeFragment) fragment).onRoomResultUpdated(result);
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
                    onRoomDataUpdated();
                }
            }

            @Override
            public void onAccountInfoUpdate(MyUser myUser) {
                refreshSlidingMenu();
            }

            @Override
            public void onInitialSyncComplete(String toToken) {
                Log.d(LOG_TAG, "## onInitialSyncComplete()");
                onRoomDataUpdated();
            }

            @Override
            public void onLiveEventsChunkProcessed(String fromToken, String toToken) {
                if ((VectorApp.getCurrentActivity() == VectorHomeActivity.this) && mRefreshOnChunkEnd) {
                    onRoomDataUpdated();
                }

                mRefreshOnChunkEnd = false;
                mSyncInProgressView.setVisibility(View.GONE);

                // treat any pending URL link workflow, that was started previously
                processIntentUniversalLink();
            }

            @Override
            public void onLiveEvent(final Event event, final RoomState roomState) {
                String eventType = event.getType();

                // refresh the UI at the end of the next events chunk
                mRefreshOnChunkEnd |= ((event.roomId != null) && RoomSummary.isSupportedEvent(event))
                        || Event.EVENT_TYPE_STATE_ROOM_MEMBER.equals(eventType)
                        || Event.EVENT_TYPE_TAGS.equals(eventType)
                        || Event.EVENT_TYPE_REDACTION.equals(eventType)
                        || Event.EVENT_TYPE_RECEIPT.equals(eventType)
                        || Event.EVENT_TYPE_STATE_ROOM_AVATAR.equals(eventType)
                        || Event.EVENT_TYPE_STATE_ROOM_THIRD_PARTY_INVITE.equals(eventType);
            }

            @Override
            public void onReceiptEvent(String roomId, List<String> senderIds) {
                // refresh only if the current user read some messages (to update the unread messages counters)
                for (MXSession session: mTchapSession.getSessions()) {
                    mRefreshOnChunkEnd |= (senderIds.indexOf(session.getCredentials().userId) >= 0);
                }
            }

            @Override
            public void onRoomTagEvent(String roomId) {
                mRefreshOnChunkEnd = true;
            }

            @Override
            public void onStoreReady() {
                onForceRefresh();

                if (null != mSharedFilesIntent) {
                    Log.d(LOG_TAG, "shared intent : the store is now ready, display sendFilesTo");
                    CommonActivityUtils.sendFilesTo(VectorHomeActivity.this, mSharedFilesIntent);
                    mSharedFilesIntent = null;
                }
            }

            @Override
            public void onLeaveRoom(final String roomId) {
                // FIXME MULTI-ACCOUNT: Support tchapSession to handle notifications in EventStreamService.
                // clear any pending notification for this room
                EventStreamService.cancelNotificationsForRoomId(mTchapSession.getMainSession().getMyUserId(), roomId);
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
                TchapRoom tchapRoom = mTchapSession.getRoom(event.roomId);
                RoomSummary summary = tchapRoom.getSession().getDataHandler().getStore().getSummary(event.roomId);

                if (null != summary) {
                    // test if the latest event is refreshed
                    Event latestReceivedEvent = summary.getLatestReceivedEvent();
                    if ((null != latestReceivedEvent) && TextUtils.equals(latestReceivedEvent.eventId, event.eventId)) {
                        onRoomDataUpdated();
                    }
                }
            }
        };

        for (MXSession session: mTchapSession.getSessions()) {
            session.getDataHandler().addListener(mEventsListener);
        }
    }

    /**
     * Remove the MXEventListener to the session listeners.
     */
    private void removeEventsListener() {
        if (mTchapSession.isAlive()) {
            for (MXSession session: mTchapSession.getSessions()) {
                session.getDataHandler().removeListener(mEventsListener);
            }
        }
    }
}
