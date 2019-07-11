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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import java.util.Random;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTextChanged;

import fr.gouv.tchap.sdk.session.room.model.RoomAccessRulesKt;
import fr.gouv.tchap.util.DinsicUtils;
import fr.gouv.tchap.util.HexagonMaskView;
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

    @BindView(R.id.tv_external_access)
    TextView externalAccessRoomText;

    @BindView(R.id.switch_external_access_room)
    Switch externalAccessRoomSwitch;

    @BindView(R.id.switch_public_private_room)
    Switch publicPrivateRoomSwitch;

    @BindView(R.id.ll_federation_option)
    View federationOption;

    @BindView(R.id.tv_disable_federation)
    TextView disableFederationText;

    @BindView(R.id.switch_disable_federation)
    Switch disableFederationSwitch;

    @BindView(R.id.tv_public_private_room_description)
    TextView tvPublicPrivateRoomDescription;

    private MXSession mSession;
    private Uri mThumbnailUri = null;
    private CreateRoomParams mRoomParams = new CreateRoomParams();
    private List<String> mParticipantsIds = new ArrayList<>();

    @Override
    public int getLayoutRes() {
        return R.layout.activity_tchap_room_creation;
    }

    @Override
    public void initUiAndData() {
        setWaitingView(findViewById(R.id.room_creation_spinner_views));

        mSession = Matrix.getInstance(this).getDefaultSession();

        setTitle(R.string.tchap_room_creation_title);

        // Initialize default room params as private and restricted
        externalAccessRoomSwitch.setChecked(false);
        setRoomAccessRule(RoomAccessRulesKt.RESTRICTED);
        publicPrivateRoomSwitch.setChecked(false);
        mRoomParams.visibility = RoomDirectoryVisibility.DIRECTORY_VISIBILITY_PRIVATE;
        mRoomParams.preset = CreateRoomParams.PRESET_PRIVATE_CHAT;
        // Hide the encrypted messages sent before the member is invited.
        mRoomParams.setHistoryVisibility(RoomState.HISTORY_VISIBILITY_INVITED);

        // A Tchap room member must be moderator to invite
        mRoomParams.powerLevelContentOverride = new HashMap<String, Object>() {{
            put("invite", CommonActivityUtils.UTILS_POWER_LEVEL_MODERATOR);
        }};

        // Prepare disable federation label by adding the hs display name of the current user.
        String userHSDomain = DinsicUtils.getHomeServerDisplayNameFromMXIdentifier(mSession.getMyUserId());
        disableFederationText.setText(getString(R.string.tchap_room_creation_disable_federation, userHSDomain));

        // Set the right border color on avatar
        hexagonMaskView.setBorderColor(ContextCompat.getColor(this, R.color.restricted_room_avatar_border_color));
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

                    mRoomParams.roomAliasName = mRoomParams.name.trim().replace(" ", "");

                    if (mRoomParams.roomAliasName.contains(":")) {
                        mRoomParams.roomAliasName = mRoomParams.roomAliasName.replace(":","");
                    }

                    if (mRoomParams.roomAliasName.isEmpty()) {
                        mRoomParams.roomAliasName = getRandomString();
                    } else {
                        mRoomParams.roomAliasName = mRoomParams.roomAliasName + getRandomString();
                    }
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

    @OnCheckedChanged(R.id.switch_external_access_room)
    void setRoomExternalAccess() {
        if (externalAccessRoomSwitch.isChecked()) {
            Log.d(LOG_TAG, "## unrestricted");
            setRoomAccessRule(RoomAccessRulesKt.UNRESTRICTED);
            hexagonMaskView.setBorderColor(ContextCompat.getColor(this, R.color.unrestricted_room_avatar_border_color));
        } else {
            Log.d(LOG_TAG, "## restricted");
            setRoomAccessRule(RoomAccessRulesKt.RESTRICTED);
            hexagonMaskView.setBorderColor(ContextCompat.getColor(this, R.color.restricted_room_avatar_border_color));
        }
    }

    @OnCheckedChanged(R.id.switch_public_private_room)
    void setRoomPrivacy() {
        if (publicPrivateRoomSwitch.isChecked()) {
            tvPublicPrivateRoomDescription.setTextColor(ContextCompat.getColor(this, R.color.vector_fuchsia_color));
            mRoomParams.visibility = RoomDirectoryVisibility.DIRECTORY_VISIBILITY_PUBLIC;
            mRoomParams.preset = CreateRoomParams.PRESET_PUBLIC_CHAT;
            mRoomParams.setHistoryVisibility(RoomState.HISTORY_VISIBILITY_WORLD_READABLE);
            Log.d(LOG_TAG, "## public");
            federationOption.setVisibility(View.VISIBLE);
            // Disable room external access option
            updateRoomExternalAccessOption(false);

        } else {
            tvPublicPrivateRoomDescription.setTextColor(ContextCompat.getColor(this, R.color.vector_tchap_text_color_light_grey));
            mRoomParams.visibility = RoomDirectoryVisibility.DIRECTORY_VISIBILITY_PRIVATE;
            mRoomParams.preset = CreateRoomParams.PRESET_PRIVATE_CHAT;
            // Hide the encrypted messages sent before the member is invited.
            mRoomParams.setHistoryVisibility(RoomState.HISTORY_VISIBILITY_INVITED);
            Log.d(LOG_TAG, "## private");
            // Remove potential change related to the federation
            disableFederationSwitch.setChecked(false);
            federationOption.setVisibility(View.GONE);
            mRoomParams.creation_content = null;
            // Enable the room access option
            updateRoomExternalAccessOption(true);
        }
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

                for (int index = 0; index < mParticipantsIds.size();) {
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
                    // This list can not be empty because the add button for the members selection is only activated if at least 1 member is selected.
                    // This list contains only matrixIds because the RoomInviteMembersActivity was opened in TCHAP_ONLY or NON_FEDERATED_TCHAP_ONLY mode.
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
     * Force the room access rule in the room creation parameters.
     *
     * @param roomAccessRule the expected room access rule, set null to remove any existing value.
     *                          see {@link RoomAccessRulesKt}
     */
    private void setRoomAccessRule(@Nullable String roomAccessRule) {
        // Remove the existing value if any.
        if (mRoomParams.initialStates != null && !mRoomParams.initialStates.isEmpty()) {
            final List<Event> newInitialStates = new ArrayList<>();
            for (Event event : mRoomParams.initialStates) {
                if (!event.type.equals(RoomAccessRulesKt.STATE_EVENT_TYPE)) {
                    newInitialStates.add(event);
                }
            }
            mRoomParams.initialStates = newInitialStates;
        }

        if (!TextUtils.isEmpty(roomAccessRule)) {
            Event roomAccessRulesEvent = new Event();
            roomAccessRulesEvent.type = RoomAccessRulesKt.STATE_EVENT_TYPE;

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

    private void updateRoomExternalAccessOption(boolean enable) {
        externalAccessRoomSwitch.setEnabled(enable);
        if (enable) {
            externalAccessRoomText.setTextColor(ContextCompat.getColor(this, R.color.vector_tchap_text_color_dark));
        } else {
            externalAccessRoomText.setTextColor(ContextCompat.getColor(this, R.color.vector_tchap_text_color_light_grey));
            // Remove any pending change
            externalAccessRoomSwitch.setChecked(false);
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
                            mRoomParams.roomAliasName = getRandomString();
                            createNewRoom();
                            break;
                        case ERROR_CODE_ROOM_ALIAS_ALREADY_TAKEN:
                            hideWaitingView();
                            mRoomParams.roomAliasName = getRandomString();
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
     * @param roomId          the room id.
     * @param thumbnailUri    the uri of the avatar image.
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
     * @param roomId        the room id.
     * @param contentUri    the uri of the avatar image.
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
     * @param roomId    the room id.
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
     * @param requestCode    the request id.
     */
    private void inviteMembers(int requestCode) {
        Intent intent = new Intent(TchapRoomCreationActivity.this, VectorRoomInviteMembersActivity.class);
        intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_MATRIX_ID, mSession.getMyUserId());
        intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_ACTION_ACTIVITY_MODE, VectorRoomInviteMembersActivity.ActionMode.RETURN_SELECTED_USER_IDS);
        // Check whether the federation has been disabled to limit the invitation to the non federated users
        if (null == mRoomParams.creation_content) {
            intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_CONTACTS_FILTER, VectorRoomInviteMembersActivity.ContactsFilter.TCHAP_ONLY);
        } else {
            intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_CONTACTS_FILTER, VectorRoomInviteMembersActivity.ContactsFilter.NON_FEDERATED_TCHAP_ONLY);
        }

        if (!mParticipantsIds.isEmpty()) {
            intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_IN_SELECTED_USER_IDS, (Serializable) mParticipantsIds);
        }

        startActivityForResult(intent, requestCode);
    }

    /**
     * Generate a random room alias of 10 characters to avoid empty room alias.
     */
    protected String getRandomString() {
        String RANDOMCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder stringBuilder = new StringBuilder();
        Random rnd = new Random();
        while (stringBuilder.length() < 7) { // length of the random string.
            int index = (int) (rnd.nextFloat() * RANDOMCHARS.length());
            stringBuilder.append(RANDOMCHARS.charAt(index));
        }
        String randomString = stringBuilder.toString();
        return randomString;
    }
}
