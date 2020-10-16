/*
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

package fr.gouv.tchap.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.SpannableString;
import android.text.TextUtils;

import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ValueCallback;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.JsonUtils;
import org.matrix.androidsdk.data.RoomState;
import org.matrix.androidsdk.listeners.MXMediaUploadListener;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.callback.SimpleApiCallback;
import org.matrix.androidsdk.rest.model.CreateRoomParams;
import org.matrix.androidsdk.core.model.MatrixError;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.RoomDirectoryVisibility;
import org.matrix.androidsdk.core.Log;
import org.matrix.androidsdk.core.ResourceUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTextChanged;

import fr.gouv.tchap.sdk.session.room.model.RoomAccessRulesKt;
import fr.gouv.tchap.sdk.session.room.model.RoomRetentionKt;
import fr.gouv.tchap.util.DinsicUtils;
import fr.gouv.tchap.util.DinumUtilsKt;
import fr.gouv.tchap.util.HexagonMaskView;

import fr.gouv.tchap.util.RoomRetentionPeriodPickerDialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import im.vector.Matrix;
import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.MXCActionBarActivity;
import im.vector.activity.SelectPictureActivity;
import im.vector.activity.VectorRoomActivity;
import im.vector.activity.VectorRoomInviteMembersActivity;
import im.vector.util.VectorUtils;

import static fr.gouv.tchap.config.TargetConfigurationKt.ENABLE_ROOM_RETENTION;

public class TchapRoomCreationActivity extends MXCActionBarActivity {

    private static final String LOG_TAG = TchapRoomCreationActivity.class.getSimpleName();

    private static final int REQ_CODE_UPDATE_ROOM_AVATAR = 0x10;
    private static final int REQ_CODE_ADD_PARTICIPANTS = 0x11;
    private static final String ERROR_CODE_ROOM_ALIAS_ALREADY_TAKEN = "Room alias already taken";
    private static final String ERROR_CODE_ROOM_ALIAS_INVALID_CHARACTERS = "Invalid characters in room alias";


    @BindView(R.id.hexagon_mask_view)
    HexagonMaskView hexagonMaskView;

    @BindView(R.id.rly_hexagon_avatar)
    View hexagonAvatar;

    @BindView(R.id.tv_add_avatar_image)
    TextView addAvatarText;

    @BindView(R.id.et_room_name)
    TextInputEditText etRoomName;

    @BindView(R.id.tv_room_msg_retention)
    TextView roomMessageRetentionText;

    @BindView(R.id.switch_disable_federation)
    Switch disableFederationSwitch;

    @BindView(R.id.private_room_layout)
    ViewGroup privateRoomLayout;

    @BindView(R.id.extern_room_layout)
    ViewGroup externRoomLayout;

    @BindView(R.id.forum_room_layout)
    ViewGroup forumRoomLayout;

    @BindView(R.id.federation_layout)
    ViewGroup federationLayout;

    private MXSession mSession;
    private Uri mThumbnailUri = null;
    private int mRetentionPeriod = RoomRetentionKt.DEFAULT_RETENTION_VALUE_IN_DAYS;
    private CreateRoomParams mRoomParams = new CreateRoomParams();
    private List<String> mParticipantsIds = new ArrayList<>();
    private boolean mRestricted;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_tchap_room_creation;
    }

    @Override
    public void initUiAndData() {
        setWaitingView(findViewById(R.id.room_creation_spinner_views));

        mSession = Matrix.getInstance(this).getDefaultSession();

        configureToolbar();
        setTitle(R.string.tchap_room_creation_title);

        // Initialize the room messages retention period which is underlined and clickable
        setRoomRetentionPeriod(RoomRetentionKt.DEFAULT_RETENTION_VALUE_IN_DAYS);
        roomMessageRetentionText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RoomRetentionPeriodPickerDialogFragment(TchapRoomCreationActivity.this)
                        .create(mRetentionPeriod, (number) -> {
                            setRoomRetentionPeriod(number);
                            return null;
                        })
                        .show();

            }
        });

        if (!ENABLE_ROOM_RETENTION) {
            roomMessageRetentionText.setVisibility(View.GONE);
        }

        // Initialize default room params as private and restricted
        mRestricted = true;
        if (DinumUtilsKt.isSecure()) {
            // There is no external users on Tchap secure, so hide this option
            externRoomLayout.setVisibility(View.GONE);
        }
        setRoomAccessRule(RoomAccessRulesKt.RESTRICTED);

        mRoomParams.visibility = RoomDirectoryVisibility.DIRECTORY_VISIBILITY_PRIVATE;
        mRoomParams.preset = CreateRoomParams.PRESET_PRIVATE_CHAT;
        // Hide the encrypted messages sent before the member is invited.
        mRoomParams.setHistoryVisibility(RoomState.HISTORY_VISIBILITY_INVITED);

        // Prepare disable federation label by adding the hs display name of the current user.
        String userHSDomain = DinsicUtils.getHomeServerDisplayNameFromMXIdentifier(mSession.getMyUserId());
        disableFederationSwitch.setText(getString(R.string.tchap_room_creation_disable_federation, userHSDomain));

        // Set the right border color on avatar
        hexagonMaskView.setBorderSettings(ContextCompat.getColor(this, R.color.restricted_room_avatar_border_color), 3);
    }

    @Override
    public int getMenuRes() {
        return R.menu.tchap_menu_next;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_next:
                if (mRoomParams.preset.equals(CreateRoomParams.PRESET_PUBLIC_CHAT)) {
                    // In case of a public room, the room alias is mandatory.
                    // That's why, we deduce the room alias from the room name.
                    mRoomParams.roomAliasName = DinumUtilsKt.createRoomAliasName(mRoomParams.name);
                }
                inviteMembers(REQ_CODE_ADD_PARTICIPANTS);
                hideKeyboard();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_next);

        item.setEnabled(null != mRoomParams.name && !isWaitingViewVisible());

        return super.onPrepareOptionsMenu(menu);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @OnClick(R.id.rly_hexagon_avatar)
    void addRoomAvatar() {
        Intent intent = new Intent(TchapRoomCreationActivity.this, SelectPictureActivity.class);
        startActivityForResult(intent, REQ_CODE_UPDATE_ROOM_AVATAR);
    }

    void setRoomExternalAccess() {
        if (!mRestricted) {
            Log.d(LOG_TAG, "## unrestricted");
            setRoomAccessRule(RoomAccessRulesKt.UNRESTRICTED);
            hexagonMaskView.setBorderSettings(ContextCompat.getColor(this, R.color.unrestricted_room_avatar_border_color), 10);
        } else {
            Log.d(LOG_TAG, "## restricted");
            setRoomAccessRule(RoomAccessRulesKt.RESTRICTED);
            hexagonMaskView.setBorderSettings(ContextCompat.getColor(this, R.color.restricted_room_avatar_border_color), 3);

            if (!mParticipantsIds.isEmpty()) {
                // Remove the potential selected external users
                for (int index = 0; index < mParticipantsIds.size(); ) {
                    String selectedUserId = mParticipantsIds.get(index);
                    if (DinsicUtils.isExternalTchapUser(selectedUserId)) {
                        mParticipantsIds.remove(selectedUserId);
                    } else {
                        index++;
                    }
                }
            }
        }
    }

    @OnClick(R.id.private_room_layout)
    void enablePrivateRoom() {
        disableExternRoom();
        disableForumRoom();
        privateRoomLayout.setSelected(true);

        mRoomParams.visibility = RoomDirectoryVisibility.DIRECTORY_VISIBILITY_PRIVATE;
        mRoomParams.preset = CreateRoomParams.PRESET_PRIVATE_CHAT;
        // Hide the encrypted messages sent before the member is invited.
        mRoomParams.setHistoryVisibility(RoomState.HISTORY_VISIBILITY_INVITED);
        Log.d(LOG_TAG, "## private restricted");
        // Private rooms are all federated
        disableFederationSwitch.setChecked(false);
        mRoomParams.creation_content = null;
        // Disable room external access option
        mRestricted = true;
        setRoomExternalAccess();
    }

    @OnClick(R.id.extern_room_layout)
    void enableExternRoom() {
        disablePrivateRoom();
        disableForumRoom();
        externRoomLayout.setSelected(true);

        mRoomParams.visibility = RoomDirectoryVisibility.DIRECTORY_VISIBILITY_PRIVATE;
        mRoomParams.preset = CreateRoomParams.PRESET_PRIVATE_CHAT;
        // Hide the encrypted messages sent before the member is invited.
        mRoomParams.setHistoryVisibility(RoomState.HISTORY_VISIBILITY_INVITED);
        Log.d(LOG_TAG, "## private unrestricted");
        // Private rooms are all federated
        disableFederationSwitch.setChecked(false);
        mRoomParams.creation_content = null;
        // Disable room external access option
        mRestricted = false;
        setRoomExternalAccess();
    }

    @OnClick(R.id.forum_room_layout)
    void enableForumRoom() {
        disablePrivateRoom();
        disableExternRoom();

        federationLayout.setVisibility(View.VISIBLE);
        forumRoomLayout.setSelected(true);

        mRoomParams.visibility = RoomDirectoryVisibility.DIRECTORY_VISIBILITY_PUBLIC;
        mRoomParams.preset = CreateRoomParams.PRESET_PUBLIC_CHAT;
        mRoomParams.setHistoryVisibility(RoomState.HISTORY_VISIBILITY_WORLD_READABLE);
        Log.d(LOG_TAG, "## public");
        // Public rooms are not federated by default
        disableFederationSwitch.setChecked(true);
        // Disable room external access option
        mRestricted = true;
        setRoomExternalAccess();
    }

    void disablePrivateRoom() {
        privateRoomLayout.setSelected(false);
    }

    void disableExternRoom() {
        externRoomLayout.setSelected(false);
    }

    void disableForumRoom() {
        federationLayout.setVisibility(View.GONE);
        forumRoomLayout.setSelected(false);
    }

    @OnCheckedChanged(R.id.switch_disable_federation)
    void setRoomFederation() {
        if (disableFederationSwitch.isChecked()) {
            Map<String, Object> params = new HashMap<>();
            params.put("m.federate", false);
            mRoomParams.creation_content = params;
            Log.d(LOG_TAG, "## not federated");

            if (!mParticipantsIds.isEmpty()) {
                // Remove the potential selected users who don't belong to the user HS
                String currentUserHS = DinsicUtils.getHomeServerNameFromMXIdentifier(mSession.getMyUserId());

                for (int index = 0; index < mParticipantsIds.size(); ) {
                    String selectedUserId = mParticipantsIds.get(index);
                    if (!DinsicUtils.getHomeServerNameFromMXIdentifier(selectedUserId).equals(currentUserHS)) {
                        mParticipantsIds.remove(selectedUserId);
                    } else {
                        index++;
                    }
                }
            }
        } else {
            mRoomParams.creation_content = null;
            Log.d(LOG_TAG, "## federated");
        }
    }

    @OnTextChanged(R.id.et_room_name)
    protected void onTextChanged(CharSequence text) {
        String roomName = text.toString().trim();

        if (!roomName.isEmpty()) {
            mRoomParams.name = roomName;
        } else {
            mRoomParams.name = null;
        }

        invalidateOptionsMenu();
        Log.i(LOG_TAG, "room name:" + mRoomParams.name);
    }

    /**
     * Process the result of the startActivityForResult()
     *
     * @param requestCode the request id.
     * @param resultCode  the request status code.
     * @param intent      the result data.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case REQ_CODE_UPDATE_ROOM_AVATAR:
                onActivityResultRoomAvatarUpdate(resultCode, intent);
                break;
            case REQ_CODE_ADD_PARTICIPANTS:

                mParticipantsIds = intent.getStringArrayListExtra(VectorRoomInviteMembersActivity.EXTRA_OUT_SELECTED_USER_IDS);

                if (resultCode == RESULT_OK) {
                    // We have retrieved the list of members to invite from RoomInviteMembersActivity.
                    // This list contains only matrixIds because the RoomInviteMembersActivity was opened in TCHAP_USERS_ONLY, TCHAP_USERS_ONLY_WITHOUT_EXTERNALS or TCHAP_USERS_ONLY_WITHOUT_FEDERATION mode.
                    showWaitingView();
                    invalidateOptionsMenu();
                    mRoomParams.invitedUserIds = mParticipantsIds;
                    createNewRoom();
                } else if (resultCode == RESULT_CANCELED) {
                    invalidateOptionsMenu();
                }
                break;
        }
    }

    /**
     * Force the room retention period in the room creation parameters.
     *
     * @param periodInDays the retention period in days.
     */
    private void setRoomRetentionPeriod(int periodInDays) {
        // Remove the existing value if any.
        if (mRoomParams.initialStates != null && !mRoomParams.initialStates.isEmpty()) {
            final List<Event> newInitialStates = new ArrayList<>();
            for (Event event : mRoomParams.initialStates) {
                if (!event.type.equals(RoomRetentionKt.EVENT_TYPE_STATE_ROOM_RETENTION)) {
                    newInitialStates.add(event);
                }
            }
            mRoomParams.initialStates = newInitialStates;
        }

        Event roomRetentionEvent = new Event();
        roomRetentionEvent.type = RoomRetentionKt.EVENT_TYPE_STATE_ROOM_RETENTION;

        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put(RoomRetentionKt.STATE_EVENT_CONTENT_MAX_LIFETIME, DinumUtilsKt.convertDaysToMs(periodInDays));
        contentMap.put(RoomRetentionKt.STATE_EVENT_CONTENT_EXPIRE_ON_CLIENTS, true);
        roomRetentionEvent.updateContent(JsonUtils.getGson(false).toJsonTree(contentMap));
        roomRetentionEvent.stateKey = "";

        if (null == mRoomParams.initialStates) {
            mRoomParams.initialStates = Arrays.asList(roomRetentionEvent);
        } else {
            mRoomParams.initialStates.add(roomRetentionEvent);
        }

        // Update room retention text label
        SpannableString ss = new SpannableString(getResources().getQuantityString(R.plurals.tchap_room_creation_retention,
                periodInDays, periodInDays));
        String valueString = String.valueOf(periodInDays);
        int pos = ss.toString().indexOf(valueString);

        if (pos >= 0) {
            ss.setSpan(new UnderlineSpan(), pos, ss.length(), 0);
        }

        roomMessageRetentionText.setText(ss);
        mRetentionPeriod = periodInDays;
    }

    /**
     * Force the room access rule in the room creation parameters.
     *
     * @param roomAccessRule the expected room access rule, set null to remove any existing value.
     *                       see {@link RoomAccessRulesKt}
     */
    private void setRoomAccessRule(@Nullable String roomAccessRule) {
        // Remove the existing value if any.
        if (mRoomParams.initialStates != null && !mRoomParams.initialStates.isEmpty()) {
            final List<Event> newInitialStates = new ArrayList<>();
            for (Event event : mRoomParams.initialStates) {
                if (!event.type.equals(RoomAccessRulesKt.EVENT_TYPE_STATE_ROOM_ACCESS_RULES)) {
                    newInitialStates.add(event);
                }
            }
            mRoomParams.initialStates = newInitialStates;
        }

        if (!TextUtils.isEmpty(roomAccessRule)) {
            Event roomAccessRulesEvent = new Event();
            roomAccessRulesEvent.type = RoomAccessRulesKt.EVENT_TYPE_STATE_ROOM_ACCESS_RULES;

            Map<String, String> contentMap = new HashMap<>();
            contentMap.put(RoomAccessRulesKt.STATE_EVENT_CONTENT_KEY_RULE, roomAccessRule);
            roomAccessRulesEvent.updateContent(JsonUtils.getGson(false).toJsonTree(contentMap));
            roomAccessRulesEvent.stateKey = "";

            if (null == mRoomParams.initialStates) {
                mRoomParams.initialStates = Arrays.asList(roomAccessRulesEvent);
            } else {
                mRoomParams.initialStates.add(roomAccessRulesEvent);
            }
        }
    }

    /**
     * Update the avatar from the data provided the medias picker.
     *
     * @param aResultCode the result code.
     * @param intent      the provided data.
     */
    private void onActivityResultRoomAvatarUpdate(int aResultCode, final Intent intent) {
        // sanity check
        if (null == mSession) {
            return;
        }

        if (aResultCode == Activity.RESULT_OK) {
            mThumbnailUri = VectorUtils.getThumbnailUriFromIntent(this, intent, mSession.getMediaCache());

            if (null != mThumbnailUri) {
                addAvatarText.setVisibility(View.GONE);
                hexagonMaskView.setBackgroundColor(Color.WHITE);
                Glide.with(this)
                        .load(mThumbnailUri)
                        .apply(new RequestOptions()
                                .override(hexagonMaskView.getWidth(), hexagonMaskView.getHeight())
                                .centerCrop()
                        )
                        .into(hexagonMaskView);
            }
        }
    }

    /**
     * Create a new room with params.
     * The room name is mandatory.
     */
    private void createNewRoom() {
        mSession.createRoom(mRoomParams, new SimpleApiCallback<String>(TchapRoomCreationActivity.this) {
            @Override
            public void onSuccess(final String roomId) {
                if (null != mThumbnailUri) {
                    // save the bitmap URL on the server
                    hideWaitingView();
                    uploadRoomAvatar(roomId, mThumbnailUri);
                } else {
                    hideWaitingView();
                    openRoom(roomId);
                }
            }

            private void onError(final String message) {
                getWaitingView().post(new Runnable() {
                    @Override
                    public void run() {
                        if (null != message) {
                            Log.e(LOG_TAG, "Fail to create the room");
                            Toast.makeText(TchapRoomCreationActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                        hideWaitingView();
                        invalidateOptionsMenu();
                    }
                });
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(final MatrixError e) {
                // Catch here the consent request if any.
                if (MatrixError.M_CONSENT_NOT_GIVEN.equals(e.errcode)) {
                    hideWaitingView();
                    getConsentNotGivenHelper().displayDialog(e);
                } else {
                    switch (e.error) {
                        case ERROR_CODE_ROOM_ALIAS_INVALID_CHARACTERS:
                            hideWaitingView();
                            mRoomParams.roomAliasName = DinumUtilsKt.getRandomString();
                            createNewRoom();
                            break;
                        case ERROR_CODE_ROOM_ALIAS_ALREADY_TAKEN:
                            hideWaitingView();
                            mRoomParams.roomAliasName = DinumUtilsKt.getRandomString();
                            createNewRoom();
                            break;
                        default:
                            onError(e.getLocalizedMessage());
                            break;
                    }
                }
            }

            @Override
            public void onUnexpectedError(final Exception e) {
                onError(e.getLocalizedMessage());
            }
        });
    }

    /**
     * Upload the avatar on the server.
     *
     * @param roomId       the room id.
     * @param thumbnailUri the uri of the avatar image.
     */
    private void uploadRoomAvatar(final String roomId, final Uri thumbnailUri) {
        showWaitingView();
        ResourceUtils.Resource resource = ResourceUtils.openResource(TchapRoomCreationActivity.this, mThumbnailUri, null);
        if (null != resource) {
            mSession.getMediaCache().uploadContent(resource.mContentStream, null, resource.mMimeType, null, new MXMediaUploadListener() {

                @Override
                public void onUploadError(String uploadId, int serverResponseCode, final String serverErrorMessage) {
                    hideWaitingView();
                    Log.e(LOG_TAG, "Fail to upload the avatar");
                    promptRoomAvatarError(new ValueCallback<Boolean>() {
                        @Override
                        public void onReceiveValue(Boolean retry) {
                            if (retry) {
                                // Try again
                                uploadRoomAvatar(roomId, thumbnailUri);
                            } else {
                                // Despite an error in the treatment of the avatar image
                                // the user chooses to ignore the problem and continue the process of opening the room
                                openRoom(roomId);
                            }
                        }
                    });
                }

                @Override
                public void onUploadComplete(final String uploadId, final String contentUri) {
                    hideWaitingView();
                    updateRoomAvatar(roomId, contentUri);
                }
            });
        }
    }

    /**
     * Update the room avatar.
     *
     * @param roomId     the room id.
     * @param contentUri the uri of the avatar image.
     */
    private void updateRoomAvatar(final String roomId, final String contentUri) {
        showWaitingView();
        Log.d(LOG_TAG, "The avatar has been uploaded, update the room avatar");
        mSession.getDataHandler().getRoom(roomId).updateAvatarUrl(contentUri, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void info) {
                hideWaitingView();
                openRoom(roomId);
            }

            private void onError(String message) {
                if (null != this) {
                    hideWaitingView();
                    Log.e(LOG_TAG, "## updateAvatarUrl() failed " + message);
                    promptRoomAvatarError(new ValueCallback<Boolean>() {
                        @Override
                        public void onReceiveValue(Boolean retry) {
                            if (retry) {
                                // Try again
                                updateRoomAvatar(roomId, contentUri);
                            } else {
                                // Despite an error in the treatment of the avatar image
                                // the user chooses to ignore the problem and continue the process of opening the room
                                openRoom(roomId);
                            }
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

    /**
     * Open the room that has just been created.
     *
     * @param roomId the room id.
     */
    private void openRoom(final String roomId) {
        Log.d(LOG_TAG, "## openRoom(): start VectorHomeActivity..");

        HashMap<String, Object> params = new HashMap<>();
        params.put(VectorRoomActivity.EXTRA_MATRIX_ID, mSession.getMyUserId());
        params.put(VectorRoomActivity.EXTRA_ROOM_ID, roomId);
        params.put(VectorRoomActivity.EXTRA_EXPAND_ROOM_HEADER, true);
        CommonActivityUtils.goToRoomPage(TchapRoomCreationActivity.this, mSession, params);
    }

    private void promptRoomAvatarError(final ValueCallback<Boolean> valueCallback) {
        hideWaitingView();

        new AlertDialog.Builder(TchapRoomCreationActivity.this)
                .setMessage(R.string.tchap_room_creation_save_avatar_failed)
                .setPositiveButton(R.string.resend, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Try again
                        valueCallback.onReceiveValue(true);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.auth_skip, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Despite an error in the treatment of the avatar image
                        // the user chooses to ignore the problem and continue the process of opening the room
                        valueCallback.onReceiveValue(false);
                        dialog.dismiss();
                    }
                })
                .show();
    }

    /**
     * Open the screen to select the members to invite in the room.
     *
     * @param requestCode the request id.
     */
    private void inviteMembers(int requestCode) {
        Intent intent = new Intent(TchapRoomCreationActivity.this, VectorRoomInviteMembersActivity.class);
        intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_MATRIX_ID, mSession.getMyUserId());
        intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_ACTION_ACTIVITY_MODE, VectorRoomInviteMembersActivity.ActionMode.RETURN_SELECTED_USER_IDS);
        // Check whether the federation has been disabled to limit the invitation to the non federated users
        if (null == mRoomParams.creation_content) {
            // Check whether the external users are allowed or not
            if (!mRestricted) {
                intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_CONTACTS_FILTER, VectorRoomInviteMembersActivity.ContactsFilter.TCHAP_USERS_ONLY);
            } else {
                intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_CONTACTS_FILTER, VectorRoomInviteMembersActivity.ContactsFilter.TCHAP_USERS_ONLY_WITHOUT_EXTERNALS);
            }
        } else {
            intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_CONTACTS_FILTER, VectorRoomInviteMembersActivity.ContactsFilter.TCHAP_USERS_ONLY_WITHOUT_FEDERATION);
        }

        if (!mParticipantsIds.isEmpty()) {
            intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_IN_SELECTED_USER_IDS, (Serializable) mParticipantsIds);
        }

        startActivityForResult(intent, requestCode);
    }
}
