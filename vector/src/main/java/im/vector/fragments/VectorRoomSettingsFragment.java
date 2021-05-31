/*
 * Copyright 2016 OpenMarket Ltd
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

package im.vector.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.BingRulesManager;
import org.matrix.androidsdk.core.Log;
import org.matrix.androidsdk.core.MXPatterns;
import org.matrix.androidsdk.core.ResourceUtils;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.callback.SimpleApiCallback;
import org.matrix.androidsdk.core.listeners.IMXNetworkEventListener;
import org.matrix.androidsdk.core.model.MatrixError;
import org.matrix.androidsdk.crypto.CryptoConstantsKt;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomAccountData;
import org.matrix.androidsdk.data.RoomState;
import org.matrix.androidsdk.data.RoomTag;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.listeners.MXMediaUploadListener;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.PowerLevels;
import org.matrix.androidsdk.rest.model.RoomDirectoryVisibility;
import org.matrix.androidsdk.rest.model.RoomMember;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.gouv.tchap.activity.TchapRoomAccessByLinkActivity;
import fr.gouv.tchap.sdk.session.room.model.RoomAccessRulesKt;
import fr.gouv.tchap.sdk.session.room.model.RoomRetentionKt;
import fr.gouv.tchap.util.DinsicUtils;
import fr.gouv.tchap.util.DinumPermalinkUtilsKt;
import fr.gouv.tchap.util.DinumUtilsKt;
import fr.gouv.tchap.util.RetentionConstants;
import im.vector.activity.SelectPictureActivity;
import im.vector.Matrix;
import im.vector.R;
import im.vector.activity.CommonActivityUtils;

import im.vector.activity.VectorRoomDetailsActivity;
import im.vector.activity.VectorMemberDetailsActivity;
import im.vector.preference.VectorEditTextPreference;
import im.vector.preference.VectorListPreference;
import im.vector.preference.VectorPreference;
import im.vector.push.PushManager;
import im.vector.settings.VectorLocale;
import im.vector.ui.themes.ThemeUtils;
import im.vector.util.RoomUtils;
import im.vector.util.SystemUtilsKt;
import im.vector.util.VectorUtils;

import fr.gouv.tchap.preference.TchapRoomAvatarPreference;

import static fr.gouv.tchap.config.TargetConfigurationKt.ENABLE_JOIN_BY_LINK;
import static fr.gouv.tchap.config.TargetConfigurationKt.ENABLE_ROOM_RETENTION;

public class VectorRoomSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    // internal constants values
    private static final String LOG_TAG = VectorRoomSettingsFragment.class.getSimpleName();
    private static final boolean UPDATE_UI = true;
    private static final boolean DO_NOT_UPDATE_UI = false;
    private static final int REQ_CODE_UPDATE_ROOM_AVATAR = 0x10;

    // Room access rules values
    private static final String ACCESS_RULES_ONLY_PEOPLE_INVITED = "1";
    private static final String ACCESS_RULES_ANYONE_WITH_LINK_APART_GUEST = "2";
    private static final String ACCESS_RULES_ANYONE_WITH_LINK_INCLUDING_GUEST = "3";

    // fragment extra args keys
    private static final String EXTRA_MATRIX_ID = "KEY_EXTRA_MATRIX_ID";
    private static final String EXTRA_ROOM_ID = "KEY_EXTRA_ROOM_ID";

    // preference keys: public API to access preference
    private static final String PREF_KEY_ROOM_PHOTO_AVATAR = "roomPhotoAvatar";
    private static final String PREF_KEY_ROOM_NAME = "roomNameEditText";
    private static final String PREF_KEY_ROOM_TOPIC = "roomTopicEditText";
    private static final String PREF_KEY_ROOM_TAG_LIST = "roomTagList";
    private static final String PREF_KEY_ROOM_ACCESS_RULES_LIST = "roomAccessRulesList";
    private static final String PREF_KEY_ROOM_HISTORY_READABILITY_LIST = "roomReadHistoryRulesList";
    private static final String PREF_KEY_ROOM_NOTIFICATIONS_LIST = "roomNotificationPreference";
    private static final String PREF_KEY_ROOM_LEAVE = "roomLeave";
    private static final String PREF_KEY_REMOVE_FROM_ROOMS_DIRECTORY = "removeFromRoomsDirectory";
    private static final String PREF_KEY_ROOM_ACCESS_RULE = "roomAccessRule";
    private static final String PREF_KEY_ROOM_RETENTION = "roomRetention";
    private static final String PREF_KEY_ROOM_ACCESS_BY_LINK = "roomAccessByLink";
    private static final String PREF_KEY_ROOM_INTERNAL_ID = "roomInternalId";
    private static final String PREF_KEY_ADDRESSES = "addresses";
    private static final String PREF_KEY_ADVANCED = "advanced";

    private static final String PREF_KEY_BANNED = "banned";
    private static final String PREF_KEY_BANNED_DIVIDER = "banned_divider";
    private static final String PREF_KEY_ENCRYPTION = "encryptionKey";

    private static final String PREF_KEY_FLAIR = "flair";
    private static final String PREF_KEY_FLAIR_DIVIDER = "flair_divider";

    private static final String ADDRESSES_PREFERENCE_KEY_BASE = "ADDRESSES_PREFERENCE_KEY_BASE";
    private static final String NO_LOCAL_ADDRESS_PREFERENCE_KEY = "NO_LOCAL_ADDRESS_PREFERENCE_KEY";
    private static final String ADD_ADDRESSES_PREFERENCE_KEY = "ADD_ADDRESSES_PREFERENCE_KEY";

    private static final String BANNED_PREFERENCE_KEY_BASE = "BANNED_PREFERENCE_KEY_BASE";

    private static final String FLAIR_PREFERENCE_KEY_BASE = "FLAIR_PREFERENCE_KEY_BASE";

    private static final String UNKNOWN_VALUE = "UNKNOWN_VALUE";

    // business code
    private MXSession mSession;
    private Room mRoom;
    private BingRulesManager mBingRulesManager;
    private boolean mIsUiUpdateSkipped;
    private Boolean mIsForumRoom;
    private Boolean mIsMentionsOnlyNotSupported = false;

    // addresses
    private PreferenceCategory mAddressesSettingsCategory;

    // other
    private PreferenceCategory mAdvancedSettingsCategory;

    // banned members
    private PreferenceCategory mBannedMembersSettingsCategory;
    private Preference mBannedMembersSettingsCategoryDivider;

    // flair
    private PreferenceCategory mFlairSettingsCategory;

    // UI elements
    private TchapRoomAvatarPreference mRoomPhotoAvatar;
    private EditTextPreference mRoomNameEditTxt;
    private EditTextPreference mRoomTopicEditTxt;
    private Preference mRemoveFromDirectoryPreference;
    private ListPreference mRoomRetentionPreference;
    private VectorPreference mRoomAccessByLinkPreference;
    private Preference mRoomAccessRulePreference;
    private ListPreference mRoomTagListPreference;
    private VectorListPreference mRoomAccessRulesListPreference;
    private ListPreference mRoomHistoryReadabilityRulesListPreference;
    private View mParentLoadingView;
    private View mParentFragmentContainerView;
    private ListPreference mRoomNotificationsPreference;

    // disable some updates if there is
    private final IMXNetworkEventListener mNetworkListener = new IMXNetworkEventListener() {
        @Override
        public void onNetworkConnectionUpdate(boolean isConnected) {
            updateUi();
        }
    };

    // update field listener
    private final ApiCallback<Void> mUpdateCallback = new ApiCallback<Void>() {
        /**
         * refresh the fragment.
         * @param mode true to force refresh
         */
        private void onDone(final String message, final boolean mode) {
            if (null != getActivity()) {
                if (!TextUtils.isEmpty(message)) {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                }

                // ensure that the response has been sent in the UI thread
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingView(mode);
                    }
                });
            }
        }

        @Override
        public void onSuccess(Void info) {
            Log.d(LOG_TAG, "##update succeed");
            onDone(null, UPDATE_UI);
        }

        @Override
        public void onNetworkError(Exception e) {
            Log.e(LOG_TAG, "##NetworkError " + e.getLocalizedMessage());
            onDone(getString(R.string.network_error_please_check_and_retry), UPDATE_UI);
        }

        @Override
        public void onMatrixError(MatrixError e) {
            Log.e(LOG_TAG, "##MatrixError " + e.getLocalizedMessage());
            onDone(getString(R.string.tchap_error_message_default), UPDATE_UI);
        }

        @Override
        public void onUnexpectedError(Exception e) {
            Log.e(LOG_TAG, "##UnexpectedError " + e.getLocalizedMessage());
            onDone(getString(R.string.tchap_error_message_default), UPDATE_UI);
        }
    };

    // update field listener
    private final ApiCallback<Void> mUpdateAvatarCallback = new ApiCallback<Void>() {
        /**
         * refresh the fragment.
         * @param mode true to force refresh
         */
        private void onDone(final String message, final boolean mode) {
            if (null != getActivity()) {
                if (!TextUtils.isEmpty(message)) {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                }

                // ensure that the response has been sent in the UI thread
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingView(mode);
                    }
                });
            }
        }

        @Override
        public void onSuccess(Void info) {
            Log.d(LOG_TAG, "##update succeed");
            onDone(null, UPDATE_UI);
            if (getActivity() instanceof VectorRoomDetailsActivity){
                ((VectorRoomDetailsActivity) getActivity()).refreshAvatar();
            }
        }

        @Override
        public void onNetworkError(Exception e) {
            Log.w(LOG_TAG, "##NetworkError " + e.getLocalizedMessage());
            onDone(e.getLocalizedMessage(), DO_NOT_UPDATE_UI);
        }

        @Override
        public void onMatrixError(MatrixError e) {
            Log.w(LOG_TAG, "##MatrixError " + e.getLocalizedMessage());
            onDone(e.getLocalizedMessage(), DO_NOT_UPDATE_UI);
        }

        @Override
        public void onUnexpectedError(Exception e) {
            Log.w(LOG_TAG, "##UnexpectedError " + e.getLocalizedMessage());
            onDone(e.getLocalizedMessage(), DO_NOT_UPDATE_UI);
        }
    };

    // MX system events listener
    private final MXEventListener mEventListener = new MXEventListener() {
        @Override
        public void onLiveEvent(final Event event, RoomState roomState) {
            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    String eventType = event.getType();

                    // The various events that could possibly change the fragment items
                    if (Event.EVENT_TYPE_STATE_ROOM_NAME.equals(eventType)
                            || Event.EVENT_TYPE_STATE_ROOM_ALIASES.equals(eventType)
                            || Event.EVENT_TYPE_STATE_ROOM_MEMBER.equals(eventType)
                            || Event.EVENT_TYPE_STATE_ROOM_AVATAR.equals(eventType)
                            || Event.EVENT_TYPE_STATE_ROOM_TOPIC.equals(eventType)
                            || Event.EVENT_TYPE_STATE_ROOM_POWER_LEVELS.equals(eventType)
                            || Event.EVENT_TYPE_STATE_HISTORY_VISIBILITY.equals(eventType)
                            || Event.EVENT_TYPE_STATE_ROOM_JOIN_RULES.equals(eventType)    // room access rules
                            || Event.EVENT_TYPE_STATE_ROOM_GUEST_ACCESS.equals(eventType)  // room access rules
                            || RoomAccessRulesKt.EVENT_TYPE_STATE_ROOM_ACCESS_RULES.equals(eventType)
                            || RoomRetentionKt.EVENT_TYPE_STATE_ROOM_RETENTION.equals(eventType)
                    ) {
                        Log.d(LOG_TAG, "## onLiveEvent() event = " + eventType);
                        updateUi();

                        if (RoomAccessRulesKt.EVENT_TYPE_STATE_ROOM_ACCESS_RULES.equals(eventType)
                                && getActivity() instanceof VectorRoomDetailsActivity) {
                            // Refresh the avatar border color
                            ((VectorRoomDetailsActivity) getActivity()).refreshAvatar();
                        }
                    }

                    if (Event.EVENT_TYPE_MESSAGE_ENCRYPTION.equals(eventType)) {
                        refreshEndToEnd();
                    }

                    // aliases
                    if (Event.EVENT_TYPE_STATE_CANONICAL_ALIAS.equals(eventType)
                            || Event.EVENT_TYPE_STATE_ROOM_ALIASES.equals(eventType)
                            || Event.EVENT_TYPE_STATE_ROOM_POWER_LEVELS.equals(eventType)
                    ) {
                        Log.d(LOG_TAG, "## onLiveEvent() refresh the addresses list");
                        refreshAddresses();
                    }

                    if (Event.EVENT_TYPE_STATE_ROOM_MEMBER.equals(eventType)) {
                        Log.d(LOG_TAG, "## onLiveEvent() refresh the banned members list");
                        refreshBannedMembersList();
                    }
                }
            });
        }

        @Override
        public void onRoomFlush(String roomId) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUi();
                }
            });
        }

        @Override
        public void onRoomTagEvent(String roomId) {
            Log.d(LOG_TAG, "## onRoomTagEvent()");
            updateUi();
        }

        @Override
        public void onBingRulesUpdate() {
            updateUi();
        }
    };

    public static VectorRoomSettingsFragment newInstance(String aMatrixId, String aRoomId) {
        VectorRoomSettingsFragment theFragment = new VectorRoomSettingsFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_MATRIX_ID, aMatrixId);
        args.putString(EXTRA_ROOM_ID, aRoomId);
        theFragment.setArguments(args);

        return theFragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(LOG_TAG, "## onCreatePreferences() IN");

        // retrieve fragment extras
        String matrixId = getArguments().getString(EXTRA_MATRIX_ID);
        String roomId = getArguments().getString(EXTRA_ROOM_ID);

        if (TextUtils.isEmpty(matrixId) || TextUtils.isEmpty(roomId)) {
            Log.e(LOG_TAG, "## onCreatePreferences(): fragment extras (MatrixId or RoomId) are missing");
            getActivity().finish();
        } else {
            mSession = Matrix.getInstance(getActivity()).getSession(matrixId);
            if ((null != mSession) && mSession.isAlive()) {
                mRoom = mSession.getDataHandler().getRoom(roomId);
                mBingRulesManager = mSession.getDataHandler().getBingRulesManager();
            }

            if (null == mRoom) {
                Log.e(LOG_TAG, "## onCreatePreferences(): unable to retrieve Room object");
                getActivity().finish();
            }

            // Tchap: The "Mentions Only" mode is not supported for the encrypted rooms on Google Play variant.
            // The pusher is not able to send pushes only on mentions because the content is encrypted.
            final PushManager pushManager = Matrix.getInstance(getActivity()).getPushManager();
            mIsMentionsOnlyNotSupported = (pushManager.useFcm() && mRoom.isEncrypted());
        }

        // load preference xml file
        addPreferencesFromResource(R.xml.vector_room_settings_preferences);

        // init preference fields
        mRoomPhotoAvatar = (TchapRoomAvatarPreference) findPreference(PREF_KEY_ROOM_PHOTO_AVATAR);
        mRoomNameEditTxt = (EditTextPreference) findPreference(PREF_KEY_ROOM_NAME);
        mRoomTopicEditTxt = (EditTextPreference) findPreference(PREF_KEY_ROOM_TOPIC);
        mRoomTagListPreference = (ListPreference) findPreference(PREF_KEY_ROOM_TAG_LIST);
        mRoomAccessRulesListPreference = (VectorListPreference) findPreference(PREF_KEY_ROOM_ACCESS_RULES_LIST);
        mRoomHistoryReadabilityRulesListPreference = (ListPreference) findPreference(PREF_KEY_ROOM_HISTORY_READABILITY_LIST);
        mAddressesSettingsCategory = (PreferenceCategory) getPreferenceManager().findPreference(PREF_KEY_ADDRESSES);
        mAdvancedSettingsCategory = (PreferenceCategory) getPreferenceManager().findPreference(PREF_KEY_ADVANCED);
        mBannedMembersSettingsCategory = (PreferenceCategory) getPreferenceManager().findPreference(PREF_KEY_BANNED);
        mBannedMembersSettingsCategoryDivider = getPreferenceManager().findPreference(PREF_KEY_BANNED_DIVIDER);
        mFlairSettingsCategory = (PreferenceCategory) getPreferenceManager().findPreference(PREF_KEY_FLAIR);
        mRoomNotificationsPreference = (ListPreference) getPreferenceManager().findPreference(PREF_KEY_ROOM_NOTIFICATIONS_LIST);

        if (null != mRoomNotificationsPreference && mIsMentionsOnlyNotSupported) {
            mRoomNotificationsPreference.setEntries(R.array.minimized_notification_entries);
            mRoomNotificationsPreference.setEntryValues(R.array.minimized_notification_values);
        }

        if (null != mRoomAccessRulesListPreference) {
            mRoomAccessRulesListPreference.setOnPreferenceWarningIconClickListener(new VectorListPreference.OnPreferenceWarningIconClickListener() {
                @Override
                public void onWarningIconClick(Preference preference) {
                    displayAccessRoomWarning();
                }
            });
        }

        // display the room Id.
        Preference roomInternalIdPreference = findPreference(PREF_KEY_ROOM_INTERNAL_ID);
        if (null != roomInternalIdPreference) {
            roomInternalIdPreference.setSummary(mRoom.getRoomId());

            roomInternalIdPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SystemUtilsKt.copyToClipboard(getActivity(), mRoom.getRoomId());
                    return false;
                }
            });
        }

        // leave room
        Preference leaveRoomPreference = findPreference(PREF_KEY_ROOM_LEAVE);

        if (null != leaveRoomPreference) {
            leaveRoomPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // leave room
                    RoomUtils.showLeaveRoomDialog(getActivity(), mSession, mRoom, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            displayLoadingView();

                            mRoom.leave(new ApiCallback<Void>() {
                                @Override
                                public void onSuccess(Void info) {
                                    if (null != getActivity()) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                getActivity().finish();
                                            }
                                        });
                                    }
                                }

                                private void onError(final String errorMessage) {
                                    if (null != getActivity()) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                hideLoadingView(true);
                                                Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
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

                        }
                    });
                    return true;
                }
            });
        }

        // Remove the room from the rooms directory
        mRemoveFromDirectoryPreference = findPreference(PREF_KEY_REMOVE_FROM_ROOMS_DIRECTORY);

        if (null != mRemoveFromDirectoryPreference) {
            mRemoveFromDirectoryPreference.setEnabled(false);
            mRemoveFromDirectoryPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.dialog_title_warning)
                            .setMessage(R.string.tchap_room_settings_remove_from_directory_prompt_msg)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    removeFromRoomsDirectory();
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                    return true;
                }
            });
        }

        // Display the room retention period
        mRoomRetentionPreference = (ListPreference) getPreferenceManager().findPreference(PREF_KEY_ROOM_RETENTION);

        // Handle the room access by link
        mRoomAccessByLinkPreference = (VectorPreference)findPreference(PREF_KEY_ROOM_ACCESS_BY_LINK);

        // Handle the room access rules
        mRoomAccessRulePreference = findPreference(PREF_KEY_ROOM_ACCESS_RULE);
        if (null != mRoomAccessRulePreference && DinumUtilsKt.isSecure()) {
            // remove this option in Tchap-secure
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            preferenceScreen.removePreference(mRoomAccessRulePreference);
            mRoomAccessRulePreference = null;
        }

        // init the room avatar: session and room
        mRoomPhotoAvatar.setConfiguration(mSession, mRoom);
        mRoomPhotoAvatar.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if ((null != mRoomPhotoAvatar) && mRoomPhotoAvatar.isEnabled()) {
                    onRoomAvatarPreferenceChanged();
                    return true; //True if the click was handled.
                } else
                    return false;
            }
        });

        // listen to preference changes
        enableSharedPreferenceListener(true);

        setRetainInstance(true);
    }

    private void removeFromRoomsDirectory() {
        displayLoadingView();

        // Several changes are required:
        // The new members can access only on invite
        // The encryption has to be enabled by default
        // The room will become private
        // The history visibility value is replaced with invited

        // Update first the joinrule to INVITE
        mRoom.updateJoinRules(RoomState.JOIN_RULE_INVITE, new ApiCallback<Void>() {

            @Override
            public void onSuccess(Void info) {
                // Turn on the encryption in this room (if this is not already done)
                if (!mRoom.isEncrypted()) {
                    mRoom.enableEncryptionWithAlgorithm(CryptoConstantsKt.MXCRYPTO_ALGORITHM_MEGOLM, new ApiCallback<Void>() {

                        @Override
                        public void onSuccess(Void info) {
                            // Remove the room from the room directory (and update history visibility)
                            updateDirectoryVisibilityAndRoomHistoryVisibility();
                        }

                        @Override
                        public void onNetworkError(Exception e) {
                            onRemoveFromDirectoryError(e.getLocalizedMessage());
                        }

                        @Override
                        public void onMatrixError(MatrixError e) {
                            onRemoveFromDirectoryError(e.getLocalizedMessage());
                        }

                        @Override
                        public void onUnexpectedError(Exception e) {
                            onRemoveFromDirectoryError(e.getLocalizedMessage());
                        }
                    });
                } else {
                    // Remove the room from the room directory (and update history visibility)
                    updateDirectoryVisibilityAndRoomHistoryVisibility();
                }
            }

            @Override
            public void onNetworkError(Exception e) {
                onRemoveFromDirectoryError(e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(MatrixError e) {
                onRemoveFromDirectoryError(e.getLocalizedMessage());
            }

            @Override
            public void onUnexpectedError(Exception e) {
                onRemoveFromDirectoryError(e.getLocalizedMessage());
            }
        });
    }

    private void updateDirectoryVisibilityAndRoomHistoryVisibility() {
        // Remove the room from the room directory
        mRoom.updateDirectoryVisibility(RoomDirectoryVisibility.DIRECTORY_VISIBILITY_PRIVATE, mUpdateCallback);

        // Update the history visibility value (by ignoring a potential error)
        mRoom.updateHistoryVisibility(RoomState.HISTORY_VISIBILITY_INVITED, new SimpleApiCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Do nothing
            }
        });
    }

    private void onRemoveFromDirectoryError(final String errorMessage) {
        hideLoadingView(UPDATE_UI);
        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void allowExternalsToJoin() {
        displayLoadingView();

        // The room will become unrestricted
        DinsicUtils.setRoomAccessRule(mSession, mRoom, RoomAccessRulesKt.UNRESTRICTED, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void info) {
                mUpdateCallback.onSuccess(info);
            }

            @Override
            public void onNetworkError(Exception e) {
                mUpdateCallback.onNetworkError(e);
            }

            @Override
            public void onMatrixError(MatrixError e) {
                // Notify the user that this change is not allowed for the moment, this will allowed soon
                String joinRule = mRoom.getState().join_rule;
                if (MatrixError.FORBIDDEN.equals(e.errcode) && RoomState.JOIN_RULE_PUBLIC.equals(joinRule)) {
                    Log.e(LOG_TAG, "##MatrixError " + e.getLocalizedMessage());
                    if (null != getActivity()) {
                        Toast.makeText(getActivity(), R.string.tchap_room_settings_allow_external_users_forbidden, Toast.LENGTH_LONG).show();
                        hideLoadingView(UPDATE_UI);
                    }
                } else {
                    mUpdateCallback.onMatrixError(e);
                }
            }

            @Override
            public void onUnexpectedError(Exception e) {
                mUpdateCallback.onUnexpectedError(e);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        View listView = view.findViewById(android.R.id.list);

        if (null != listView) {
            listView.setPadding(0, 0, 0, 0);
        }

        // seems known issue that the preferences screen does not use the activity theme
        view.setBackgroundColor(ThemeUtils.INSTANCE.getColor(getActivity(), android.R.attr.colorBackground));
        return view;
    }

    /**
     * This method expects a view with the id "settings_loading_layout",
     * that is present in the parent activity layout.
     *
     * @param view               fragment view
     * @param savedInstanceState bundle instance state
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // retrieve the loading screen in the parent view
        View parent = getView();
        if (null == mParentLoadingView) {
            while ((null != parent) && (null == mParentLoadingView)) {
                mParentLoadingView = parent.findViewById(R.id.settings_loading_layout);
                parent = (View) parent.getParent();
            }
        }

        // retrieve the parent fragment container view to disable access to the settings
        // while the loading screen is enabled
        parent = getView();
        if (null == mParentFragmentContainerView) {
            while ((null != parent) && (null == mParentFragmentContainerView)) {
                mParentFragmentContainerView = parent.findViewById(R.id.room_details_fragment_container);
                parent = (View) parent.getParent();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (null != mRoom) {
            Matrix.getInstance(getActivity()).removeNetworkEventListener(mNetworkListener);
            mRoom.removeEventListener(mEventListener);
        }

        // remove preference changes listener
        enableSharedPreferenceListener(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (null != mRoom) {
            Matrix.getInstance(getActivity()).addNetworkEventListener(mNetworkListener);
            mRoom.addEventListener(mEventListener);
            updateUi();

            refreshAddresses();
            refreshFlair();
            refreshBannedMembersList();
            refreshEndToEnd();
        }
    }

    /**
     * Enable the preference listener according to the aIsListenerEnabled value.
     *
     * @param aIsListenerEnabled true to enable the listener, false otherwise
     */
    private void enableSharedPreferenceListener(boolean aIsListenerEnabled) {
        Log.d(LOG_TAG, "## enableSharedPreferenceListener(): aIsListenerEnabled=" + aIsListenerEnabled);

        mIsUiUpdateSkipped = !aIsListenerEnabled;

        try {
            //SharedPreferences prefMgr = getActivity().getSharedPreferences("VectorSettingsFile", Context.MODE_PRIVATE);
            SharedPreferences prefMgr = PreferenceManager.getDefaultSharedPreferences(getActivity());

            if (aIsListenerEnabled) {
                prefMgr.registerOnSharedPreferenceChangeListener(this);
            } else {
                prefMgr.unregisterOnSharedPreferenceChangeListener(this);
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "## enableSharedPreferenceListener(): Exception Msg=" + ex.getMessage(), ex);
        }
    }

    /**
     * Update the preferences according to the power levels and its values.
     * To prevent the preference change listener to be triggered, the listener
     * is removed when the preferences are updated.
     */
    private void updateUi() {
        // configure the preferences that are allowed to be modified by the user
        updatePreferenceAccessFromPowerLevel();

        // need to run on the UI thread to be taken into account
        // when updatePreferenceUiValues() will be performed
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // disable listener during preferences update, otherwise it will
                // be seen as a user action..
                enableSharedPreferenceListener(false);
                // set settings UI values
                updatePreferenceUiValues();
                // re enable preferences listener..
                enableSharedPreferenceListener(true);

                updateRoomDirectoryVisibilityAsync();
            }
        });
    }

    /**
     * delayed refresh the preferences items.
     */
    private void updateUiOnUiThread() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUi();
            }
        });
    }

    /**
     * Retrieve the room visibility directory value and update the corresponding preference.
     * This is an asynchronous request: the call-back response will be processed on the UI thread.
     * For now, the room visibility directory value is not provided in the sync API, a specific request
     * must performed.
     */
    private void updateRoomDirectoryVisibilityAsync() {
        if (null == mRoom) {
            Log.w(LOG_TAG, "## updateRoomDirectoryVisibilityUi(): not processed due to invalid parameters");
        } else {
            displayLoadingView();

            // server request: is the room listed in the room directory?
            mRoom.getDirectoryVisibility(mRoom.getRoomId(), new ApiCallback<String>() {

                private void handleResponseOnUiThread(final String aVisibilityValue) {
                    if (!isAdded()) {
                        return;
                    }

                    boolean isPublicRoom = RoomDirectoryVisibility.DIRECTORY_VISIBILITY_PUBLIC.equals(aVisibilityValue);
                    mIsForumRoom = isPublicRoom;

                    updatePreferenceAccessFromPowerLevel();

                    if (null != mRemoveFromDirectoryPreference) {
                        if (isPublicRoom) {
                            mRemoveFromDirectoryPreference.setEnabled(true);
                        } else {
                            // Remove this option
                            PreferenceScreen preferenceScreen = getPreferenceScreen();
                            preferenceScreen.removePreference(mRemoveFromDirectoryPreference);
                            mRemoveFromDirectoryPreference = null;
                        }
                    }

                    // Stop loading screen and do not update UI since the
                    // update is just done above.
                    hideLoadingView(DO_NOT_UPDATE_UI);
                }

                @Override
                public void onSuccess(String visibility) {
                    handleResponseOnUiThread(visibility);
                }

                @Override
                public void onNetworkError(Exception e) {
                    Log.w(LOG_TAG, "## getDirectoryVisibility(): onNetworkError Msg=" + e.getLocalizedMessage());
                    handleResponseOnUiThread(null);
                }

                @Override
                public void onMatrixError(MatrixError matrixError) {
                    Log.w(LOG_TAG, "## getDirectoryVisibility(): onMatrixError Msg=" + matrixError.getLocalizedMessage());
                    handleResponseOnUiThread(null);
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    Log.w(LOG_TAG, "## getDirectoryVisibility(): onUnexpectedError Msg=" + e.getLocalizedMessage());
                    handleResponseOnUiThread(null);
                }
            });
        }
    }


    /**
     * Display the access room warning.
     */
    private void displayAccessRoomWarning() {
        Toast.makeText(getActivity(), R.string.room_settings_room_access_warning, Toast.LENGTH_SHORT).show();
    }

    /**
     * Enable / disable preferences according to the power levels.
     */
    private void updatePreferenceAccessFromPowerLevel() {
        boolean canUpdateAvatar = false;
        boolean canUpdateName = false;
        boolean canUpdateTopic = false;
        boolean isAdmin = false;
        boolean isConnected = Matrix.getInstance(getActivity()).isConnected();

        // cannot refresh if there is no valid session / room
        if ((null != mRoom) && (null != mSession)) {
            PowerLevels powerLevels = mRoom.getState().getPowerLevels();

            if (null != powerLevels) {
                int powerLevel = powerLevels.getUserPowerLevel(mSession.getMyUserId());
                canUpdateAvatar = powerLevel >= powerLevels.minimumPowerLevelForSendingEventAsStateEvent(Event.EVENT_TYPE_STATE_ROOM_AVATAR);
                canUpdateName = powerLevel >= powerLevels.minimumPowerLevelForSendingEventAsStateEvent(Event.EVENT_TYPE_STATE_ROOM_NAME);
                canUpdateTopic = powerLevel >= powerLevels.minimumPowerLevelForSendingEventAsStateEvent(Event.EVENT_TYPE_STATE_ROOM_TOPIC);
                isAdmin = (powerLevel >= CommonActivityUtils.UTILS_POWER_LEVEL_ADMIN);
            }
        } else {
            Log.w(LOG_TAG, "## updatePreferenceAccessFromPowerLevel(): session or room may be missing");
        }

        if (null != mRoomPhotoAvatar)
            mRoomPhotoAvatar.setEnabled(canUpdateAvatar && isConnected);


        if (null != mRoomNameEditTxt)
            mRoomNameEditTxt.setEnabled(canUpdateName && isConnected);

        if (null != mRoomTopicEditTxt)
            mRoomTopicEditTxt.setEnabled(canUpdateTopic && isConnected);

        if (null != mRemoveFromDirectoryPreference) {
            if (!isAdmin || !isConnected) {
                // Remove this option
                PreferenceScreen preferenceScreen = getPreferenceScreen();
                preferenceScreen.removePreference(mRemoveFromDirectoryPreference);
                mRemoveFromDirectoryPreference = null;
            }
        }

        if (null != mRoomAccessRulePreference) {
            // Get the current room access
            mRoomAccessRulePreference.setOnPreferenceClickListener(null);
            mRoomAccessRulePreference.setEnabled(true);

            String rule = DinsicUtils.getRoomAccessRule(mRoom);
            if (TextUtils.equals(rule, RoomAccessRulesKt.RESTRICTED)) {
                // The room admin is allowed to open a room to the external users, except if the room is published to the rooms directory.
                if (isAdmin && isConnected && (mIsForumRoom != null && !mIsForumRoom)) {
                    mRoomAccessRulePreference.setTitle(getString(R.string.tchap_room_settings_allow_external_users_to_join));
                    mRoomAccessRulePreference.setSummary(null);
                    mRoomAccessRulePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.dialog_title_warning)
                                    .setMessage(R.string.tchap_room_settings_allow_external_users_to_join_prompt_msg)
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            allowExternalsToJoin();
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, null)
                                    .show();
                            return true;
                        }
                    });
                } else {
                    mRoomAccessRulePreference.setTitle(getString(R.string.tchap_room_settings_room_access_title));
                    mRoomAccessRulePreference.setSummary(getString(R.string.tchap_room_settings_room_access_restricted));
                }
            } else {
                mRoomAccessRulePreference.setTitle(getString(R.string.tchap_room_settings_room_access_title));
                mRoomAccessRulePreference.setSummary(getString(R.string.tchap_room_settings_room_access_unrestricted));
            }
        }

        if (null != mRoomRetentionPreference) {
            if (ENABLE_ROOM_RETENTION) {
                // Only the room admin is able to change this value
                mRoomRetentionPreference.setEnabled(isAdmin && isConnected);
            } else {
                // Remove this option
                PreferenceScreen preferenceScreen = getPreferenceScreen();
                preferenceScreen.removePreference(mRoomRetentionPreference);
                mRoomRetentionPreference = null;
            }
        }

        if (null != mRoomAccessByLinkPreference) {
            if (ENABLE_JOIN_BY_LINK) {
                mRoomAccessByLinkPreference.setOnPreferenceClickListener(null);
                mRoomAccessByLinkPreference.setEnabled(true);
                Boolean isChevronIconVisible;

                String joinRule = mRoom.getState().join_rule;
                if (RoomState.JOIN_RULE_INVITE.equals(joinRule)) {
                    mRoomAccessByLinkPreference.setSummary(getString(R.string.tchap_room_settings_room_access_by_link_disabled));
                    // Only the room admin is able to change this value
                    isChevronIconVisible = (isAdmin && isConnected);
                } else {
                    mRoomAccessByLinkPreference.setSummary(getString(R.string.tchap_room_settings_room_access_by_link_enabled));
                    // Allow anyone to open "Access by link screen" to get the link. Admin will be able to change option too.
                    isChevronIconVisible = true;
                }

                if (isChevronIconVisible) {
                    mRoomAccessByLinkPreference.setChevronIconVisible(true);
                    mRoomAccessByLinkPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent roomAccessByLinkIntent = new Intent(getActivity(), TchapRoomAccessByLinkActivity.class);
                            roomAccessByLinkIntent.putExtra(TchapRoomAccessByLinkActivity.EXTRA_ROOM_ID, mRoom.getRoomId());
                            if (mIsForumRoom != null) {
                                roomAccessByLinkIntent.putExtra(TchapRoomAccessByLinkActivity.EXTRA_IS_FORUM_ROOM, mIsForumRoom);
                            }
                            getActivity().startActivity(roomAccessByLinkIntent);
                            return false;
                        }
                    });
                }
            } else {
                // Remove this option
                PreferenceScreen preferenceScreen = getPreferenceScreen();
                preferenceScreen.removePreference(mRoomAccessByLinkPreference);
                mRoomAccessByLinkPreference = null;
            }
        }

        // room tagging: no power condition
        if (null != mRoomTagListPreference)
            mRoomTagListPreference.setEnabled(isConnected);

        // room access rules: admin only
        if (null != mRoomAccessRulesListPreference) {
            mRoomAccessRulesListPreference.setEnabled(isAdmin && isConnected);
            mRoomAccessRulesListPreference.setWarningIconVisible((0 == mRoom.getAliases().size())
                    && !TextUtils.equals(RoomState.JOIN_RULE_INVITE, mRoom.getState().join_rule));
        }

        // room read history: admin only
        if (null != mRoomHistoryReadabilityRulesListPreference) {
            mRoomHistoryReadabilityRulesListPreference.setEnabled(isAdmin && isConnected);
        }

        if (null != mRoomNotificationsPreference) {
            mRoomNotificationsPreference.setEnabled(isConnected);
        }
    }


    /**
     * Update the UI preference from the values taken from
     * the SDK layer.
     */
    private void updatePreferenceUiValues() {
        String value;

        if ((null == mSession) || (null == mRoom)) {
            Log.w(LOG_TAG, "## updatePreferenceUiValues(): session or room may be missing");
            return;
        }

        if (null != mRoomPhotoAvatar) {
            mRoomPhotoAvatar.refreshAvatar();
        }

        // update the room name preference
        if (null != mRoomNameEditTxt) {
            value = mRoom.getState().name;
            mRoomNameEditTxt.setSummary(value);
            mRoomNameEditTxt.setText(value);
        }

        // update the room topic preference
        if (null != mRoomTopicEditTxt) {
            value = mRoom.getTopic();
            mRoomTopicEditTxt.setSummary(value);
            mRoomTopicEditTxt.setText(value);
        }

        // check if fragment is added to its Activity
        if (!isAdded()) {
            Log.e(LOG_TAG, "## updatePreferenceUiValues(): fragment not added to Activity - isAdded()=false");
            return;
        }

        // room guest access rules
        if ((null != mRoomAccessRulesListPreference)) {
            String joinRule = mRoom.getState().join_rule;
            String guestAccessRule = mRoom.getState().getGuestAccess();

            if (RoomState.JOIN_RULE_INVITE.equals(joinRule)/* && RoomState.GUEST_ACCESS_CAN_JOIN.equals(guestAccessRule)*/) {
                // "Only people who have been invited" requires: {join_rule: "invite"} and {guest_access: "can_join"}
                value = ACCESS_RULES_ONLY_PEOPLE_INVITED;
            } else if (RoomState.JOIN_RULE_PUBLIC.equals(joinRule) && RoomState.GUEST_ACCESS_FORBIDDEN.equals(guestAccessRule)) {
                // "Anyone who knows the room's link, apart from guests" requires: {join_rule: "public"} and {guest_access: "forbidden"}
                value = ACCESS_RULES_ANYONE_WITH_LINK_APART_GUEST;
            } else if (RoomState.JOIN_RULE_PUBLIC.equals(joinRule) && RoomState.GUEST_ACCESS_CAN_JOIN.equals(guestAccessRule)) {
                // "Anyone who knows the room's link, including guests" requires: {join_rule: "public"} and {guest_access: "can_join"}
                value = ACCESS_RULES_ANYONE_WITH_LINK_INCLUDING_GUEST;
            } else {
                // unknown combination value
                value = null;
                Log.w(LOG_TAG, "## updatePreferenceUiValues(): unknown room access configuration joinRule=" + joinRule +
                        " and guestAccessRule=" + guestAccessRule);
            }

            if (null != value) {
                mRoomAccessRulesListPreference.setValue(value);
            } else {
                mRoomAccessRulesListPreference.setValue(UNKNOWN_VALUE);
            }
        }

        if (null != mRoomRetentionPreference) {
            int period = DinumUtilsKt.getRoomRetention(mRoom);
            mRoomRetentionPreference.setValue(DinumUtilsKt.getRetentionPreferenceValue(period));
            mRoomRetentionPreference.setSummary(DinumUtilsKt.getRetentionLabel(getActivity(), period));
        }

        if (null != mRoomNotificationsPreference) {
            String stateValue = BingRulesManager.RoomNotificationState.MUTE.name();
            BingRulesManager.RoomNotificationState state = mSession.getDataHandler().getBingRulesManager().getRoomNotificationState(mRoom.getRoomId());
            if (state != null) {
                switch (state) {
                    case ALL_MESSAGES_NOISY:
                    case ALL_MESSAGES:
                        // Tchap: We don't distinguish these 2 modes
                        // All the room notifications are noisy by default in the bing rules
                        stateValue = BingRulesManager.RoomNotificationState.ALL_MESSAGES.name();
                        break;
                    case MENTIONS_ONLY:
                        if (mIsMentionsOnlyNotSupported) {
                            // The room is considered mute.
                            stateValue = BingRulesManager.RoomNotificationState.MUTE.name();
                            break;
                        }
                    case MUTE:
                        stateValue = state.name();
                }
            }
            mRoomNotificationsPreference.setValue(stateValue);
        }

        // update the room tag preference
        if (null != mRoomTagListPreference) {

            if (null != mRoom.getAccountData()) {
                if (null != mRoom.getAccountData().roomTag(RoomTag.ROOM_TAG_FAVOURITE)) {
                    value = RoomTag.ROOM_TAG_FAVOURITE;
                } else if (null != mRoom.getAccountData().roomTag(RoomTag.ROOM_TAG_LOW_PRIORITY)) {
                    value = RoomTag.ROOM_TAG_LOW_PRIORITY;
                } else {
                    // no tag associated to the room
                    value = RoomTag.ROOM_TAG_NO_TAG;
                }

                mRoomTagListPreference.setValue(value);
            }
        }

        // room history readability
        if (null != mRoomHistoryReadabilityRulesListPreference) {
            value = mRoom.getState().getHistoryVisibility();

            if (null != value) {
                mRoomHistoryReadabilityRulesListPreference.setValue(value);
            }
        }
    }

    // OnSharedPreferenceChangeListener implementation

    /**
     * Main entry point handler for any preference changes. For each setting a dedicated handler is
     * called to process the setting.
     *
     * @param aSharedPreferences preference instance
     * @param aKey               preference key as it is defined in the XML
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences aSharedPreferences, String aKey) {

        if (mIsUiUpdateSkipped) {
            Log.d(LOG_TAG, "## onSharedPreferenceChanged(): Skipped");
            return;
        }

        if (null == getActivity()) {
            Log.d(LOG_TAG, "## onSharedPreferenceChanged(): no attached to an activity");
            return;
        }

        if (aKey.equals(PREF_KEY_ROOM_PHOTO_AVATAR)) {
            // unused flow: onSharedPreferenceChanged not triggered for room avatar photo
            onRoomAvatarPreferenceChanged();
        } else if (aKey.equals(PREF_KEY_ROOM_NAME)) {
            onRoomNamePreferenceChanged();
        } else if (aKey.equals(PREF_KEY_ROOM_TOPIC)) {
            onRoomTopicPreferenceChanged();
        } else if (aKey.equals(PREF_KEY_ROOM_NOTIFICATIONS_LIST)) {
            onRoomNotificationsPreferenceChanged();
        } else if (aKey.equals(PREF_KEY_ROOM_TAG_LIST)) {
            onRoomTagPreferenceChanged(); // TBT
        } else if (aKey.equals(PREF_KEY_ROOM_ACCESS_RULES_LIST)) {
            onRoomAccessPreferenceChanged();
        } else if (aKey.equals(PREF_KEY_ROOM_RETENTION)) {
            onRoomRetentionPreferenceChanged();
        } else if (aKey.equals(PREF_KEY_ROOM_HISTORY_READABILITY_LIST)) {
            onRoomHistoryReadabilityPreferenceChanged(); // TBT
        } else {
            Log.w(LOG_TAG, "## onSharedPreferenceChanged(): unknown aKey = " + aKey);
        }
    }

    /**
     * The room history readability has been updated.
     */
    private void onRoomHistoryReadabilityPreferenceChanged() {
        // sanity check
        if ((null == mRoom) || (null == mRoomHistoryReadabilityRulesListPreference)) {
            Log.w(LOG_TAG, "## onRoomHistoryReadabilityPreferenceChanged(): not processed due to invalid parameters");
            return;
        }

        // get new and previous values
        String previousValue = mRoom.getState().history_visibility;
        String newValue = mRoomHistoryReadabilityRulesListPreference.getValue();

        if (!TextUtils.equals(newValue, previousValue)) {
            displayLoadingView();
            mRoom.updateHistoryVisibility(newValue, mUpdateCallback);
        }
    }

    private void onRoomTagPreferenceChanged() {
        boolean isSupportedTag = false;

        // sanity check
        if ((null == mRoom) || (null == mRoomTagListPreference)) {
            Log.w(LOG_TAG, "## onRoomTagPreferenceChanged(): not processed due to invalid parameters");
        } else {
            String newTag = mRoomTagListPreference.getValue();
            String currentTag = null;
            Double tagOrder = 0.0;

            // retrieve the tag from the room info
            RoomAccountData accountData = mRoom.getAccountData();
            if ((null != accountData) && accountData.hasRoomTags()) {
                currentTag = accountData.getRoomTagsKeys().iterator().next();
            }

            if (!newTag.equals(currentTag)) {
                if (newTag.equals(RoomTag.ROOM_TAG_FAVOURITE)
                        || newTag.equals(RoomTag.ROOM_TAG_LOW_PRIORITY)) {
                    isSupportedTag = true;
                } else if (newTag.equals(RoomTag.ROOM_TAG_NO_TAG)) {
                    isSupportedTag = true;
                    newTag = null;
                } else {
                    // unknown tag.. very unlikely
                    Log.w(LOG_TAG, "## onRoomTagPreferenceChanged() not supported tag = " + newTag);
                }
            }

            if (isSupportedTag) {
                displayLoadingView();
                mRoom.replaceTag(currentTag, newTag, tagOrder, mUpdateCallback);
            }
        }
    }

    private void onRoomAccessPreferenceChanged() {

        if ((null == mRoom) || (null == mRoomAccessRulesListPreference)) {
            Log.w(LOG_TAG, "## onRoomAccessPreferenceChanged(): not processed due to invalid parameters");
        } else {
            String joinRuleToApply = null;
            String guestAccessRuleToApply = null;

            // get new and previous values
            String previousJoinRule = mRoom.getState().join_rule;
            String previousGuestAccessRule = mRoom.getState().getGuestAccess();
            String newValue = mRoomAccessRulesListPreference.getValue();

            if (ACCESS_RULES_ONLY_PEOPLE_INVITED.equals(newValue)) {
                // requires: {join_rule: "invite"} and {guest_access: "can_join"}
                joinRuleToApply = !RoomState.JOIN_RULE_INVITE.equals(previousJoinRule) ? RoomState.JOIN_RULE_INVITE : null;
                guestAccessRuleToApply = !RoomState.GUEST_ACCESS_CAN_JOIN.equals(previousGuestAccessRule) ? RoomState.GUEST_ACCESS_CAN_JOIN : null;
            } else if (ACCESS_RULES_ANYONE_WITH_LINK_APART_GUEST.equals(newValue)) {
                // requires: {join_rule: "public"} and {guest_access: "forbidden"}
                joinRuleToApply = !RoomState.JOIN_RULE_PUBLIC.equals(previousJoinRule) ? RoomState.JOIN_RULE_PUBLIC : null;
                guestAccessRuleToApply = !RoomState.GUEST_ACCESS_FORBIDDEN.equals(previousGuestAccessRule) ? RoomState.GUEST_ACCESS_FORBIDDEN : null;

                if (0 == mRoom.getAliases().size()) {
                    displayAccessRoomWarning();
                }
            } else if (ACCESS_RULES_ANYONE_WITH_LINK_INCLUDING_GUEST.equals(newValue)) {
                // requires: {join_rule: "public"} and {guest_access: "can_join"}
                joinRuleToApply = !RoomState.JOIN_RULE_PUBLIC.equals(previousJoinRule) ? RoomState.JOIN_RULE_PUBLIC : null;
                guestAccessRuleToApply = !RoomState.GUEST_ACCESS_CAN_JOIN.equals(previousGuestAccessRule) ? RoomState.GUEST_ACCESS_CAN_JOIN : null;

                if (0 == mRoom.getAliases().size()) {
                    displayAccessRoomWarning();
                }
            } else {
                // unknown value
                Log.d(LOG_TAG, "## onRoomAccessPreferenceChanged(): unknown selected value = " + newValue);
            }

            if (null != joinRuleToApply) {
                displayLoadingView();
                mRoom.updateJoinRules(joinRuleToApply, mUpdateCallback);
            }

            if (null != guestAccessRuleToApply) {
                displayLoadingView();
                mRoom.updateGuestAccess(guestAccessRuleToApply, mUpdateCallback);
            }
        }
    }

    private void onRoomRetentionPreferenceChanged() {
        // sanity check
        if ((null == mRoom) || (null == mRoomRetentionPreference)) {
            return;
        }

        String value = mRoomRetentionPreference.getValue();
        int period = DinumUtilsKt.getRetentionPeriodFromPreferenceValue(value);

        displayLoadingView();
        DinumUtilsKt.setRoomRetention(mSession, mRoom, period, mUpdateCallback);
    }

    /**
     */
    private void onRoomNotificationsPreferenceChanged() {
        // sanity check
        if ((null == mRoom) || (null == mBingRulesManager)) {
            return;
        }

        String value = mRoomNotificationsPreference.getValue();
        BingRulesManager.RoomNotificationState updatedState;

        if (TextUtils.equals(value, BingRulesManager.RoomNotificationState.ALL_MESSAGES.name())) {
            updatedState = BingRulesManager.RoomNotificationState.ALL_MESSAGES;
        } else if (TextUtils.equals(value, BingRulesManager.RoomNotificationState.MENTIONS_ONLY.name())) {
            updatedState = BingRulesManager.RoomNotificationState.MENTIONS_ONLY;
        } else if (mIsMentionsOnlyNotSupported) {
            // The mention only option is not supported for the encrypted room on Tchap-Android (GooglePlay variant).
            // That's why we could not suggest it to the user,
            // but this option seems to be the most appropriate when they decide to mute the notifications for a room.
            // By selecting "Mention_Only" here, the notifications will be disabled on Mobile clients,
            // but the user will keep receiving notifications on Tchap-Web in case of mention.
            updatedState = BingRulesManager.RoomNotificationState.MENTIONS_ONLY;
        } else {
            updatedState = BingRulesManager.RoomNotificationState.MUTE;
        }

        // update only, if values are different
        if (mBingRulesManager.getRoomNotificationState(mRoom.getRoomId()) != updatedState) {
            displayLoadingView();
            mBingRulesManager.updateRoomNotificationState(mRoom.getRoomId(), updatedState,
                    new BingRulesManager.onBingRuleUpdateListener() {
                        @Override
                        public void onBingRuleUpdateSuccess() {
                            Log.d(LOG_TAG, "##onRoomNotificationsPreferenceChanged(): update succeed");
                            hideLoadingView(UPDATE_UI);
                        }

                        @Override
                        public void onBingRuleUpdateFailure(String errorMessage) {
                            Log.w(LOG_TAG, "##onRoomNotificationsPreferenceChanged(): BingRuleUpdateFailure");
                            hideLoadingView(DO_NOT_UPDATE_UI);
                        }
                    });
        }
    }

    /**
     * Action when updating the room name.
     */
    private void onRoomNamePreferenceChanged() {
        // sanity check
        if ((null == mRoom) || (null == mSession) || (null == mRoomNameEditTxt)) {
            return;
        }

        // get new and previous values
        String previousName = mRoom.getState().name;
        String newName = mRoomNameEditTxt.getText();
        // update only, if values are different
        if (!TextUtils.equals(previousName, newName)) {
            displayLoadingView();

            Log.d(LOG_TAG, "##onRoomNamePreferenceChanged to " + newName);
            mRoom.updateName(newName, mUpdateCallback);
            if (getActivity() instanceof VectorRoomDetailsActivity){
                ((VectorRoomDetailsActivity) getActivity()).setRoomTitle(newName);
            }
        }
    }

    /**
     * Action when updating the room topic.
     */
    private void onRoomTopicPreferenceChanged() {
        // sanity check
        if (null == mRoom) {
            return;
        }

        // get new and previous values
        String previousTopic = mRoom.getTopic();
        String newTopic = mRoomTopicEditTxt.getText();
        // update only, if values are different
        if (!TextUtils.equals(previousTopic, newTopic)) {
            displayLoadingView();
            Log.d(LOG_TAG, "## update topic to " + newTopic);
            mRoom.updateTopic(newTopic, mUpdateCallback);
            if (getActivity() instanceof VectorRoomDetailsActivity){
                ((VectorRoomDetailsActivity) getActivity()).setTopic(newTopic);
            }
        }

    }

    /**
     * Update the room avatar.
     * Start the camera activity to take the avatar picture.
     */
    private void onRoomAvatarPreferenceChanged() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getActivity(), SelectPictureActivity.class);
                startActivityForResult(intent, REQ_CODE_UPDATE_ROOM_AVATAR);
            }
        });
    }

    /**
     * Process the result of the room avatar picture.
     *
     * @param aRequestCode request ID
     * @param aResultCode  request status code
     * @param aData        result data
     */
    @Override
    public void onActivityResult(int aRequestCode, int aResultCode, final Intent aData) {
        super.onActivityResult(aRequestCode, aResultCode, aData);

        if (REQ_CODE_UPDATE_ROOM_AVATAR == aRequestCode) {
            onActivityResultRoomAvatarUpdate(aResultCode, aData);
        }
    }

    /**
     * Update the avatar from the data provided the medias picker.
     *
     * @param aResultCode the result code.
     * @param aData       the provided data.
     */
    private void onActivityResultRoomAvatarUpdate(int aResultCode, final Intent aData) {
        // sanity check
        if (null == mSession) {
            return;
        }

        if (aResultCode == Activity.RESULT_OK) {
            Uri thumbnailUri = VectorUtils.getThumbnailUriFromIntent(getActivity(), aData, mSession.getMediaCache());

            if (null != thumbnailUri) {
                displayLoadingView();

                // save the bitmap URL on the server
                ResourceUtils.Resource resource = ResourceUtils.openResource(getActivity(), thumbnailUri, null);
                if (null != resource) {
                    mSession.getMediaCache().uploadContent(resource.mContentStream, null, resource.mMimeType, null, new MXMediaUploadListener() {

                        @Override
                        public void onUploadError(String uploadId, int serverResponseCode, String serverErrorMessage) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e(LOG_TAG, "Fail to upload the avatar");
                                    hideLoadingView(DO_NOT_UPDATE_UI);
                                }
                            });
                        }

                        @Override
                        public void onUploadComplete(final String uploadId, final String contentUri) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(LOG_TAG, "The avatar has been uploaded, update the room avatar");
                                    mRoom.updateAvatarUrl(contentUri, mUpdateAvatarCallback);

                                }
                            });
                        }
                    });
                }
            }
        }
    }

    /**
     * Display the loading view in the parent activity layout.
     * This view is disabled/enabled to achieve a waiting screen.
     */
    private void displayLoadingView() {
        Activity parentActivity = getActivity();
        if (null != parentActivity) {
            parentActivity.runOnUiThread(new Runnable() {
                public void run() {

                    // disable the fragment container view to disable preferences access
                    //enablePreferenceWidgets(false);

                    // disable preference screen during server updates
                    if (null != mParentFragmentContainerView)
                        mParentFragmentContainerView.setEnabled(false);

                    // display the loading progress bar screen
                    if (null != mParentLoadingView) {
                        mParentLoadingView.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    /**
     * Hide the loading progress bar screen and
     * update the UI if required.
     */
    private void hideLoadingView(boolean aIsUiRefreshRequired) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {

                // enable preference screen after server updates finished
                if (null != mParentFragmentContainerView)
                    mParentFragmentContainerView.setEnabled(true);

                // enable preference widgets
                //enablePreferenceWidgets(true);

                if (null != mParentLoadingView) {
                    mParentLoadingView.setVisibility(View.GONE);
                }
            }
        });

        if (aIsUiRefreshRequired) {
            updateUiOnUiThread();
        }
    }

    //================================================================================
    // Banned members management
    //================================================================================

    /**
     * Refresh the banned users list.
     */
    private void refreshBannedMembersList() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();

        preferenceScreen.removePreference(mBannedMembersSettingsCategoryDivider);
        preferenceScreen.removePreference(mBannedMembersSettingsCategory);
        mBannedMembersSettingsCategory.removeAll();

        mRoom.getMembersAsync(new SimpleApiCallback<List<RoomMember>>(getActivity()) {
            @Override
            public void onSuccess(List<RoomMember> members) {
                List<RoomMember> bannedMembers = new ArrayList<>();

                if (null != members) {
                    for (RoomMember member : members) {
                        if (TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_BAN)) {
                            bannedMembers.add(member);
                        }
                    }
                }

                Collections.sort(bannedMembers, new Comparator<RoomMember>() {
                    @Override
                    public int compare(RoomMember m1, RoomMember m2) {
                        return m1.getUserId().toLowerCase(VectorLocale.INSTANCE.getApplicationLocale())
                                .compareTo(m2.getUserId().toLowerCase(VectorLocale.INSTANCE.getApplicationLocale()));
                    }
                });

                if (bannedMembers.size() > 0) {
                    preferenceScreen.addPreference(mBannedMembersSettingsCategoryDivider);
                    preferenceScreen.addPreference(mBannedMembersSettingsCategory);

                    for (RoomMember member : bannedMembers) {
                        Preference preference = new VectorPreference(getActivity());

                        final String userId = member.getUserId();
                        final String displayName = DinsicUtils.computeDisplayNameFromUserId(userId);

                        preference.setTitle(displayName);
                        preference.setKey(BANNED_PREFERENCE_KEY_BASE + userId);

                        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                Intent startRoomInfoIntent = new Intent(getActivity(), VectorMemberDetailsActivity.class);
                                startRoomInfoIntent.putExtra(VectorMemberDetailsActivity.EXTRA_MEMBER_ID, userId);
                                startRoomInfoIntent.putExtra(VectorMemberDetailsActivity.EXTRA_MEMBER_DISPLAY_NAME, displayName);
                                startRoomInfoIntent.putExtra(VectorMemberDetailsActivity.EXTRA_ROOM_ID, mRoom.getRoomId());
                                startRoomInfoIntent.putExtra(VectorMemberDetailsActivity.EXTRA_MATRIX_ID, mSession.getCredentials().userId);
                                getActivity().startActivity(startRoomInfoIntent);
                                return false;
                            }
                        });

                        mBannedMembersSettingsCategory.addPreference(preference);
                    }
                }
            }
        });
    }

    //================================================================================
    // flair management
    //================================================================================

    private final ApiCallback mFlairUpdatesCallback = new ApiCallback<Void>() {
        @Override
        public void onSuccess(Void info) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideLoadingView(false);
                    refreshFlair();
                }
            });
        }

        /**
         * Error management.
         * @param errorMessage the error message
         */
        private void onError(final String errorMessage) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
                    hideLoadingView(false);
                    refreshFlair();
                }
            });
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
    };

    /**
     * Tells if the current user can updates the related group aka flairs
     *
     * @return true if the user is allowed.
     */
    private boolean canUpdateFlair() {
        boolean canUpdateFlair = false;

        PowerLevels powerLevels = mRoom.getState().getPowerLevels();

        if (null != powerLevels) {
            int powerLevel = powerLevels.getUserPowerLevel(mSession.getMyUserId());
            canUpdateFlair = powerLevel >= powerLevels.minimumPowerLevelForSendingEventAsStateEvent(Event.EVENT_TYPE_STATE_RELATED_GROUPS);
        }

        return canUpdateFlair;
    }

    /**
     * Refresh the flair list
     */
    private void refreshFlair() {
        if (null == mFlairSettingsCategory) {
            return;
        }

        final List<String> groups = mRoom.getState().getRelatedGroups();

        Collections.sort(groups, String.CASE_INSENSITIVE_ORDER);

        mFlairSettingsCategory.removeAll();

        if (!groups.isEmpty()) {
            for (final String groupId : groups) {
                VectorPreference preference = new VectorPreference(getActivity());
                preference.setTitle(groupId);
                preference.setKey(FLAIR_PREFERENCE_KEY_BASE + groupId);

                preference.setOnPreferenceLongClickListener(new VectorPreference.OnPreferenceLongClickListener() {
                    @Override
                    public boolean onPreferenceLongClick(Preference preference) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                displayLoadingView();
                                mRoom.removeRelatedGroup(groupId, mFlairUpdatesCallback);
                            }
                        });

                        return true;
                    }
                });
                mFlairSettingsCategory.addPreference(preference);
            }
        } else {
            VectorPreference preference = new VectorPreference(getActivity());
            preference.setTitle(getString(R.string.room_settings_no_flair));
            preference.setKey(FLAIR_PREFERENCE_KEY_BASE + "no_flair");

            mFlairSettingsCategory.addPreference(preference);
        }

        if (canUpdateFlair()) {
            // display the "add addresses" entry
            EditTextPreference addAddressPreference = new VectorEditTextPreference(getActivity());
            addAddressPreference.setTitle(R.string.room_settings_add_new_group);
            addAddressPreference.setDialogTitle(R.string.room_settings_add_new_group);
            addAddressPreference.setKey(FLAIR_PREFERENCE_KEY_BASE + "__add");
            addAddressPreference.setIcon(ThemeUtils.INSTANCE.tintDrawable(getActivity(),
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_add_black), R.attr.vctr_settings_icon_tint_color));

            addAddressPreference.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            final String groupId = ((String) newValue).trim();

                            // ignore empty alias
                            if (!TextUtils.isEmpty(groupId)) {
                                if (!MXPatterns.isGroupId(groupId)) {
                                    new AlertDialog.Builder(getActivity())
                                            .setTitle(R.string.room_settings_invalid_group_format_dialog_title)
                                            .setMessage(getString(R.string.room_settings_invalid_group_format_dialog_body, groupId))
                                            .setPositiveButton(R.string.ok, null)
                                            .show();
                                } else if (!groups.contains(groupId)) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            displayLoadingView();
                                            mRoom.addRelatedGroup(groupId, mFlairUpdatesCallback);
                                        }
                                    });
                                }
                            }
                            return false;
                        }
                    });

            mFlairSettingsCategory.addPreference(addAddressPreference);
        }
    }

    //================================================================================
    // Aliases management
    //================================================================================

    private final ApiCallback<Void> mAliasUpdatesCallback = new ApiCallback<Void>() {
        @Override
        public void onSuccess(Void info) {
            hideLoadingView(false);
            refreshAddresses();
        }

        /**
         * Error management.
         * @param errorMessage the error message
         */
        private void onError(final String errorMessage) {
            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
            hideLoadingView(false);
            refreshAddresses();
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
    };

    /**
     * Manage the long click on an address.
     *
     * @param roomAlias  the room alias.
     * @param anchorView the popup menu anchor view.
     */
    @SuppressLint("NewApi")
    private void onAddressLongClick(final String roomAlias, final View anchorView) {
        Context context = getActivity();
        final PopupMenu popup = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) ?
                new PopupMenu(context, anchorView, Gravity.END) : new PopupMenu(context, anchorView);

        popup.getMenuInflater().inflate(R.menu.vector_room_settings_addresses, popup.getMenu());

        // force to display the icons
        try {
            Field[] fields = popup.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popup);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "onMessageClick : force to display the icons failed " + e.getLocalizedMessage(), e);
        }

        Menu menu = popup.getMenu();
        ThemeUtils.INSTANCE.tintMenuIcons(menu, ThemeUtils.INSTANCE.getColor(context, R.attr.vctr_icon_tint_on_light_action_bar_color));

        String canonicalAlias = mRoom.getState().getCanonicalAlias();
        final boolean canUpdateCanonicalAlias = canUpdateCanonicalAlias();

        /*
         * For alias, it seems that you can only delete alias you have created, even if the Admin has set it as canonical.
         * So let the server answer if it's possible to delete an alias.
         *
         * For canonical alias, you need to have the right power level, so keep the test
         */
        menu.findItem(R.id.ic_action_vector_delete_alias).setVisible(true);
        menu.findItem(R.id.ic_action_vector_set_as_main_address).setVisible(canUpdateCanonicalAlias && !TextUtils.equals(roomAlias, canonicalAlias));
        menu.findItem(R.id.ic_action_vector_unset_main_address).setVisible(canUpdateCanonicalAlias && TextUtils.equals(roomAlias, canonicalAlias));

        // display the menu
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                if (item.getItemId() == R.id.ic_action_vector_unset_main_address) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.room_settings_addresses_disable_main_address_prompt_msg)
                            .setTitle(R.string.room_settings_addresses_disable_main_address_prompt_title)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    displayLoadingView();
                                    mRoom.updateCanonicalAlias(null, mAliasUpdatesCallback);
                                }
                            })
                            .setNegativeButton(R.string.no, null)
                            .show();
                } else if (item.getItemId() == R.id.ic_action_vector_set_as_main_address) {
                    displayLoadingView();
                    mRoom.updateCanonicalAlias(roomAlias, mAliasUpdatesCallback);
                } else if (item.getItemId() == R.id.ic_action_vector_delete_alias) {
                    displayLoadingView();
                    mRoom.removeAlias(roomAlias, new SimpleApiCallback<Void>(mAliasUpdatesCallback) {
                        @Override
                        public void onSuccess(Void info) {
                            // when there is only one alias, it becomes the canonical alias.
                            if (mRoom.getAliases().size() == 1 && canUpdateCanonicalAlias) {
                                mRoom.updateCanonicalAlias(mRoom.getAliases().get(0), mAliasUpdatesCallback);
                            } else {
                                mAliasUpdatesCallback.onSuccess(info);
                            }
                        }
                    });
                } else if (item.getItemId() == R.id.ic_action_vector_room_url) {
                    SystemUtilsKt.copyToClipboard(getActivity(), DinumPermalinkUtilsKt.createPermalink(context.getString(R.string.permalink_prefix), roomAlias));
                } else {
                    SystemUtilsKt.copyToClipboard(getActivity(), roomAlias);
                }

                return true;
            }
        });

        popup.show();
    }

    /**
     * Tells if the current user can updates the room canonical alias.
     *
     * @return true if the user is allowed to update the canonical alias.
     */
    private boolean canUpdateCanonicalAlias() {
        boolean canUpdateCanonicalAlias = false;

        PowerLevels powerLevels = mRoom.getState().getPowerLevels();

        if (null != powerLevels) {
            int powerLevel = powerLevels.getUserPowerLevel(mSession.getMyUserId());
            canUpdateCanonicalAlias = powerLevel >= powerLevels.minimumPowerLevelForSendingEventAsStateEvent(Event.EVENT_TYPE_STATE_CANONICAL_ALIAS);
        }

        return canUpdateCanonicalAlias;
    }

    /**
     * Refresh the addresses section
     */
    private void refreshAddresses() {
        return; //no access to this section in tchap at this step of the product
        /*
        final String localSuffix = ":" + mSession.getHomeServerConfig().getHomeserverUri().getHost();
        final String canonicalAlias = mRoom.getState().getCanonicalAlias();
        final List<String> aliases = new ArrayList<>(mRoom.getAliases());

        // remove the displayed preferences
        mAddressesSettingsCategory.removeAll();

        if (0 == aliases.size()) {
            AddressPreference preference = new AddressPreference(getActivity());
            preference.setTitle(getString(R.string.room_settings_addresses_no_local_addresses));
            preference.setKey(NO_LOCAL_ADDRESS_PREFERENCE_KEY);
            mAddressesSettingsCategory.addPreference(preference);
        } else {
            List<String> localAliases = new ArrayList<>();
            List<String> remoteAliases = new ArrayList<>();

            for (String alias : aliases) {
                if (alias.endsWith(localSuffix)) {
                    localAliases.add(alias);
                } else {
                    remoteAliases.add(alias);
                }
            }

            // the local aliases are displayed first in the list
            aliases.clear();
            aliases.addAll(localAliases);
            aliases.addAll(remoteAliases);

            int index = 0;

            for (String alias : aliases) {
                AddressPreference preference = new AddressPreference(getActivity());
                preference.setTitle(alias);
                preference.setKey(ADDRESSES_PREFERENCE_KEY_BASE + index);
                preference.setMainIconVisible(TextUtils.equals(alias, canonicalAlias));

                final String fAlias = alias;
                final AddressPreference fAddressPreference = preference;

                preference.setOnPreferenceLongClickListener(new VectorPreference.OnPreferenceLongClickListener() {
                    @Override
                    public boolean onPreferenceLongClick(Preference preference) {
                        onAddressLongClick(fAlias, fAddressPreference.getMainIconView());
                        return true;
                    }
                });

                mAddressesSettingsCategory.addPreference(preference);
                index++;
            }
        }

        // Everyone can add an alias: display the "add address" entry
        EditTextPreference addAddressPreference = new VectorEditTextPreference(getActivity());
        addAddressPreference.setTitle(R.string.room_settings_addresses_add_new_address);
        addAddressPreference.setDialogTitle(R.string.room_settings_addresses_add_new_address);
        addAddressPreference.setKey(ADD_ADDRESSES_PREFERENCE_KEY);
        addAddressPreference.setIcon(ThemeUtils.INSTANCE.tintDrawable(getActivity(),
                ContextCompat.getDrawable(getActivity(), R.drawable.ic_add_black), R.attr.vctr_settings_icon_tint_color));

        addAddressPreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String newAddress = ((String) newValue).trim();

                        // ignore empty alias
                        if (!TextUtils.isEmpty(newAddress)) {
                            if (!MXPatterns.isRoomAlias(newAddress)) {
                                new AlertDialog.Builder(getActivity())
                                        .setTitle(R.string.room_settings_addresses_invalid_format_dialog_title)
                                        .setMessage(getString(R.string.room_settings_addresses_invalid_format_dialog_body, newAddress))
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        })
                                        .show();
                            } else if (aliases.indexOf(newAddress) < 0) {
                                displayLoadingView();
                                mRoom.addAlias(newAddress, new SimpleApiCallback<Void>(mAliasUpdatesCallback) {
                                    @Override
                                    public void onSuccess(Void info) {
                                        // when there is only one alias, it becomes the canonical alias.
                                        if (mRoom.getAliases().size() == 1 && canUpdateCanonicalAlias()) {
                                            mRoom.updateCanonicalAlias(mRoom.getAliases().get(0), mAliasUpdatesCallback);
                                        } else {
                                            mAliasUpdatesCallback.onSuccess(info);
                                        }
                                    }
                                });
                            }
                        }
                        return false;
                    }
                });

            mAddressesSettingsCategory.addPreference(addAddressPreference);
        }
        */
    }


    /**
     * Refresh the addresses section
     */
    private void refreshEndToEnd() {
        return; //no etoe information in tchap at this stage of the product
        /*
        // encrypt to unverified devices
        final SwitchPreference sendToUnverifiedDevicesPref =
                (SwitchPreference) findPreference(getString(R.string.room_settings_never_send_to_unverified_devices_title));

        // reported by GA
        if (null == sendToUnverifiedDevicesPref) {
            Log.e(LOG_TAG, "## refreshEndToEnd() : sendToUnverifiedDevicesPref is null");
            return;
        }

        // test if the crypto is
        if (null == mSession.getCrypto()) {
            mAdvancedSettingsCategory.removePreference(sendToUnverifiedDevicesPref);
        } else if (null != sendToUnverifiedDevicesPref) {
            if (mRoom.isEncrypted()) {
                sendToUnverifiedDevicesPref.setChecked(false);

                mSession.getCrypto().getGlobalBlacklistUnverifiedDevices(
                        new SimpleApiCallback<Boolean>() {
                            @Override
                            public void onSuccess(final Boolean sendToVerifiedDevicesInAnyRoom) {
                                mSession.getCrypto().isRoomBlacklistUnverifiedDevices(mRoom.getRoomId(), new SimpleApiCallback<Boolean>() {
                                    @Override
                                    public void onSuccess(final Boolean sendToVerifiedDevicesInRoom) {
                                        sendToUnverifiedDevicesPref.setChecked(sendToVerifiedDevicesInRoom || sendToVerifiedDevicesInAnyRoom);
                                        sendToUnverifiedDevicesPref.setEnabled(!sendToVerifiedDevicesInAnyRoom);
                                    }
                                });
                            }
                        }
                );

            } else if (null != sendToUnverifiedDevicesPref) {
                mAdvancedSettingsCategory.removePreference(sendToUnverifiedDevicesPref);
            }

            sendToUnverifiedDevicesPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mSession.getCrypto().isRoomBlacklistUnverifiedDevices(mRoom.getRoomId(), new SimpleApiCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean status) {
                            if (sendToUnverifiedDevicesPref.isChecked() != status) {
                                ApiCallback<Void> callback = new SimpleApiCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void info) {
                                    }
                                };

                                if (sendToUnverifiedDevicesPref.isChecked()) {
                                    mSession.getCrypto().setRoomBlacklistUnverifiedDevices(mRoom.getRoomId(), callback);
                                } else {
                                    mSession.getCrypto().setRoomUnBlacklistUnverifiedDevices(mRoom.getRoomId(), callback);
                                }
                            }
                        }
                    });

                    return true;
                }
            });
        }

        final String key = PREF_KEY_ENCRYPTION + mRoom.getRoomId();

        // remove the displayed preferences
        Preference e2ePref = mAdvancedSettingsCategory.findPreference(key);

        if (null != e2ePref) {
            mAdvancedSettingsCategory.removePreference(e2ePref);
        }

        // remove the preference because it might switch from a SwitchPreference to  VectorPreference
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .remove(key)
                .apply();

        if (mRoom.isEncrypted()) {
            Preference isEncryptedPreference = new VectorPreference(getActivity());
            isEncryptedPreference.setTitle(R.string.room_settings_addresses_e2e_enabled);
            isEncryptedPreference.setKey(key);
            isEncryptedPreference.setIcon(getResources().getDrawable(R.drawable.e2e_verified));
            mAdvancedSettingsCategory.addPreference(isEncryptedPreference);
        } else {
            PowerLevels powerLevels = mRoom.getState().getPowerLevels();
            int myPowerLevel = -1;
            int minimumPowerLevel = 0;

            if (null != powerLevels) {
                myPowerLevel = powerLevels.getUserPowerLevel(mSession.getMyUserId());
                minimumPowerLevel = powerLevels.minimumPowerLevelForSendingEventAsStateEvent(Event.EVENT_TYPE_MESSAGE_ENCRYPTION);
            }

            // Test if the user has enough power levels to enable the crypto is in this room
            if (myPowerLevel < minimumPowerLevel) {
                Preference isEncryptedPreference = new VectorPreference(getActivity());
                isEncryptedPreference.setTitle(R.string.room_settings_addresses_e2e_disabled);
                isEncryptedPreference.setKey(key);
                isEncryptedPreference.setIcon(ThemeUtils.INSTANCE.tintDrawable(getActivity(),
                        getResources().getDrawable(R.drawable.e2e_unencrypted), R.attr.vctr_settings_icon_tint_color));
                mAdvancedSettingsCategory.addPreference(isEncryptedPreference);
            } else if (mSession.isCryptoEnabled()) {
                final SwitchPreference encryptSwitchPreference = new VectorSwitchPreference(getActivity());
                encryptSwitchPreference.setTitle(R.string.room_settings_addresses_e2e_encryption_warning);
                encryptSwitchPreference.setKey(key);
                encryptSwitchPreference.setIcon(ThemeUtils.INSTANCE.tintDrawable(getActivity(),
                        getResources().getDrawable(R.drawable.e2e_unencrypted), R.attr.vctr_settings_icon_tint_color));
                encryptSwitchPreference.setChecked(false);
                mAdvancedSettingsCategory.addPreference(encryptSwitchPreference);

                encryptSwitchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValueAsVoid) {
                        boolean newValue = (boolean) newValueAsVoid;
                        if (newValue != mRoom.isEncrypted()) {
                            displayLoadingView();

                            mRoom.enableEncryptionWithAlgorithm(CryptoConstantsKt.MXCRYPTO_ALGORITHM_MEGOLM, new ApiCallback<Void>() {

                                private void onDone() {
                                    hideLoadingView(false);
                                    refreshEndToEnd();
                                }

                                @Override
                                public void onSuccess(Void info) {
                                    onDone();
                                }

                                @Override
                                public void onNetworkError(Exception e) {
                                    onDone();
                                }

                                @Override
                                public void onMatrixError(MatrixError e) {
                                    onDone();
                                }

                                @Override
                                public void onUnexpectedError(Exception e) {
                                    onDone();
                                }
                            });

                        }
                        return true;
                    }
                });

            }
        }*/
    }

}
