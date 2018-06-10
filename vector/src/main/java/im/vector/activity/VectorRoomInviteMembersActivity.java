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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SearchView;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.RoomMember;
import org.matrix.androidsdk.rest.model.User;
import org.matrix.androidsdk.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.gouv.tchap.activity.TchapLoginActivity;
import im.vector.Matrix;
import im.vector.R;
import im.vector.adapters.ParticipantAdapterItem;
import im.vector.adapters.VectorParticipantsAdapter;
import im.vector.contacts.Contact;
import im.vector.contacts.ContactsManager;
import fr.gouv.tchap.util.DinsicUtils;
import im.vector.util.VectorUtils;
import im.vector.view.VectorAutoCompleteTextView;

/**
 * This class provides a way to search other user to invite them in a dedicated room
 */
public class VectorRoomInviteMembersActivity extends MXCActionBarActivity implements SearchView.OnQueryTextListener {

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

    // add an extra to precise the type of filter we want to display contacts
    public static final String EXTRA_INVITE_CONTACTS_FILTER = "EXTRA_INVITE_CONTACTS_FILTER";

    // This enum is used to filter the display of the contacts
    public enum ContactsFilter { ALL, TCHAP_ONLY, NO_TCHAP_ONLY }
    private ContactsFilter mContactsFilter = ContactsFilter.ALL;

    // This enum is used to select a mode for room creation
    private VectorRoomCreationActivity.RoomCreationModes mMode = VectorRoomCreationActivity.RoomCreationModes.NEW_ROOM;

    // account data
    private String mMatrixId;

    // main UI items
    private View mParentLayout;
    private SearchView mSearchView;
    private ExpandableListView mListView;

    // participants list
    private List<ParticipantAdapterItem> mHiddenParticipantItems = new ArrayList<>();

    // adapter
    private VectorParticipantsAdapter mAdapter;

    // tell if a confirmation dialog must be displayed to validate the user ids list
    private boolean mAddConfirmationDialog;

    // The list of the identifiers of the current selected contacts
    // The type of these identifiers depends on the mContactsFilter:
    // - matrix id when mContactsFilter = ContactsFilter.TCHAP_ONLY
    // - email address when mContactsFilter = ContactsFilter.NO_TCHAP_ONLY
    // - both in the other cases
    ArrayList<String> userIdsToInvite = new ArrayList<>();

    // TODO Remove this array usage
    ArrayList<ParticipantAdapterItem> participantsItemToInvite = new ArrayList<>();

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
        mParentLayout = findViewById(R.id.vector_invite_members_layout);
        mSearchView = findViewById(R.id.external_search_view);
        mSearchView.setOnQueryTextListener(this);

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

        if (getIntent().hasExtra(VectorRoomCreationActivity.EXTRA_ROOM_CREATION_ACTIVITY_MODE)) {
            mMode = (VectorRoomCreationActivity.RoomCreationModes) intent.getSerializableExtra(VectorRoomCreationActivity.EXTRA_ROOM_CREATION_ACTIVITY_MODE);
        }

        if (getIntent().hasExtra(EXTRA_INVITE_CONTACTS_FILTER)) {
            mContactsFilter = (ContactsFilter) intent.getSerializableExtra(EXTRA_INVITE_CONTACTS_FILTER);
        }

        // Initialize action bar title
        switch (mMode) {
            case DIRECT_CHAT:
                setTitle(R.string.tchap_room_invite_member_direct_chat);
                break;
            case NEW_ROOM:
                setTitle(R.string.tchap_room_invite_member_title);
                break;
            case INVITE:
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

        setWaitingView(findViewById(R.id.search_in_progress_view));

        mListView = findViewById(R.id.room_details_members_list);
        // the chevron is managed in the header view
        mListView.setGroupIndicator(null);

        mAdapter = new VectorParticipantsAdapter(this,
                R.layout.adapter_item_vector_add_participants,
                R.layout.adapter_item_vector_people_header,
                mSession, roomId, mContactsFilter);

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

        mAdapter.setHiddenParticipantItems(mHiddenParticipantItems);

        mListView.setAdapter(mAdapter);

        mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                boolean ret = false;
                final Object item = mAdapter.getChild(groupPosition, childPosition);

                if (item instanceof ParticipantAdapterItem) {
                    final ParticipantAdapterItem participantItem = (ParticipantAdapterItem) item;

                    if (null != mMode && mMode == VectorRoomCreationActivity.RoomCreationModes.DIRECT_CHAT) {
                        DinsicUtils.startDirectChat(VectorRoomInviteMembersActivity.this, mSession, participantItem);
                    } else {
                        updateParticipantListToInvite(participantItem);
                        mAdapter.mCurrentSelectedUsers = userIdsToInvite;
                        mAdapter.notifyDataSetChanged();
                        invalidateOptionsMenu();
                    }
                }
                return ret;
            }
        });

        View inviteByIdTextView = findViewById(R.id.search_invite_by_id);
        if (mMode.equals(VectorRoomCreationActivity.RoomCreationModes.INVITE)) {
            inviteByIdTextView.setVisibility(View.VISIBLE);
            inviteByIdTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(TchapLoginActivity.isUserExternal(mSession)) {
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
        CommonActivityUtils.checkPermissions(CommonActivityUtils.REQUEST_CODE_PERMISSION_MEMBERS_SEARCH, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tchap_room_invite_member_menu, menu);
        hideKeyboard();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_invite_members:
                // Return the list of the members ids selected to invite for the room creation
                Intent intent = new Intent();
                intent.putExtra(EXTRA_OUT_SELECTED_USER_IDS, userIdsToInvite);
                intent.putExtra(EXTRA_OUT_SELECTED_PARTICIPANT_ITEMS, participantsItemToInvite);
                setResult(RESULT_OK, intent);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_invite_members);

        item.setEnabled(!userIdsToInvite.isEmpty());

        switch (mMode) {
            case DIRECT_CHAT:
                item.setTitle("");
                break;
            case NEW_ROOM:
                item.setTitle(R.string.tchap_room_invite_member_action);
                break;
            case INVITE:
                item.setTitle(R.string.invite);
                break;
        }

        return super.onPrepareOptionsMenu(menu);
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
    public void onRequestPermissionsResult(int aRequestCode, @NonNull String[] aPermissions, @NonNull int[] aGrantResults) {
        if (0 == aPermissions.length) {
            Log.e(LOG_TAG, "## onRequestPermissionsResult(): cancelled " + aRequestCode);
        } else if (aRequestCode == CommonActivityUtils.REQUEST_CODE_PERMISSION_MEMBERS_SEARCH) {
            if (PackageManager.PERMISSION_GRANTED == aGrantResults[0]) {
                Log.d(LOG_TAG, "## onRequestPermissionsResult(): READ_CONTACTS permission granted");
                ContactsManager.getInstance().refreshLocalContactsSnapshot();
                onPatternUpdate(false);
            } else {
                Log.d(LOG_TAG, "## onRequestPermissionsResult(): READ_CONTACTS permission not granted");
                CommonActivityUtils.displayToast(this, getString(R.string.missing_permissions_warning));
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
     * Display a selection confirmation dialog.
     *
     * @param participantAdapterItems the selected participants
     */
    private void finish(final ArrayList<ParticipantAdapterItem> participantAdapterItems) {
        final List<String> hiddenUserIds = new ArrayList<>();
        final ArrayList<String> userIds = new ArrayList<>();
        final ArrayList<String> displayNames = new ArrayList<>();

        // list the hidden user Ids
        for (ParticipantAdapterItem item : mHiddenParticipantItems) {
            hiddenUserIds.add(item.mUserId);
        }

        // if a room is defined
        if (null != mRoom) {
            // the room members must not be added again
            Collection<RoomMember> members = mRoom.getLiveState().getDisplayableMembers();
            for (RoomMember member : members) {
                if (TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_JOIN) || TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_INVITE)) {
                    hiddenUserIds.add(member.getUserId());
                }
            }
        }

        boolean isStrangers = false;
        // build the output lists
        for (ParticipantAdapterItem item : participantAdapterItems) {
            // check if the user id can be added
            if (!hiddenUserIds.contains(item.mUserId)) {
                userIds.add(item.mUserId);
                // display name
                if (MXSession.isUserId(item.mUserId)) {
                    User user = mSession.getDataHandler().getStore().getUser(item.mUserId);
                    if ((null != user) && !TextUtils.isEmpty(user.displayname)) {
                        displayNames.add(user.displayname);
                    } else {
                        displayNames.add(item.mUserId);
                    }
                } else {
                    displayNames.add(item.mUserId);
                    if (item.mContact != null && item.mContact.getEmails().size()>0)
                        isStrangers |= !DinsicUtils.isFromFrenchGov(item.mContact.getEmails());
                    else
                    if (item.mUserId!= null && android.util.Patterns.EMAIL_ADDRESS.matcher(item.mUserId).matches()){
                        List<String>myAddress = new ArrayList<>();
                        myAddress.add(item.mUserId);
                        isStrangers |= !DinsicUtils.isFromFrenchGov(myAddress);
                    }
                }
            }
        }
        // a confirmation dialog has been requested
        if ((isStrangers || mAddConfirmationDialog) && (displayNames.size() > 0)) {
            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(VectorRoomInviteMembersActivity.this);
            builder.setTitle(R.string.dialog_title_confirmation);

            String message = "";

            if (displayNames.size() == 1) {
                message = displayNames.get(0);
            } else {
                for (int i = 0; i < (displayNames.size() - 2); i++) {
                    message += displayNames.get(i) + ", ";
                }

                message += displayNames.get(displayNames.size() - 2) + " " + getText(R.string.and) + " " + displayNames.get(displayNames.size() - 1);
            }
            if (isStrangers) {
                builder.setMessage(getString(R.string.room_invite_non_gov_people));
            }
            else {
                builder.setMessage(getString(R.string.room_participants_invite_prompt_msg, message));
            }
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // returns the selected users
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_OUT_SELECTED_USER_IDS, userIds);
                    intent.putExtra(EXTRA_OUT_SELECTED_PARTICIPANT_ITEMS, participantAdapterItems);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });

            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // nothing to do
                }
            });

            builder.show();
        } else {
            // returns the selected users
            Intent intent = new Intent();
            intent.putExtra(EXTRA_OUT_SELECTED_USER_IDS, userIds);
            intent.putExtra(EXTRA_OUT_SELECTED_PARTICIPANT_ITEMS, participantAdapterItems);
            setResult(RESULT_OK, intent);
            finish();
        }
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
            if (!userIdsToInvite.contains(participantAdapterItem.mUserId)) {
                userIdsToInvite.add(participantAdapterItem.mUserId);
                participantsItemToInvite.add(participantAdapterItem);
                participantAdapterItem.mIsSelectedToInvite = true;
            } else {
                userIdsToInvite.remove(participantAdapterItem.mUserId);
                participantsItemToInvite.remove(participantAdapterItem);
                participantAdapterItem.mIsSelectedToInvite = false;
            }
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

        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.people_search_invite_by_id_dialog_title);
        dialog.setView(dialogLayout);

        final VectorAutoCompleteTextView inviteTextView = dialogLayout.findViewById(R.id.invite_by_id_edit_text);
        // Tchap : only email is accepted so disable autocompletion
        // inviteTextView.initAutoCompletion(mSession);

        dialog.setPositiveButton(R.string.invite, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // will be overridden to avoid dismissing the dialog while displaying the progress
            }
        });

        dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog inviteDialog = dialog.show();
        final Button inviteButton = inviteDialog.getButton(AlertDialog.BUTTON_POSITIVE);

        if (null != inviteButton) {
            inviteButton.setEnabled(false);

            inviteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String text = inviteTextView.getText().toString();
                    ArrayList<ParticipantAdapterItem> items = new ArrayList<>();

                    Pattern pattern = android.util.Patterns.EMAIL_ADDRESS;
                    Matcher matcher = pattern.matcher(text);

                    while (matcher.find()) {
                        try {
                            String userEmail = text.substring(matcher.start(0), matcher.end(0));
                            items.add(new ParticipantAdapterItem(userEmail, null, userEmail, true));
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "## displayInviteByUserEmail() " + e.getMessage());
                        }
                    }

                    finish(items);

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

                    boolean containMXID = MXSession.PATTERN_CONTAIN_MATRIX_USER_IDENTIFIER.matcher(text).find();
                    boolean containEmailAddress = android.util.Patterns.EMAIL_ADDRESS.matcher(text).find();

                    inviteButton.setEnabled(containMXID || containEmailAddress);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    //==============================================================================================================
    // Handle keyboard visibility
    //==============================================================================================================

    private void hideKeyboard () {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(this.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
