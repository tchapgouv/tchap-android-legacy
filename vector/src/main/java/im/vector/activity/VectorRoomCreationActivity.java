/*
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import org.matrix.androidsdk.rest.model.CreateRoomParams;
import org.matrix.androidsdk.rest.model.RoomMember;
import org.matrix.androidsdk.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


import org.matrix.androidsdk.data.store.IMXStore;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.rest.callback.SimpleApiCallback;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.MXSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import im.vector.R;
import im.vector.adapters.ParticipantAdapterItem;
import im.vector.adapters.VectorRoomCreationAdapter;
import fr.gouv.tchap.util.DinsicUtils;
import im.vector.util.ThemeUtils;

public class VectorRoomCreationActivity extends MXCActionBarActivity {
    // tags
    private static final String LOG_TAG = VectorRoomCreationActivity.class.getSimpleName();

    private static final int INVITE_USER_REQUEST_CODE = 456;

    // participants list
    private static final String PARTICIPANTS_LIST = "PARTICIPANTS_LIST";

    // add an extra to precise the type of mode we want to open the VectorRoomCreationActivity
    public static final String EXTRA_ROOM_CREATION_ACTIVITY_MODE = "EXTRA_ROOM_CREATION_ACTIVITY_MODE";

    // This enum is used to select a mode for a room creation
    public enum RoomCreationModes { DIRECT_CHAT, INVITE, NEW_ROOM }
    private RoomCreationModes mMode = RoomCreationModes.NEW_ROOM;

    // UI items
    private ListView membersListView;
    private VectorRoomCreationAdapter mAdapter;

    // the search is displayed at first call
    private boolean mIsFirstResume = true;

    // displayed participants
    private ArrayList<ParticipantAdapterItem> mParticipants = new ArrayList<>();

    @Override
    public int getLayoutRes() {
        return R.layout.activity_vector_room_creation;
    }

    @Override
    public void initUiAndData() {
        if (CommonActivityUtils.shouldRestartApp(this)) {
            Log.e(LOG_TAG, "onCreate : Restart the application.");
            CommonActivityUtils.restartApp(this);
            return;
        }

        Intent intent = getIntent();

        // Get extras of intent
        if (getIntent().hasExtra(EXTRA_ROOM_CREATION_ACTIVITY_MODE)) {
            mMode = (RoomCreationModes) getIntent().getSerializableExtra(EXTRA_ROOM_CREATION_ACTIVITY_MODE);
        }

        mSession = getSession(intent);

        if (mSession == null) {
            Log.e(LOG_TAG, "No MXSession.");
            finish();
            return;
        }

        // get the UI items
        setWaitingView(findViewById(R.id.room_creation_spinner_views));
        membersListView = findViewById(R.id.room_creation_members_list_view);
        mAdapter = new VectorRoomCreationAdapter(this, R.layout.adapter_item_vector_creation_add_member, R.layout.adapter_item_vector_add_participants, mSession);

        // init the content
        if (!isFirstCreation() && getSavedInstanceState().containsKey(PARTICIPANTS_LIST)) {
            mParticipants.clear();
            mParticipants = new ArrayList<>((List<ParticipantAdapterItem>) getSavedInstanceState().getSerializable(PARTICIPANTS_LIST));
        } else {
            mParticipants.add(new ParticipantAdapterItem(mSession.getMyUser()));
        }
        mAdapter.addAll(mParticipants);

        membersListView.setAdapter(mAdapter);

        mAdapter.setRoomCreationAdapterListener(new VectorRoomCreationAdapter.IRoomCreationAdapterListener() {
            @Override
            public void OnRemoveParticipantClick(ParticipantAdapterItem item) {
                mParticipants.remove(item);
                mAdapter.remove(item);
            }
        });

        membersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // the first one is "add a member"
                if (0 == position) {
                    switch (mMode) {
                        // case mMode == DIRECT_CHAT is ignored here because it is unexpected
                        // we can not click on "Add member" in this case
                        case INVITE:
                            launchInviteMembersActivity(mMode , VectorRoomInviteMembersActivity.ContactsFilter.NO_TCHAP_ONLY, INVITE_USER_REQUEST_CODE);
                            break;
                        case NEW_ROOM:
                            launchInviteMembersActivity(mMode, VectorRoomInviteMembersActivity.ContactsFilter.ALL, INVITE_USER_REQUEST_CODE);
                            break;
                    }
                }
            }
        });
    }

    /***
     * Launch contacts search activity
     *
     * @param requestCode correspond to the room creation mode
     */
    public void launchInviteMembersActivity(RoomCreationModes mode, VectorRoomInviteMembersActivity.ContactsFilter contactsFilter, int requestCode) {
        Intent intent = new Intent(VectorRoomCreationActivity.this, VectorRoomInviteMembersActivity.class);
        intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_MATRIX_ID, mSession.getMyUserId());
        intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_HIDDEN_PARTICIPANT_ITEMS, mParticipants);
        intent.putExtra(EXTRA_ROOM_CREATION_ACTIVITY_MODE, mode);
        intent.putExtra(VectorRoomInviteMembersActivity.EXTRA_INVITE_CONTACTS_FILTER, contactsFilter);
        startActivity(intent);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mIsFirstResume && null != mMode) {
            mIsFirstResume = false;

            switch (mMode) {
                case DIRECT_CHAT:
                    launchInviteMembersActivity(mMode, VectorRoomInviteMembersActivity.ContactsFilter.ALL, INVITE_USER_REQUEST_CODE);
                    break;
                case INVITE:
                    launchInviteMembersActivity(mMode, VectorRoomInviteMembersActivity.ContactsFilter.NO_TCHAP_ONLY, INVITE_USER_REQUEST_CODE);
                    break;
                case NEW_ROOM:
                    launchInviteMembersActivity(mMode, VectorRoomInviteMembersActivity.ContactsFilter.ALL, INVITE_USER_REQUEST_CODE);
                    break;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(PARTICIPANTS_LIST, mParticipants);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (null != savedInstanceState) {
            if (savedInstanceState.containsKey(PARTICIPANTS_LIST)) {
                mParticipants = new ArrayList<>((List<ParticipantAdapterItem>) savedInstanceState.getSerializable(PARTICIPANTS_LIST));
            } else {
                mParticipants.clear();
                mParticipants.add(new ParticipantAdapterItem(mSession.getMyUser()));
            }
            mAdapter.clear();
            mAdapter.addAll(mParticipants);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case INVITE_USER_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    List<ParticipantAdapterItem> items = (List<ParticipantAdapterItem>) data.getSerializableExtra(VectorRoomInviteMembersActivity.EXTRA_OUT_SELECTED_PARTICIPANT_ITEMS);
                    mParticipants.addAll(items);
                    mAdapter.addAll(items);
                    mAdapter.sort(mAlphaComparator);
                } else if (1 == mParticipants.size()) {
                    // the user cancels the first user selection so assume he wants to cancel the room creation.
                    this.finish();
                }
                break;
        }
    }

    // Comparator to order members alphabetically
    // the self item is always kept at top
    private final Comparator<ParticipantAdapterItem> mAlphaComparator = new Comparator<ParticipantAdapterItem>() {
        @Override
        public int compare(ParticipantAdapterItem part1, ParticipantAdapterItem part2) {
            // keep the self user id at top
            if (TextUtils.equals(part1.mUserId, mSession.getMyUserId())) {
                return -1;
            }

            if (TextUtils.equals(part2.mUserId, mSession.getMyUserId())) {
                return +1;
            }

            String lhs = part1.getComparisonDisplayName();
            String rhs = part2.getComparisonDisplayName();

            if (lhs == null) {
                return -1;
            } else if (rhs == null) {
                return 1;
            }

            return String.CASE_INSENSITIVE_ORDER.compare(lhs, rhs);
        }
    };


    //=============================================================================================
    // Room creation
    //=============================================================================================

    /**
     * Return the first direct chat room for a given user ID.
     *
     * @param aUserId user ID to search for
     * @param mSession current session
     * @param includeInvite boolean to tell us if pending invitations have to be consider or not
     * @return a room ID if search succeed, null otherwise.
     */
    public static Room isDirectChatRoomAlreadyExist(String aUserId, MXSession mSession, boolean includeInvite) {
        if (null != mSession) {
            IMXStore store = mSession.getDataHandler().getStore();
            HashMap<String, List<String>> directChatRoomsDict;

            if (null != store.getDirectChatRoomsDict()) {
                directChatRoomsDict = new HashMap<>(store.getDirectChatRoomsDict());

                if (directChatRoomsDict.containsKey(aUserId)) {
                    ArrayList<String> roomIdsList = new ArrayList<>(directChatRoomsDict.get(aUserId));

                    if (!roomIdsList.isEmpty()) {
                        // In the description of the memberships, we display first the current user status and the other member in second.
                        // We review all the direct chats by considering the memberships in the following priorities :
                        // 1. join-join
                        // 2. invite-join
                        // 3. join-invite
                        // 4. join-left (or invite-left)
                        // The case left-x isn't possible because we ignore for the moment the left rooms.
                        Room roomCandidateLeftByOther = null;
                        Room roomCandidatePendingInvite = null;
                        boolean isPendingInvite = false;

                        for (String roomId : roomIdsList) {
                            Room room = mSession.getDataHandler().getRoom(roomId, false);
                            // check if the room is already initialized
                            if ((null != room) && room.isReady() && !room.isLeaving()) {
                                isPendingInvite = room.isInvited();
                                if (includeInvite || !isPendingInvite) {
                                    // dinsic: if the member is not already in matrix and just invited he's not active but
                                    // the room can be considered as ok
                                    if (!MXSession.isUserId(aUserId)) {
                                        Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): for user: " + aUserId + " room id: " + roomId);
                                        return room;
                                    } else {
                                        RoomMember member = room.getMember(aUserId);

                                        if (null != member) {
                                            if (member.membership.equals(RoomMember.MEMBERSHIP_JOIN)) {
                                                if (!isPendingInvite) {
                                                    // the other user is present in this room (join-join)
                                                    Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): for user: " + aUserId + " (join) room id: " + roomId);
                                                    return room;
                                                } else {
                                                    // I am invited by the other member (invite-join)
                                                    // We consider first de case "invite-join" compare to "join-invite"
                                                    Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): set candidate (invite-join) room id: " + roomId);
                                                    roomCandidatePendingInvite = room;
                                                }
                                            } else if (member.membership.equals(RoomMember.MEMBERSHIP_INVITE)) {
                                                // the other user is invited (join-invite)
                                                if (roomCandidatePendingInvite == null) {
                                                    Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): set candidate (join-invite) room id: " + roomId);
                                                    roomCandidatePendingInvite = room;
                                                }
                                            } else if (member.membership.equals(RoomMember.MEMBERSHIP_LEAVE)) {
                                                // the other member has left this room
                                                // and I can be invite or join
                                                Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): set candidate (join-left) room id: " + roomId);
                                                roomCandidateLeftByOther = room;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // check if an invitation is pending
                        if (null != roomCandidatePendingInvite) {
                            Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): user: " + aUserId + " (invite) room id: " + roomCandidatePendingInvite.getRoomId());
                            return roomCandidatePendingInvite;
                        }

                        // by default we consider the room left by the other member
                        if (null != roomCandidateLeftByOther) {
                            Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): user: " + aUserId + " (leave) room id: " + roomCandidateLeftByOther.getRoomId());
                            return roomCandidateLeftByOther;
                        }
                    }
                }
            }
        }
        Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): for user=" + aUserId + " no found room");
        return null;
    }
}
