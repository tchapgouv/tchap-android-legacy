/*
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

package im.vector.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Toast;

import org.matrix.androidsdk.MXDataHandler;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomTag;
import org.matrix.androidsdk.data.store.IMXStore;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.RoomMember;
import org.matrix.androidsdk.rest.model.User;
import org.matrix.androidsdk.rest.model.search.SearchUsersResponse;
import org.matrix.androidsdk.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.LoginActivity;
import im.vector.activity.VectorRoomActivity;
import im.vector.activity.VectorRoomCreationActivity;
import im.vector.adapters.ParticipantAdapterItem;
import im.vector.adapters.ContactAdapter;
import im.vector.contacts.Contact;
import im.vector.contacts.ContactsManager;
import im.vector.contacts.PIDsRetriever;
import im.vector.util.DinsicUtils;
import im.vector.util.VectorUtils;
import im.vector.view.EmptyViewItemDecoration;
import im.vector.view.SimpleDividerItemDecoration;

public class ContactFragment extends AbsHomeFragment implements ContactsManager.ContactsManagerListener, AbsHomeFragment.OnRoomChangedListener {
    private static final String LOG_TAG = ContactFragment.class.getSimpleName();

    private static final String MATRIX_USER_ONLY_PREF_KEY = "MATRIX_USER_ONLY_PREF_KEY";

    private static final int MAX_KNOWN_CONTACTS_FILTER_COUNT = 50;

    @BindView(R.id.recyclerview)
    RecyclerView mRecycler;

    @BindView(R.id.listView_spinner_views)
    View mWaitingView;

    private CheckBox mMatrixUserOnlyCheckbox;

    private ContactAdapter mAdapter;

    private final List<Room> mDirectChats = new ArrayList<>();
    private final List<ParticipantAdapterItem> mLocalContacts = new ArrayList<>();
    // the known contacts are not sorted
    private final List<ParticipantAdapterItem> mKnownContacts = new ArrayList<>();

    // way to detect that the contacts list has been updated
    private int mContactsSnapshotSession = -1;
    private MXEventListener mEventsListener;

    /*
     * *********************************************************************************************
     * Static methods
     * *********************************************************************************************
     */

    public static ContactFragment newInstance() {
        return new ContactFragment();
    }

    /*
     * *********************************************************************************************
     * Fragment lifecycle
     * *********************************************************************************************
     */

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_people, container, false);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mEventsListener = new MXEventListener() {

            @Override
            public void onPresenceUpdate(final Event event, final User user) {
                mAdapter.updateKnownContact(user);
            }
        };

        mPrimaryColor = ContextCompat.getColor(getActivity(), R.color.tab_people);
        mSecondaryColor = ContextCompat.getColor(getActivity(), R.color.tab_people_secondary);

        initViews();

        mOnRoomChangedListener = this;

        // dont use Matrix filter
        if (mMatrixUserOnlyCheckbox!= null)
            mMatrixUserOnlyCheckbox.setChecked(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(MATRIX_USER_ONLY_PREF_KEY, false));

        mAdapter.onFilterDone(mCurrentFilter);

        if (!ContactsManager.getInstance().isContactBookAccessRequested()) {
            CommonActivityUtils.checkPermissions(CommonActivityUtils.REQUEST_CODE_PERMISSION_MEMBERS_SEARCH, this);
        }

        initKnownContacts();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSession.getDataHandler().addListener(mEventsListener);
        ContactsManager.getInstance().addListener(this);

        // Local address book
        initContactsData();

        // Direct chats
        initDirectChatsData();

        initContactsViews();
        initDirectChatsViews();//todo: to be removed

        mAdapter.setInvitation(mActivity.getRoomInvitations());

        mRecycler.addOnScrollListener(mScrollListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSession.isAlive()) {
            mSession.getDataHandler().removeListener(mEventsListener);
        }
        ContactsManager.getInstance().removeListener(this);

        mRecycler.removeOnScrollListener(mScrollListener);

        // cancel any search
        mSession.cancelUsersSearch();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CommonActivityUtils.REQUEST_CODE_PERMISSION_MEMBERS_SEARCH) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ContactsManager.getInstance().refreshLocalContactsSnapshot();
            } else {
                initContactsData();
            }

            // refresh the contact views
            // the placeholders might need to be updated
            initContactsViews();
        }
    }

    /*
     * *********************************************************************************************
     * Abstract methods implementation
     * *********************************************************************************************
     */

    @Override
    protected List<Room> getRooms() {
        return new ArrayList<>(mDirectChats);
    }

    @Override
    protected void onFilter(final String pattern, final OnFilterListener listener) {
        mAdapter.getFilter().filter(pattern, new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                boolean newSearch = TextUtils.isEmpty(mCurrentFilter) && !TextUtils.isEmpty(pattern);

                Log.i(LOG_TAG, "onFilterComplete " + count);
                if (listener != null) {
                    listener.onFilterDone(count);
                }

                startRemoteKnownContactsSearch(newSearch);
            }
        });
    }

    @Override
    protected void onResetFilter() {
        mAdapter.getFilter().filter("", new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                Log.i(LOG_TAG, "onResetFilter " + count);
            }
        });
    }

    /*
     * *********************************************************************************************
     * UI management
     * *********************************************************************************************
     */

    /**
     * Prepare views
     */
    private void initViews() {
        int margin = (int) getResources().getDimension(R.dimen.item_decoration_left_margin);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mRecycler.addItemDecoration(new SimpleDividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL, margin));
        mRecycler.addItemDecoration(new EmptyViewItemDecoration(getActivity(), DividerItemDecoration.VERTICAL, 40, 16, 14));
        mAdapter = new ContactAdapter(getActivity(), new ContactAdapter.OnSelectItemListener() {
            @Override
            public void onSelectItem(Room room, int position) {
                openRoom(room);
            }

            @Override
            public void onSelectItem(ParticipantAdapterItem contact, int position) {
                onContactSelected(contact);
            }
        }, this, this);
        mRecycler.setAdapter(mAdapter);

        // dont use Matrix filter
        /*
        View checkBox = mAdapter.findSectionSubViewById(R.id.matrix_only_filter_checkbox);
        if (checkBox != null && checkBox instanceof CheckBox) {
            mMatrixUserOnlyCheckbox = (CheckBox) checkBox;
            mMatrixUserOnlyCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(MATRIX_USER_ONLY_PREF_KEY, mMatrixUserOnlyCheckbox.isChecked());
                    editor.commit();

                    initContactsViews();
                }
            });
        }*/
    }

    /*
     * *********************************************************************************************
     * Data management
     * *********************************************************************************************
     */

    /**
     * Fill the direct chats adapter with data
     */
    private void initDirectChatsData() {
        if ((null == mSession) || (null == mSession.getDataHandler())) {
            Log.e(LOG_TAG, "## initDirectChatsData() : null session");
        }

        final MXDataHandler dataHandler = mSession.getDataHandler();
        final IMXStore store = dataHandler.getStore();
        final List<String> directChatIds = dataHandler.getDirectChatRoomIdsList();

        mDirectChats.clear();
        if (directChatIds != null && !directChatIds.isEmpty()) {
            for (String roomId : directChatIds) {
                Room room = store.getRoom(roomId);

                if ((null != room) && !room.isConferenceUserRoom()) {
                    // it seems that the server syncs some left rooms
                    if (null == room.getMember(mSession.getMyUserId())) {
                        Log.e(LOG_TAG, "## initDirectChatsData(): invalid room " + room.getRoomId() + ", the user is not anymore member of it");
                    } else {
                        final Set<String> tags = room.getAccountData().getKeys();
                        if ((null == tags) || !tags.contains(RoomTag.ROOM_TAG_LOW_PRIORITY)) {
                            mDirectChats.add(dataHandler.getRoom(roomId));
                        }
                    }
                }
            }
        }
    }
    /* get contacts from direct chats */
    private List<ParticipantAdapterItem> getContactsFromDirectChats() {
        List<ParticipantAdapterItem> participants = new ArrayList<>();

        if ((null == mSession) || (null == mSession.getDataHandler())) {
            Log.e(LOG_TAG, "## getContactsFromDirectChats() : null session");
        }

        final MXDataHandler dataHandler = mSession.getDataHandler();
        final IMXStore store = dataHandler.getStore();
        final List<String> directChatIds = dataHandler.getDirectChatRoomIdsList();

        if (directChatIds != null && !directChatIds.isEmpty()) {
            for (String roomId : directChatIds) {
                Room room = store.getRoom(roomId);

                if ((null != room) && !room.isConferenceUserRoom()) {
                    // it seems that the server syncs some left rooms
                    if (null == room.getMember(mSession.getMyUserId())) {
                        Log.e(LOG_TAG, "## getContactsFromDirectChats(): invalid room " + room.getRoomId() + ", the user is not anymore member of it");
                    } else {
                        final Set<String> tags = room.getAccountData().getKeys();
                        if ((null == tags) || !tags.contains(RoomTag.ROOM_TAG_LOW_PRIORITY)) {
                            Collection<RoomMember> rMembers = dataHandler.getRoom(roomId).getLiveState().getDisplayableMembers();
                            if (rMembers.size()==2) {
                                for (RoomMember myMember : rMembers){
                                    if (!myMember.getUserId().equals(mSession.getMyUserId())){
                                        if (MXSession.isUserId(myMember.getUserId())) {
                                            Contact dummyContact = new Contact("null");
                                            dummyContact.setDisplayName(myMember.displayname);
                                            ParticipantAdapterItem participant = new ParticipantAdapterItem(dummyContact);
                                            participant.mUserId = myMember.getUserId();
                                            participants.add(participant);
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return participants;
    }

    /**
     * Fill the local address book and known contacts adapters with data
     */
    private void initContactsData() {
        ContactsManager.getInstance().retrievePids();

        if (mContactsSnapshotSession == -1
                || mContactsSnapshotSession != ContactsManager.getInstance().getLocalContactsSnapshotSession()
                || !ContactsManager.getInstance().didPopulateLocalContacts()) {
            // First time on the screen or contact data outdated
            mLocalContacts.clear();
            List<ParticipantAdapterItem> participants = new ArrayList<>(getContacts());

            // Build lists
            for (ParticipantAdapterItem item : participants) {
                if (item.mContact != null) {
                    mLocalContacts.add(item);
                }
            }
        }
        //clear contacts that come from directchats, i.e without contact id
        else {
            List<ParticipantAdapterItem> tobeRemoved = new ArrayList<>();
            for (ParticipantAdapterItem item : mLocalContacts) {
                if (item.mContact.getContactId() == "null") {
                    tobeRemoved.add(item);
                }
            }
            for (ParticipantAdapterItem item : tobeRemoved){
                mLocalContacts.remove(item);
            }
        }
        //add participants from direct chats
        List<ParticipantAdapterItem> myDirectContacts = getContactsFromDirectChats();
        for (ParticipantAdapterItem myContact : myDirectContacts){
            if (!DinsicUtils.participantAlreadyAdded(mLocalContacts,myContact))
                mLocalContacts.add(myContact);
        }

    }

    /**
     * Get the known contacts list, sort it by presence and give it to adapter
     */
    private void initKnownContacts() {
        final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // do not sort anymore the full known participants list
                // as they are not displayed unfiltered
                // it saves a lot of times
                // eg with about 17000 items
                // sort requires about 2 seconds
                // sort a 1000 items subset during a search requires about 75ms
                mKnownContacts.clear();
                mKnownContacts.addAll(new ArrayList<>(VectorUtils.listKnownParticipants(mSession).values()));
                return null;
            }

            @Override
            protected void onPostExecute(Void args) {
                mAdapter.setKnownContacts(mKnownContacts);
            }
        };

        try {
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (final Exception e) {
            Log.e(LOG_TAG, "## initKnownContacts() failed " + e.getMessage());
            asyncTask.cancel(true);

            (new android.os.Handler(Looper.getMainLooper())).postDelayed(new Runnable() {
                @Override
                public void run() {
                    initKnownContacts();
                }
            }, 1000);
        }
    }

    /**
     * Display the public rooms loading view
     */
    private void showKnownContactLoadingView() {
        mAdapter.getSectionViewForSectionIndex(mAdapter.getSectionsCount() - 1).showLoadingView();
    }

    /**
     * Hide the public rooms loading view
     */
    private void hideKnownContactLoadingView() {
        mAdapter.getSectionViewForSectionIndex(mAdapter.getSectionsCount() - 1).hideLoadingView();
    }

    /**
     * Trigger a request to search known contacts.
     *
     * @param isNewSearch true if the search is a new one
     */
    private void startRemoteKnownContactsSearch(boolean isNewSearch) {
        if (!TextUtils.isEmpty(mCurrentFilter)) {

            // display the known contacts section
            if (isNewSearch) {
                mAdapter.setFilteredKnownContacts(new ArrayList<ParticipantAdapterItem>(), mCurrentFilter);
                showKnownContactLoadingView();
            }

            final String fPattern = mCurrentFilter;

            mSession.searchUsers(mCurrentFilter, MAX_KNOWN_CONTACTS_FILTER_COUNT, new HashSet<String>(), new ApiCallback<SearchUsersResponse>() {
                @Override
                public void onSuccess(SearchUsersResponse searchUsersResponse) {
                    if (TextUtils.equals(fPattern, mCurrentFilter)) {
                        hideKnownContactLoadingView();

                        List<ParticipantAdapterItem> list = new ArrayList<>();

                        if (null != searchUsersResponse.results) {
                            for (User user : searchUsersResponse.results) {
                                list.add(new ParticipantAdapterItem(user));
                            }
                        }

                        mAdapter.setKnownContactsExtraTitle(null);
                        mAdapter.setKnownContactsLimited((null != searchUsersResponse.limited) ? searchUsersResponse.limited : false);
                        mAdapter.setFilteredKnownContacts(list, mCurrentFilter);
                    }
                }

                private void onError(String errorMessage) {
                    Log.e(LOG_TAG, "## startRemoteKnownContactsSearch() : failed " + errorMessage);
                    //
                    if (TextUtils.equals(fPattern, mCurrentFilter)) {
                        hideKnownContactLoadingView();
                        mAdapter.setKnownContactsExtraTitle(ContactFragment.this.getContext().getString(R.string.offline));
                        mAdapter.filterAccountKnownContacts(mCurrentFilter);
                    }
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
            });
        }
    }

    /*
     * *********************************************************************************************
     * User action management
     * *********************************************************************************************
     */

    /**
     * Handle the click on a local or known contact
     *
     * @param item
     */
    private void onContactSelected(final ParticipantAdapterItem item) {
        if (item.mIsValid) {

            // tell if contact is tchap user
            if (MXSession.isUserId(item.mUserId))// || DinsicUtils.isFromFrenchGov(item.mContact.getEmails()))
                openDirectChat(item, true);
            else {
                //don't have to ask the question if a room already exists
                Room existingRoom;
                String msg = getString(R.string.room_invite_non_gov_people);
                if (DinsicUtils.isFromFrenchGov(item.mContact.getEmails()))
                    msg = getString(R.string.room_invite_gov_people);
                if (null != (existingRoom = VectorRoomCreationActivity.isDirectChatRoomAlreadyExist(item.mUserId, mSession, false))) {
                    openDirectChat(item, false);
                } else if (LoginActivity.isUserExternal(mSession)) {
                    DinsicUtils.alertSimpleMsg(getActivity(), getString(R.string.room_creation_forbidden));
                } else {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    alertDialogBuilder.setMessage(msg);

                    // set dialog message
                    alertDialogBuilder
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            openDirectChat(item,true);
                                        }
                                    })
                            .setNegativeButton(R.string.cancel, null);

                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    // show it
                    alertDialog.show();
                }
            }

        } else {// tell the user that the email must be filled. Propose to fill it
            DinsicUtils.editContact(mActivity, this.getContext(), item);
        }
    }

    /**
     * Open the current direct chat with the corresponding user id.
     *
     * @param item : participant id
     * @param canCreate create the direct chat if it does not exist.
     * @return the corresponding room id or null
     */
    private boolean openDirectChat (final ParticipantAdapterItem item, boolean canCreate) {
        Room existingRoom = VectorRoomCreationActivity.isDirectChatRoomAlreadyExist(item.mUserId, mSession, true);
        boolean directChatOpened = false;

        if (null != existingRoom) {
            if (existingRoom.isInvited()) {
                directChatOpened = true;
                mActivity.showWaitingView();

                mSession.joinRoom(existingRoom.getRoomId(), new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String roomId) {
                        mActivity.stopWaitingView();

                        HashMap<String, Object> params = new HashMap<>();
                        params.put(VectorRoomActivity.EXTRA_MATRIX_ID, mSession.getMyUserId());
                        params.put(VectorRoomActivity.EXTRA_ROOM_ID, roomId);
                        CommonActivityUtils.goToRoomPage(mActivity, mSession, params);
                    }

                    private void onError(final String message) {
                        mWaitingView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (null != message) {
                                    Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
                                }
                                mActivity.stopWaitingView();
                            }
                        });
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        onError(e.getLocalizedMessage());
                    }

                    @Override
                    public void onMatrixError(final MatrixError e) {
                        onError(e.getLocalizedMessage());
                    }

                    @Override
                    public void onUnexpectedError(final Exception e) {
                        onError(e.getLocalizedMessage());
                    }
                });
            } else {
                directChatOpened = true;
                HashMap<String, Object> params = new HashMap<>();
                params.put(VectorRoomActivity.EXTRA_MATRIX_ID, item.mUserId);
                params.put(VectorRoomActivity.EXTRA_ROOM_ID, existingRoom.getRoomId());
                CommonActivityUtils.goToRoomPage(mActivity, mSession, params);

            }
        } else if (canCreate){
            // direct message flow
            //it will be more open on next sprints ...
            if (!LoginActivity.isUserExternal(mSession)) {
                directChatOpened = true;
                mActivity.showWaitingView();
                mSession.createDirectMessageRoom(item.mUserId, mCreateDirectMessageCallBack);
            } else {
                DinsicUtils.alertSimpleMsg(this.getActivity(), getString(R.string.room_creation_forbidden));
            }
        }
        return directChatOpened;
    }

    // direct message
    private final ApiCallback<String> mCreateDirectMessageCallBack = new ApiCallback<String>() {
        @Override
        public void onSuccess(final String roomId) {
            mActivity.stopWaitingView();

            HashMap<String, Object> params = new HashMap<>();
            params.put(VectorRoomActivity.EXTRA_MATRIX_ID, mSession.getMyUserId());
            params.put(VectorRoomActivity.EXTRA_ROOM_ID, roomId);
            params.put(VectorRoomActivity.EXTRA_EXPAND_ROOM_HEADER, true);

            Log.d(LOG_TAG, "## mCreateDirectMessageCallBack: onSuccess - start goToRoomPage");
            CommonActivityUtils.goToRoomPage(getActivity(), mSession, params);
        }

        private void onError(final String message) {
            mWaitingView.post(new Runnable() {
                @Override
                public void run() {
                    if (null != message) {
                        Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
                    }
                    mActivity.stopWaitingView();
                }
            });
        }

        @Override
        public void onNetworkError(Exception e) {
            onError(e.getLocalizedMessage());
        }

        @Override
        public void onMatrixError(final MatrixError e) {
            onError(e.getLocalizedMessage());
        }

        @Override
        public void onUnexpectedError(final Exception e) {
            onError(e.getLocalizedMessage());
        }
    };

    /*
     * *********************************************************************************************
     * Utils
     * *********************************************************************************************
     */

    /**
     * Retrieve the contacts
     *
     * @return
     */
    private List<ParticipantAdapterItem> getContacts() {
        List<ParticipantAdapterItem> participants = new ArrayList<>();

        Collection<Contact> contacts = ContactsManager.getInstance().getLocalContactsSnapshot();
        mContactsSnapshotSession = ContactsManager.getInstance().getLocalContactsSnapshotSession();

        if (null != contacts) {
            for (Contact contact : contacts) {
                // injecter les contacts sans emails
                //------------------------------------
                if (contact.getEmails().size()==0){
                    Contact dummyContact = new Contact(contact.getContactId());
                    dummyContact.setDisplayName(contact.getDisplayName());
                    dummyContact.addEmailAdress(getString(R.string.no_email));
                    dummyContact.setThumbnailUri(contact.getThumbnailUri());
                    //dummyContact.

                    ParticipantAdapterItem participant = new ParticipantAdapterItem(dummyContact);

                    participant.mUserId = "null";
                    participant.mIsValid = false;
                    participants.add(participant);


                }
                else {
                    //select just one email, in priority the french gov email
                    boolean findGovEmail = false;
                    ParticipantAdapterItem candidatParticipant=null;
                    for (String email : contact.getEmails()) {
                        if (!TextUtils.isEmpty(email) && !ParticipantAdapterItem.isBlackedListed(email)) {
                            Contact dummyContact = new Contact(email);
                            dummyContact.setDisplayName(contact.getDisplayName());
                            dummyContact.addEmailAdress(email);
                            dummyContact.setThumbnailUri(contact.getThumbnailUri());

                            ParticipantAdapterItem participant = new ParticipantAdapterItem(dummyContact);

                            Contact.MXID mxid = PIDsRetriever.getInstance().getMXID(email);

                            if (null != mxid) {
                                participant.mUserId = mxid.mMatrixId;
                            } else {
                                participant.mUserId = email;
                            }
                            if (DinsicUtils.isFromFrenchGov(email)) {
                                findGovEmail = true;
                                participants.add(participant);
                            }
                            else if (!findGovEmail && candidatParticipant==null)
                                candidatParticipant = participant;
                        }
                    }
                    if (!findGovEmail && candidatParticipant!=null)
                        participants.add(candidatParticipant);
                }

            }
        }
        //move this code outside
        /*
        //add participants from direct chats
        List<ParticipantAdapterItem> myDirectContacts = getContactsFromDirectChats();
        for (ParticipantAdapterItem myContact : myDirectContacts){
            if (!DinsicUtils.participantAlreadyAdded(participants,myContact))
                participants.add(myContact);
        }
        */
        return participants;
    }


    private List<ParticipantAdapterItem> getMatrixUsers() {
        List<ParticipantAdapterItem> matrixUsers = new ArrayList<>();
        for (ParticipantAdapterItem item : mLocalContacts) {
            if (!item.mContact.getMatrixIdMediums().isEmpty()) {
                matrixUsers.add(item);
            }
        }
        return matrixUsers;
    }

    /**
     * Init direct chats view with data and update its display
     */
    private void initDirectChatsViews() {
        mAdapter.setRooms(mDirectChats);
    }

    /**
     * Init contacts views with data and update their display
     */
    private void initContactsViews() {
        mAdapter.setLocalContacts(mMatrixUserOnlyCheckbox != null && mMatrixUserOnlyCheckbox.isChecked()
                ? getMatrixUsers()
                : mLocalContacts);
    }

    /*
     * *********************************************************************************************
     * Listeners
     * *********************************************************************************************
     */

    @Override
    public void onSummariesUpdate() {
        super.onSummariesUpdate();

        if (isResumed()) {
            mAdapter.setInvitation(mActivity.getRoomInvitations());
            initContactsData();
        }
    }

    @Override
    public void onRefresh() {
        initContactsData();
        initContactsViews();
    }

    @Override
    public void onPIDsUpdate() {
        final List<ParticipantAdapterItem> newContactList = getContacts();
        //add participants from direct chats
        List<ParticipantAdapterItem> myDirectContacts = getContactsFromDirectChats();
        for (ParticipantAdapterItem myContact : myDirectContacts){
            if (!DinsicUtils.participantAlreadyAdded(newContactList,myContact))
                newContactList.add(myContact);
        }

        if (!mLocalContacts.containsAll(newContactList)) {
            mLocalContacts.clear();
            mLocalContacts.addAll(newContactList);
            initContactsViews();
        }
    }

    @Override
    public void onContactPresenceUpdate(Contact contact, String matrixId) {
        //TODO
    }

    @Override
    public void onToggleDirectChat(String roomId, boolean isDirectChat) {
        if (!isDirectChat) {
            mAdapter.removeDirectChat(roomId);
        }
    }

    @Override
    public void onRoomLeft(String roomId) {
        mAdapter.removeDirectChat(roomId);
    }

    @Override
    public void onRoomForgot(String roomId) {
        mAdapter.removeDirectChat(roomId);
    }
}
