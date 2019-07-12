/*
 * Copyright 2014 OpenMarket Ltd
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

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.core.model.MatrixError;
import org.matrix.androidsdk.rest.model.User;
import org.matrix.androidsdk.rest.model.pid.ThreePid;
import org.matrix.androidsdk.core.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.gouv.tchap.activity.TchapLoginActivity;
import fr.gouv.tchap.sdk.rest.client.TchapThirdPidRestClient;
import fr.gouv.tchap.sdk.rest.model.Platform;

import butterknife.BindView;
import im.vector.Matrix;
import im.vector.R;
import im.vector.adapters.ParticipantAdapterItem;
import im.vector.adapters.VectorParticipantsAdapter;
import im.vector.contacts.Contact;
import im.vector.contacts.ContactsManager;
import fr.gouv.tchap.util.DinsicUtils;
import im.vector.settings.VectorLocale;
import im.vector.util.PermissionsToolsKt;
import im.vector.util.VectorUtils;
import im.vector.view.VectorAutoCompleteTextView;

/**
 * This class provides a way to search other user to invite them in a dedicated room
 */
public class VectorRoomInviteMembersActivity extends MXCActionBarActivity implements android.support.v7.widget.SearchView.OnQueryTextListener {

    private static final String LOG_TAG = VectorRoomInviteMembersActivity.class.getSimpleName();

    // room identifier
    public static final String EXTRA_ROOM_ID = "VectorInviteMembersActivity.EXTRA_ROOM_ID";

    // participants to hide in the list
    public static final String EXTRA_HIDDEN_PARTICIPANT_ITEMS = "VectorInviteMembersActivity.EXTRA_HIDDEN_PARTICIPANT_ITEMS";

    // boolean : true displays a dialog to confirm the member selection
    public static final String EXTRA_ADD_CONFIRMATION_DIALOG = "VectorInviteMembersActivity.EXTRA_ADD_CONFIRMATION_DIALOG";

    // the selected user ids list
    public static final String EXTRA_OUT_SELECTED_USER_IDS = "VectorInviteMembersActivity.EXTRA_OUT_SELECTED_USER_IDS";

    // the selected participants list
    public static final String EXTRA_OUT_SELECTED_PARTICIPANT_ITEMS = "VectorInviteMembersActivity.EXTRA_OUT_SELECTED_PARTICIPANT_ITEMS";

    // the already selected participants list
    public static final String EXTRA_IN_SELECTED_USER_IDS = "VectorInviteMembersActivity.EXTRA_IN_SELECTED_USER_IDS";

    // add an extra to precise the type of mode we want to open the VectorRoomInviteMembersActivity
    public static final String EXTRA_ACTION_ACTIVITY_MODE = "EXTRA_ACTION_ACTIVITY_MODE";

    // add an extra to precise the type of filter we want to display contacts
    public static final String EXTRA_CONTACTS_FILTER = "EXTRA_CONTACTS_FILTER";

    // This enum is used to define the behavior of the activity on the contact selection and/or on the action selection:
    // START_DIRECT_CHAT: the multi-selection is disabled, when a contact is selected a direct chat is opened to chat with him.
    // SEND_INVITE: the multi-selection is enabled, an invite is sent for each selected contact when the user presses the Invite option (from the options menu).
    // RETURN_SELECTED_USER_IDS: the multi-selection is enabled, the identifier (matrix id or email) of the selected contacts are returned when the user presses
    // the Add option from the options menu.
    public enum ActionMode { START_DIRECT_CHAT, SEND_INVITE, RETURN_SELECTED_USER_IDS }
    private ActionMode mActionMode = ActionMode.RETURN_SELECTED_USER_IDS;

    // This enum is used to filter the displayed contacts.
    // Note: the Tchap users for who a discussion (direct chat) exists will be considered as local contacts.
    // This means they will appear in the local contacts section.
    public enum ContactsFilter { ALL, TCHAP_ONLY, TCHAP_ONLY_WITHOUT_EXTERNALS, TCHAP_ONLY_WITHOUT_FEDERATION, NO_TCHAP_ONLY }
    private ContactsFilter mContactsFilter = ContactsFilter.ALL;

    // account data
    private String mMatrixId;

    // main UI items
    private View mParentLayout;
    private android.support.v7.widget.SearchView mSearchView;

    @BindView(R.id.room_details_members_list)
    ExpandableListView mListView;

    // participants list
    private List<ParticipantAdapterItem> mHiddenParticipantItems = new ArrayList<>();

    // adapter
    private VectorParticipantsAdapter mAdapter;

    // tell if a confirmation dialog must be displayed to validate the user ids list
    private boolean mAddConfirmationDialog;

    // The list of the identifiers of the current selected contacts
    // The type of these identifiers depends on the mContactsFilter:
    // - matrix id when mContactsFilter = ContactsFilter.TCHAP_ONLY, ContactsFilter.TCHAP_ONLY_WITHOUT_EXTERNALS or ContactsFilter.TCHAP_ONLY_WITHOUT_FEDERATION
    // - email address when mContactsFilter = ContactsFilter.NO_TCHAP_ONLY
    // - both in the other cases
    ArrayList<String> mUserIdsToInvite = new ArrayList<>();

    // Counts used to send invites by email
    private int mCount;
    private int mSuccessCount;

    // retrieve a matrix Id from an email
    private final ContactsManager.ContactsManagerListener mContactsListener = new ContactsManager.ContactsManagerListener() {
        @Override
        public void onRefresh() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onPatternUpdate(false);
                }
            });
        }

        @Override
        public void onContactPresenceUpdate(final Contact contact, final String matrixId) {
        }

        @Override
        public void onPIDsUpdate() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.onPIdsUpdate();
                }
            });
        }
    };

    // refresh the presence asap
    private final MXEventListener mEventsListener = new MXEventListener() {
        @Override
        public void onPresenceUpdate(final Event event, final User user) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Map<Integer, List<Integer>> visibleChildViews = VectorUtils.getVisibleChildViews(mListView, mAdapter);

                    for (Integer groupPosition : visibleChildViews.keySet()) {
                        List<Integer> childPositions = visibleChildViews.get(groupPosition);

                        for (Integer childPosition : childPositions) {
                            Object item = mAdapter.getChild(groupPosition, childPosition);

                            if (item instanceof ParticipantAdapterItem) {
                                ParticipantAdapterItem participantAdapterItem = (ParticipantAdapterItem) item;

                                if (TextUtils.equals(user.user_id, participantAdapterItem.mUserId)) {
                                    mAdapter.notifyDataSetChanged();
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        }
    };

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        onPatternUpdate(true);
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.activity_vector_invite_members;
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

        // Initialize search view
        setWaitingView(findViewById(R.id.search_progress_view));
        mParentLayout = findViewById(R.id.vector_invite_members_layout);
        mSearchView = findViewById(R.id.external_search_view);
        initSearchView();

        // Check if no view has focus:
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchView.getApplicationWindowToken(), 0);

        Intent intent = getIntent();

        // Get extras of intent
        if (intent.hasExtra(EXTRA_MATRIX_ID)) {
            mMatrixId = intent.getStringExtra(EXTRA_MATRIX_ID);
        }

        if (intent.hasExtra(EXTRA_HIDDEN_PARTICIPANT_ITEMS)) {
            mHiddenParticipantItems = (List<ParticipantAdapterItem>) intent.getSerializableExtra(EXTRA_HIDDEN_PARTICIPANT_ITEMS);
        }

        if (getIntent().hasExtra(EXTRA_ACTION_ACTIVITY_MODE)) {
            mActionMode = (ActionMode) intent.getSerializableExtra(EXTRA_ACTION_ACTIVITY_MODE);
        }

        if (getIntent().hasExtra(EXTRA_CONTACTS_FILTER)) {
            mContactsFilter = (ContactsFilter) intent.getSerializableExtra(EXTRA_CONTACTS_FILTER);
        }

        if (getIntent().hasExtra(EXTRA_IN_SELECTED_USER_IDS)) {
            mUserIdsToInvite = (ArrayList<String>) intent.getSerializableExtra(VectorRoomInviteMembersActivity.EXTRA_IN_SELECTED_USER_IDS);
        }

        // Initialize action bar title
        switch (mActionMode) {
            case START_DIRECT_CHAT:
                setTitle(R.string.tchap_room_invite_member_direct_chat);
                break;
            case RETURN_SELECTED_USER_IDS:
                setTitle(R.string.tchap_room_invite_member_title);
                break;
            case SEND_INVITE:
                setTitle(R.string.room_creation_invite_members);
                break;
        }

        // get current session
        mSession = Matrix.getInstance(getApplicationContext()).getSession(mMatrixId);

        if ((null == mSession) || !mSession.isAlive()) {
            finish();
            return;
        }

        String roomId = intent.getStringExtra(EXTRA_ROOM_ID);

        if (null != roomId) {
            mRoom = mSession.getDataHandler().getStore().getRoom(roomId);
        }

        // tell if a confirmation dialog must be displayed.
        mAddConfirmationDialog = intent.getBooleanExtra(EXTRA_ADD_CONFIRMATION_DIALOG, false);

        // the chevron is managed in the header view
        mListView.setGroupIndicator(null);

        mAdapter = new VectorParticipantsAdapter(this,
                R.layout.adapter_item_vector_add_participants,
                R.layout.adapter_item_vector_people_header,
                mSession, roomId, mContactsFilter);

        mAdapter.setSelectedUserIds(mUserIdsToInvite);

        // Support the contact edition in case of no tchap users
        if (mContactsFilter.equals(ContactsFilter.NO_TCHAP_ONLY)) {
            mAdapter.setEditParticipantListener(new VectorParticipantsAdapter.VectorParticipantsAdapterEditListener() {
                @Override
                public void editContactForm(final ParticipantAdapterItem participant) {
                    if (null != participant.mContact) {
                        DinsicUtils.editContactForm(VectorRoomInviteMembersActivity.this, VectorRoomInviteMembersActivity.this, getString(R.string.people_edit_contact_warning_msg), participant.mContact);
                    }
                }
            });
        }

        // To avoid inviting myself, I do not have to appear in the list of users to invite.
        if (!mHiddenParticipantItems.contains(mSession.getMyUser())) {
            ParticipantAdapterItem me = new ParticipantAdapterItem(mSession.getMyUser());
            mHiddenParticipantItems.add(me);
        }

        mAdapter.setHiddenParticipantItems(mHiddenParticipantItems);

        mListView.setAdapter(mAdapter);

        mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                boolean ret = false;
                final Object item = mAdapter.getChild(groupPosition, childPosition);

                if (item instanceof ParticipantAdapterItem) {
                    final ParticipantAdapterItem participantItem = (ParticipantAdapterItem) item;

                    if (mActionMode == ActionMode.START_DIRECT_CHAT) {
                        DinsicUtils.startDirectChat(VectorRoomInviteMembersActivity.this, mSession, participantItem);
                    } else {
                        updateParticipantListToInvite(participantItem);
                        mAdapter.notifyDataSetChanged();
                        invalidateOptionsMenu();
                    }
                }
                return ret;
            }
        });

        View inviteByIdTextView = findViewById(R.id.ly_invite_contacts_by_email);
        if (mActionMode == ActionMode.SEND_INVITE) {
            inviteByIdTextView.setVisibility(View.VISIBLE);
            inviteByIdTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(DinsicUtils.isExternalTchapSession(mSession)) {
                        DinsicUtils.alertSimpleMsg(VectorRoomInviteMembersActivity.this, getString(R.string.action_forbidden));
                    } else {
                        displayInviteByUserId();
                    }
                }
            });
        } else {
            inviteByIdTextView.setVisibility(View.GONE);
        }

        // Check permission to access contacts
        PermissionsToolsKt.checkPermissions(PermissionsToolsKt.PERMISSIONS_FOR_MEMBERS_SEARCH, this, PermissionsToolsKt.PERMISSION_REQUEST_CODE);
    }

    /**
     * Init search view
     */
    private void initSearchView() {
        // init the search view
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        // Remove unwanted left margin
        LinearLayout searchEditFrame = mSearchView.findViewById(R.id.search_edit_frame);
        if (null != searchEditFrame) {
            ViewGroup.MarginLayoutParams searchEditFrameParams = (ViewGroup.MarginLayoutParams) searchEditFrame.getLayoutParams();
            searchEditFrameParams.leftMargin = -30;
            searchEditFrameParams.rightMargin = -15;
            searchEditFrame.setLayoutParams(searchEditFrameParams);
        }
        ImageView searchMagIcon = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_mag_icon);
        searchMagIcon.setColorFilter(ContextCompat.getColor(VectorRoomInviteMembersActivity.this, R.color.tchap_search_bar_text));

        ImageView searchCloseIcon = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
        searchCloseIcon.setColorFilter(ContextCompat.getColor(this, R.color.tchap_search_bar_text));

        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setQueryHint(getString(R.string.search_hint));
        //necessary for getting focus when click on search icon:
        mSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v != null) {
                    mSearchView.setIconified(false);
                }

            }
        });
    }

    @Override
    public int getMenuRes() {
        hideKeyboard();
        return R.menu.tchap_room_invite_member_menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mActionMode == ActionMode.RETURN_SELECTED_USER_IDS) {
                    // Return the current list of the selected users in order to selected them if the picker is opened again
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_OUT_SELECTED_USER_IDS, mUserIdsToInvite);
                    setResult(RESULT_CANCELED, intent);
                } else {
                    startActivity(new Intent(this, VectorHomeActivity.class));
                }
                finish();
                return true;
            case R.id.action_invite_members:
                if (mActionMode == ActionMode.RETURN_SELECTED_USER_IDS) {
                    // Return the current list of the selected users in order to selected them if the picker is opened again
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_OUT_SELECTED_USER_IDS, mUserIdsToInvite);
                    setResult(RESULT_OK, intent);

                    finish();
                } else {
                    // Invite each selected email by creating a direct chat
                    inviteNoTchapContactsByEmail(mUserIdsToInvite, true);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_invite_members);
        
        switch (mActionMode) {
            case START_DIRECT_CHAT:
                item.setTitle("");
                item.setEnabled(!mUserIdsToInvite.isEmpty());
                break;
            case RETURN_SELECTED_USER_IDS:
                item.setTitle(R.string.tchap_room_invite_member_action);
                item.setEnabled(true);
                break;
            case SEND_INVITE:
                item.setTitle(R.string.invite);
                item.setEnabled(!mUserIdsToInvite.isEmpty());
                break;
            default:
                item.setEnabled(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        switch (mActionMode) {
            case RETURN_SELECTED_USER_IDS:
                Intent intentWithResult = new Intent();
                intentWithResult.putExtra(EXTRA_OUT_SELECTED_USER_IDS, mUserIdsToInvite);
                setResult(RESULT_CANCELED, intentWithResult);
                finish();
                break;
            default:
                super.onBackPressed();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSession.getDataHandler().addListener(mEventsListener);
        ContactsManager.getInstance().addListener(mContactsListener);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onPatternUpdate(false);
            }
        });

        mSearchView.setQuery("", false);
        mParentLayout.requestFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSession.getDataHandler().removeListener(mEventsListener);
        ContactsManager.getInstance().removeListener(mContactsListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (0 == permissions.length) {
            Log.d(LOG_TAG, "## onRequestPermissionsResult(): cancelled " + requestCode);
        } else if (requestCode == PermissionsToolsKt.PERMISSION_REQUEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                Log.d(LOG_TAG, "## onRequestPermissionsResult(): READ_CONTACTS permission granted");
                ContactsManager.getInstance().refreshLocalContactsSnapshot();
                onPatternUpdate(false);
            } else {
                Log.d(LOG_TAG, "## onRequestPermissionsResult(): READ_CONTACTS permission not granted");
                Toast.makeText(this, R.string.missing_permissions_warning, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * The search pattern has been updated
     */
    protected void onPatternUpdate(boolean isTypingUpdate) {
        String pattern = mSearchView.getQuery().toString();

        // display a spinner while the other room members are listed
        if (!mAdapter.isKnownMembersInitialized()) {
            showWaitingView();
        }

        // wait that the local contacts are populated
        if (!ContactsManager.getInstance().didPopulateLocalContacts()) {
            Log.d(LOG_TAG, "## onPatternUpdate() : The local contacts are not yet populated");
            mAdapter.reset();
            showWaitingView();
            return;
        }

        mAdapter.setSearchedPattern(pattern, null, new VectorParticipantsAdapter.OnParticipantsSearchListener() {
            @Override
            public void onSearchEnd(final int count) {
                if (mListView == null) {
                    // Activity is dead
                    return;
                }

                mListView.post(new Runnable() {
                    @Override
                    public void run() {
                        hideWaitingView();
                    }
                });
            }
        });
    }

    /**
     * Add or remove selected contacts to a list to invite them
     *
     * @param item the selected participants
     */
    private void updateParticipantListToInvite(ParticipantAdapterItem item) {
        boolean ret = false;
        ParticipantAdapterItem participantAdapterItem = item;
        if (item.mIsValid) {
            if (!mUserIdsToInvite.contains(participantAdapterItem.mUserId)) {
                mUserIdsToInvite.add(participantAdapterItem.mUserId);
            } else {
                mUserIdsToInvite.remove(participantAdapterItem.mUserId);
            }
            mAdapter.setSelectedUserIds(mUserIdsToInvite);
            ret = true;
        } else {
            DinsicUtils.editContact(VectorRoomInviteMembersActivity.this, getApplicationContext(), item);
        }
    }

    /**
     * Display the invitation dialog.
     */
    private void displayInviteByUserId() {
        View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_invite_by_id, null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.people_search_invite_by_id_dialog_title)
                .setView(dialogLayout);

        final VectorAutoCompleteTextView inviteTextView = dialogLayout.findViewById(R.id.invite_by_id_edit_text);
        // Tchap : only email is accepted so disable autocompletion
        // inviteTextView.initAutoCompletion(mSession);

        final AlertDialog inviteDialog = builder
                .setPositiveButton(R.string.invite, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // will be overridden to avoid dismissing the dialog while displaying the progress
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();

        final Button inviteButton = inviteDialog.getButton(AlertDialog.BUTTON_POSITIVE);

        if (null != inviteButton) {
            inviteButton.setEnabled(false);

            inviteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String text = inviteTextView.getText().toString();
                    text = text.toLowerCase(VectorLocale.INSTANCE.getApplicationLocale()).trim();
                    final ArrayList<String> emails = new ArrayList<>();

                    Pattern pattern = android.util.Patterns.EMAIL_ADDRESS;
                    Matcher matcher = pattern.matcher(text);

                    while (matcher.find()) {
                        try {
                            String userEmail = text.substring(matcher.start(0), matcher.end(0));
                            emails.add(userEmail);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "## displayInviteByUserEmail() " + e.getMessage());
                        }
                    }

                    // In order to prepare the lookup on 3pids,
                    // We have to specify the type of media : email or phone number.
                    // In this case, the media type always is an email.
                    // That's why we create a new list of the same size as the list of emails,
                    // in which the media type always is an email.
                    final List<String> medias = new ArrayList<>();

                    for (String email : emails) {
                        medias.add(ThreePid.MEDIUM_EMAIL);
                    }

                    // Check for each email whether there is an associated account with a Matrix id.
                    showWaitingView();

                    final ApiCallback<List<String>> callback = new ApiCallback<List<String>>() {
                        @Override
                        public void onSuccess(final List<String> pids) {
                            Log.e(LOG_TAG, "bulkLookup: success " + pids.size());

                            for (int index = 0; index < emails.size();) {
                                final String email = emails.get(index);
                                String mxId = pids.get(index);

                                if (!TextUtils.isEmpty(mxId)) {
                                    // We check here if a discussion already exists for this Tchap user.
                                    // We consider the pendingInvites because we could have a pending invite from this Tchap user.
                                    Room existingRoom = DinsicUtils.isDirectChatRoomAlreadyExist(mxId, mSession, true);

                                    if (null != existingRoom) {
                                        // If a direct chat already exists, we do not invite him
                                        // We remove this email from the list to invite.
                                        emails.remove(index);
                                        pids.remove(index);

                                        // and we notify the user by a toast
                                        String message = getString(R.string.tchap_discussion_already_exist, email);
                                        Toast.makeText(VectorRoomInviteMembersActivity.this, message, Toast.LENGTH_LONG).show();

                                    } else {
                                        // Presently, invite a Tchap user by his email is not supported correctly.
                                        // The resulting room is seen as direct for the inviter and not for the receiver.
                                        // Patch : we invite him by considering his id instead of the email.
                                        emails.remove(index);
                                        pids.remove(index);
                                        inviteDiscoveredTchapUser(mxId, email);
                                    }
                                } else {
                                    index ++;
                                }
                            }

                            hideWaitingView();

                            if (!emails.isEmpty()) {
                                // Invite each typed email by creating a direct chat
                                // Stay in the activity if there is at least one contact selected
                                inviteNoTchapContactsByEmail(emails, mUserIdsToInvite.isEmpty());
                            }
                        }

                        /**
                         * Common error routine
                         * @param errorMessage the error message
                         */
                        private void onError(String errorMessage) {
                            Log.e(LOG_TAG, "## bulkLookup: failed " + errorMessage);
                            Toast.makeText(VectorRoomInviteMembersActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onNetworkError(Exception e) {
                            onError(e.getMessage());
                        }

                        @Override
                        public void onMatrixError(MatrixError e) {
                            onError(e.getMessage());
                        }

                        @Override
                        public void onUnexpectedError(Exception e) {
                            onError(e.getMessage());
                        }
                    };

                    // Use the proxied lookup API
                    TchapThirdPidRestClient tchapThirdPidRestClient = new TchapThirdPidRestClient(mSession.getHomeServerConfig());
                    tchapThirdPidRestClient.bulkLookup(emails, medias, callback);

                    inviteDialog.dismiss();
                }
            });
        }

        inviteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (null != inviteButton) {
                    String text = inviteTextView.getText().toString();
                    inviteButton.setEnabled(android.util.Patterns.EMAIL_ADDRESS.matcher(text).find());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    //==============================================================================================
    // Handle keyboard visibility
    //==============================================================================================

    private void hideKeyboard () {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(this.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    //==============================================================================================
    // Handle room creation with a list  of users id
    //==============================================================================================

    /**
     * Invite by email one or more no-Tchap user(s)
     *
     * @param emails  the participant's email list
     * @param finish  tell whether the activity should be closed after this operation.
     */
    private void inviteNoTchapContactsByEmail (final ArrayList<String> emails, final boolean finish) {

        if (0 != mCount) {
            Log.e(LOG_TAG, "##inviteNoTchapContactsByEmail : invitations are being sent");
            return;
        }

        showWaitingView();
        mCount = emails.size();
        mSuccessCount = 0;

        for (final String email : emails) {
            // We check if this email has been already invited
            // (pendingInvites are ignored here because we could not have a pending invite related to an email)
            Room existingRoom = DinsicUtils.isDirectChatRoomAlreadyExist(email, mSession, false);

            if (null != existingRoom) {
                // If a direct chat already exists, we do not re-invite the NoTchapUser
                // and we notify the user by a toast
                String message = getString(R.string.tchap_invite_already_send_message, email);
                Toast.makeText(VectorRoomInviteMembersActivity.this, message, Toast.LENGTH_LONG).show();

                // We decrement the counter before testing if it is equal to zero.
                // If the counter is equal to zero, it means that we have reached the end of the list.
                if (-- mCount == 0) {
                    onNoTchapInviteDone(finish);
                }
            } else {
                // For each email of the list, call server to check if Tchap registration is available for this email
                TchapLoginActivity.discoverTchapPlatform(this, email, new ApiCallback<Platform>() {
                    private void onError(String message) {
                        Toast.makeText(VectorRoomInviteMembersActivity.this, message, Toast.LENGTH_LONG).show();

                        // We decrement the counter before testing if it is equal to zero.
                        // If the counter is equal to zero, it means that we have reached the end of the list.
                        if (-- mCount == 0) {
                            onNoTchapInviteDone(finish);
                        }
                    }

                    @Override
                    public void onSuccess(Platform platform) {
                        // Check whether the returned platform is valid
                        if (null == platform.hs || platform.hs.isEmpty()) {
                            // The email owner is not able to create a tchap account,
                            onError(getString(R.string.tchap_invite_unreachable_message, email));
                            return;
                        }

                        // The email owner is able to create a tchap account,
                        // we create a direct chat with him, and invite him by email to join Tchap.
                        DinsicUtils.createDirectChat(mSession, email, new ApiCallback<String>() {
                            @Override
                            public void onSuccess(final String roomId) {
                                // For each successful direct chat creation and invitation,
                                // we increment the counter "mSuccessCount".
                                mSuccessCount ++;

                                if (-- mCount == 0) {
                                    onNoTchapInviteDone(finish);
                                }
                            }

                            private void onError(final String message) {
                                Log.e(LOG_TAG, "##inviteNoTchapUserByEmail failed : " + message);
                                new AlertDialog.Builder(VectorRoomInviteMembersActivity.this)
                                        .setMessage(getString(R.string.tchap_send_invite_failed, email))
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                                // Despite the error, we continue the process
                                                // until we reach the end of the list.
                                                if (-- mCount == 0) {
                                                    onNoTchapInviteDone(finish);
                                                }
                                            }
                                        })
                                        .show();
                            }

                            @Override
                            public void onNetworkError(Exception e) {
                                onError(e.getLocalizedMessage());
                            }

                            @Override
                            public void onMatrixError(final MatrixError e) {
                                if (MatrixError.M_CONSENT_NOT_GIVEN.equals(e.errcode)) {
                                    // Request the consent only once, and cancel the invite(s)
                                    if (mCount != 0) {
                                        mCount = 0;
                                        getConsentNotGivenHelper().displayDialog(e);
                                    }
                                } else {
                                    onError(e.getLocalizedMessage());
                                }
                            }

                            @Override
                            public void onUnexpectedError(final Exception e) {
                                onError(e.getLocalizedMessage());
                            }
                        });
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        onError(getString(R.string.tchap_send_invite_network_error));
                    }

                    @Override
                    public void onMatrixError(MatrixError matrixError) {
                        onError(getString(R.string.tchap_invite_unreachable_message, email));
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        onError(getString(R.string.tchap_invite_unreachable_message, email));
                    }
                } );
            }
        }
    }

    /**
     * Invite a Tchap user discovered by filling his email
     *
     * @param tchapUserId  the Tchap user id
     * @param email        the email used to discovere him
     */
    private void inviteDiscoveredTchapUser(final String tchapUserId, final String email) {
        DinsicUtils.createDirectChat(mSession, tchapUserId, new ApiCallback<String>() {
            @Override
            public void onSuccess(final String roomId) {
                // and we notify the user by a toast
                String message = getString(R.string.tchap_start_discussion_with_discovered_user, email);
                Toast.makeText(VectorRoomInviteMembersActivity.this, message, Toast.LENGTH_LONG).show();
            }

            private void onError(final String message) {
                Log.e(LOG_TAG, "##inviteDiscoveredTchapUser failed : " + message);
                new AlertDialog.Builder(VectorRoomInviteMembersActivity.this)
                        .setMessage(getString(R.string.tchap_send_invite_failed, email))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(final MatrixError e) {
                if (MatrixError.M_CONSENT_NOT_GIVEN.equals(e.errcode)) {
                    getConsentNotGivenHelper().displayDialog(e);
                } else {
                    onError(e.getLocalizedMessage());
                }
            }

            @Override
            public void onUnexpectedError(final Exception e) {
                onError(e.getLocalizedMessage());
            }
        });
    }

    private void onNoTchapInviteDone(boolean finish) {
        hideWaitingView();

        if (mSuccessCount > 0) {
            displayLocalNotification();
        }

        if (finish) {
            // We close the current screen and we come back on the hme screen.
            startActivity(new Intent(this, VectorHomeActivity.class));
            finish();
        }
    }

    private void displayLocalNotification() {
        Log.e(LOG_TAG, "##inviteNoTchapUserByEmail : display notification" );

        // Handle notification
        SpannableString text = new SpannableString(getResources().getQuantityString(R.plurals.tchap_succes_invite__notification, mSuccessCount, mSuccessCount));
        Toast.makeText(VectorRoomInviteMembersActivity.this, text + " \n" + getString(R.string.tchap_send_invite_confirmation), Toast.LENGTH_SHORT).show();

        // @TODO FIXME create a new function in NotificationUtils to handle the local notif on sent invitations
//        Intent intent = new Intent(this, VectorHomeActivity.class);
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SILENT_NOTIFICATION_CHANNEL_ID);
//        builder.setAutoCancel(true)
//                .setDefaults(Notification.FLAG_LOCAL_ONLY)
//                .setWhen(System.currentTimeMillis())
//                .setSmallIcon(R.drawable.logo_transparent)
//                .setTicker(text)
//                .setContentTitle(text)
//                .setContentText(getString(R.string.tchap_send_invite_confirmation))
//                .setDefaults(Notification.DEFAULT_LIGHTS| Notification.DEFAULT_SOUND)
//                .setContentIntent(contentIntent)
//                .setContentInfo("Info");
//
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.notify(1, builder.build());
    }
}
